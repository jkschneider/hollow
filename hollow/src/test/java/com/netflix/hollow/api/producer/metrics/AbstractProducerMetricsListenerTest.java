package com.netflix.hollow.api.producer.metrics;

import static org.mockito.Mockito.when;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.Status;
import com.netflix.hollow.api.producer.listener.CycleListener;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractProducerMetricsListenerTest {

    private final long testVersion = 123L;
    private final long testLastCycleNanos = 100L;
    private final long testLastAnnouncementNanos = 200L;
    private final long testDataSize = 55L;
    private final com.netflix.hollow.api.producer.Status testStatusSuccess = new Status(Status.StatusType.SUCCESS, null);
    private final com.netflix.hollow.api.producer.Status testStatusFail = new Status(Status.StatusType.FAIL, null);
    private final Duration testCycleDurationMillis = Duration.ofMillis(4l);
    private final long testAnnouncementDurationMillis = 2L;

    @Mock
    private HollowProducer.ReadState mockReadState;

    @Mock
    private HollowReadStateEngine mockStateEngine;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(mockReadState.getStateEngine()).thenReturn(mockStateEngine);
        when(mockStateEngine.calcApproxDataSize()).thenReturn(testDataSize);
    }

    @Test
    public void testCycleSkipWhenNeverBeenPrimaryProducer() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assert.assertNotNull(cycleMetrics);
                Assert.assertEquals(0l, cycleMetrics.getConsecutiveFailures());
                Assert.assertEquals(Optional.empty(), cycleMetrics.getIsCycleSuccess());
                Assert.assertEquals(OptionalLong.empty(), cycleMetrics.getCycleDurationMillis());
                Assert.assertEquals(OptionalLong.empty(), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }
        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.onCycleSkip(CycleListener.CycleSkipReason.NOT_PRIMARY_PRODUCER);
    }

    @Test
    public void testCycleSkipWhenPreviouslyPrimaryProducer() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assert.assertNotNull(cycleMetrics);
                Assert.assertEquals(0l, cycleMetrics.getConsecutiveFailures());
                Assert.assertEquals(Optional.empty(), cycleMetrics.getIsCycleSuccess());
                Assert.assertEquals(OptionalLong.empty(), cycleMetrics.getCycleDurationMillis());
                Assert.assertEquals(OptionalLong.of(testLastCycleNanos), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }
        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastCycleSuccessTimeNanoOptional = OptionalLong.of(testLastCycleNanos);
        concreteProducerMetricsListener.onCycleSkip(CycleListener.CycleSkipReason.NOT_PRIMARY_PRODUCER);
    }

    @Test
    public void testCycleCompleteWithSuccess() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assert.assertNotNull(cycleMetrics);
                Assert.assertEquals(0l, cycleMetrics.getConsecutiveFailures());
                Assert.assertEquals(Optional.of(true), cycleMetrics.getIsCycleSuccess());
                Assert.assertEquals(OptionalLong.of(testCycleDurationMillis.toMillis()), cycleMetrics.getCycleDurationMillis());
                Assert.assertNotEquals(OptionalLong.of(testLastCycleNanos), cycleMetrics.getLastCycleSuccessTimeNano());
                Assert.assertNotEquals(OptionalLong.empty(), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastCycleSuccessTimeNanoOptional = OptionalLong.of(testLastCycleNanos);
        concreteProducerMetricsListener.onCycleStart(testVersion);
        concreteProducerMetricsListener.onCycleComplete(testStatusSuccess, mockReadState, testVersion, testCycleDurationMillis);
    }

    @Test
    public void testCycleCompleteWithFail() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assert.assertNotNull(cycleMetrics);
                Assert.assertEquals(1l, cycleMetrics.getConsecutiveFailures());
                Assert.assertEquals(Optional.of(false), cycleMetrics.getIsCycleSuccess());
                Assert.assertEquals(OptionalLong.of(testCycleDurationMillis.toMillis()), cycleMetrics.getCycleDurationMillis());
                Assert.assertEquals(OptionalLong.of(testLastCycleNanos), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastCycleSuccessTimeNanoOptional = OptionalLong.of(testLastCycleNanos);
        concreteProducerMetricsListener.onCycleStart(testVersion);
        concreteProducerMetricsListener.onCycleComplete(testStatusFail, mockReadState, testVersion, testCycleDurationMillis);
    }

    @Test
    public void testAnnouncementCompleteWithSuccess() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void announcementMetricsReporting(AnnouncementMetrics announcementMetrics) {
                Assert.assertNotNull(announcementMetrics);
                Assert.assertEquals(testDataSize, announcementMetrics.getDataSizeBytes());
                Assert.assertEquals(true, announcementMetrics.getIsAnnouncementSuccess());
                Assert.assertEquals(testAnnouncementDurationMillis,
                        announcementMetrics.getAnnouncementDurationMillis());
                Assert.assertNotEquals(OptionalLong.of(testLastAnnouncementNanos),
                        announcementMetrics.getLastAnnouncementSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastAnnouncementSuccessTimeNanoOptional = OptionalLong.of(
                testLastAnnouncementNanos);
        concreteProducerMetricsListener.onAnnouncementStart(testVersion);
        concreteProducerMetricsListener.onAnnouncementComplete(testStatusSuccess, mockReadState, testVersion, Duration.ofMillis(testAnnouncementDurationMillis));
    }

    @Test
    public void testAnnouncementCompleteWithFail() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void announcementMetricsReporting(AnnouncementMetrics announcementMetrics) {
                Assert.assertNotNull(announcementMetrics);
                Assert.assertEquals(testDataSize, announcementMetrics.getDataSizeBytes());
                Assert.assertFalse(announcementMetrics.getIsAnnouncementSuccess());
                Assert.assertEquals(testAnnouncementDurationMillis,
                        announcementMetrics.getAnnouncementDurationMillis());
                Assert.assertEquals(OptionalLong.of(testLastAnnouncementNanos),
                        announcementMetrics.getLastAnnouncementSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastAnnouncementSuccessTimeNanoOptional = OptionalLong.of(
                testLastAnnouncementNanos);
        concreteProducerMetricsListener.onAnnouncementStart(testVersion);
        concreteProducerMetricsListener.onAnnouncementComplete(testStatusFail, mockReadState, testVersion, Duration.ofMillis(testAnnouncementDurationMillis));
    }
}
