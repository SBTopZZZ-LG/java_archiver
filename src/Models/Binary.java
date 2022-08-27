package Models;

import Utilities.ByteArrayConversion;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class Binary extends SerializableObject {
    private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    private static final ByteBuffer staticBuffer = ByteBuffer.allocate(Long.BYTES);

    /**
     * Converts and returns size to byte array
     * @return Byte array
     */
    public byte[] sizeToByteArray() {
        buffer.putLong(0, getSize());
        return buffer.array();
    }

    /**
     * Converts and returns size to byte array
     * @param size size to convert
     * @return Byte array
     */
    public static byte[] sizeToByteArray(long size) {
        return staticBuffer.position(0).putLong(size).array();
    }

    /**
     * Converts and returns size from byte array
     * @param byteArray Byte array to convert
     * @return Size after conversion
     */
    public static long byteArrayToSize(Byte[] byteArray) {
        return staticBuffer.position(0).put(ByteArrayConversion.toPByteArray(byteArray), 0, 8).flip().getLong();
    }

    /**
     * Returns the size or length of the byte data
     * @return Size or length of byte data
     */
    public abstract int getSize();

    /**
     * Formats specified size in bytes into a string with appropriate sizing units
     * @param size Size in bytes
     * @return Formatted string
     */
    public static String getFormattedSize(long size) {
        if (size < 1024)
            return size + " bytes";
        if (size < 1048576)
            return (size / 1024) + " KB";
        if (size < 1073741824)
            return (size / 1048576) + " MB";
        if (size < Long.parseLong("1099511627776"))
            return (size / 1073741824) + " GB";
        return (size / Long.parseLong("1099511627776")) + " TB";
    }
}
