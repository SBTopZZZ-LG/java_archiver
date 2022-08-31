package Utilities.Binaries;

import Models.Binary;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BinaryString extends Binary {
    public String data;

    public BinaryString() {}
    public BinaryString(String data) {
        this.data = data;
    }

    @Override
    public int getSize() {
        return data.length();
    }

    @Override
    public byte[] toByteArray() {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Deserializes byte array and updates the object data
     * @param bytes Byte array to deserialize
     * @param hasSizeBytes If byte array is a segment and includes the length
     */
    public void fromByteArray(byte[] bytes, boolean hasSizeBytes) {
        data = new String(Arrays.copyOfRange(bytes, hasSizeBytes ? 8 : 0, hasSizeBytes ? (int) (byteArrayToSize(bytes) + 8) : bytes.length), StandardCharsets.UTF_8);
    }
    @Override
    public void fromByteArray(byte[] bytes) {
        fromByteArray(bytes, true);
    }
}
