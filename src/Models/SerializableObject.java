package Models;

public abstract class SerializableObject {
    /**
     * Returns the size in bytes occupied by the data
     * @return Size in bytes
     */
    public abstract int getSize();

    /**
     * Serializes the object instance into a byte array
     * @return Serialized object
     */
    public abstract byte[] toByteArray();

    /**
     * Deserializes byte array and updates the object data
     * @param bytes Byte array to deserialize
     * @param hasSizeBytes `true` if `bytes` includes the size bytes, otherwise `false`
     */
    public abstract void fromByteArray(final byte[] bytes, boolean hasSizeBytes);
}
