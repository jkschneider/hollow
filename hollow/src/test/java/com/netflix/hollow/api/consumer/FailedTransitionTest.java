package com.netflix.hollow.api.consumer;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.fs.HollowInMemoryBlobStager;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FailedTransitionTest {

    @Test
    public void testSnapshotBlobFailure() {
        InMemoryBlobStore bs = new InMemoryBlobStore();

        HollowProducer producer = HollowProducer.withPublisher(bs)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        long version = producer.runCycle(ws -> {
            ws.add(1);
        });

        AtomicBoolean failer = new AtomicBoolean();
        HollowConsumer consumer = HollowConsumer
                .withBlobRetriever(new FailingBlobRetriever(failer::get, bs))
                .build();

        // Fail transitioning to snapshot
        failer.set(true);
        try {
            consumer.triggerRefreshTo(version);
            Assertions.fail();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            Assertions.assertTrue(cause instanceof IOException);
            Assertions.assertEquals("FAILED", cause.getMessage());
            Assertions.assertEquals(1, consumer.getNumFailedSnapshotTransitions());
        }

        // Fail for existing transition
        failer.set(false);
        try {
            consumer.triggerRefreshTo(version);
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertEquals(1, consumer.getNumFailedSnapshotTransitions());
        }

        try {
            consumer.triggerRefreshTo(version);
            Assertions.fail();
        } catch (RuntimeException e) {
            Assertions.assertEquals(1, consumer.getNumFailedSnapshotTransitions());
        }


        version = producer.runCycle(ws -> {
            ws.add(2);
        });

        // Pass for new transition
        // Consumer double snapshots
        consumer.triggerRefreshTo(version);
        Assertions.assertEquals(1, consumer.getNumFailedSnapshotTransitions());
    }


    @Test
    public void testDeltaBlobFailure() {
        InMemoryBlobStore bs = new InMemoryBlobStore();

        HollowProducer producer = HollowProducer.withPublisher(bs)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        long version = producer.runCycle(ws -> {
            ws.add(1);
        });

        AtomicBoolean failer = new AtomicBoolean();
        HollowConsumer consumer = HollowConsumer
                .withBlobRetriever(new FailingBlobRetriever(failer::get, bs))
                .build();

        // Transition to snapshot
        consumer.triggerRefreshTo(version);


        version = producer.runCycle(ws -> {
            ws.add(2);
        });

        // Fail transitioning to delta
        failer.set(true);
        try {
            consumer.triggerRefreshTo(version);
            Assertions.fail();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            Assertions.assertTrue(cause instanceof IOException);
            Assertions.assertEquals("FAILED", cause.getMessage());
            Assertions.assertEquals(1, consumer.getNumFailedDeltaTransitions());
        }


        // Pass for new transition
        // Consumer double snapshots
        failer.set(false);
        consumer.triggerRefreshTo(version);
    }


    @Test
    public void testSnapshotBlobFailureNoDoubleSnapshot() {
        InMemoryBlobStore bs = new InMemoryBlobStore();

        HollowProducer producer = HollowProducer.withPublisher(bs)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        long version = producer.runCycle(ws -> {
            ws.add(1);
        });

        AtomicBoolean failer = new AtomicBoolean();
        HollowConsumer consumer = HollowConsumer
                .withBlobRetriever(new FailingBlobRetriever(failer::get, bs))
                .withDoubleSnapshotConfig(new NoDoubleSnapshotConfig())
                .build();

        // Fail transitioning to snapshot
        failer.set(true);
        try {
            consumer.triggerRefreshTo(version);
            Assertions.fail();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            Assertions.assertTrue(cause instanceof IOException);
            Assertions.assertEquals("FAILED", cause.getMessage());
            Assertions.assertEquals(1, consumer.getNumFailedSnapshotTransitions());
        }

        // Pass on retry
        failer.set(false);
        consumer.triggerRefreshTo(version);
    }

    @Test
    public void testDeltaBlobFailureNoDoubleSnapshot() {
        InMemoryBlobStore bs = new InMemoryBlobStore();

        HollowProducer producer = HollowProducer.withPublisher(bs)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        long version = producer.runCycle(ws -> {
            ws.add(1);
        });

        AtomicBoolean failer = new AtomicBoolean();
        HollowConsumer consumer = HollowConsumer
                .withBlobRetriever(new FailingBlobRetriever(failer::get, bs))
                .withDoubleSnapshotConfig(new NoDoubleSnapshotConfig())
                .build();

        // Transition to snapshot
        consumer.triggerRefreshTo(version);


        version = producer.runCycle(ws -> {
            ws.add(2);
        });

        // Fail transitioning to delta
        failer.set(true);
        try {
            consumer.triggerRefreshTo(version);
            Assertions.fail();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            Assertions.assertTrue(cause instanceof IOException);
            Assertions.assertEquals("FAILED", cause.getMessage());
            Assertions.assertEquals(1, consumer.getNumFailedDeltaTransitions());
        }

        // Pass on retry
        failer.set(false);
        consumer.triggerRefreshTo(version);
    }


    static class NoDoubleSnapshotConfig implements HollowConsumer.DoubleSnapshotConfig {
        @Override public boolean allowDoubleSnapshot() {
            return false;
        }

        @Override public int maxDeltasBeforeDoubleSnapshot() {
            return 32;
        }
    }

    static class FailingBlobRetriever implements HollowConsumer.BlobRetriever {
        final BooleanSupplier failer;
        final HollowConsumer.BlobRetriever br;

        FailingBlobRetriever(BooleanSupplier failer, HollowConsumer.BlobRetriever br) {
            this.failer = failer;
            this.br = br;
        }

        @Override public HollowConsumer.Blob retrieveSnapshotBlob(long desiredVersion) {
            HollowConsumer.Blob blob = br.retrieveSnapshotBlob(desiredVersion);
            return new HollowConsumer.Blob(desiredVersion) {
                @Override public InputStream getInputStream() throws IOException {
                    if (failer.getAsBoolean()) {
                        throw new IOException("FAILED");
                    }
                    return blob.getInputStream();
                }
            };
        }

        @Override public HollowConsumer.Blob retrieveDeltaBlob(long currentVersion) {
            HollowConsumer.Blob blob = br.retrieveDeltaBlob(currentVersion);
            return new HollowConsumer.Blob(blob.getFromVersion(), blob.getToVersion()) {
                @Override public InputStream getInputStream() throws IOException {
                    if (failer.getAsBoolean()) {
                        throw new IOException("FAILED");
                    }
                    return blob.getInputStream();
                }
            };
        }

        @Override public HollowConsumer.Blob retrieveReverseDeltaBlob(long currentVersion) {
            HollowConsumer.Blob blob = br.retrieveReverseDeltaBlob(currentVersion);
            return new HollowConsumer.Blob(blob.getFromVersion(), blob.getToVersion()) {
                @Override public InputStream getInputStream() throws IOException {
                    if (failer.getAsBoolean()) {
                        throw new IOException("FAILED");
                    }
                    return blob.getInputStream();
                }
            };
        }
    }
}
