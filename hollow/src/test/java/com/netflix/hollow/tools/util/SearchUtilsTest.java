package com.netflix.hollow.tools.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.hollow.core.index.key.PrimaryKey;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import com.netflix.hollow.core.schema.HollowObjectSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SearchUtilsTest {

    @Mock
    HollowReadStateEngine stateEngine;

    @Mock
    PrimaryKey primaryKey;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(primaryKey.numFields()).thenReturn(2);
        when(primaryKey.getFieldType(eq(stateEngine), anyInt())).thenReturn(HollowObjectSchema.FieldType.STRING);
    }

    @Test
    public void testParseKey() {
        String keyString = "a:b";
        Object[] key = SearchUtils.parseKey(stateEngine, primaryKey, keyString);
        assertEquals(2, key.length);
        assertEquals("a", key[0]);
        assertEquals("b", key[1]);

        // two fields, where the second field contains a ':' char
        // NOTE that this split based on delimiter works even without escaping the delimiter because
        // string split is performed based on no. of fields in the key. So if delimiter exists in the
        // last field then the parsing logic doesn't break
        keyString = "a:b1:b2";
        key = SearchUtils.parseKey(stateEngine, primaryKey, keyString);
        assertEquals(2, key.length);
        assertEquals("a", key[0]);
        assertEquals("b1:b2", key[1]);

        // again two fields, where the second field contains a ':' char
        keyString = "a:b1\\:b2";
        key = SearchUtils.parseKey(stateEngine, primaryKey, keyString);
        assertEquals(2, key.length);
        assertEquals("a", key[0]);
        assertEquals("b1:b2", key[1]);

        // two fields, where the first field contains a ':' char
        keyString = "a1\\:a2:b";
        key = SearchUtils.parseKey(stateEngine, primaryKey, keyString);
        assertEquals(2, key.length);
        assertEquals("a1:a2", key[0]);
        assertEquals("b", key[1]);
    }

}
