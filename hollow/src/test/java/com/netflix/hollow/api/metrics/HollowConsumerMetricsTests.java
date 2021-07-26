package com.netflix.hollow.api.metrics;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.InMemoryBlobStore;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.fs.HollowInMemoryBlobStager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HollowConsumerMetricsTests {

    private InMemoryBlobStore blobStore;

    @BeforeEach
    public void setUp() {
        blobStore = new InMemoryBlobStore();
    }

    @Test
    public void metricsWhenLoadingSnapshot() {
        HollowProducer producer = HollowProducer.withPublisher(blobStore)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        long version = producer.runCycle(new HollowProducer.Populator() {
            public void populate(HollowProducer.WriteState state) throws Exception {
                state.add(Integer.valueOf(1));
            }
        });

        HollowConsumer consumer = HollowConsumer.withBlobRetriever(blobStore).build();
        consumer.triggerRefreshTo(version);

        HollowConsumerMetrics hollowConsumerMetrics = consumer.getMetrics();
        Assertions.assertEquals(version, consumer.getCurrentVersionId());
        Assertions.assertEquals(hollowConsumerMetrics.getCurrentVersion(), consumer.getCurrentVersionId());
        Assertions.assertEquals(hollowConsumerMetrics.getRefreshSucceded(), 1);
        Assertions.assertEquals(hollowConsumerMetrics.getTotalPopulatedOrdinals(), 1);
    }

    @Test
    public void metricsWhenRefreshFails() {
        HollowConsumer consumer = HollowConsumer.withBlobRetriever(blobStore).build();
        try {
            consumer.triggerRefreshTo(0);
        } catch (Exception ignored) { }

        HollowConsumerMetrics hollowConsumerMetrics = consumer.getMetrics();
        Assertions.assertEquals(hollowConsumerMetrics.getRefreshFailed(), 1);
        Assertions.assertEquals(hollowConsumerMetrics.getTotalPopulatedOrdinals(), 0);
    }

    @Test
    public void metricsWhenRefreshFailsDoNotRestartPreviousOnes() {
        HollowProducer producer = HollowProducer.withPublisher(blobStore)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        /// Showing verbose version of `runCycle(producer, 1);`
        long version = producer.runCycle(new HollowProducer.Populator() {
            public void populate(HollowProducer.WriteState state) throws Exception {
                state.add(Integer.valueOf(1));
            }
        });

        HollowConsumer consumer = HollowConsumer.withBlobRetriever(blobStore).build();
        consumer.triggerRefreshTo(version);

        try {
            consumer.triggerRefreshTo(0);
        } catch (Exception ignored) { }

        HollowConsumerMetrics hollowConsumerMetrics = consumer.getMetrics();
        Assertions.assertEquals(hollowConsumerMetrics.getRefreshFailed(), 1);
        Assertions.assertEquals(hollowConsumerMetrics.getRefreshSucceded(), 1);
        Assertions.assertEquals(hollowConsumerMetrics.getTotalPopulatedOrdinals(), 1);
    }
}
