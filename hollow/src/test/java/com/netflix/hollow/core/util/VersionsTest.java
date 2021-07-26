package com.netflix.hollow.core.util;

import static com.netflix.hollow.core.HollowConstants.VERSION_LATEST;
import static com.netflix.hollow.core.HollowConstants.VERSION_NONE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionsTest {

    @Test
    public void testPrettyPrint() {
        Assertions.assertEquals(Versions.PRETTY_VERSION_NONE, Versions.prettyVersion(VERSION_NONE));
        Assertions.assertEquals(Versions.PRETTY_VERSION_LATEST, Versions.prettyVersion(VERSION_LATEST));
        Assertions.assertEquals("123", Versions.prettyVersion(123l));
    }

}
