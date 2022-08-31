import Models.Binary;
import Utilities.Binaries.BinaryString;
import Utilities.BufferedStream;
import Utilities.CipherKit;
import Utilities.IO;
import Utilities.SerializableFile;
import com.bethecoder.ascii_table.ASCIITable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    final static Scanner sc = new Scanner(System.in);
    final static BinaryString SIGNATURE = new BinaryString("archivitfile");

    public static final int NONCE_LENGTH = 12;

    public static void main(String[] args) {
        System.out.print("Archivit v2\n1] Create Archive\n2] Extract an archive\n3] List archive contents\n> ");
        int option = sc.nextInt();
        sc.nextLine();

        try {
            if (option == 1)
                createArchive();
            else if (option == 2)
                extractArchive();
            else if (option == 3)
                listArchive();
            else
                System.out.println("Invalid option " + option);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pause();
        }
    }

    static void createArchive() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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

        boolean isPasswordProtected = false;
        CipherKit kit = null;
        {
            System.out.println("Add password protection? (Y/n): ");
            char option = sc.next().toLowerCase().charAt(0);
            if (option == 'y') {
                isPasswordProtected = true;
                do {
                    String password = sc.nextLine();
                    if (password.length() < 6 || password.length() > 16 || password.trim().length() == 0)
                        System.out.print("Please enter a password which is:\n* At least 6 characters and at most 16 characters long\n* Must not be a whitespace sequence\n> ");
                    else {
                        kit = new CipherKit(CipherKit.generateNonce(NONCE_LENGTH), password);
                        break;
                    }
                } while (true);
            }
        }

        System.out.println("Creating archive...");

        BufferedStream.Output bso = new BufferedStream.Output(new FileOutputStream(archivePath));

        // Write file signature
        bso.write(SIGNATURE.toByteArray());

        // If user wishes to use password protection, set byte to `1`
        bso.write(new byte[] {(byte)(isPasswordProtected ? 1 : 0)});

        if (isPasswordProtected)
            // Write 12 bytes long nonce
            bso.write(kit.nonce);

        // Final variables
        final String folderPath2 = folderPath;
        final boolean isPasswordProtected2 = isPasswordProtected;
        final CipherKit kit2 = kit;
        IO.getFilesAndDirs(folderPath, new IO.OnRetrieve() {
            @Override
            public void onFileRetrieve(String file) {
                String relativePath = file.replace(folderPath2, "");

                long fileSize = 0;
                try {
                    fileSize = Files.size(Path.of(file));
                    System.out.print("Add file: " + relativePath + " (" + Binary.getFormattedSize(fileSize) + ") ");
                } catch (IOException ignored) {
                    System.out.print("Add file: " + relativePath + " (N/A) ");
                }

                try {
                    SerializableFile serializableFile = new SerializableFile(file, relativePath);

                    // Add metadata segment
                    bso.writeSegment(serializableFile.toByteArray(), BufferedStream.JavaStreamSegmentType.LONG);

                    // Binary
                    if (isPasswordProtected2)
                        // Append `0` byte if file is truncated, otherwise `1`
                        bso.putBoolean(fileSize > 0);

                    final BufferedStream.Input bsi = new BufferedStream.Input(new FileInputStream(file));
                    final byte[] buffer = new byte[102400];
                    int bufferReadLength = 102400;

                    long fileSizeLeft = fileSize;
                    while (bsi.available() > 0) {
                        if (bsi.available() < buffer.length)
                            bufferReadLength = bsi.available();

                        bsi.readNBytes(buffer, 0, bufferReadLength);

                        if (isPasswordProtected2) {
                            boolean hasNextSegment = (fileSizeLeft - bufferReadLength > 0);

                            if (bufferReadLength == buffer.length) {
                                byte[] encoded = kit2.exec(buffer, CipherKit.CipherMode.ENCRYPT);

                                // Write encoded bytes segment
                                bso.writeSegment(encoded, BufferedStream.JavaStreamSegmentType.LONG);
                            } else {
                                byte[] buffer2 = new byte[bufferReadLength];
                                {
                                    // Copy `buffer` bytes to `buffer2`
                                    System.arraycopy(buffer, 0, buffer2, 0, bufferReadLength);
                                }

                                byte[] encoded = kit2.exec(buffer2, CipherKit.CipherMode.ENCRYPT);

                                // Write encoded bytes segment
                                bso.writeSegment(encoded, BufferedStream.JavaStreamSegmentType.LONG);
                            }

                            // Add `1` byte to indicate if there is next segment
                            bso.putBoolean(hasNextSegment);

                            fileSizeLeft -= bufferReadLength;
                        } else
                            bso.write(bufferReadLength != buffer.length ? Arrays.copyOfRange(buffer, 0, bufferReadLength) : buffer);
                    }
                    bsi.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                System.out.println("OK");
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
            bso.flush();
            bso.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Archive was successfully created");
    }

    static void extractArchive() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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

        BufferedStream.Input bsi = new BufferedStream.Input(new FileInputStream(archivePath));

        // Check signature
        BinaryString signature = new BinaryString() {{
            fromByteArray(bsi.readNBytes(SIGNATURE.getSize()), false);
        }};
        if (!signature.data.equals(SIGNATURE.data)) {
            System.out.println("Specified archive file is not a valid Archivit archive!");
            return;
        }

        // Check if archive is password-protected
        boolean isPasswordProtected = bsi.getBoolean();

        CipherKit kit = null;
        if (isPasswordProtected) {
            System.out.print("This archive is password-protected!\nEnter password: ");
            do {
                String password = sc.nextLine();
                if (password.length() < 6 || password.length() > 16 || password.trim().length() == 0)
                    System.out.print("Please enter a password which is:\n* At least 6 characters and at most 16 characters long\n* Must not be a whitespace sequence\n> ");
                else {
                    kit = new CipherKit(bsi.readNBytes(NONCE_LENGTH), password);
                    break;
                }
            } while (true);
        }

        System.out.println("Extracting archive...");

        // Final variables
        final CipherKit kit2 = kit;
        while (bsi.available() > 0) {
            SerializableFile embeddedFile = new SerializableFile() {{
                fromByteArray(bsi.readSegment(BufferedStream.JavaStreamSegmentType.LONG));
            }};
            embeddedFile.path.data = extractPath + embeddedFile.path.data;

            System.out.print("Extract file: " + embeddedFile.path.data.replace(extractPath, "") + " (" + Binary.getFormattedSize(embeddedFile.size.data) + ") ");

            if (isPasswordProtected) {
                if (!embeddedFile.createFile(new SerializableFile.CreateFileCallback() {
                    @Override
                    public void writeBinaryData(File file) {
                        try {
                            final FileOutputStream fos = new FileOutputStream(file);

                            while (bsi.getBoolean()) {
                                bsi.readSegment(BufferedStream.JavaStreamSegmentType.LONG, new BufferedStream.JavaStreamReadSegmentCallback() {
                                    @Override
                                    public void onSegmentRetrieve(byte[] bytes, BufferedStream.JavaStreamSegmentType segmentType) {
                                        try {
                                            fos.write(kit2.exec(bytes, CipherKit.CipherMode.DECRYPT));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }

                            fos.flush();
                            fos.close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                })) {
                    System.out.println("Failed to write to file \"" + embeddedFile.name.data + "\"");
                    return;
                }
            } else {
                if (!embeddedFile.createFile(new SerializableFile.CreateFileCallback() {
                    @Override
                    public void writeBinaryData(File file) {
                        try {
                            FileOutputStream fos = new FileOutputStream(file);

                            long length = embeddedFile.size.data;
                            while (length > 0) {
                                fos.write(bsi.readNBytes(length < 102400 ? (int) length : 102400));

                                length -= 102400;
                                if (length < 0)
                                    length = 0;
                            }

                            fos.flush();
                            fos.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })) {
                    System.out.println("Failed to write to file \"" + embeddedFile.name.data + "\"");
                    return;
                }
            }

            System.out.println("OK");
        }

        bsi.close();

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

        BufferedStream.Input bis = new BufferedStream.Input(new FileInputStream(archivePath));

        // Check signature
        BinaryString signature = new BinaryString() {{
            fromByteArray(bis.readNBytes(SIGNATURE.getSize()), false);
        }};
        if (!signature.data.equals(SIGNATURE.data)) {
            System.out.println("Specified archive file is not a valid Archivit archive!");
            return;
        }

        // Skip password-protection flag byte and nonce bytes (if exists)
        boolean isPasswordProtected = bis.getBoolean();
        if (isPasswordProtected)
            bis.skipNBytes(NONCE_LENGTH);

        List<String[]> dataSet = new ArrayList<>();

        while (bis.available() > 0) {
            SerializableFile embeddedFile = new SerializableFile() {{
                fromByteArray(bis.readSegment(BufferedStream.JavaStreamSegmentType.LONG));
            }};

            // Append to `dataSet`
            dataSet.add(new String[] {
                    embeddedFile.name.data,
                    "\\" + embeddedFile.path.data.replaceFirst(escapeMetaCharacters(embeddedFile.name.data) + "$", ""),
                    embeddedFile.canRead.data ? "Yes" : "No",
                    embeddedFile.canExecute.data ? "Yes" : "No",
                    embeddedFile.canWrite.data ? "Yes" : "No",
                    String.valueOf(embeddedFile.lastModified.data),
                    Binary.getFormattedSize(embeddedFile.size.data)
            });

            // Skip file binary data
            if (!isPasswordProtected)
                bis.skipNBytes((int) embeddedFile.size.data);
            else {
                while (bis.getBoolean())
                    bis.skipNBytes(bis.getLong());
            }
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

    private static void pause() {
        System.out.println("\nPress any key to continue...");
        try {
            System.in.read();
        } catch (IOException ignored) {}
    }

    private static String escapeMetaCharacters(String inputString){
        final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")",".","*","+","?","|","<",">","-","&","%"};

        for (String metaCharacter : metaCharacters)
            if (inputString.contains(metaCharacter))
                inputString = inputString.replace(metaCharacter, "\\" + metaCharacter);

        return inputString;
    }
}
