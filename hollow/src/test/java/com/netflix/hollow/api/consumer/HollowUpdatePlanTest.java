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

import com.netflix.hollow.api.client.HollowUpdatePlan;
import com.netflix.hollow.test.consumer.TestBlob;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HollowUpdatePlanTest {

    @Test
    public void testIsSnapshot() {
        HollowUpdatePlan plan = new HollowUpdatePlan();
        plan.add(new TestBlob(1));

        Assertions.assertTrue(plan.isSnapshotPlan());

        plan.add(new TestBlob(1, 2));

        Assertions.assertTrue(plan.isSnapshotPlan());

        plan = new HollowUpdatePlan();

        Assertions.assertFalse(plan.isSnapshotPlan());

        plan.add(new TestBlob(1, 2));

        Assertions.assertFalse(plan.isSnapshotPlan());
    }

    @Test
    public void testGetSnapshotTransition() {
        TestBlob snapshotTransition = new TestBlob(1);

        HollowUpdatePlan plan = new HollowUpdatePlan();
        plan.add(snapshotTransition);

        Assertions.assertSame(snapshotTransition, plan.getSnapshotTransition());

        plan.add(new TestBlob(1, 2));

        Assertions.assertSame(snapshotTransition, plan.getSnapshotTransition());
    }

    @Test
    public void testGetDeltaTransitionsForSnapshotPlan() {
        TestBlob snapshotTransition = new TestBlob(1);

        HollowUpdatePlan plan = new HollowUpdatePlan();
        plan.add(snapshotTransition);

        Assertions.assertTrue(plan.getDeltaTransitions().isEmpty());

        TestBlob delta1 = new TestBlob(1, 2);
        plan.add(delta1);

        Assertions.assertEquals(1, plan.getDeltaTransitions().size());

        TestBlob delta2 = new TestBlob(2, 3);
        plan.add(delta2);

        Assertions.assertEquals(2, plan.getDeltaTransitions().size());
        Assertions.assertSame(snapshotTransition, plan.getSnapshotTransition());
        Assertions.assertSame(delta1, plan.getDeltaTransitions().get(0));
        Assertions.assertSame(delta2, plan.getDeltaTransitions().get(1));
    }

    @Test
    public void testGetDeltaTransitionsForDeltaPlan() {
        HollowUpdatePlan plan = new HollowUpdatePlan();

        Assertions.assertTrue(plan.getDeltaTransitions().isEmpty());

        TestBlob delta1 = new TestBlob(1, 2);
        plan.add(delta1);

        Assertions.assertEquals(1, plan.getDeltaTransitions().size());

        TestBlob delta2 = new TestBlob(2, 3);
        plan.add(delta2);

        Assertions.assertEquals(2, plan.getDeltaTransitions().size());
        Assertions.assertNull(plan.getSnapshotTransition());
        Assertions.assertSame(delta1, plan.getDeltaTransitions().get(0));
        Assertions.assertSame(delta2, plan.getDeltaTransitions().get(1));
    }

}
