/*
 *  Copyright 2021 Netflix, Inc.
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
package com.netflix.hollow.api.codegen.testdata;

import com.netflix.hollow.core.HollowDataset;
import com.netflix.hollow.core.schema.HollowListSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.core.schema.HollowObjectSchema.FieldType;
import com.netflix.hollow.core.schema.HollowSchema;
import com.netflix.hollow.core.schema.HollowSchema.SchemaType;

class HollowListTypeTestDataAPIClassGenerator {
    
    private final HollowDataset dataset;
    private final HollowListSchema schema;
    private final String packageName;
    private final String className;
    private final String elementClassName;
    
    public HollowListTypeTestDataAPIClassGenerator(HollowDataset dataset, HollowListSchema schema, String packageName) {
        this.dataset = dataset;
        this.schema = schema;
        this.packageName = packageName;
        this.className = schema.getName() + "TestData";
        this.elementClassName = schema.getElementType() + "TestData";
    }
    
    public String generate() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("package ").append(packageName).append(";\n\n");
        
        builder.append("import com.netflix.hollow.api.testdata.HollowTestListRecord;\n" + 
                       "import com.netflix.hollow.core.schema.HollowListSchema;\n\n");
        
        builder.append("public class ").append(className).append("<T> extends HollowTestListRecord<T> {\n\n");

        builder.append("    ").append(className).append("(T parent) {\n");
        builder.append("        super(parent);\n");
        builder.append("    }\n\n");
        
        String elementReturnType = elementClassName + "<" + className + "<T>>";
        
        builder.append("    public ").append(elementReturnType).append(" ").append(schema.getElementType()).append("() {\n");
        builder.append("        ").append(elementReturnType).append(" __e = new ").append(elementReturnType).append("(this);\n");
        builder.append("        super.addElement(__e);\n");
        builder.append("        return __e;\n");
        builder.append("    }\n\n");
        
        HollowSchema elementSchema = dataset.getSchema(schema.getElementType());
        if(elementSchema.getSchemaType() == SchemaType.OBJECT) {
            HollowObjectSchema elementObjSchema = (HollowObjectSchema)elementSchema;
            if(elementObjSchema.numFields() == 1 && elementObjSchema.getFieldType(0) != FieldType.REFERENCE) {
                switch(elementObjSchema.getFieldType(0)) {
                case INT:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(Integer value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                case LONG:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(Long value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                case FLOAT:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(Float value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                case DOUBLE:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(Double value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                case BOOLEAN:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(Boolean value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                case BYTES:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(byte[] value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                case STRING:
                    builder.append("    public ").append(className).append("<T> ").append(schema.getElementType()).append("(String value) {\n");
                    builder.append("        ").append(schema.getElementType()).append("().").append(elementObjSchema.getFieldName(0)).append("(value);\n");
                    builder.append("        return this;\n");
                    builder.append("    }\n\n");
                    break;
                default:
                    break;
                }
            }
        }

        builder.append("    private static final HollowListSchema SCHEMA = new HollowListSchema(\"").append(schema.getName()).append("\", \"").append(schema.getElementType()).append("\");\n\n");
        
        builder.append("    @Override public HollowListSchema getSchema() { return SCHEMA; }\n\n");

        builder.append("}");
        
        return builder.toString();
    }
    
}