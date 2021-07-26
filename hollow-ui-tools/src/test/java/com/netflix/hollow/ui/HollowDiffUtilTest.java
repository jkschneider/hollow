package com.netflix.hollow.ui;

import static com.netflix.hollow.ui.HollowDiffUtil.formatBytes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HollowDiffUtilTest {

    @Test
    public void testFormatBytes() {
        sampleTesting(1,"B", -10, 2, 0, 2, 10);
        sampleTesting(Math.pow(2, 10),"KiB", -100, 50, 30, 100);
        sampleTesting(Math.pow(2, 20),"MiB", -100, 50, 30, 100);
        sampleTesting(Math.pow(2, 30),"GiB", -10, 30, 30, 100);
        sampleTesting(Math.pow(2, 40),"TiB", -100, 50, 30, 100);
        sampleTesting(Math.pow(2, 50),"PiB", -100, 50, 30, 100);

        Assertions.assertEquals( "-1,023 B", formatBytes(-1023));
        Assertions.assertEquals( "-1 KiB", formatBytes(-1024));

        Assertions.assertEquals( "1,000 TiB", formatBytes(1000 * (long)Math.pow(2, 40)));
        Assertions.assertEquals( "1 PiB", formatBytes(1024 * (long)Math.pow(2, 40)));

        Assertions.assertEquals( "8 EiB", formatBytes(Long.MAX_VALUE));

        // Validate Decimal
        Assertions.assertEquals( "95.37 MiB", formatBytes(100000000));
        Assertions.assertEquals( "-9.54 MiB", formatBytes(-10000000));
        Assertions.assertEquals( "1.95 KiB", formatBytes(2001));
        Assertions.assertEquals( "19.53 KiB", formatBytes(20000));
        Assertions.assertEquals( "186.26 GiB", formatBytes(200000000000L));
    }

    private void sampleTesting(double multiple, String unit, long ... bytes) {
        for(long b : bytes) {
            Assertions.assertEquals(b + " " + unit, formatBytes(b * (long)multiple));
        }
    }
}
