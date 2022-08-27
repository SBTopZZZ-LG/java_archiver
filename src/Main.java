import Models.Binary;
import Utilities.Binaries.BinaryString;
import Utilities.ByteArrayConversion;
import Utilities.IO;
import Utilities.SerializableFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.bethecoder.ascii_table.ASCIITable;

public class Main {
    final static Scanner sc = new Scanner(System.in);
    final static BinaryString SIGNATURE = new BinaryString("archivitfile");

    public static void main(String[] args) throws Exception {
        System.out.print("1] Create Archive\n2] Extract an archive\n3] List archive contents\n> ");
        int option = sc.nextInt();
        sc.nextLine();

        if (option == 1)
            createArchive();
        else if (option == 2)
            extractArchive();
        else if (option == 3)
            listArchive();
        else
            System.out.println("Invalid option " + option);
    }

    static void createArchive() throws IOException {
        System.out.println("Enter folder path to archive: ");
        String folderPath = sc.nextLine();
        if (!folderPath.endsWith("\\"))
            folderPath += "\\";

        if (!Files.exists(Path.of(folderPath)) || !(new File(folderPath).isDirectory())) {
            System.out.println("Provided path is not a valid directory!");
            return;
        }

        System.out.println("Enter destination archive path: ");
        String archivePath = sc.nextLine();
        if (!archivePath.endsWith(".archivit"))
            archivePath += ".archivit";

        if (Files.exists(Path.of(archivePath))) {
            System.out.println("File \"" + new File(archivePath).getName() + "\" already exists. Overwrite? (Y/n): ");
            char option = sc.next().toLowerCase().charAt(0);
            if (option == 'n')
                return;
            if (option != 'y') {
                System.out.println("Invalid option.");
                return;
            } else {
                try {
                    Files.delete(Path.of(archivePath));
                } catch (IOException e) {
                    System.out.println("Failed to overwrite file.");
                    return;
                }
            }
        }

        System.out.println("Creating archive...");

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(archivePath));

        // Write file signature
        bos.write(SIGNATURE.toByteArray());

        String finalFolderPath = folderPath;
        IO.getFilesAndDirs(folderPath, new IO.OnRetrieve() {
            @Override
            public void onFileRetrieve(String file) {
                String relativePath = file.replace(finalFolderPath, "");

                try {
                    SerializableFile serializableFile = new SerializableFile(file, relativePath);

                    // Size
                    bos.write(Binary.sizeToByteArray(serializableFile.getSize()));

                    // Metadata
                    bos.write(new SerializableFile(file, relativePath).toByteArray());

                    // Binary
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                    byte[] buffer = new byte[10240];
                    int bufferReadLength = 10240;

                    while (bis.available() > 0) {
                        if (bis.available() < buffer.length)
                            bufferReadLength = bis.available();

                        bis.readNBytes(buffer, 0, bufferReadLength);
                        bos.write(bufferReadLength != buffer.length ? Arrays.copyOfRange(buffer, 0, bufferReadLength) : buffer);
                    }
                    bis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("Added file: " + relativePath);
            }

            @Override
            public void onFolderRetrieve(String folder) {}

            @Override
            public void onSymLinkFileRetrieve(String symLinkPath) {
                System.out.println("Excluding SymLink file: " + new File(symLinkPath).getName());
            }

            @Override
            public void onExclusion(String file) {}
        });

        try {
            bos.flush();
            bos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Archive was successfully created");
    }

    static void extractArchive() throws IOException {
        System.out.println("Enter archive path: ");
        String archivePath = sc.nextLine();
        if (!archivePath.endsWith(".archivit"))
            archivePath += ".archivit";
        if (!Files.exists(Path.of(archivePath)) || !(new File(archivePath).isFile())) {
            System.out.println("Provided path is not a valid archive file");
            return;
        }

        System.out.println("Enter extraction path (a folder with the archive name will be created): ");
        String extractPath = sc.nextLine();
        if (!extractPath.endsWith("\\"))
            extractPath += "\\";
        if (new File(extractPath).isFile()) {
            System.out.println("Provided path is not a valid directory");
            return;
        }
        extractPath += new File(archivePath).getName().split("\\.")[0] + "\\";
        if (!(new File(extractPath).exists()) && !(new File(extractPath).mkdirs())) {
            System.out.println("Cannot create extraction path");
            return;
        }

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivePath));

