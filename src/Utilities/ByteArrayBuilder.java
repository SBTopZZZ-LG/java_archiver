package Utilities;

import java.util.ArrayList;
import java.util.List;

public class ByteArrayBuilder {
    final List<Byte> bytes;

    public ByteArrayBuilder() {
        bytes = new ArrayList<>();
    }

    /**
     * Appends single byte to the byte array
     * @param b Byte to append
     * @return ByteArrayBuilder instance
     */
    public ByteArrayBuilder appendByte(byte b) {
        bytes.add(b);
        return this;
    }

    /**
     * Appends specified sequence of byte to the byte array
     * @param bytes Sequence of byte
     * @return ByteArrayBuilder instance
     */
    public ByteArrayBuilder appendBytes(byte...bytes) {
        for (final byte b : bytes)
            this.bytes.add(b);
        return this;
    }

    /**
     * Appends specified sequence of bytes to the byte array
     * @param bytes Sequence of bytes
     * @return ByteArrayBuilder instance
     */
    public ByteArrayBuilder appendBytes(byte[]...bytes) {
        for (final byte[] b : bytes)
            appendBytes(b);
        return this;
    }

    /**
     * Finalises and returns the result byte array
     * @return Byte array
     */
    public byte[] toByteArray() {
        byte[] bytes = new byte[this.bytes.size()];
        int index = 0;
        for (final byte b : this.bytes)
            bytes[index++] = b;

        return bytes;
    }

    /**
     * Instantiates and returns a new ByteArrayBuilder instance
     * @return ByteArrayBuilder instance
     */
    public static ByteArrayBuilder build() {
        return new ByteArrayBuilder();
    }
}
