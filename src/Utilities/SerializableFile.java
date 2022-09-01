package Utilities;

import Models.Binary;
import Models.SerializableObject;
import Utilities.Binaries.BinaryBoolean;
import Utilities.Binaries.BinaryLong;
import Utilities.Binaries.BinaryString;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class SerializableFile extends SerializableObject {
    public BinaryString name, path;
    public BinaryBoolean canRead, canExecute, canWrite;
    public BinaryLong lastModified, size;

    public SerializableFile() {
        name = new BinaryString();
        path = new BinaryString();

        canRead = new BinaryBoolean();
        canExecute = new BinaryBoolean();
        canWrite = new BinaryBoolean();

        lastModified = new BinaryLong();
        size = new BinaryLong();
    }

    /**
     * Initializes a new SerializableFile instance and retrieves all details from the file
     * @param path File path
     * @param relativePath File relative path
     * @exception InvalidPathException Thrown if specified file does not exist or is invalid
     */
    public SerializableFile(String path, String relativePath) throws InvalidPathException {
        File file = new File(path);
        if (!Files.exists(Path.of(path)) || file.isDirectory())
            throw new InvalidPathException(path, "Path does not exist or is a directory");

        name = new BinaryString(file.getName());
        this.path = new BinaryString(relativePath != null ? relativePath : path);

        canRead = new BinaryBoolean(file.canRead());
        canExecute = new BinaryBoolean(file.canExecute());
        canWrite = new BinaryBoolean(file.canWrite());

        lastModified = new BinaryLong(file.lastModified());
        size = new BinaryLong(file.length());
    }

    public interface CreateFileCallback {
        void writeBinaryData(final File file);
    }

    /**
     * Creates a new file at desired path and adds the necessary attributes
     * @param callback Callback (to write file binary data)
     * @return `true` if successful, otherwise `false`
     */
    public boolean createFile(CreateFileCallback callback) throws IOException {
        {
            // Create path if it does not exist
            File temp = new File(path.data);
            if (!temp.exists())
                temp.mkdirs();

            // Create file (or overwrite if already exists)
            if (temp.exists())
                if (!temp.delete())
                    return false;
            if (!temp.createNewFile())
                return false;
        }

        File file = new File(path.data);

        callback.writeBinaryData(file);

        // Write metadata in the end to avoid permission(s) error (when canWrite.data is False)
        file.setReadable(canRead.data);
        file.setExecutable(canExecute.data);
        file.setWritable(canWrite.data);
        file.setLastModified(lastModified.data);

        return true;
    }

    @Override
    public int getSize() {
        return name.getSize()
                + path.getSize()
                + canRead.getSize()
                + canExecute.getSize()
                + canWrite.getSize()
                + lastModified.getSize()
                + size.getSize()
                + 2 * 2; // Additional bytes that represent lengths for each metadata (name and path)
    }

    /**
     * Serializes the object into a ByteArray
     * <li>Name length (Max. of 260 bytes for DOS, and 255 for UNIX) (2 bytes) + Name</li>
     * <li>Path length (Max. of 32,000 bytes for NTFS, and 4,096 for UNIX) (2 bytes) + Path</li>
     * <li>Can read (1 byte)</li>
     * <li>Can execute (1 byte)</li>
     * <li>Can write (1 byte)</li>
     * <li>Last modified (8 bytes)</li>
     * <li>File binary size (8 bytes)</li>
     * Metadata size >= 23 bytes (2 * 2 + 1 * 3 + 8 * 2)
     *
     * @return Serialized object
     */
    @Override
    public byte[] toByteArray() {
        return ByteArrayBuilder.build()
                .appendBytes(name.sizeToByteArray(Binary.SizeType.SHORT), name.toByteArray()) // Name
                .appendBytes(path.sizeToByteArray(Binary.SizeType.SHORT), path.toByteArray()) // Path
                .appendBytes(canRead.toByteArray()) // Can read
                .appendBytes(canExecute.toByteArray()) // Can execute
                .appendBytes(canWrite.toByteArray()) // Can write
                .appendBytes(lastModified.toByteArray()) // Last modified
                .appendBytes(size.toByteArray()) // Size
                .toByteArray();
    }

    /**
     * Deserializes byte array and updates the object data
     * @param byteStream Byte stream to deserialize
     */
    public void fromByteArray(BufferedInputStream byteStream) {
        BufferedInputStream bis = new BufferedInputStream(byteStream);

        int segmentSize;
        byte[] shortSegmentSizeBytes = new byte[2];
        try {
            // Name (2 + x bytes)
            bis.readNBytes(shortSegmentSizeBytes, 0, 2);
            segmentSize = (int) Binary.byteArrayToSize(shortSegmentSizeBytes, Binary.SizeType.SHORT);

            name.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(shortSegmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize))
                            .toByteArray()
            );
            // Name

            // Path (2 + x bytes)
            bis.readNBytes(shortSegmentSizeBytes, 0, 2);
            segmentSize = (int) Binary.byteArrayToSize(shortSegmentSizeBytes, Binary.SizeType.SHORT);

            path.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(shortSegmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize))
                            .toByteArray()
            );
            // Path

            // Can read (1 byte)
            canRead.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(bis.readNBytes(1))
                            .toByteArray()
            );
            // Can read

            // Can execute (1 byte)
            canExecute.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(bis.readNBytes(1))
                            .toByteArray()
            );
            // Can execute

            // Can write (1 byte)
            canWrite.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(bis.readNBytes(1))
                            .toByteArray()
            );
            // Can write

            // Last modified (8 bytes)
            lastModified.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(bis.readNBytes(8))
                            .toByteArray()
            );
            // Last modified

            // Size (8 bytes)
            size.fromByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(bis.readNBytes(8))
                            .toByteArray()
            );
            // Size
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromByteArray(byte[] bytes) {
        fromByteArray(new BufferedInputStream(new ByteArrayInputStream(bytes)));
    }
}
