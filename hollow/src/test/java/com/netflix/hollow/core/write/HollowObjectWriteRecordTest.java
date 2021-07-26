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
package com.netflix.hollow.core.write;

import com.netflix.hollow.core.memory.ByteDataArray;
import com.netflix.hollow.core.memory.encoding.VarInt;
import com.netflix.hollow.core.memory.encoding.ZigZag;
import com.netflix.hollow.core.memory.pool.WastefulRecycler;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HollowObjectWriteRecordTest {

    HollowObjectSchema schema;

    @BeforeEach
    public void setUp() {
        schema = new HollowObjectSchema("Test", 3);

        schema.addField("FieldA", FieldType.INT);
        schema.addField("FieldB", FieldType.LONG);
        schema.addField("FieldC", FieldType.BOOLEAN);
    }

    @Test
    public void translatesSchemas() {
        HollowObjectWriteRecord rec = new HollowObjectWriteRecord(schema);

        rec.setInt("FieldA", 1023);
        rec.setLong("FieldB", 123556);
        rec.setBoolean("FieldC", true);

        HollowObjectSchema translatedSchema = new HollowObjectSchema("Test", 3);

        translatedSchema.addField("FieldB", FieldType.LONG);
        translatedSchema.addField("FieldD", FieldType.STRING);
        translatedSchema.addField("FieldA", FieldType.INT);

        ByteDataArray buf = new ByteDataArray(WastefulRecycler.DEFAULT_INSTANCE);

        rec.writeDataTo(buf, translatedSchema);

        long field0 = VarInt.readVLong(buf.getUnderlyingArray(), 0);
        int field0Length = VarInt.sizeOfVLong(field0);
        int field2 = VarInt.readVInt(buf.getUnderlyingArray(), field0Length + 1);

        Assertions.assertEquals(123556, ZigZag.decodeLong(field0));
        Assertions.assertTrue(VarInt.readVNull(buf.getUnderlyingArray(), field0Length));
        Assertions.assertEquals(1023, ZigZag.decodeInt(field2));

    }

}
