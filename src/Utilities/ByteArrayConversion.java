package Utilities;

import java.util.List;

public class ByteArrayConversion {
    public static byte[] toPByteArray(Byte[] bytes) {
        final byte[] bytes2 = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            bytes2[i] = bytes[i];

        return bytes2;
    }

    public static Byte[] toByteArray(byte[] bytes) {
        final Byte[] bytes2 = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            bytes2[i] = bytes[i];

        return bytes2;
    }
}
