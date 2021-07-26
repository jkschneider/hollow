/*
 *  Copyright 2016-2019 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.hollow.api.consumer;

import com.netflix.hollow.api.consumer.HollowConsumer.AbstractRefreshListener;
import com.netflix.hollow.api.consumer.HollowConsumer.Blob;
import com.netflix.hollow.api.consumer.HollowConsumer.ObjectLongevityConfig;
import com.netflix.hollow.api.custom.HollowAPI;
import com.netflix.hollow.api.objects.generic.GenericHollowObject;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.Populator;
import com.netflix.hollow.api.producer.HollowProducer.WriteState;
import com.netflix.hollow.api.producer.fs.HollowInMemoryBlobStager;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HollowRefreshListenerTests {

    private InMemoryBlobStore blobStore;
    private RecordingRefreshListener listener;
    
    private HollowProducer producer;
    private HollowConsumer consumer;

    @BeforeEach
    public void setUp() {
        blobStore = new InMemoryBlobStore();
        listener = new RecordingRefreshListener();
        producer = HollowProducer.withPublisher(blobStore)
                                 .withBlobStager(new HollowInMemoryBlobStager())
                                 .withNumStatesBetweenSnapshots(Integer.MAX_VALUE)
                                 .build();
        
        consumer = HollowConsumer.withBlobRetriever(blobStore)
                                 .withRefreshListener(listener)
                                 .withObjectLongevityConfig(new ObjectLongevityConfig() {
                                        @Override public long usageDetectionPeriodMillis() { return 100L; }
                                        @Override public long gracePeriodMillis() { return 100L; }
                                        @Override public boolean forceDropData() { return false; }
                                        @Override public boolean enableLongLivedObjectSupport() { return true; }
                                        @Override public boolean enableExpiredUsageStackTraces() { return false; }
                                        @Override public boolean dropDataAutomatically() { return true; }
                                 })
                                 .build();
    }

    @Test
    public void testRemoveDuplicateRefreshListeners() {
        HollowConsumer consumer = HollowConsumer.withBlobRetriever(blobStore)
                .withRefreshListener(listener)
                .withRefreshListener(listener)
                .build();

        long v1 = runCycle(producer, 1);
        consumer.triggerRefreshTo(v1+1);

        Assertions.assertEquals(1, listener.cycles);

        listener.clear();

        long v2 = runCycle(producer, 2);

        consumer.addRefreshListener(listener);
        consumer.triggerRefreshTo(v2+1);

        Assertions.assertEquals(1, listener.cycles);
    }

    @Test
    public void testCopyRefreshListeners() {
        List<HollowConsumer.RefreshListener> listeners = new ArrayList<>();
        listeners.add(listener);

        HollowConsumer.Builder<?> b = new HollowConsumer.Builder() {
            @Override
            public HollowConsumer build() {
                return new HollowConsumer(blobRetriever,
                        announcementWatcher,
                        listeners,
                        apiFactory,
                        filterConfig,
                        objectLongevityConfig,
                        objectLongevityDetector,
                        doubleSnapshotConfig,
                        hashCodeFinder,
                        refreshExecutor,
                        memoryMode,
                        metricsCollector);
            }
        };
        HollowConsumer consumer = b.withBlobRetriever(blobStore).build();

        long v1 = runCycle(producer, 1);
        listeners.clear();
        consumer.triggerRefreshTo(v1+1);

        Assertions.assertEquals(1, listener.cycles);
    }

    @Test
    public void testMethodSemanticsOnInitialRefresh() {
        long v1 = runCycle(producer, 1);
        long v2 = runCycle(producer, 2);
        long v3 = runCycle(producer, 3);
        long v4 = runCycle(producer, 4);
        long v5 = runCycle(producer, 5);
        
        consumer.triggerRefreshTo(v5+1);
        
        /// update occurred semantics
        Assertions.assertEquals(1, listener.snapshotUpdateOccurredVersions.size());
        Assertions.assertEquals(v5, listener.snapshotUpdateOccurredVersions.get(0).longValue());
        
        Assertions.assertTrue(listener.deltaUpdateOccurredVersions.isEmpty());
        
        /// applied semantics
        Assertions.assertEquals(1, listener.snapshotAppliedVersions.size());
        Assertions.assertEquals(v1, listener.snapshotAppliedVersions.get(0).longValue());
        
        Assertions.assertEquals(4, listener.deltaAppliedVersions.size());
        Assertions.assertEquals(v2, listener.deltaAppliedVersions.get(0).longValue());
        Assertions.assertEquals(v3, listener.deltaAppliedVersions.get(1).longValue());
        Assertions.assertEquals(v4, listener.deltaAppliedVersions.get(2).longValue());
        Assertions.assertEquals(v5, listener.deltaAppliedVersions.get(3).longValue());
        
        /// blobs loaded semantics
        Assertions.assertEquals(5, listener.blobsLoadedVersions.size());
        Assertions.assertEquals(v1, listener.blobsLoadedVersions.get(0).longValue());
        Assertions.assertEquals(v2, listener.blobsLoadedVersions.get(1).longValue());
        Assertions.assertEquals(v3, listener.blobsLoadedVersions.get(2).longValue());
        Assertions.assertEquals(v4, listener.blobsLoadedVersions.get(3).longValue());
        Assertions.assertEquals(v5, listener.blobsLoadedVersions.get(4).longValue());
        
        Assertions.assertEquals(Long.MIN_VALUE, listener.refreshStartCurrentVersion);
        Assertions.assertEquals(v5+1, listener.refreshStartRequestedVersion);
        
        Assertions.assertEquals(Long.MIN_VALUE, listener.refreshSuccessBeforeVersion);
        Assertions.assertEquals(v5, listener.refreshSuccessAfterVersion);
        Assertions.assertEquals(v5+1, listener.refreshSuccessRequestedVersion);
    }
    
    @Test
    public void testMethodSemanticsOnSubsequentRefreshes() {
        long v0 = runCycle(producer, 0);
        consumer.triggerRefreshTo(v0);
        listener.clear();
        long v1 = runCycle(producer, 1);
        long v2 = runCycle(producer, 2);
        long v3 = runCycle(producer, 3);
        consumer.triggerRefreshTo(v3);

        /// update occurred semantics
        Assertions.assertEquals(0, listener.snapshotUpdateOccurredVersions.size());

        Assertions.assertEquals(3, listener.deltaUpdateOccurredVersions.size());
        Assertions.assertEquals(v1, listener.deltaUpdateOccurredVersions.get(0).longValue());
        Assertions.assertEquals(v2, listener.deltaUpdateOccurredVersions.get(1).longValue());
        Assertions.assertEquals(v3, listener.deltaUpdateOccurredVersions.get(2).longValue());
        
        /// applied semantics
        Assertions.assertEquals(0, listener.snapshotAppliedVersions.size());

        Assertions.assertEquals(3, listener.deltaAppliedVersions.size());
        Assertions.assertEquals(v1, listener.deltaAppliedVersions.get(0).longValue());
        Assertions.assertEquals(v2, listener.deltaAppliedVersions.get(1).longValue());
        Assertions.assertEquals(v3, listener.deltaAppliedVersions.get(2).longValue());

        /// blobs loaded semantics
        Assertions.assertEquals(3, listener.blobsLoadedVersions.size());
        Assertions.assertEquals(v1, listener.blobsLoadedVersions.get(0).longValue());
        Assertions.assertEquals(v2, listener.blobsLoadedVersions.get(1).longValue());
        Assertions.assertEquals(v3, listener.blobsLoadedVersions.get(2).longValue());

        Assertions.assertEquals(v0, listener.refreshStartCurrentVersion);
        Assertions.assertEquals(v3, listener.refreshStartRequestedVersion);

        Assertions.assertEquals(v0, listener.refreshSuccessBeforeVersion);
        Assertions.assertEquals(v3, listener.refreshSuccessAfterVersion);
        Assertions.assertEquals(v3, listener.refreshSuccessRequestedVersion);
    }
    
    @Test
    public void testObjectLongevityOnInitialUpdateCallbacks() {
        runCycle(producer, 1);
        runCycle(producer, 2);
        runCycle(producer, 3);
        runCycle(producer, 4);
        long v5 = runCycle(producer, 5);
        
        final List<GenericHollowObject> snapshotOrdinal0Objects = new ArrayList<GenericHollowObject>();
        final List<GenericHollowObject> deltaOrdinal0Objects = new ArrayList<GenericHollowObject>();
        final List<GenericHollowObject> deltaOrdinal1Objects = new ArrayList<GenericHollowObject>();

        HollowConsumer.RefreshListener longevityListener = new AbstractRefreshListener() {
            public void snapshotApplied(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
                snapshotOrdinal0Objects.add(new GenericHollowObject(api.getDataAccess(), "Integer", 0));
            }
            
            public void deltaApplied(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
                deltaOrdinal0Objects.add(new GenericHollowObject(api.getDataAccess(), "Integer", 0));
                deltaOrdinal1Objects.add(new GenericHollowObject(api.getDataAccess(), "Integer", 1));
            }
        };
        
        consumer.addRefreshListener(longevityListener);
        
        consumer.triggerRefreshTo(v5);

        Assertions.assertEquals(1, snapshotOrdinal0Objects.get(0).getInt("value"));
        Assertions.assertEquals(2, deltaOrdinal1Objects.get(0).getInt("value"));
        Assertions.assertEquals(3, deltaOrdinal0Objects.get(1).getInt("value"));
        Assertions.assertEquals(4, deltaOrdinal1Objects.get(2).getInt("value"));
        Assertions.assertEquals(5, deltaOrdinal0Objects.get(3).getInt("value"));
    }

    @Test
    public void testAddListenerDuringRefresh() {
        HollowConsumer consumer = HollowConsumer.withBlobRetriever(blobStore)
                .build();

        class SecondRefreshListener extends AbstractRefreshListener {
            int refreshStarted;
            int refreshSuccessful;
            @Override public void refreshStarted(long currentVersion, long requestedVersion) {
                refreshStarted++;
            }

            @Override public void refreshSuccessful(long beforeVersion, long afterVersion, long requestedVersion) {
                refreshSuccessful++;
            }
        };

        class FirstRefreshListener extends SecondRefreshListener {
            SecondRefreshListener srl = new SecondRefreshListener();

            @Override public void refreshStarted(long currentVersion, long requestedVersion) {
                super.refreshStarted(currentVersion, requestedVersion);
                // Add the second listener concurrently during a refresh
                consumer.addRefreshListener(srl);
            }
        };

        FirstRefreshListener frl = new FirstRefreshListener();
        consumer.addRefreshListener(frl);

        long v1 = runCycle(producer, 1);
        consumer.triggerRefreshTo(v1+1);

        Assertions.assertEquals(1, frl.refreshStarted);
        Assertions.assertEquals(1, frl.refreshSuccessful);
        Assertions.assertEquals(0, frl.srl.refreshStarted);
        Assertions.assertEquals(0, frl.srl.refreshSuccessful);

        long v2 = runCycle(producer, 2);
        consumer.triggerRefreshTo(v2+1);

        Assertions.assertEquals(2, frl.refreshStarted);
        Assertions.assertEquals(2, frl.refreshSuccessful);
        Assertions.assertEquals(1, frl.srl.refreshStarted);
        Assertions.assertEquals(1, frl.srl.refreshSuccessful);
    }

    @Test
    public void testRemoveListenerDuringRefresh() {
        HollowConsumer consumer = HollowConsumer.withBlobRetriever(blobStore)
                .build();

        class SecondRefreshListener extends AbstractRefreshListener {
            int refreshStarted;
            int refreshSuccessful;
            @Override public void refreshStarted(long currentVersion, long requestedVersion) {
                refreshStarted++;
            }

            @Override public void refreshSuccessful(long beforeVersion, long afterVersion, long requestedVersion) {
                refreshSuccessful++;
            }
        };

        class FirstRefreshListener extends SecondRefreshListener {
            SecondRefreshListener srl;

            FirstRefreshListener(SecondRefreshListener srl) {
                this.srl = srl;
            }

            @Override public void refreshStarted(long currentVersion, long requestedVersion) {
                super.refreshStarted(currentVersion, requestedVersion);
                // Remove the second listener concurrently during a refresh
                consumer.removeRefreshListener(srl);
            }
        };

        SecondRefreshListener srl = new SecondRefreshListener();
        FirstRefreshListener frl = new FirstRefreshListener(srl);
        consumer.addRefreshListener(frl);
        consumer.addRefreshListener(srl);

        long v1 = runCycle(producer, 1);
        consumer.triggerRefreshTo(v1+1);

        Assertions.assertEquals(1, frl.refreshStarted);
        Assertions.assertEquals(1, frl.refreshSuccessful);
        Assertions.assertEquals(1, frl.srl.refreshStarted);
        Assertions.assertEquals(1, frl.srl.refreshSuccessful);

        long v2 = runCycle(producer, 2);
        consumer.triggerRefreshTo(v2+1);

        Assertions.assertEquals(2, frl.refreshStarted);
        Assertions.assertEquals(2, frl.refreshSuccessful);
        Assertions.assertEquals(1, frl.srl.refreshStarted);
        Assertions.assertEquals(1, frl.srl.refreshSuccessful);
    }

    private long runCycle(HollowProducer producer, final int cycleNumber) {
        return producer.runCycle(new Populator() {
            public void populate(WriteState state) throws Exception {
                state.add(Integer.valueOf(cycleNumber));
            }
        });
    }
    
    private class RecordingRefreshListener extends AbstractRefreshListener {
        long cycles;

        long refreshStartCurrentVersion;
        long refreshStartRequestedVersion;
        
        long refreshSuccessBeforeVersion;
        long refreshSuccessAfterVersion;
        long refreshSuccessRequestedVersion; 
        
        List<Long> snapshotUpdateOccurredVersions = new ArrayList<Long>();
        List<Long> deltaUpdateOccurredVersions = new ArrayList<Long>();

        List<Long> blobsLoadedVersions = new ArrayList<Long>();
        
        List<Long> snapshotAppliedVersions = new ArrayList<Long>();
        List<Long> deltaAppliedVersions = new ArrayList<Long>();
        
        @Override
        public void refreshStarted(long currentVersion, long requestedVersion) {
            cycles++;
            this.refreshStartCurrentVersion = currentVersion;
            this.refreshStartRequestedVersion = requestedVersion;
        }

        @Override
        public void snapshotUpdateOccurred(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
            snapshotUpdateOccurredVersions.add(version);
        }

        @Override
        public void deltaUpdateOccurred(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
            deltaUpdateOccurredVersions.add(version);
        }

        @Override
        public void blobLoaded(Blob transition) {
            blobsLoadedVersions.add(transition.getToVersion());
        }

        @Override
        public void refreshSuccessful(long beforeVersion, long afterVersion, long requestedVersion) {
            refreshSuccessBeforeVersion = beforeVersion;
            refreshSuccessAfterVersion = afterVersion;
            refreshSuccessRequestedVersion = requestedVersion;
        }

        @Override
        public void refreshFailed(long beforeVersion, long afterVersion, long requestedVersion, Throwable failureCause) {
        }

        @Override
        public void snapshotApplied(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
            snapshotAppliedVersions.add(version);
        }

        @Override
        public void deltaApplied(HollowAPI api, HollowReadStateEngine stateEngine, long version) throws Exception {
            deltaAppliedVersions.add(version);
        }
        
        public void clear() {
            cycles = 0;
            snapshotUpdateOccurredVersions.clear();
            deltaUpdateOccurredVersions.clear();
            blobsLoadedVersions.clear();
            snapshotAppliedVersions.clear();
            deltaAppliedVersions.clear();
        }
    }
}
