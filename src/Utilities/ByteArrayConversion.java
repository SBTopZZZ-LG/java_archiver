package Utilities;

public class ByteArrayConversion {
    /**
     * Casts and returns a primitive byte array from a non-primitive byte array
     * @param bytes Non-primitive byte array
     * @return Primitive byte array
     */
    public static byte[] toPByteArray(Byte[] bytes) {
        final byte[] bytes2 = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            bytes2[i] = bytes[i];

        return bytes2;
    }

    /**
     * Casts and returns a non-primitive byte array from a primitive byte array
     * @param bytes Primitive byte array
     * @return Non-primitive byte array
     */
    public static Byte[] toByteArray(byte[] bytes) {
        final Byte[] bytes2 = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            bytes2[i] = bytes[i];

        return bytes2;
    }
}
