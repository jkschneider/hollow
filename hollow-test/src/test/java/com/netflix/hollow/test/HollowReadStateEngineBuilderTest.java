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
package com.netflix.hollow.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.netflix.hollow.api.objects.generic.GenericHollowObject;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

public class HollowReadStateEngineBuilderTest {
    @Test
    public void testBuild_withoutTypeInitialization() {
        HollowReadStateEngine readEngine = new HollowReadStateEngineBuilder()
            .add("penguins3peat").add(3L).build();
        assertEquals(new HashSet<String>(Arrays.asList(
                        String.class.getSimpleName(), Long.class.getSimpleName())),
                new HashSet<String>(readEngine.getAllTypes()),
                "Should have both types");
    }

    @Test
    public void testBuild_withTypeInitialization() {
        HollowReadStateEngine readEngine =
            new HollowReadStateEngineBuilder(Arrays.<Class<?>>asList(String.class, Long.class))
            .add("foo").add(3L).build();
        assertEquals(new HashSet<String>(Arrays.asList(
                        String.class.getSimpleName(), Long.class.getSimpleName())),
                new HashSet<String>(readEngine.getAllTypes()),
                "Should have both types");
        assertEquals(1, readEngine.getTypeDataAccess(
                    String.class.getSimpleName()).getTypeState().getPopulatedOrdinals().cardinality(), "Should have one String");
        assertEquals("foo", new GenericHollowObject(readEngine,
                    String.class.getSimpleName(), 0).getString("value"), "The one String should be foo");
        assertEquals(1, readEngine.getTypeDataAccess(
                    Long.class.getSimpleName()).getTypeState().getPopulatedOrdinals().cardinality(), "Should have one Long");
        assertEquals(3L, new GenericHollowObject(readEngine,
                    Long.class.getSimpleName(), 0).getLong("value"), "The one Long should be 3L");
    }

    @Test
    public void testBuild_canConstructMultiple() {
        HollowReadStateEngineBuilder builder = new HollowReadStateEngineBuilder();
        builder.build();
        builder.build();
    }

    @Test
    public void testBuild_cannotAddAfterBuild() {
        assertThrows(IllegalArgumentException.class, () -> {
            HollowReadStateEngineBuilder builder = new HollowReadStateEngineBuilder();
            builder.build();
            builder.add("foo");
        });
    }
}
