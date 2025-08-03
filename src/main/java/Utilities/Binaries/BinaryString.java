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
     */
    @Override
    public void fromByteArray(byte[] bytes) {
        data = new String(bytes, StandardCharsets.UTF_8);
    }
}
