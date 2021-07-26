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
package com.netflix.hollow.core.index;

import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class PrimaryKeyTest {
    
    HollowWriteStateEngine writeEngine;
    
    @BeforeEach
    public void setUp() {
        writeEngine = new HollowWriteStateEngine();
        HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);

        mapper.initializeTypeState(TypeWithTraversablePrimaryKey.class);
    }

    @Test
    public void automaticallyTraversesSomeIncompletelyDefinedFieldPaths() {
        HollowObjectSchema schema = (HollowObjectSchema) writeEngine.getTypeState("TypeWithTraversablePrimaryKey").getSchema();
        PrimaryKey traversablePrimaryKey = schema.getPrimaryKey();
        
        Assertions.assertEquals(2, traversablePrimaryKey.getFieldPathIndex(writeEngine, 0).length);
        Assertions.assertEquals(3, traversablePrimaryKey.getFieldPathIndex(writeEngine, 1).length);
        Assertions.assertEquals(1, traversablePrimaryKey.getFieldPathIndex(writeEngine, 2).length);
        
        PrimaryKey anotherTraversablePrimaryKey = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType.id");
        Assertions.assertEquals(3, anotherTraversablePrimaryKey.getFieldPathIndex(writeEngine, 0).length);
        
        PrimaryKey hardStopPrimaryKey = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType.id!");
        Assertions.assertEquals(2, hardStopPrimaryKey.getFieldPathIndex(writeEngine, 0).length);
        
        PrimaryKey hardStopPrimaryKey2 = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType2!");
        Assertions.assertEquals(1, hardStopPrimaryKey2.getFieldPathIndex(writeEngine, 0).length);
        
        PrimaryKey hardStopPrimaryKey3 = new PrimaryKey("TypeWithTraversablePrimaryKey", "strList!");
        Assertions.assertEquals(1, hardStopPrimaryKey3.getFieldPathIndex(writeEngine, 0).length);
    }
    
    @Test
    public void throwsMeaningfulExceptions() {
        try {
            PrimaryKey invalidFieldDefinition = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType.nofield");
            invalidFieldDefinition.getFieldPathIndex(writeEngine, 0);
            Assertions.fail("IllegalArgumentException expected");
        } catch (FieldPaths.FieldPathException expected) {
            Assertions.assertEquals(FieldPaths.FieldPathException.ErrorKind.NOT_FOUND, expected.error);
            Assertions.assertEquals(1, expected.fieldSegments.size());
            Assertions.assertEquals(1, expected.segmentIndex);
            Assertions.assertEquals("SubTypeWithTraversablePrimaryKey", expected.enclosingSchema.getName());
        }
        
        try {
            PrimaryKey invalidFieldDefinition = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType.id.value.alldone");
            invalidFieldDefinition.getFieldPathIndex(writeEngine, 0);
            Assertions.fail("IllegalArgumentException expected");
        } catch (FieldPaths.FieldPathException expected) {
            Assertions.assertEquals(FieldPaths.FieldPathException.ErrorKind.NOT_TRAVERSABLE, expected.error);
            Assertions.assertEquals(3, expected.fieldSegments.size());
            Assertions.assertEquals(2, expected.segmentIndex);
            Assertions.assertEquals("value", expected.fieldSegments.get(2).getName());
            Assertions.assertEquals("String", expected.enclosingSchema.getName());
        }
        
        try {
            PrimaryKey invalidFieldDefinition = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType2");
            invalidFieldDefinition.getFieldPathIndex(writeEngine, 0);
            Assertions.fail("IllegalArgumentException expected");
        } catch (FieldPaths.FieldPathException expected) {
            Assertions.assertEquals(FieldPaths.FieldPathException.ErrorKind.NOT_EXPANDABLE, expected.error);
            Assertions.assertEquals(1, expected.fieldSegments.size());
            Assertions.assertEquals("subType2", expected.fieldSegments.get(0).getName());
            Assertions.assertEquals("SubTypeWithNonTraversablePrimaryKey", expected.enclosingSchema.getName());
        }
        
        try {
            PrimaryKey invalidFieldDefinition = new PrimaryKey("TypeWithTraversablePrimaryKey", "strList.element.value");
            invalidFieldDefinition.getFieldPathIndex(writeEngine, 0);
            Assertions.fail("IllegalArgumentException expected");
        } catch (FieldPaths.FieldPathException expected) {
            Assertions.assertEquals(FieldPaths.FieldPathException.ErrorKind.NOT_TRAVERSABLE, expected.error);
            Assertions.assertEquals(1, expected.fieldSegments.size());
            Assertions.assertEquals(1, expected.segmentIndex);
            Assertions.assertEquals("element", expected.segments[expected.segmentIndex]);
            Assertions.assertEquals("ListOfString", expected.enclosingSchema.getName());
        }
        
        try {
            PrimaryKey invalidFieldDefinition = new PrimaryKey("UnknownType", "id");
            invalidFieldDefinition.getFieldPathIndex(writeEngine, 0);
            Assertions.fail("IllegalArgumentException expected");
        } catch (FieldPaths.FieldPathException expected) {
            Assertions.assertEquals(FieldPaths.FieldPathException.ErrorKind.NOT_BINDABLE, expected.error);
            Assertions.assertEquals(0, expected.fieldSegments.size());
            Assertions.assertEquals(0, expected.segmentIndex);
            Assertions.assertEquals("UnknownType", expected.rootType);
        }
    }
    
    
    @Test
    public void testAutoExpand() {
        { // verify fieldPath auto expand
            PrimaryKey autoExpandPK = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType");
            Assertions.assertEquals(FieldType.STRING, autoExpandPK.getFieldType(writeEngine, 0));
            Assertions.assertEquals(null, autoExpandPK.getFieldSchema(writeEngine, 0));
        }

        { // verify disabled fieldPath auto expand with ending "!" 
            PrimaryKey autoExpandPK = new PrimaryKey("TypeWithTraversablePrimaryKey", "subType!");
            Assertions.assertNotEquals(FieldType.STRING, autoExpandPK.getFieldType(writeEngine, 0));
            Assertions.assertEquals(FieldType.REFERENCE, autoExpandPK.getFieldType(writeEngine, 0));
            Assertions.assertEquals("SubTypeWithTraversablePrimaryKey", autoExpandPK.getFieldSchema(writeEngine, 0).getName());
        }
    }
    
    
    @HollowPrimaryKey(fields={"pk1", "subType", "intId"})
    private static class TypeWithTraversablePrimaryKey {
        String pk1;
        SubTypeWithTraversablePrimaryKey subType;
        SubTypeWithNonTraversablePrimaryKey subType2;
        int intId;
        List<String> strList;
    }
    
    @HollowPrimaryKey(fields="id")
    private static class SubTypeWithTraversablePrimaryKey {
        String id;
        int anotherField;
    }
    
    @HollowPrimaryKey(fields={"id1", "id2"})
    private static class SubTypeWithNonTraversablePrimaryKey {
        long id1;
        float id2;
    }

}
