package com.netflix.hollow.api.producer.enforcer;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BasicSingleProducerEnforcerTest {

    @Test
    public void testEnableDisable() {
        BasicSingleProducerEnforcer se = new BasicSingleProducerEnforcer();
        Assertions.assertTrue(se.isPrimary());

        se.disable();
        Assertions.assertFalse(se.isPrimary());

        se.enable();
        Assertions.assertTrue(se.isPrimary());
    }

    @Test
    public void testEnabledDisabledCyle() {
        BasicSingleProducerEnforcer se = new BasicSingleProducerEnforcer();
        Assertions.assertTrue(se.isPrimary());

        se.onCycleStart(1234L);
        se.onCycleComplete(null, 10L, TimeUnit.SECONDS);

        se.disable();
        Assertions.assertFalse(se.isPrimary());
    }

    @Test
    public void testMultiCycle() {
        BasicSingleProducerEnforcer se = new BasicSingleProducerEnforcer();

        for (int i = 0; i < 10; i++) {
            se.enable();
            Assertions.assertTrue(se.isPrimary());

            se.onCycleStart(1234L);

            se.disable();
            Assertions.assertTrue(se.isPrimary());

            se.onCycleComplete(null, 10L, TimeUnit.SECONDS);
            Assertions.assertFalse(se.isPrimary());
        }
    }

    @Test
    public void testTransitions() {
        BasicSingleProducerEnforcer se = new BasicSingleProducerEnforcer();
        Assertions.assertTrue(se.isPrimary());

        se.onCycleStart(1234L);
        Assertions.assertTrue(se.isPrimary());

        se.disable();
        Assertions.assertTrue(se.isPrimary());

        se.onCycleComplete(null, 10L, TimeUnit.SECONDS);
        Assertions.assertFalse(se.isPrimary());

        se.enable();
        Assertions.assertTrue(se.isPrimary());

        se.onCycleStart(1235L);
        Assertions.assertTrue(se.isPrimary());

        se.disable();
        Assertions.assertTrue(se.isPrimary());

        se.onCycleComplete(null, 10L, TimeUnit.SECONDS);
        Assertions.assertFalse(se.isPrimary());
    }

}