        // Check signature
        BinaryString signature = new BinaryString() {{
            fromByteArray(ByteArrayConversion.toByteArray(bis.readNBytes(SIGNATURE.getSize())), false);
        }};
        if (!signature.data.equals(SIGNATURE.data)) {
            System.out.println("Specified archive file is not a valid Archivit archive!");
            return;
        }

        System.out.println("Extracting archive...");

        while (bis.available() > 0) {
            int segmentSize = (int) Binary.byteArrayToSize(bis.readNBytes(8));

            SerializableFile embeddedFile = new SerializableFile() {{
                fromByteArray(ByteArrayConversion.toByteArray(bis.readNBytes(segmentSize)));
            }};
            embeddedFile.path.data = extractPath + embeddedFile.path.data;

            System.out.print("Extract file: " + embeddedFile.path.data.replace(extractPath, "") + " (" + Binary.getFormattedSize(embeddedFile.size.data) + ") ");

            if (!embeddedFile.createFile(bis.readNBytes((int) embeddedFile.size.data))) {
                System.out.println("Failed to write to file \"" + embeddedFile.name.data + "\"");
                return;
            }

            System.out.println("OK");
        }

        bis.close();

        System.out.println("Archive contents were extracted");
    }

    static void listArchive() throws IOException {
        System.out.println("Enter archive path: ");
        String archivePath = sc.nextLine();
        if (!archivePath.endsWith(".archivit"))
            archivePath += ".archivit";
        if (!Files.exists(Path.of(archivePath)) || !(new File(archivePath).isFile())) {
            System.out.println("Provided path is not a valid archive file");
            return;
        }

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivePath));

        // Check signature
        BinaryString signature = new BinaryString() {{
            fromByteArray(ByteArrayConversion.toByteArray(bis.readNBytes(SIGNATURE.getSize())), false);
        }};
        if (!signature.data.equals(SIGNATURE.data)) {
            System.out.println("Specified archive file is not a valid Archivit archive!");
            return;
        }

        List<String[]> dataSet = new ArrayList<>();

        while (bis.available() > 0) {
            int segmentSize = (int) Binary.byteArrayToSize(bis.readNBytes(8));

            SerializableFile embeddedFile = new SerializableFile() {{
                fromByteArray(ByteArrayConversion.toByteArray(bis.readNBytes(segmentSize)));
            }};

            // Append to `dataSet`
            dataSet.add(new String[] {
                    embeddedFile.name.data,
                    "\\" + embeddedFile.path.data.replaceFirst(embeddedFile.name.data + "$", ""),
                    embeddedFile.canRead.data ? "Yes" : "No",
                    embeddedFile.canExecute.data ? "Yes" : "No",
                    embeddedFile.canWrite.data ? "Yes" : "No",
                    String.valueOf(embeddedFile.lastModified.data),
                    Binary.getFormattedSize(embeddedFile.size.data)
            });

            // Skip file binary data
            bis.skipNBytes((int) embeddedFile.size.data);
        }

        bis.close();

        // Convert List<String[]> to String[][]
        String[][] dataSet2 = new String[dataSet.size()][];
        int index = 0;
        for (final String[] dataSetRow : dataSet)
            dataSet2[index++] = dataSetRow;

        ASCIITable inst = ASCIITable.getInstance();
        inst.printTable(new String[] {
                "Name", "Path", "Readable", "Executable", "Writable", "Last modified", "Size"
        }, dataSet2);
    }
}
