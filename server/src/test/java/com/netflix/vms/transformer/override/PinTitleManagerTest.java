package com.netflix.vms.transformer.override;

import com.netflix.aws.file.FileStore;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import com.netflix.vms.transformer.SimpleTransformerContext;
import com.netflix.vms.transformer.input.VMSInputDataClient;
import com.netflix.vms.transformer.override.PinTitleManager.PinTitleProcessorJob;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PinTitleManagerTest {
    private static final String BASE_PROXY = VMSInputDataClient.TEST_PROXY_URL;
    private static final String LOCAL_BLOB_STORE = "/space/title-pinning";

    private SimpleTransformerContext ctx;
    private PinTitleManager mgr;

    @Before
    public void setup() {
        ctx = new SimpleTransformerContext();
        mgr = createNewMgr();
    }

    private PinTitleManager createNewMgr() {
        return Mockito.spy(new PinTitleManager(BASE_PROXY, "boson", "berlin", LOCAL_BLOB_STORE, ctx));
    }

    @Test
    public void testProcessSpecs() throws Exception {
        String spec = "20160829111536238:60029157";
        PinTitleJobSpec expectedJobSpec = mgr.createJobSpec(spec);

        Map<PinTitleJobSpec, PinTitleProcessorJob> processSpecs = mgr.processSpecs(new HashSet<>(Arrays.asList(spec)));
        Assert.assertEquals(1, processSpecs.size());
        Assert.assertTrue(processSpecs.containsKey(expectedJobSpec));
    }

    @Test
    public void testProcessSpecWithMerge() throws Exception {
        String spec1 = "20160829111536238:60029157";
        String spec2 = "20160829111536238:60029158";
        PinTitleJobSpec jSpec1 = mgr.createJobSpec(spec1);
        PinTitleJobSpec jSpec2 = mgr.createJobSpec(spec2);

        PinTitleJobSpec expectedJobSpec = jSpec1.merge(jSpec2);
        Assert.assertEquals(1, jSpec1.topNodes.length);
        Assert.assertEquals(1, jSpec2.topNodes.length);
        Assert.assertEquals(2, expectedJobSpec.topNodes.length);

        Map<PinTitleJobSpec, PinTitleProcessorJob> processSpecs = mgr.processSpecs(new HashSet<>(Arrays.asList(spec1, spec2)));
        Assert.assertEquals(1, processSpecs.size());
        Assert.assertTrue(processSpecs.containsKey(expectedJobSpec));
    }

    @Test
    public void testProcessSpecWithMergeWithDupes() throws Exception {
        String spec1 = "20160829111536238:60029157,60029100";
        String spec2 = "20160829111536238:60029158, 60029157";
        PinTitleJobSpec jSpec1 = mgr.createJobSpec(spec1);
        PinTitleJobSpec jSpec2 = mgr.createJobSpec(spec2);

        PinTitleJobSpec expectedJobSpec = jSpec1.merge(jSpec2);
        Assert.assertEquals(2, jSpec1.topNodes.length);
        Assert.assertEquals(2, jSpec2.topNodes.length);
        Assert.assertEquals(3, expectedJobSpec.topNodes.length);

        Map<PinTitleJobSpec, PinTitleProcessorJob> processSpecs = mgr.processSpecs(new HashSet<>(Arrays.asList(spec1, spec2)));
        Assert.assertEquals(1, processSpecs.size());
        Assert.assertTrue(processSpecs.containsKey(expectedJobSpec));
    }

    @Test
    public void testProcessSpecMultiCycle() throws Exception {
        String spec1 = "20160829111536238:60029157, 60029100";
        String spec2 = "20160829111536238:60029158,60029157";
        PinTitleJobSpec jSpec1 = mgr.createJobSpec(spec1);
        PinTitleJobSpec jSpec2 = mgr.createJobSpec(spec2);

        PinTitleJobSpec expectedMergedJobSpec = jSpec1.merge(jSpec2);
        Assert.assertEquals(2, jSpec1.topNodes.length);
        Assert.assertEquals(2, jSpec2.topNodes.length);
        Assert.assertEquals(3, expectedMergedJobSpec.topNodes.length);
        Mockito.when(mgr.createOutputBasedProcessor()).thenReturn(new DummyProcessor());

        { // cycle 1
            mgr.submitJobsToProcessASync(new HashSet<>(Arrays.asList(spec1)));
            Map<PinTitleJobSpec, PinTitleProcessorJob> activeJobs = mgr.getActiveJobs();
            Assert.assertEquals(1, activeJobs.size());
            Assert.assertTrue(activeJobs.containsKey(jSpec1));

            List<HollowReadStateEngine> results = mgr.getResults(true);
            Map<PinTitleJobSpec, PinTitleProcessorJob> completedjobs = mgr.getCompletedJobs();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(1, completedjobs.size());
            Assert.assertTrue(completedjobs.containsKey(jSpec1));
        }

        { // cycle 2 - not merge since duplicate are from from different cycles
            mgr.prepareForNextCycle();
            mgr.submitJobsToProcessASync(new HashSet<>(Arrays.asList(spec1, spec2)));
            Map<PinTitleJobSpec, PinTitleProcessorJob> activeJobs = mgr.getActiveJobs();
            Assert.assertEquals(2, activeJobs.size());
            Assert.assertTrue(activeJobs.containsKey(jSpec1));
            Assert.assertTrue(activeJobs.containsKey(jSpec2));

            List<HollowReadStateEngine> results = mgr.getResults(true);
            Map<PinTitleJobSpec, PinTitleProcessorJob> completedjobs = mgr.getCompletedJobs();
            Assert.assertEquals(2, results.size());
            Assert.assertEquals(2, completedjobs.size());
            Assert.assertTrue(completedjobs.containsKey(jSpec1));
            Assert.assertTrue(completedjobs.containsKey(jSpec2));
            Assert.assertFalse(completedjobs.containsKey(expectedMergedJobSpec));
        }

        mgr.reset(); // Simulate startup - with merge multiple spec of same blob version
        {
            mgr.prepareForNextCycle();
            mgr.submitJobsToProcessASync(new HashSet<>(Arrays.asList(spec1, spec2)));
            Map<PinTitleJobSpec, PinTitleProcessorJob> activeJobs = mgr.getActiveJobs();
            Assert.assertEquals(1, activeJobs.size());
            Assert.assertTrue(activeJobs.containsKey(expectedMergedJobSpec));

            List<HollowReadStateEngine> results = mgr.getResults(true);
            Map<PinTitleJobSpec, PinTitleProcessorJob> completedjobs = mgr.getCompletedJobs();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(1, completedjobs.size());
            Assert.assertTrue(completedjobs.containsKey(expectedMergedJobSpec));
        }

    }

    @Test
    public void testProcessFailureAndRecovers() throws Exception {
        String spec1 = "123:456";
        PinTitleJobSpec jSpec1 = mgr.createJobSpec(spec1);

        // Processor that will fail
        DummyProcessor processor = new DummyProcessor();
        processor.isThrowException = true;
        Mockito.when(mgr.createOutputBasedProcessor()).thenReturn(processor);

        { // cycle 1 - fail
            try {
                mgr.submitJobsToProcessASync(new HashSet<>(Arrays.asList(spec1)));
                Map<PinTitleJobSpec, PinTitleProcessorJob> activeJobs = mgr.getActiveJobs();
                Assert.assertEquals(1, activeJobs.size());
                Assert.assertTrue(activeJobs.containsKey(jSpec1));

                mgr.getResults(true); // Fail on first cycle
                Assert.fail("should not get here");
            } catch (Exception ex) {
                Map<PinTitleJobSpec, PinTitleProcessorJob> failedJob = mgr.getFailedJobs();
                Assert.assertEquals(1, failedJob.size());
                Assert.assertTrue(failedJob.containsKey(jSpec1));
            }
        }

        { // cycle 2 - continues to fail even when not waiting for all results
            try {
                mgr.prepareForNextCycle();
                mgr.submitJobsToProcessASync(new HashSet<>(Arrays.asList(spec1)));
                Map<PinTitleJobSpec, PinTitleProcessorJob> activeJobs = mgr.getActiveJobs();
                Assert.assertEquals(1, activeJobs.size());
                Assert.assertTrue(activeJobs.containsKey(jSpec1));

                mgr.getResults(false); // Don't wait for sub-sequent cycles
                Assert.fail("should not get here");
            } catch (Exception ex) {
                Map<PinTitleJobSpec, PinTitleProcessorJob> failedJob = mgr.getFailedJobs();
                Assert.assertEquals(1, failedJob.size());
                Assert.assertTrue(failedJob.containsKey(jSpec1));
            }
        }

        // Successful
        processor.isThrowException = false;
        { // cycle 3 - recovers
            try {
                mgr.prepareForNextCycle();
                mgr.submitJobsToProcessASync(new HashSet<>(Arrays.asList(spec1)));
                Map<PinTitleJobSpec, PinTitleProcessorJob> activeJobs = mgr.getActiveJobs();
                Assert.assertEquals(1, activeJobs.size());
                Assert.assertTrue(activeJobs.containsKey(jSpec1));

                mgr.getResults(true);
                Map<PinTitleJobSpec, PinTitleProcessorJob> completedJobs = mgr.getCompletedJobs();
                Assert.assertEquals(1, completedJobs.size());
                Assert.assertTrue(completedJobs.containsKey(jSpec1));
            } catch (Exception ex) {
                Assert.fail("should not get here");
            }
        }
    }

    public static class DummyProcessor implements PinTitleProcessor {
        boolean isThrowException;

        DummyProcessor() {
            this.isThrowException = false;
        }

        DummyProcessor(boolean isThrowException) {
            this.isThrowException = isThrowException;
        }

        @Override
        public HollowReadStateEngine process(long dataVersion, int... topNodes) throws Throwable {
            if (isThrowException) {
                throw new Exception("Failed to proceess");
            }

            return null;
        }

        @Override
        public String getVip() {
            return null;
        }

        @Override
        public File getFile(TYPE type, long version, int... topNodes) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public File process(TYPE type, long dataVersion, int... topNodes) throws Throwable {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setPinTitleFileStore(FileStore pinTitleFileStore) {
            // TODO Auto-generated method stub

        }
    }
}