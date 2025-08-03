package Utilities;

import Configs.ErrorCodes;
import Models.Binary;
import Utilities.Binaries.BinaryString;

import javax.crypto.AEADBadTagException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Non-interactive API for archiving operations
 * Provides programmatic access to create, extract and list archive operations
 */
public class ArchiverAPI {
    private final BinaryString SIGNATURE = new BinaryString("archivitfile");
    public static final int NONCE_LENGTH = 12;
    
    /**
     * Result of an archiver operation
     */
    public static class OperationResult {
        public final boolean success;
        public final String message;
        public final Exception error;
        
        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.error = null;
        }
        
        public OperationResult(boolean success, String message, Exception error) {
            this.success = success;
            this.message = message;
            this.error = error;
        }
    }
    
    /**
     * Configuration for archive creation
     */
    public static class CreateArchiveConfig {
        public final String sourcePath;
        public final String archivePath;
        public final String password; // null for no password protection
        
        public CreateArchiveConfig(String sourcePath, String archivePath) {
            this.sourcePath = sourcePath;
            this.archivePath = archivePath;
            this.password = null;
        }
        
        public CreateArchiveConfig(String sourcePath, String archivePath, String password) {
            this.sourcePath = sourcePath;
            this.archivePath = archivePath;
            this.password = password;
        }
    }
    
    /**
     * Progress callback for long-running operations
     */
    public interface ProgressCallback {
        void onProgress(String operation, String fileName, long processed, long total);
        void onComplete(String operation, long totalFiles, long totalBytes);
        void onError(String operation, String fileName, Exception error);
    }
    
    /**
     * Creates an archive from the specified directory
     * @param config Archive creation configuration
     * @param progressCallback Optional progress callback (can be null)
     * @return Operation result
     */
    public OperationResult createArchive(CreateArchiveConfig config, ProgressCallback progressCallback) {
        try {
            return createArchiveInternal(config, progressCallback);
        } catch (Exception e) {
            return new OperationResult(false, "Archive creation failed: " + e.getMessage(), e);
        }
    }
    
    private OperationResult createArchiveInternal(CreateArchiveConfig config, ProgressCallback progressCallback) throws Exception {
        // Validate inputs
        if (config.sourcePath == null || config.sourcePath.trim().isEmpty()) {
            return new OperationResult(false, "Source path cannot be empty");
        }
        
        if (config.archivePath == null || config.archivePath.trim().isEmpty()) {
            return new OperationResult(false, "Archive path cannot be empty");
        }
        
        String folderPath = config.sourcePath;
        if (!folderPath.endsWith(getFileSeparator())) {
            folderPath += getFileSeparator();
        }
        
        if (!Files.exists(Path.of(folderPath)) || !(new File(folderPath).isDirectory())) {
            return new OperationResult(false, "Source path is not a valid directory: " + folderPath);
        }
        
        String archivePath = config.archivePath;
        if (!archivePath.endsWith(".archivit")) {
            archivePath += ".archivit";
        }
        
        // Check if archive already exists
        if (Files.exists(Path.of(archivePath))) {
            return new OperationResult(false, "Archive file already exists: " + archivePath);
        }
        
        // Validate password if provided
        boolean isPasswordProtected = config.password != null;
        CipherKit kit = null;
        if (isPasswordProtected) {
            if (config.password.length() < 6 || config.password.length() > 16 || config.password.trim().length() == 0) {
                return new OperationResult(false, "Password must be 6-16 characters long and not whitespace-only");
            }
            kit = new CipherKit(CipherKit.generateNonce(NONCE_LENGTH), config.password);
        }
        
        // Count files first for progress reporting
        final List<String> filesToProcess = new ArrayList<>();
        IO.getFilesAndDirs(folderPath, new IO.OnRetrieve() {
            @Override
            public void onFileRetrieve(String file) {
                filesToProcess.add(file);
            }
            @Override
            public void onFolderRetrieve(String folder) {}
            @Override
            public void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath) {}
            @Override
            public void onExclusion(String file) {}
        });
        
        BufferedStream.Output bso = new BufferedStream.Output(new FileOutputStream(archivePath));
        
        try {
            // Write file signature
            bso.write(SIGNATURE.toByteArray());
            
            // Write Operating-system specific file separator used in the archive
            bso.write(getFileSeparator().getBytes(StandardCharsets.UTF_8));
            
            // If user wishes to use password protection, set byte to `1`
            bso.write(new byte[] {(byte)(isPasswordProtected ? 1 : 0)});
            
            if (isPasswordProtected) {
                // Write 12 bytes long nonce
                bso.write(kit.nonce);
            }
            
            // Process files
            final String folderPath2 = folderPath;
            final boolean isPasswordProtected2 = isPasswordProtected;
            final CipherKit kit2 = kit;
            final int[] processedCount = {0};
            long totalBytes = 0;
            
            // Calculate total bytes for progress reporting
            for (String file : filesToProcess) {
                try {
                    totalBytes += Files.size(Path.of(file));
                } catch (IOException ignored) {}
            }
            final long finalTotalBytes = totalBytes;
            
            IO.getFilesAndDirs(folderPath, new IO.OnRetrieve() {
                @Override
                public void onFileRetrieve(String file) {
                    try {
                        String relativePath = file.replace(folderPath2, "");
                        
                        long fileSize = 0;
                        try {
                            fileSize = Files.size(Path.of(file));
                        } catch (IOException ignored) {}
                        
                        if (progressCallback != null) {
                            progressCallback.onProgress("create", relativePath, processedCount[0], filesToProcess.size());
                        }
                        
                        SerializableFile serializableFile = new SerializableFile(file, relativePath);
                        
                        // Add metadata segment
                        bso.writeSegment(serializableFile.toByteArray(), BufferedStream.JavaStreamSegmentType.LONG);
                        
                        // Binary
                        if (isPasswordProtected2) {
                            // Append `0` byte if file is truncated, otherwise `1`
                            bso.putBoolean(fileSize > 0);
                        }
                        
                        final BufferedStream.Input bsi = new BufferedStream.Input(new FileInputStream(file));
                        final byte[] buffer = new byte[Configs.Constants.DICTIONARY_MAX_SIZE];
                        int bufferReadLength = buffer.length;
                        
                        long fileSizeLeft = fileSize;
                        while (bsi.available() > 0) {
                            if (bsi.available() < buffer.length) {
                                bufferReadLength = bsi.available();
                            }
                            
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
                            } else {
                                bso.write(bufferReadLength != buffer.length ? Arrays.copyOfRange(buffer, 0, bufferReadLength) : buffer);
                            }
                        }
                        bsi.close();
                        processedCount[0]++;
                    } catch (Exception e) {
                        if (progressCallback != null) {
                            progressCallback.onError("create", file, e);
                        }
                        throw new RuntimeException(e);
                    }
                }
                
                @Override
                public void onFolderRetrieve(String folder) {}
                
                @Override
                public void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath) {}
                
                @Override
                public void onExclusion(String file) {}
            });
            
            bso.flush();
            bso.close();
            
            if (progressCallback != null) {
                progressCallback.onComplete("create", filesToProcess.size(), finalTotalBytes);
            }
            
            return new OperationResult(true, "Archive created successfully: " + archivePath);
            
        } catch (Exception e) {
            // Clean up partial archive on error
            try {
                bso.close();
                Files.deleteIfExists(Path.of(archivePath));
            } catch (Exception ignored) {}
            throw e;
        }
    }
    
    /**
     * Configuration for archive extraction
     */
    public static class ExtractArchiveConfig {
        public final String archivePath;
        public final String extractPath;
        public final String password; // null for unencrypted archives
        
        public ExtractArchiveConfig(String archivePath, String extractPath) {
            this.archivePath = archivePath;
            this.extractPath = extractPath;
            this.password = null;
        }
        
        public ExtractArchiveConfig(String archivePath, String extractPath, String password) {
            this.archivePath = archivePath;
            this.extractPath = extractPath;
            this.password = password;
        }
    }
    
    /**
     * Extracts an archive to the specified directory
     * @param config Extraction configuration
     * @param progressCallback Optional progress callback (can be null)
     * @return Operation result
     */
    public OperationResult extractArchive(ExtractArchiveConfig config, ProgressCallback progressCallback) {
        try {
            return extractArchiveInternal(config, progressCallback);
        } catch (Exception e) {
            return new OperationResult(false, "Archive extraction failed: " + e.getMessage(), e);
        }
    }
    
    private OperationResult extractArchiveInternal(ExtractArchiveConfig config, ProgressCallback progressCallback) throws Exception {
        // Validate inputs
        if (config.archivePath == null || config.archivePath.trim().isEmpty()) {
            return new OperationResult(false, "Archive path cannot be empty");
        }
        
        if (config.extractPath == null || config.extractPath.trim().isEmpty()) {
            return new OperationResult(false, "Extract path cannot be empty");
        }
        
        String archivePath = config.archivePath;
        if (!archivePath.endsWith(".archivit")) {
            archivePath += ".archivit";
        }
        
        if (!Files.exists(Path.of(archivePath)) || !(new File(archivePath).isFile())) {
            return new OperationResult(false, "Archive file not found: " + archivePath);
        }
        
        String extractPath = config.extractPath;
        if (!extractPath.endsWith(getFileSeparator())) {
            extractPath += getFileSeparator();
        }
        
        if (new File(extractPath).isFile()) {
            return new OperationResult(false, "Extract path is not a valid directory: " + extractPath);
        }
        
        extractPath += IO.getFileNameWithoutExtension(new File(archivePath).getName()) + getFileSeparator();
        
        if (!(new File(extractPath).exists()) && !(new File(extractPath).mkdirs())) {
            return new OperationResult(false, "Cannot create extraction directory: " + extractPath);
        }
        
        BufferedStream.Input bsi = new BufferedStream.Input(new FileInputStream(archivePath));
        
        try {
            // Check signature
            BinaryString signature = new BinaryString() {{
                fromByteArray(bsi.readNBytes(SIGNATURE.getSize()));
            }};
            if (!signature.data.equals(SIGNATURE.data)) {
                return new OperationResult(false, "Invalid archive format - signature mismatch");
            }
            
            // Get file separator character
            final String fileSeparator = new String(bsi.readNBytes(1), StandardCharsets.UTF_8);
            
            // Check if archive is password-protected
            boolean isPasswordProtected = bsi.getBoolean();
            
            CipherKit kit = null;
            if (isPasswordProtected) {
                if (config.password == null) {
                    return new OperationResult(false, "Archive is password-protected but no password provided");
                }
                
                if (config.password.length() < 6 || config.password.length() > 16 || config.password.trim().length() == 0) {
                    return new OperationResult(false, "Invalid password format");
                }
                
                try {
                    kit = new CipherKit(bsi.readNBytes(NONCE_LENGTH), config.password);
                } catch (InvalidKeySpecException e) {
                    return new OperationResult(false, "Incorrect password");
                }
            }
            
            // Count files for progress reporting
            long currentPos = archivePath.endsWith(".archivit") ? new File(archivePath).length() : 0;
            int fileCount = 0;
            
            // Process files
            final CipherKit kit2 = kit;
            final String finalExtractPath = extractPath;
            int processedFiles = 0;
            
            while (bsi.available() > 0) {
                try {
                    SerializableFile embeddedFile = new SerializableFile() {{
                        fromByteArray(bsi.readSegment(BufferedStream.JavaStreamSegmentType.LONG));
                    }};
                    embeddedFile.path.data = finalExtractPath + embeddedFile.path.data;
                    
                    // Fix file separator (if there is a mismatch)
                    if (!fileSeparator.equals(getFileSeparator())) {
                        embeddedFile.path.data = embeddedFile.path.data.replace(fileSeparator, getFileSeparator());
                    }
                    
                    // Sanitize path to prevent directory traversal
                    String sanitizedPath = sanitizePath(embeddedFile.path.data, finalExtractPath);
                    if (!sanitizedPath.startsWith(finalExtractPath)) {
                        return new OperationResult(false, "Path traversal attempt detected: " + embeddedFile.path.data);
                    }
                    embeddedFile.path.data = sanitizedPath;
                    
                    if (progressCallback != null) {
                        progressCallback.onProgress("extract", embeddedFile.name.data, processedFiles, -1);
                    }
                    
                    boolean extractSuccess;
                    if (isPasswordProtected) {
                        extractSuccess = embeddedFile.createFile(new SerializableFile.CreateFileCallback() {
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
                                                } catch (AEADBadTagException e) {
                                                    throw new RuntimeException("Incorrect password");
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
                        });
                    } else {
                        extractSuccess = embeddedFile.createFile(new SerializableFile.CreateFileCallback() {
                            @Override
                            public void writeBinaryData(File file) {
                                try {
                                    FileOutputStream fos = new FileOutputStream(file);
                                    
                                    long length = embeddedFile.size.data;
                                    while (length > 0) {
                                        fos.write(bsi.readNBytes(length < Configs.Constants.DICTIONARY_MAX_SIZE ? (int) length : Configs.Constants.DICTIONARY_MAX_SIZE));
                                        
                                        length -= Configs.Constants.DICTIONARY_MAX_SIZE;
                                        if (length < 0) {
                                            length = 0;
                                        }
                                    }
                                    
                                    fos.flush();
                                    fos.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                    
                    if (!extractSuccess) {
                        return new OperationResult(false, "Failed to extract file: " + embeddedFile.name.data);
                    }
                    
                    processedFiles++;
                } catch (Exception e) {
                    if (progressCallback != null) {
                        progressCallback.onError("extract", "unknown", e);
                    }
                    throw e;
                }
            }
            
            bsi.close();
            
            if (progressCallback != null) {
                progressCallback.onComplete("extract", processedFiles, -1);
            }
            
            return new OperationResult(true, "Archive extracted successfully to: " + finalExtractPath);
            
        } catch (Exception e) {
            try {
                bsi.close();
            } catch (Exception ignored) {}
            throw e;
        }
    }
    
    /**
     * Archive file information
     */
    public static class ArchiveFileInfo {
        public final String name;
        public final String path;
        public final boolean canRead;
        public final boolean canExecute;
        public final boolean canWrite;
        public final long lastModified;
        public final long size;
        
        public ArchiveFileInfo(String name, String path, boolean canRead, boolean canExecute, 
                              boolean canWrite, long lastModified, long size) {
            this.name = name;
            this.path = path;
            this.canRead = canRead;
            this.canExecute = canExecute;
            this.canWrite = canWrite;
            this.lastModified = lastModified;
            this.size = size;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s) - %s", name, path, Binary.getFormattedSize(size));
        }
    }
    
    /**
     * Lists the contents of an archive
     * @param archivePath Path to the archive file
     * @return Operation result with list of files if successful
     */
    public OperationResult listArchive(String archivePath) {
        try {
            List<ArchiveFileInfo> files = listArchiveContents(archivePath);
            return new OperationResult(true, "Archive contains " + files.size() + " files");
        } catch (Exception e) {
            return new OperationResult(false, "Failed to list archive contents: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lists the contents of an archive and returns the file information
     * @param archivePath Path to the archive file
     * @return List of file information
     * @throws Exception If the archive cannot be read
     */
    public List<ArchiveFileInfo> listArchiveContents(String archivePath) throws Exception {
        if (archivePath == null || archivePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Archive path cannot be empty");
        }
        
        if (!archivePath.endsWith(".archivit")) {
            archivePath += ".archivit";
        }
        
        if (!Files.exists(Path.of(archivePath)) || !(new File(archivePath).isFile())) {
            throw new FileNotFoundException("Archive file not found: " + archivePath);
        }
        
        BufferedStream.Input bsi = new BufferedStream.Input(new FileInputStream(archivePath));
        List<ArchiveFileInfo> files = new ArrayList<>();
        
        try {
            // Check signature
            BinaryString signature = new BinaryString() {{
                fromByteArray(bsi.readNBytes(SIGNATURE.getSize()));
            }};
            if (!signature.data.equals(SIGNATURE.data)) {
                throw new IllegalArgumentException("Invalid archive format - signature mismatch");
            }
            
            // Get file separator character
            final String fileSeparator = new String(bsi.readNBytes(1), StandardCharsets.UTF_8);
            
            // Skip password-protection flag byte and nonce bytes (if exists)
            boolean isPasswordProtected = bsi.getBoolean();
            if (isPasswordProtected) {
                bsi.skipNBytes(NONCE_LENGTH);
            }
            
            while (bsi.available() > 0) {
                SerializableFile embeddedFile = new SerializableFile() {{
                    fromByteArray(bsi.readSegment(BufferedStream.JavaStreamSegmentType.LONG));
                }};
                
                // Fix file separator (if there is a mismatch)
                if (!fileSeparator.equals(getFileSeparator())) {
                    embeddedFile.path.data = embeddedFile.path.data.replace(fileSeparator, getFileSeparator());
                }
                
                files.add(new ArchiveFileInfo(
                    embeddedFile.name.data,
                    embeddedFile.path.data,
                    embeddedFile.canRead.data,
                    embeddedFile.canExecute.data,
                    embeddedFile.canWrite.data,
                    embeddedFile.lastModified.data,
                    embeddedFile.size.data
                ));
                
                // Skip file binary data
                if (!isPasswordProtected) {
                    bsi.skipNBytes((int) embeddedFile.size.data);
                } else {
                    while (bsi.getBoolean()) {
                        bsi.skipNBytes(bsi.getLong());
                    }
                }
            }
            
            bsi.close();
            return files;
            
        } catch (Exception e) {
            try {
                bsi.close();
            } catch (Exception ignored) {}
            throw e;
        }
    }
    
    /**
     * Sanitizes a file path to prevent directory traversal attacks
     * @param path The path to sanitize
     * @param basePath The base directory path
     * @return Sanitized path
     */
    private String sanitizePath(String path, String basePath) {
        if (path == null) return basePath;
        
        // Remove any path traversal attempts
        path = path.replace("../", "").replace("..\\", "");
        path = path.replace("~/", "").replace("~\\", "");
        
        // Ensure path starts with base path
        if (!path.startsWith(basePath)) {
            path = basePath + path;
        }
        
        return path;
    }
    
    /**
     * Gets the OS-specific file separator
     * @return File separator
     */
    private String getFileSeparator() {
        return System.getProperty("file.separator");
    }
    
    /**
     * Formats file size for display
     * @param size Size in bytes
     * @return Formatted size string
     */
    public static String formatSize(long size) {
        if (size < 1024)
            return size + " bytes";
        if (size < 1048576)
            return (size / 1024) + " KB";
        if (size < 1073741824)
            return (size / 1048576) + " MB";
        if (size < Long.parseLong("1099511627776"))
            try {
                return String.format("%.1f GB", (float)size / 1073741824);
            } catch (Exception ignored) {
                return (size / 1073741824) + " GB";
            }

        try {
            return String.format("%.1f TB", (float)size / Long.parseLong("1099511627776"));
        } catch (Exception ignored) {
            return (size / Long.parseLong("1099511627776")) + " TB";
        }
    }
}