package Utilities.Binaries;

import Models.Binary;

import java.nio.ByteBuffer;

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
        buffer.clear();
        return buffer.putLong(data).array();
    }

    @Override
    public void fromByteArray(byte[] bytes) {
        buffer.clear();
        buffer.put(bytes, 0, Math.min(bytes.length, Long.BYTES));
        buffer.flip();
        data = buffer.getLong();
    }
}
