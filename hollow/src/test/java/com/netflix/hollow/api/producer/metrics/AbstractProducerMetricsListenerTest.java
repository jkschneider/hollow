package com.netflix.hollow.api.producer.metrics;

import static org.mockito.Mockito.when;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.Status;
import com.netflix.hollow.api.producer.listener.CycleListener;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractProducerMetricsListenerTest {

    private final long TEST_VERSION = 123l;
    private final long TEST_LAST_CYCLE_NANOS = 100l;
    private final long TEST_LAST_ANNOUNCEMENT_NANOS = 200l;
    private final long TEST_DATA_SIZE = 55l;
    private final com.netflix.hollow.api.producer.Status TEST_STATUS_SUCCESS = new Status(Status.StatusType.SUCCESS, null);
    private final com.netflix.hollow.api.producer.Status TEST_STATUS_FAIL = new Status(Status.StatusType.FAIL, null);
    private final Duration TEST_CYCLE_DURATION_MILLIS = Duration.ofMillis(4l);
    private final long TEST_ANNOUNCEMENT_DURATION_MILLIS = 2l;

    @Mock
    private HollowProducer.ReadState mockReadState;

    @Mock
    private HollowReadStateEngine mockStateEngine;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(mockReadState.getStateEngine()).thenReturn(mockStateEngine);
        when(mockStateEngine.calcApproxDataSize()).thenReturn(TEST_DATA_SIZE);
    }

    @Test
    public void testCycleSkipWhenNeverBeenPrimaryProducer() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assertions.assertNotNull(cycleMetrics);
                Assertions.assertEquals(0l, cycleMetrics.getConsecutiveFailures());
                Assertions.assertEquals(Optional.empty(), cycleMetrics.getIsCycleSuccess());
                Assertions.assertEquals(OptionalLong.empty(), cycleMetrics.getCycleDurationMillis());
                Assertions.assertEquals(OptionalLong.empty(), cycleMetrics.getLastCycleSuccessTimeNano());
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
                Assertions.assertNotNull(cycleMetrics);
                Assertions.assertEquals(0l, cycleMetrics.getConsecutiveFailures());
                Assertions.assertEquals(Optional.empty(), cycleMetrics.getIsCycleSuccess());
                Assertions.assertEquals(OptionalLong.empty(), cycleMetrics.getCycleDurationMillis());
                Assertions.assertEquals(OptionalLong.of(TEST_LAST_CYCLE_NANOS), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }
        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastCycleSuccessTimeNanoOptional = OptionalLong.of(TEST_LAST_CYCLE_NANOS);
        concreteProducerMetricsListener.onCycleSkip(CycleListener.CycleSkipReason.NOT_PRIMARY_PRODUCER);
    }

    @Test
    public void testCycleCompleteWithSuccess() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assertions.assertNotNull(cycleMetrics);
                Assertions.assertEquals(0l, cycleMetrics.getConsecutiveFailures());
                Assertions.assertEquals(Optional.of(true), cycleMetrics.getIsCycleSuccess());
                Assertions.assertEquals(OptionalLong.of(TEST_CYCLE_DURATION_MILLIS.toMillis()), cycleMetrics.getCycleDurationMillis());
                Assertions.assertNotEquals(OptionalLong.of(TEST_LAST_CYCLE_NANOS), cycleMetrics.getLastCycleSuccessTimeNano());
                Assertions.assertNotEquals(OptionalLong.empty(), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastCycleSuccessTimeNanoOptional = OptionalLong.of(TEST_LAST_CYCLE_NANOS);
        concreteProducerMetricsListener.onCycleStart(TEST_VERSION);
        concreteProducerMetricsListener.onCycleComplete(TEST_STATUS_SUCCESS, mockReadState, TEST_VERSION, TEST_CYCLE_DURATION_MILLIS);
    }

    @Test
    public void testCycleCompleteWithFail() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void cycleMetricsReporting(CycleMetrics cycleMetrics) {
                Assertions.assertNotNull(cycleMetrics);
                Assertions.assertEquals(1l, cycleMetrics.getConsecutiveFailures());
                Assertions.assertEquals(Optional.of(false), cycleMetrics.getIsCycleSuccess());
                Assertions.assertEquals(OptionalLong.of(TEST_CYCLE_DURATION_MILLIS.toMillis()), cycleMetrics.getCycleDurationMillis());
                Assertions.assertEquals(OptionalLong.of(TEST_LAST_CYCLE_NANOS), cycleMetrics.getLastCycleSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastCycleSuccessTimeNanoOptional = OptionalLong.of(TEST_LAST_CYCLE_NANOS);
        concreteProducerMetricsListener.onCycleStart(TEST_VERSION);
        concreteProducerMetricsListener.onCycleComplete(TEST_STATUS_FAIL, mockReadState, TEST_VERSION, TEST_CYCLE_DURATION_MILLIS);
    }

    @Test
    public void testAnnouncementCompleteWithSuccess() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void announcementMetricsReporting(AnnouncementMetrics announcementMetrics) {
                Assertions.assertNotNull(announcementMetrics);
                Assertions.assertEquals(TEST_DATA_SIZE, announcementMetrics.getDataSizeBytes());
                Assertions.assertEquals(true, announcementMetrics.getIsAnnouncementSuccess());
                Assertions.assertEquals(TEST_ANNOUNCEMENT_DURATION_MILLIS,
                        announcementMetrics.getAnnouncementDurationMillis());
                Assertions.assertNotEquals(OptionalLong.of(TEST_LAST_ANNOUNCEMENT_NANOS),
                        announcementMetrics.getLastAnnouncementSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastAnnouncementSuccessTimeNanoOptional = OptionalLong.of(
                TEST_LAST_ANNOUNCEMENT_NANOS);
        concreteProducerMetricsListener.onAnnouncementStart(TEST_VERSION);
        concreteProducerMetricsListener.onAnnouncementComplete(TEST_STATUS_SUCCESS, mockReadState, TEST_VERSION, Duration.ofMillis(TEST_ANNOUNCEMENT_DURATION_MILLIS));
    }

    @Test
    public void testAnnouncementCompleteWithFail() {
        final class TestProducerMetricsListener extends AbstractProducerMetricsListener {
            @Override
            public void announcementMetricsReporting(AnnouncementMetrics announcementMetrics) {
                Assertions.assertNotNull(announcementMetrics);
                Assertions.assertEquals(TEST_DATA_SIZE, announcementMetrics.getDataSizeBytes());
                Assertions.assertFalse(announcementMetrics.getIsAnnouncementSuccess());
                Assertions.assertEquals(TEST_ANNOUNCEMENT_DURATION_MILLIS,
                        announcementMetrics.getAnnouncementDurationMillis());
                Assertions.assertEquals(OptionalLong.of(TEST_LAST_ANNOUNCEMENT_NANOS),
                        announcementMetrics.getLastAnnouncementSuccessTimeNano());
            }
        }

        AbstractProducerMetricsListener concreteProducerMetricsListener = new TestProducerMetricsListener();
        concreteProducerMetricsListener.lastAnnouncementSuccessTimeNanoOptional = OptionalLong.of(
                TEST_LAST_ANNOUNCEMENT_NANOS);
        concreteProducerMetricsListener.onAnnouncementStart(TEST_VERSION);
        concreteProducerMetricsListener.onAnnouncementComplete(TEST_STATUS_FAIL, mockReadState, TEST_VERSION, Duration.ofMillis(TEST_ANNOUNCEMENT_DURATION_MILLIS));
    }
}
