package Models;

import java.nio.ByteBuffer;

public abstract class Binary extends SerializableObject {
    private final ByteBuffer shortBuffer = ByteBuffer.allocate(Short.BYTES);
    private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
    private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

    private static final ByteBuffer shortStaticBuffer = ByteBuffer.allocate(Short.BYTES);
    private static final ByteBuffer intStaticBuffer = ByteBuffer.allocate(Integer.BYTES);
    private static final ByteBuffer longStaticBuffer = ByteBuffer.allocate(Long.BYTES);

    public enum SizeType {
        LONG,
        INTEGER,
        SHORT
    }
    /**
     * Converts and returns size to byte array
     * @param sizeType Size data type
     * @return Byte array
     */
    public byte[] sizeToByteArray(SizeType sizeType) {
        if (sizeType == SizeType.LONG) {
            longBuffer.clear();
            longBuffer.putLong(getSize());
            return longBuffer.array();
        } else if (sizeType == SizeType.INTEGER) {
            intBuffer.clear();
            intBuffer.putInt(getSize());
            return intBuffer.array();
        } else if (sizeType == SizeType.SHORT) {
            shortBuffer.clear();
            shortBuffer.putShort((short) getSize());
            return shortBuffer.array();
        }
        return null;
    }
    /**
     * Converts and returns size from byte array
     * @param byteArray Byte array to convert
     * @param sizeType Size data type
     * @return Size after conversion
     */
    public static long byteArrayToSize(byte[] byteArray, SizeType sizeType) {
        if (sizeType == SizeType.LONG) {
            longStaticBuffer.clear();
            longStaticBuffer.put(byteArray, 0, Math.min(byteArray.length, Long.BYTES));
            longStaticBuffer.flip();
            return longStaticBuffer.getLong();
        } else if (sizeType == SizeType.INTEGER) {
            intStaticBuffer.clear();
            intStaticBuffer.put(byteArray, 0, Math.min(byteArray.length, Integer.BYTES));
            intStaticBuffer.flip();
            return intStaticBuffer.getInt();
        } else if (sizeType == SizeType.SHORT) {
            shortStaticBuffer.clear();
            shortStaticBuffer.put(byteArray, 0, Math.min(byteArray.length, Short.BYTES));
            shortStaticBuffer.flip();
            return shortStaticBuffer.getShort();
        }

        return 0;
    }

    /**
     * Converts and returns size to byte array
     * @param size size to convert
     * @return Byte array
     */
    public static byte[] sizeToByteArray(long size) {
        longStaticBuffer.clear();
        longStaticBuffer.putLong(size);
        return longStaticBuffer.array();
    }

    /**
     * Converts and returns size from byte array
     * @param byteArray Byte array to convert
     * @return Size after conversion
     */
    public static long byteArrayToSize(byte[] byteArray) {
        longStaticBuffer.clear();
        longStaticBuffer.put(byteArray, 0, Math.min(byteArray.length, Long.BYTES));
        longStaticBuffer.flip();
        return longStaticBuffer.getLong();
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
            try {
                return ((float)size / 1073741824) + " GB";
            } catch (Exception ignored) {
                return (size / 1073741824) + " GB";
            }

        try {
            return ((float)size / Long.parseLong("1099511627776")) + " TB";
        } catch (Exception ignored) {
            return (size / Long.parseLong("1099511627776")) + " TB";
        }
    }
}
