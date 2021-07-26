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
package com.netflix.hollow.core.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.netflix.hollow.core.schema.HollowSchema.SchemaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HollowSchemaTest {

    @Test
    public void isNullableObjectEquals() {
        Assertions.assertTrue(HollowSchema.isNullableObjectEquals(null, null));
        Assertions.assertTrue(HollowSchema.isNullableObjectEquals("S1", "S1"));
        Assertions.assertTrue(HollowSchema.isNullableObjectEquals(1, 1));

        Assertions.assertFalse(HollowSchema.isNullableObjectEquals(null, 1));
        Assertions.assertFalse(HollowSchema.isNullableObjectEquals(null, "S1"));
        Assertions.assertFalse(HollowSchema.isNullableObjectEquals("S1", null));
        Assertions.assertFalse(HollowSchema.isNullableObjectEquals("S1", ""));
        Assertions.assertFalse(HollowSchema.isNullableObjectEquals("S1", "S2"));
        Assertions.assertFalse(HollowSchema.isNullableObjectEquals("S1", 1));
    }

    @Test
    public void fromTypeId() {
        Assertions.assertEquals(SchemaType.OBJECT, SchemaType.fromTypeId(0));
        Assertions.assertEquals(SchemaType.OBJECT, SchemaType.fromTypeId(6));

        Assertions.assertNotEquals(SchemaType.OBJECT, SchemaType.fromTypeId(1));
        Assertions.assertEquals(SchemaType.LIST, SchemaType.fromTypeId(2));

        Assertions.assertEquals(SchemaType.SET, SchemaType.fromTypeId(1));
        Assertions.assertEquals(SchemaType.SET, SchemaType.fromTypeId(4));

        Assertions.assertEquals(SchemaType.MAP, SchemaType.fromTypeId(3));
        Assertions.assertEquals(SchemaType.MAP, SchemaType.fromTypeId(5));
    }

    @Test
    public void hasKey() {
        Assertions.assertTrue(SchemaType.hasKey(4));
        Assertions.assertTrue(SchemaType.hasKey(5));
        Assertions.assertTrue(SchemaType.hasKey(6));
    }

    @Test
    public void invalidName() {
        try {
            new HollowObjectSchema("", 1);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Type name in Hollow Schema was an empty string", ex.getMessage());
        }

        try {
            new HollowObjectSchema(null, 1);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Type name in Hollow Schema was null", ex.getMessage());
        }
    }
}
