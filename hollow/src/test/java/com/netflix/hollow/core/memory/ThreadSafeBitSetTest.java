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
 */
package com.netflix.hollow.core.memory;

import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadSafeBitSetTest {

    @Test
    public void testEquality() {
        ThreadSafeBitSet set1 = new ThreadSafeBitSet();
        ThreadSafeBitSet set2 = new ThreadSafeBitSet(14, 16385);

        set1.set(100);

        set2.set(100);

        Assertions.assertEquals(set1, set2);
        Assertions.assertEquals(set2, set1);

        set1.set(100000);

        Assertions.assertNotEquals(set1, set2);
        Assertions.assertNotEquals(set2, set1);

        set1.clearAll();

        Assertions.assertNotEquals(set1, set2);
        Assertions.assertNotEquals(set2, set1);

        set1.set(100);

        Assertions.assertEquals(set1, set2);
        Assertions.assertEquals(set2, set1);

    }

    @Test
    public void testMaxSetBit() {
        ThreadSafeBitSet set1 = new ThreadSafeBitSet();

        set1.set(100);
        Assertions.assertEquals(100, set1.maxSetBit());

        set1.set(100000);
        Assertions.assertEquals(100000, set1.maxSetBit());

        set1.set(1000000);
        Assertions.assertEquals(1000000, set1.maxSetBit());

        set1.clearAll();
        set1.set(555555);
        Assertions.assertEquals(555555, set1.maxSetBit());
    }

    @Test
    public void testNextSetBit() {
        ThreadSafeBitSet set1 = new ThreadSafeBitSet();

        set1.set(100);
        set1.set(101);
        set1.set(103);
        set1.set(100000);
        set1.set(1000000);

        Assertions.assertEquals(100, set1.nextSetBit(0));
        Assertions.assertEquals(101, set1.nextSetBit(101));
        Assertions.assertEquals(103, set1.nextSetBit(102));
        Assertions.assertEquals(100000, set1.nextSetBit(104));
        Assertions.assertEquals(1000000, set1.nextSetBit(100001));
        Assertions.assertEquals(-1, set1.nextSetBit(1000001));
        Assertions.assertEquals(-1, set1.nextSetBit(1015809));

        set1.clearAll();
        set1.set(555555);
        Assertions.assertEquals(555555, set1.nextSetBit(0));
        Assertions.assertEquals(-1, set1.nextSetBit(555556));
    }

    @Test
    public void testClear() {
        ThreadSafeBitSet set1 = new ThreadSafeBitSet();

        set1.set(10);
        set1.set(20);
        set1.set(21);
        set1.set(22);

        set1.clear(21);

        Assertions.assertEquals(3, set1.cardinality());
    }

    @Test
    public void testBasicAPIs() {
        ThreadSafeBitSet tsbSet = new ThreadSafeBitSet();
        int[] ordinals = new int[] { 1, 5, 10 };

        // init
        for (int ordinal : ordinals) {
            tsbSet.set(ordinal);
        }

        // validate content
        for (int ordinal : ordinals) {
            Assertions.assertTrue(tsbSet.get(ordinal));
        }
        Assertions.assertEquals(ordinals.length, tsbSet.cardinality());

        tsbSet.clear(ordinals[0]);
        Assertions.assertFalse(tsbSet.get(0));
        Assertions.assertEquals(ordinals.length - 1, tsbSet.cardinality());
    }

    @Test
    public void testToBitSet() {
        BitSet bSet = new BitSet();
        ThreadSafeBitSet tsbSet = new ThreadSafeBitSet();
        int[] ordinals = new int[] { 1, 5, 10 };

        // init
        for (int ordinal : ordinals) {
            bSet.set(ordinal);
            tsbSet.set(ordinal);
        }

        // validate content
        for (int ordinal : ordinals) {
            Assertions.assertEquals(bSet.get(ordinal), tsbSet.get(ordinal));
        }
        Assertions.assertEquals(bSet.cardinality(), tsbSet.cardinality());

        // compare toBitSet
        BitSet bSet2 = tsbSet.toBitSet();
        Assertions.assertEquals(bSet, bSet2);

        // compare toString
        Assertions.assertEquals(bSet.toString(), bSet.toString());
    }

    @Test
    public void testOrAll() {
        BitSet bSet = new BitSet();

        ThreadSafeBitSet[] tsbSets = new ThreadSafeBitSet[3];
        int[] ordinals = new int[] { 1, 5, 10 };

        // init
        int i = 0;
        for (int ordinal : ordinals) {
            tsbSets[i] = new ThreadSafeBitSet();
            tsbSets[i].set(ordinal);
            i++;

            bSet.set(ordinal);
        }

        // validate content
        ThreadSafeBitSet result = ThreadSafeBitSet.orAll(tsbSets);
        Assertions.assertEquals(bSet.cardinality(), result.cardinality());
        Assertions.assertEquals(bSet, result.toBitSet());
    }

    @Test
    public void testAndNot() {
        ThreadSafeBitSet tsbSet1 = new ThreadSafeBitSet();
        ThreadSafeBitSet tsbSet2 = new ThreadSafeBitSet();
        for (int i = 0; i < 3; i++) {
            tsbSet1.set(i);
            tsbSet2.set(i * 2);
        }

        // determine andNot
        BitSet andNot_bSet = new BitSet();
        ThreadSafeBitSet andNot_tsbSet = new ThreadSafeBitSet();

        int ordinal = tsbSet1.nextSetBit(0);
        while (ordinal != -1) {
            if (!tsbSet2.get(ordinal)) {
                andNot_bSet.set(ordinal);
                andNot_tsbSet.set(ordinal);
            }
            ordinal = tsbSet1.nextSetBit(ordinal + 1);
        }
        Assertions.assertFalse(tsbSet1.equals(tsbSet2));
        Assertions.assertNotEquals(tsbSet1, tsbSet2);
        Assertions.assertNotEquals(tsbSet1.toBitSet(), tsbSet2.toBitSet());

        // validate content
        ThreadSafeBitSet result = tsbSet1.andNot(tsbSet2);
        Assertions.assertEquals(andNot_tsbSet.cardinality(), result.cardinality());
        Assertions.assertTrue(andNot_tsbSet.equals(result));
        Assertions.assertEquals(andNot_tsbSet, result);
        Assertions.assertEquals(andNot_bSet, result.toBitSet());
    }
}
