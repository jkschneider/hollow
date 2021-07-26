package com.netflix.hollow.core.memory.encoding;

import com.netflix.hollow.core.read.HollowBlobInput;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VarIntTest {
    private final static byte[] BYTES_EMPTY = new byte[]{};
    private final static byte[] BYTES_TRUNCATED = new byte[]{(byte) 0x81};
    private final static byte[] BYTES_VALUE_129 = new byte[]{(byte) 0x81, (byte) 0x01};

    @Test
    public void testReadVLongInputStream() throws IOException {
        InputStream is = new ByteArrayInputStream(BYTES_VALUE_129);

        Assertions.assertEquals(129, VarInt.readVLong(is));
    }

    @Test
    public void testReadVLongEmptyInputStream() throws IOException {
        assertThrows(EOFException.class, () -> {
            InputStream is = new ByteArrayInputStream(BYTES_EMPTY);

            VarInt.readVLong(is);
        });
    }

    @Test
    public void testReadVLongTruncatedInputStream() throws IOException {
        assertThrows(EOFException.class, () -> {
            InputStream is = new ByteArrayInputStream(BYTES_TRUNCATED);

            VarInt.readVLong(is);
        });
    }

    @Test
    public void testReadVIntInputStream() throws IOException {
        InputStream is = new ByteArrayInputStream(BYTES_VALUE_129);

        Assertions.assertEquals(129, VarInt.readVInt(is));
    }

    @Test
    public void testReadVIntEmptyInputStream() throws IOException {
        assertThrows(EOFException.class, () -> {
            InputStream is = new ByteArrayInputStream(BYTES_EMPTY);

            VarInt.readVInt(is);
        });
    }

    @Test
    public void testReadVIntTruncatedInputStream() throws IOException {
        assertThrows(EOFException.class, () -> {
            InputStream is = new ByteArrayInputStream(BYTES_TRUNCATED);

            VarInt.readVInt(is);
        });
    }

    @Test
    public void testReadVLongHollowBlobInput() throws IOException {
        HollowBlobInput hbi = HollowBlobInput.serial(BYTES_VALUE_129);

        Assertions.assertEquals(129l, VarInt.readVLong(hbi));
    }

    @Test
    public void testReadVLongEmptyHollowBlobInput() throws IOException {
        assertThrows(EOFException.class, () -> {
            HollowBlobInput hbi = HollowBlobInput.serial(BYTES_EMPTY);

            VarInt.readVLong(hbi);
        });
    }

    @Test
    public void testReadVLongTruncatedHollowBlobInput() throws IOException {
        assertThrows(EOFException.class, () -> {
            HollowBlobInput hbi = HollowBlobInput.serial(BYTES_TRUNCATED);

            VarInt.readVLong(hbi);
        });
    }

    @Test
    public void testReadVIntHollowBlobInput() throws IOException {
        HollowBlobInput hbi = HollowBlobInput.serial(BYTES_VALUE_129);

        Assertions.assertEquals(129l, VarInt.readVInt(hbi));
    }

    @Test
    public void testReadVIntEmptyHollowBlobInput() throws IOException {
        assertThrows(EOFException.class, () -> {
            HollowBlobInput hbi = HollowBlobInput.serial(BYTES_EMPTY);

            VarInt.readVInt(hbi);
        });
    }

    @Test
    public void testReadVIntTruncatedHollowBlobInput() throws IOException {
        assertThrows(EOFException.class, () -> {
            HollowBlobInput hbi = HollowBlobInput.serial(BYTES_TRUNCATED);

            VarInt.readVInt(hbi);
        });
    }
}
