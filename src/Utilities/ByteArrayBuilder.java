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
    
    /**
     * Converts an integer to 4 bytes in big-endian format
     * @param value Integer value
     * @return Byte array
     */
    public static byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }
    
    /**
     * Converts 4 bytes in big-endian format to an integer
     * @param bytes Byte array
     * @param offset Offset in the array
     * @return Integer value
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xff) << 24) |
               ((bytes[offset + 1] & 0xff) << 16) |
               ((bytes[offset + 2] & 0xff) << 8) |
               (bytes[offset + 3] & 0xff);
    }
}
