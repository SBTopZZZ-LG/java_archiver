package Utilities.Binaries;

import Models.Binary;
import Utilities.ByteArrayConversion;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BinaryLong extends Binary {
    public long data;
    private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    public BinaryLong() {}
    public BinaryLong(long data) {
        this.data = data;
    }

    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public byte[] toByteArray() {
        return buffer.putLong(0, data).array();
    }

    @Override
    public void fromByteArray(byte[] bytes) {
        data = buffer.position(0).put(0, ByteArrayConversion.toPByteArray(Arrays.copyOfRange(ByteArrayConversion.toByteArray(bytes), 8, 8 + getSize()))).getLong();
    }
}
