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
package com.netflix.hollow.api.codegen;

import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowInline;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HollowErgonomicAPIShortcutsTest {

    @Test
    public void test() throws IOException {
        HollowWriteStateEngine writeEngine = new HollowWriteStateEngine();
        HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);
        mapper.initializeTypeState(TypeA.class);

        HollowErgonomicAPIShortcuts shortcuts = new HollowErgonomicAPIShortcuts(writeEngine);

        Assertions.assertEquals(5, shortcuts.numShortcuts());
        
        Assertions.assertArrayEquals(new String[] { "value" }, shortcuts.getShortcut("StringReferenceReference.ref").getPath());
        Assertions.assertArrayEquals(new String[] { "StringReference" }, shortcuts.getShortcut("StringReferenceReference.ref").getPathTypes());
        Assertions.assertArrayEquals(new String[] { "value" }, shortcuts.getShortcut("TypeA.a2").getPath());
        Assertions.assertArrayEquals(new String[] { "StringReference" }, shortcuts.getShortcut("TypeA.a2").getPathTypes());
        Assertions.assertArrayEquals(new String[] { "value" }, shortcuts.getShortcut("TypeB.b1").getPath());
        Assertions.assertArrayEquals(new String[] { "StringReference" }, shortcuts.getShortcut("TypeB.b1").getPathTypes());
        Assertions.assertArrayEquals(new String[] { "ref", "value" }, shortcuts.getShortcut("TypeA.a3").getPath());
        Assertions.assertArrayEquals(new String[] { "StringReferenceReference", "StringReference" }, shortcuts.getShortcut("TypeA.a3").getPathTypes());
        Assertions.assertArrayEquals(new String[] { "ref", "value" }, shortcuts.getShortcut("TypeB.b2").getPath());
        Assertions.assertArrayEquals(new String[] { "StringReferenceReference", "StringReference" }, shortcuts.getShortcut("TypeB.b2").getPathTypes());

        Assertions.assertEquals(FieldType.STRING, shortcuts.getShortcut("StringReferenceReference.ref").getType());
        Assertions.assertEquals(FieldType.STRING, shortcuts.getShortcut("TypeA.a2").getType());
        Assertions.assertEquals(FieldType.STRING, shortcuts.getShortcut("TypeB.b1").getType());
        Assertions.assertEquals(FieldType.STRING, shortcuts.getShortcut("TypeA.a3").getType());
        Assertions.assertEquals(FieldType.STRING, shortcuts.getShortcut("TypeB.b2").getType());
    }

    @SuppressWarnings("unused")
    private static class TypeA {
        int a1;
        StringReference a2;
        StringReferenceReference a3;
        TypeB a4;
    }

    @SuppressWarnings("unused")
    private static class TypeB {
        StringReference b1;
        StringReferenceReference b2;
        @HollowInline String b3;
    }

    @SuppressWarnings("unused")
    private static class StringReferenceReference {
        StringReference ref;
    }

    private static class StringReference {
        @HollowInline String value;
    }

}
