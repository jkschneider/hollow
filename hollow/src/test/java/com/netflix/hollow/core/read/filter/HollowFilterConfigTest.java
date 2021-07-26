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
package com.netflix.hollow.core.read.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HollowFilterConfigTest {

    @Test
    public void includeFilterSpecifiesTypesAndFields() {
        HollowFilterConfig conf = new HollowFilterConfig();

        conf.addType("TypeA");
        conf.addField("TypeB", "b1");
        conf.addField("TypeB", "b2");

        Assertions.assertTrue(conf.doesIncludeType("TypeA"));
        Assertions.assertTrue(conf.getObjectTypeConfig("TypeA").includesField("anyGivenField"));
        Assertions.assertTrue(conf.doesIncludeType("TypeB"));
        Assertions.assertTrue(conf.getObjectTypeConfig("TypeB").includesField("b1"));
        Assertions.assertFalse(conf.getObjectTypeConfig("TypeB").includesField("b3"));
        Assertions.assertFalse(conf.doesIncludeType("TypeC"));
        Assertions.assertFalse(conf.getObjectTypeConfig("TypeC").includesField("asdf"));
    }

    @Test
    public void excludeFilterSpecifiesTypesAndFields() {
        HollowFilterConfig conf = new HollowFilterConfig(true);

        conf.addType("TypeA");
        conf.addField("TypeB", "b1");
        conf.addField("TypeB", "b2");

        Assertions.assertFalse(conf.doesIncludeType("TypeA"));
        Assertions.assertFalse(conf.getObjectTypeConfig("TypeA").includesField("anyGivenField"));
        Assertions.assertTrue(conf.doesIncludeType("TypeB"));
        Assertions.assertFalse(conf.getObjectTypeConfig("TypeB").includesField("b1"));
        Assertions.assertTrue(conf.getObjectTypeConfig("TypeB").includesField("b3"));
        Assertions.assertTrue(conf.doesIncludeType("TypeC"));
        Assertions.assertTrue(conf.getObjectTypeConfig("TypeC").includesField("anyGivenField"));
    }

    @Test
    public void serializesAndDeserializes() {
        HollowFilterConfig conf = new HollowFilterConfig(true);

        conf.addType("TypeA");
        conf.addField("TypeB", "b1");
        conf.addField("TypeB", "b2");

        String configStr = conf.toString();
        conf = HollowFilterConfig.fromString(configStr);

        System.out.println(configStr);

        Assertions.assertFalse(conf.doesIncludeType("TypeA"));
        Assertions.assertFalse(conf.getObjectTypeConfig("TypeA").includesField("anyGivenField"));
        Assertions.assertTrue(conf.doesIncludeType("TypeB"));
        Assertions.assertFalse(conf.getObjectTypeConfig("TypeB").includesField("b1"));
        Assertions.assertTrue(conf.getObjectTypeConfig("TypeB").includesField("b3"));
        Assertions.assertTrue(conf.doesIncludeType("TypeC"));
        Assertions.assertTrue(conf.getObjectTypeConfig("TypeC").includesField("anyGivenField"));
    }
}
