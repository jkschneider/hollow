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
package com.netflix.hollow.api.consumer.index;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.InMemoryBlobStore;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.api.objects.HollowRecord;
import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.fs.HollowInMemoryBlobStager;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UniqueKeyIndexTest {
    // Map of primitive class to box class
    static final Map<Class<?>, Class<?>> primitiveClasses;
    static {
        primitiveClasses = new HashMap<>();
        primitiveClasses.put(boolean.class, Boolean.class);
        primitiveClasses.put(byte.class, Byte.class);
        primitiveClasses.put(short.class, Short.class);
        primitiveClasses.put(char.class, Character.class);
        primitiveClasses.put(int.class, Integer.class);
        primitiveClasses.put(long.class, Long.class);
        primitiveClasses.put(float.class, Float.class);
        primitiveClasses.put(double.class, Double.class);
    }

    static HollowConsumer consumer;

    static DataModel.Consumer.Api api;

    @BeforeAll
    public static void setup() {
        InMemoryBlobStore blobStore = new InMemoryBlobStore();

        HollowProducer producer = HollowProducer.withPublisher(blobStore)
                .withBlobStager(new HollowInMemoryBlobStager())
                .build();

        long v1 = producer.runCycle(ws -> {
            ws.add(new DataModel.Producer.References());

            for (int i = 0; i < 100; i++) {
                ws.add(new DataModel.Producer.TypeA(1, "TypeA" + i));
            }
            ws.add(new DataModel.Producer.TypeWithPrimaryKey2(1));
        });

        consumer = HollowConsumer.withBlobRetriever(blobStore)
                .withGeneratedAPIClass(DataModel.Consumer.Api.class)
                .build();
        consumer.triggerRefreshTo(v1);

        api = consumer.getAPI(DataModel.Consumer.Api.class);
    }

    public static abstract class MatchTestParameterized<T extends HollowObject, Q> extends UniqueKeyIndexTest {
         String path;
         Class<Q> type;
         Q value;
         Class<T> uniqueType;

        public void initMatchOnValuesTest(String path, Class<Q> type, Q value, Class<T> uniqueType) {
            this.path = path;
            this.type = type;
            this.value = value;
            this.uniqueType = uniqueType;
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void test(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            UniqueKeyIndex<T, Q> pki = UniqueKeyIndex
                    .from(consumer, uniqueType)
                    .usingPath(path, type);

            T r = pki.findMatch(value);

            Assertions.assertNotNull(r);
            Assertions.assertEquals(0, r.getOrdinal());
        }
    }

    // path, type, value
    static List<Object[]> valuesDataProvider() {
        DataModel.Producer.Values values = new DataModel.Producer.Values();
        return Stream.of(DataModel.Producer.Values.class.getDeclaredFields())
                .flatMap(f -> {
                    String path = f.getName();
                    Class<?> type = f.getType();
                    Object value;
                    try {
                        value = f.get(values);
                    } catch (IllegalAccessException e) {
                        throw new InternalError();
                    }

                    Object[] args = new Object[] {path, type, value};
                    if (type.isPrimitive()) {
                        return Stream.of(args,
                                new Object[] {path, primitiveClasses.get(type), value}
                        );
                    } else {
                        return Stream.<Object[]>of(args);
                    }
                })
                .collect(toList());
    }

    public static class MatchOnValuesTest<Q> extends MatchTestParameterized<DataModel.Consumer.Values, Q> {
        // path[type] = value
        public static Collection<Object[]> data() {
            return valuesDataProvider();
        }

        public void initMatchOnValuesTest(String path, Class<Q> type, Q value) {
            super(path, type, value, DataModel.Consumer.Values.class);
        }
    }

    public static class MatchOnValuesIllegalTypeTest extends UniqueKeyIndexTest {
        // path[type] = value
        public static Collection<Object[]> data() {
            return valuesDataProvider();
        }

         String path;
         Class<?> type;
         Object value;

        public void initMatchOnValuesTest(String path, Class<?> type, Object value) {
            this.path = path;
            this.type = type;
            this.value = value;
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void test(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            assertThrows(IllegalArgumentException.class, () -> {
                UniqueKeyIndex
                        .from(consumer, DataModel.Consumer.Values.class)
                        .usingPath(path, Object.class);
            });
        }
    }

    public static class MatchOnValuesBeanTest extends UniqueKeyIndexTest {
        static class ValueFieldsQuery {
            @FieldPath
            boolean _boolean;
            @FieldPath
            byte _byte;
            @FieldPath
            short _short;
            @FieldPath
            char _char;
            @FieldPath
            int _int;
            @FieldPath
            long _long;
            @FieldPath
            float _float;
            @FieldPath
            double _double;
            @FieldPath
            byte[] _bytes;
            @FieldPath
            char[] _chars;

            void initMatchOnValuesTest(DataModel.Producer.Values v) {
                this._boolean = v._boolean;
                this._byte = v._byte;
                this._short = v._short;
                this._char = v._char;
                this._int = v._int;
                this._long = v._long;
                this._float = v._float;
                this._double = v._double;
                this._bytes = v._bytes.clone();
                this._chars = v._chars.clone();
            }

            static MatchOnValuesBeanTest.ValueFieldsQuery create() {
                return new MatchOnValuesBeanTest.ValueFieldsQuery(new DataModel.Producer.Values());
            }
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testFields(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            UniqueKeyIndex<DataModel.Consumer.Values, ValueFieldsQuery> hi = UniqueKeyIndex
                    .from(consumer, DataModel.Consumer.Values.class)
                    .usingBean(MatchOnValuesBeanTest.ValueFieldsQuery.class);

            DataModel.Consumer.Values r = hi.findMatch(MatchOnValuesBeanTest.ValueFieldsQuery.create());

            Assertions.assertNotNull(r);
            Assertions.assertEquals(0, r.getOrdinal());
        }

        static class ValueMethodsQuery {
            boolean _boolean;
            byte _byte;
            short _short;
            char _char;
            int _int;
            long _long;
            float _float;
            double _double;
            byte[] _bytes;
            char[] _chars;

            @FieldPath("_boolean")
            boolean is_boolean() {
                return _boolean;
            }

            @FieldPath("_byte")
            byte get_byte() {
                return _byte;
            }

            @FieldPath("_short")
            short get_short() {
                return _short;
            }

            @FieldPath("_char")
            char get_char() {
                return _char;
            }

            @FieldPath("_int")
            int get_int() {
                return _int;
            }

            @FieldPath("_long")
            long get_long() {
                return _long;
            }

            @FieldPath("_float")
            float get_float() {
                return _float;
            }

            @FieldPath("_double")
            double get_double() {
                return _double;
            }

            @FieldPath("_bytes")
            byte[] get_bytes() {
                return _bytes;
            }

            @FieldPath("_chars")
            char[] get_chars() {
                return _chars;
            }

            void initMatchOnValuesTest(DataModel.Producer.Values v) {
                this._boolean = v._boolean;
                this._byte = v._byte;
                this._short = v._short;
                this._char = v._char;
                this._int = v._int;
                this._long = v._long;
                this._float = v._float;
                this._double = v._double;
                this._bytes = v._bytes.clone();
                this._chars = v._chars.clone();
            }

            static MatchOnValuesBeanTest.ValueMethodsQuery create() {
                return new MatchOnValuesBeanTest.ValueMethodsQuery(new DataModel.Producer.Values());
            }
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testMethods(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            UniqueKeyIndex<DataModel.Consumer.Values, ValueMethodsQuery> hi = UniqueKeyIndex
                    .from(consumer, DataModel.Consumer.Values.class)
                    .usingBean(MatchOnValuesBeanTest.ValueMethodsQuery.class);

            DataModel.Consumer.Values r = hi.findMatch(MatchOnValuesBeanTest.ValueMethodsQuery.create());

            Assertions.assertNotNull(r);
            Assertions.assertEquals(0, r.getOrdinal());
        }
    }


    // path, type, value
    static List<Object[]> boxesDataProvider() {
        DataModel.Producer.Boxes values = new DataModel.Producer.Boxes();
        return Stream.of(DataModel.Producer.Boxes.class.getDeclaredFields())
                .map(f -> {
                    // Path will be auto-expanded to append ".value"
                    String path = f.getName();
                    Class<?> type = f.getType();
                    Object value;
                    try {
                        value = f.get(values);
                    } catch (IllegalAccessException e) {
                        throw new InternalError();
                    }

                    return new Object[] {path, type, value};
                })
                .collect(toList());
    }

    public static class MatchOnBoxesValuesTest<Q> extends MatchTestParameterized<DataModel.Consumer.Boxes, Q> {
        // path[type] = value
        public static Collection<Object[]> data() {
            return boxesDataProvider();
        }

        public void initMatchOnValuesTest(String path, Class<Q> type, Q value) {
            super(path, type, value, DataModel.Consumer.Boxes.class);
        }
    }


    static List<Object[]> inlineBoxesDataProvider() {
        DataModel.Producer.InlineBoxes values = new DataModel.Producer.InlineBoxes();
        return Stream.of(DataModel.Producer.InlineBoxes.class.getDeclaredFields())
                .map(f -> {
                    String path = f.getName();
                    Class<?> type = f.getType();
                    Object value;
                    try {
                        value = f.get(values);
                    } catch (IllegalAccessException e) {
                        throw new InternalError();
                    }

                    return new Object[] {path, type, value};
                })
                .collect(toList());
    }

    public static class MatchOnInlineBoxesTest<Q> extends
            MatchTestParameterized<DataModel.Consumer.InlineBoxes, Q> {
        // path[type] = value
        public static Collection<Object[]> data() {
            return inlineBoxesDataProvider();
        }

        public void initMatchOnValuesTest(String path, Class<Q> type, Q value) {
            super(path, type, value, DataModel.Consumer.InlineBoxes.class);
        }
    }

    public static class MatchOnMappedReferencesTest<Q>
            extends MatchTestParameterized<DataModel.Consumer.MappedReferencesToValues, Q> {
        // path[type] = value
        public static Collection<Object[]> data() {
            return Arrays.<Object[]>asList(
                    new Object[] {"date.value", long.class, 0L},
                    new Object[] {"number._name", String.class, "ONE"}
            );
        }

        public void initMatchOnValuesTest(String path, Class<Q> type, Q value) {
            super(path, type, value, DataModel.Consumer.MappedReferencesToValues.class);
        }
    }

    public static class MatchOnMappedReferencesNoAutoExpansionTest<Q extends HollowRecord> extends UniqueKeyIndexTest {
        public static Collection<Object[]> data() {
            return Arrays.asList(
                    args("values!", DataModel.Consumer.Values.class,
                            () -> api.getValues(0)),
                    args("boxes._string!", DataModel.Consumer.HString.class,
                            () -> api.getHString(0)),
                    args("referenceWithStrings!", DataModel.Consumer.ReferenceWithStringsRenamed.class,
                            () -> api.getReferenceWithStringsRenamed(0)),
                    args("referenceWithStrings._string1!", DataModel.Consumer.HString.class,
                            () -> api.getHString(0)),
                    args("referenceWithStrings._string2!", DataModel.Consumer.FieldOfStringRenamed.class,
                            () -> api.getFieldOfStringRenamed(0))
            );
        }

        static <Q extends HollowRecord> Object[] args(String path, Class<Q> type, Supplier<Q> s) {
            return new Object[] {path, type, s};
        }

         String path;
         Class<Q> type;
         Q value;

        public void initMatchOnValuesTest(String path, Class<Q> type, Supplier<Q> value) {
            this.path = path;
            this.type = type;
            this.value = value.get();
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void test(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            UniqueKeyIndex<DataModel.Consumer.References, Q> uki = UniqueKeyIndex
                    .from(consumer, DataModel.Consumer.References.class)
                    .usingPath(path, type);

            DataModel.Consumer.References r = uki.findMatch(value);

            Assertions.assertNotNull(r);
            Assertions.assertEquals(0, r.getOrdinal());
        }
    }


    public static class ErrorsTest extends UniqueKeyIndexTest {
        static class Unknown extends HollowObject {

            void initMatchOnValuesTest(HollowObjectDelegate delegate, int ordinal) {
                super(delegate, ordinal);
            }
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testUnknownRootSelectType(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            assertThrows(IllegalArgumentException.class, () -> {
                UniqueKeyIndex
                        .from(consumer, ErrorsTest.Unknown.class)
                        .usingPath("values", DataModel.Consumer.Values.class);
            });
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testEmptyMatchPath(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            assertThrows(IllegalArgumentException.class, () -> {
                UniqueKeyIndex
                        .from(consumer, DataModel.Consumer.References.class)
                        .usingPath("", DataModel.Consumer.References.class);
            });
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testNoPrimaryKey(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            assertThrows(IllegalArgumentException.class, () -> {
                UniqueKeyIndex
                        .from(consumer, DataModel.Consumer.References.class)
                        .bindToPrimaryKey()
                        .usingPath("values._int", int.class);
            });
        }
    }


    public static class PrimaryKeyDeclarationTest extends UniqueKeyIndexTest {
        // This class declares fields in the same order as those declared in
        // the @HollowPrimaryKey on TypeWithPrimaryKey
        static class KeyTypeSameOrder {
            @FieldPath("i")
            int i;
            @FieldPath("sub1.s")
            String sub1_s;
            @FieldPath("sub2.i")
            int sub2_i;

            void initMatchOnValuesTest(int i, String sub1_s, int sub2_i) {
                this.i = i;
                this.sub1_s = sub1_s;
                this.sub2_i = sub2_i;
            }
        }

        // This class declares fields in the reverse order as those declared in
        // the @HollowPrimaryKey on TypeWithPrimaryKey
        static class KeyTypeReverseOrder {
            @FieldPath("sub2.i")
            int sub2_i;
            @FieldPath("sub1.s")
            String sub1_s;
            @FieldPath("i")
            int i;

            void initMatchOnValuesTest(int i, String sub1_s, int sub2_i) {
                this.i = i;
                this.sub1_s = sub1_s;
                this.sub2_i = sub2_i;
            }
        }

        static class KeyWithMissingPath {
            @FieldPath("i")
            int i;
            @FieldPath("sub1.s")
            String sub1_s;
            int sub2_i;

            void initMatchOnValuesTest(int i, String sub1_s, int sub2_i) {
                this.i = i;
                this.sub1_s = sub1_s;
                this.sub2_i = sub2_i;
            }
        }

        static class KeyWithWrongPath {
            @FieldPath("i")
            int i;
            @FieldPath("sub1.s")
            String sub1_s;
            @FieldPath("sub2.s")
            String sub2_s;

            void initMatchOnValuesTest(int i, String sub1_s, String sub2_s) {
                this.i = i;
                this.sub1_s = sub1_s;
                this.sub2_s = sub2_s;
            }
        }

        static class KeyWithSinglePath {
            @FieldPath("i")
            int i;

            void initMatchOnValuesTest(int i) {
                this.i = i;
            }
        }

        public <T> void test(Class<T> keyType, T key) {
            UniqueKeyIndex<DataModel.Consumer.TypeWithPrimaryKey, T> pki = UniqueKeyIndex
                    .from(consumer, DataModel.Consumer.TypeWithPrimaryKey.class)
                    .bindToPrimaryKey()
                    .usingBean(keyType);

            DataModel.Consumer.TypeWithPrimaryKey match = pki.findMatch(key);
            Assertions.assertNotNull(match);
            Assertions.assertEquals(0, match.getOrdinal());
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testSameOrder(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            test(KeyTypeSameOrder.class, new KeyTypeSameOrder(1, "1", 2));
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testWithHollowTypeName(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            UniqueKeyIndex<DataModel.Consumer.TypeWithPrimaryKeySuffixed, Integer> pki =
                    UniqueKeyIndex.from(consumer, DataModel.Consumer.TypeWithPrimaryKeySuffixed.class)
                            .bindToPrimaryKey()
                            .usingPath("i", Integer.class);

            DataModel.Consumer.TypeWithPrimaryKeySuffixed match = pki.findMatch(1);
            Assertions.assertNotNull(match);
            Assertions.assertEquals(0, match.getOrdinal());

            UniqueKeyIndex<DataModel.Consumer.TypeWithPrimaryKeySuffixed, KeyWithSinglePath> pki2 =
                    UniqueKeyIndex.from(consumer, DataModel.Consumer.TypeWithPrimaryKeySuffixed.class)
                            .bindToPrimaryKey()
                            .usingBean(KeyWithSinglePath.class);
            match = pki2.findMatch(new KeyWithSinglePath(1));
            Assertions.assertNotNull(match);
            Assertions.assertEquals(0, match.getOrdinal());
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testReverseOrder(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            test(KeyTypeReverseOrder.class, new KeyTypeReverseOrder(1, "1", 2));
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testMissingPath(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            assertThrows(IllegalArgumentException.class, () -> {
                test(KeyWithMissingPath.class, new KeyWithMissingPath(1, "1", 2));
            });
        }

        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @MethodSource("data")
        @ParameterizedTest(name = "{index}: {0}[{1}] = {2}")
        public void testWrongPath(String path, Class<Q> type, Supplier<Q> value) {
            initMatchOnMappedReferencesNoAutoExpansionTest(path, type, value);
            initMatchOnMappedReferencesTest(path, type, value);
            initMatchOnInlineBoxesTest(path, type, value);
            initMatchOnBoxesValuesTest(path, type, value);
            initMatchOnValuesIllegalTypeTest(path, type, value);
            initMatchOnValuesTest(path, type, value);
            assertThrows(IllegalArgumentException.class, () -> {
                test(KeyWithWrongPath.class, new KeyWithWrongPath(1, "1", "2"));
            });
        }
    }
}
