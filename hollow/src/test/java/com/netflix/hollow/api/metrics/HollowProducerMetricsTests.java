package com.netflix.hollow.api.metrics;

import com.netflix.hollow.api.consumer.InMemoryBlobStore;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducerFakeListener;
import com.netflix.hollow.api.producer.HollowProducerListener;
import com.netflix.hollow.api.producer.fs.HollowInMemoryBlobStager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class HollowProducerMetricsTests {

    private InMemoryBlobStore blobStore;
    private HollowProducerMetrics hollowProducerMetrics;
    private HollowProducerFakeListener hollowProducerFakeListener;

    @BeforeEach
    public void setUp() {
        blobStore = new InMemoryBlobStore();
        hollowProducerMetrics = new HollowProducerMetrics();
        hollowProducerFakeListener = new HollowProducerFakeListener();
    }

    @Test
    public void metricsDoNotBreakWithNullStateEngineInSuccess() {
        HollowProducerListener.ProducerStatus producerStatus = hollowProducerFakeListener.getSuccessFakeStatus(1L);
        hollowProducerMetrics.updateCycleMetrics(producerStatus);
        Assertions.assertEquals(hollowProducerMetrics.getCyclesSucceeded(), 1);
    }

    @Test
    public void metricsDoNotBreakWithNullStateEngineInFail() {
        HollowProducerListener.ProducerStatus producerStatus = hollowProducerFakeListener.getFailFakeStatus(1L);
        hollowProducerMetrics.updateCycleMetrics(producerStatus);
        Assertions.assertEquals(hollowProducerMetrics.getCycleFailed(), 1);
    }

    @Test
    public void metricsWhenPublishingSnapshot() {
        HollowProducer producer = HollowProducer.withPublisher(blobStore)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        producer.runCycle(new HollowProducer.Populator() {
            public void populate(HollowProducer.WriteState state) throws Exception {
                state.add(Integer.valueOf(1));
            }
        });

        HollowProducerMetrics hollowProducerMetrics = producer.getMetrics();
        Assertions.assertEquals(hollowProducerMetrics.getCyclesSucceeded(), 1);
        Assertions.assertEquals(hollowProducerMetrics.getCyclesCompleted(), 1);
        Assertions.assertEquals(hollowProducerMetrics.getTotalPopulatedOrdinals(), 1);
        Assertions.assertEquals(hollowProducerMetrics.getSnapshotsCompleted(), 1);
    }

    @Test
    public void metricsWhenPublishingFails() {
        HollowProducer producer = HollowProducer.withPublisher(blobStore)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();


        try {
            producer.runCycle(new HollowProducer.Populator() {
                public void populate(HollowProducer.WriteState state) throws Exception {
                    state.add(null);
                }
            });
        } catch (Exception ignored){ }

        HollowProducerMetrics hollowProducerMetrics = producer.getMetrics();
        Assertions.assertEquals(hollowProducerMetrics.getCyclesSucceeded(), 0);
        Assertions.assertEquals(hollowProducerMetrics.getCyclesCompleted(), 1);
        Assertions.assertEquals(hollowProducerMetrics.getCycleFailed(), 1);
    }
}
