package Utilities;

import java.util.ArrayList;
import java.util.List;

public class ByteArrayBuilder {
    List<Byte> bytes;

    public ByteArrayBuilder() {
        bytes = new ArrayList<>();
    }

    public ByteArrayBuilder appendByte(byte b) {
        bytes.add(b);
        return this;
    }
    public ByteArrayBuilder appendBytes(byte...bytes) {
        for (final byte b : bytes)
            this.bytes.add(b);
        return this;
    }
    public ByteArrayBuilder appendBytes(byte[]...bytes) {
        for (final byte[] b : bytes)
            appendBytes(b);
        return this;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[this.bytes.size()];
        int index = 0;
        for (final byte b : this.bytes)
            bytes[index++] = b;

        return bytes;
    }

    public static ByteArrayBuilder build() {
        return new ByteArrayBuilder();
    }
}
