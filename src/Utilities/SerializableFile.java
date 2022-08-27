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
    public SerializableFile(String path, String relativePath) throws IOException {
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

    public boolean createFile(byte[] binaryData) throws IOException {
        {
            // Create path if it does not exist
            File temp = new File(path.data);
            if (!temp.exists())
                temp.mkdirs();
        }
        {
            // Create file (or overwrite if already exists)
            File temp = new File(path.data);
            if (temp.exists())
                temp.delete();
            if (!temp.createNewFile())
                return false;
        }

        File file = new File(path.data);
        file.setReadable(canRead.data);
        file.setExecutable(canExecute.data);
        file.setWritable(canWrite.data);
        file.setLastModified(lastModified.data);

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(binaryData);

        fos.flush();
        fos.close();

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
                + 8 * 7; // Additional bytes that represent lengths for each metadata
    }

    @Override
    public byte[] toByteArray() {
        return ByteArrayBuilder.build()
                .appendBytes(name.sizeToByteArray(), name.toByteArray())                    // Name
                .appendBytes(path.sizeToByteArray(), path.toByteArray())                    // Path
                .appendBytes(canRead.sizeToByteArray(), canRead.toByteArray())              // Can read
                .appendBytes(canExecute.sizeToByteArray(), canExecute.toByteArray())        // Can execute
                .appendBytes(canWrite.sizeToByteArray(), canWrite.toByteArray())            // Can write
                .appendBytes(lastModified.sizeToByteArray(), lastModified.toByteArray())    // Last modified
                .appendBytes(size.sizeToByteArray(), size.toByteArray())                    // Size
                .toByteArray();
    }
    public void fromByteArray(BufferedInputStream byteStream) {
        BufferedInputStream bis = new BufferedInputStream(byteStream);

        int segmentSize;
        byte[] segmentSizeBytes = new byte[8];
        try {
            // Name
//            System.out.println("Reading name... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

            name.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()));

//            System.out.println(name.data);
            // Name

            // Path
//            System.out.print("Reading path... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

            path.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()));

//            System.out.println(path.data);
            // Path

            // Can read
//            System.out.print("Reading Can read... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

            canRead.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()));

//            System.out.println(canRead.data);
            // Can read

            // Can execute
//            System.out.print("Reading Can execute... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

            canExecute.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()));

//            System.out.println(canExecute.data);
            // Can execute

            // Can write
//            System.out.print("Reading Can write... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

            canWrite.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()));

//            System.out.println(canWrite.data);
            // Can write

            // Last modified
//            System.out.print("Reading Last modified... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

//            System.out.println("Available bytes is " + bis.available());
            lastModified.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()
            ));

//            System.out.println(lastModified.data);
            // Last modified

            // Size
//            System.out.print("Reading size... ");

            segmentSize = 8;
            bis.readNBytes(segmentSizeBytes, 0, segmentSize);
            segmentSize += Binary.byteArrayToSize(ByteArrayConversion.toByteArray(segmentSizeBytes));

            size.fromByteArray(ByteArrayConversion.toByteArray(
                    ByteArrayBuilder.build()
                            .appendBytes(segmentSizeBytes)
                            .appendBytes(bis.readNBytes(segmentSize - 8))
                            .toByteArray()
            ));

//            System.out.println(size.data);
            // Size
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromByteArray(Byte[] bytes) {
        fromByteArray(new BufferedInputStream(new ByteArrayInputStream(ByteArrayConversion.toPByteArray(bytes))));
    }
}
