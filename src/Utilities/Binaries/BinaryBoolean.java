package Utilities.Binaries;

import Models.Binary;

public class BinaryBoolean extends Binary {
    public boolean data = false;

    public BinaryBoolean() {}
    public BinaryBoolean(boolean data) {
        this.data = data;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public byte[] toByteArray() {
        return new byte[]{(byte) (data ? 1 : 0)};
    }

    @Override
    public void fromByteArray(Byte[] bytes) {
        data = (int)bytes[8] == 1;
    }
}
