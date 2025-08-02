package Utilities;

import Configs.Constants;
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
 * Enhanced archiver with compression and integrity verification
 * Version 2 of the archive format with enhanced features
 */
public class EnhancedArchiverAPI {
    private final BinaryString SIGNATURE = new BinaryString("archivitv2");
    public static final int NONCE_LENGTH = 12;
    public static final byte FORMAT_VERSION = 2;
    
    /**
     * Archive creation configuration with compression options
     */
    public static class CreateArchiveConfig {
        public final String sourcePath;
        public final String archivePath;
        public final String password;
        public final boolean enableCompression;
        public final boolean enableIntegrityCheck;
        
        public CreateArchiveConfig(String sourcePath, String archivePath) {
            this.sourcePath = sourcePath;
            this.archivePath = archivePath;
            this.password = null;
            this.enableCompression = true;
            this.enableIntegrityCheck = true;
        }
        
        public CreateArchiveConfig(String sourcePath, String archivePath, String password) {
            this.sourcePath = sourcePath;
            this.archivePath = archivePath;
            this.password = password;
            this.enableCompression = true;
            this.enableIntegrityCheck = true;
        }
        
        public CreateArchiveConfig(String sourcePath, String archivePath, String password, 
                                 boolean enableCompression, boolean enableIntegrityCheck) {
            this.sourcePath = sourcePath;
            this.archivePath = archivePath;
            this.password = password;
            this.enableCompression = enableCompression;
            this.enableIntegrityCheck = enableIntegrityCheck;
        }
    }
    
    /**
     * Creates an enhanced archive with compression and integrity checks
     * @param config Archive creation configuration
     * @param progressCallback Optional progress callback
     * @return Operation result
     */
    public ArchiverAPI.OperationResult createArchive(CreateArchiveConfig config, 
                                                    ArchiverAPI.ProgressCallback progressCallback) {
        try (ResourceManager rm = new ResourceManager()) {
            return createArchiveInternal(config, progressCallback, rm);
        } catch (Exception e) {
            return new ArchiverAPI.OperationResult(false, "Enhanced archive creation failed: " + e.getMessage(), e);
        }
    }
    
    private ArchiverAPI.OperationResult createArchiveInternal(CreateArchiveConfig config, 
                                                             ArchiverAPI.ProgressCallback progressCallback,
                                                             ResourceManager rm) throws Exception {
        // Validate inputs
        if (config.sourcePath == null || config.sourcePath.trim().isEmpty()) {
            return new ArchiverAPI.OperationResult(false, "Source path cannot be empty");
        }
        
        String folderPath = config.sourcePath;
        if (!folderPath.endsWith(getFileSeparator())) {
            folderPath += getFileSeparator();
        }
        
        if (!Files.exists(Path.of(folderPath)) || !(new File(folderPath).isDirectory())) {
            return new ArchiverAPI.OperationResult(false, "Source path is not a valid directory: " + folderPath);
        }
        
        String archivePath = config.archivePath;
        if (!archivePath.endsWith(".archivit")) {
            archivePath += ".archivit";
        }
        
        if (Files.exists(Path.of(archivePath))) {
            return new ArchiverAPI.OperationResult(false, "Archive file already exists: " + archivePath);
        }
        
        // Setup encryption if requested
        boolean isPasswordProtected = config.password != null;
        CipherKit kit = null;
        if (isPasswordProtected) {
            if (config.password.length() < 6 || config.password.length() > 16 || config.password.trim().length() == 0) {
                return new ArchiverAPI.OperationResult(false, "Password must be 6-16 characters long and not whitespace-only");
            }
            kit = rm.manage(new CipherKit(CipherKit.generateNonce(NONCE_LENGTH), config.password));
        }
        
        // Count files for progress
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
        
        BufferedStream.Output bso = rm.manage(new BufferedStream.Output(new FileOutputStream(archivePath)));
        
        try {
            // Write enhanced format signature
            bso.write(SIGNATURE.toByteArray());
            
            // Write format version
            bso.write(new byte[]{FORMAT_VERSION});
            
            // Write file separator
            bso.write(getFileSeparator().getBytes(StandardCharsets.UTF_8));
            
            // Write feature flags
            byte flags = 0;
            if (isPasswordProtected) flags |= 0x01;
            if (config.enableCompression) flags |= 0x02;
            if (config.enableIntegrityCheck) flags |= 0x04;
            bso.write(new byte[]{flags});
            
            // Write nonce if password protected
            if (isPasswordProtected) {
                bso.write(kit.nonce);
            }
            
            // Process files
            final String folderPath2 = folderPath;
            final boolean isPasswordProtected2 = isPasswordProtected;
            final CipherKit kit2 = kit;
            final int[] processedCount = {0};
            long totalBytes = 0;
            
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
                        
                        if (progressCallback != null) {
                            progressCallback.onProgress("create", relativePath, processedCount[0], filesToProcess.size());
                        }
                        
                        // Read file data
                        byte[] fileData = Files.readAllBytes(Path.of(file));
                        byte[] processedData = fileData;
                        
                        // Apply compression if enabled and beneficial
                        boolean isCompressed = false;
                        if (config.enableCompression && DataIntegrity.shouldCompress(fileData)) {
                            try {
                                byte[] compressed = DataIntegrity.compress(fileData);
                                if (compressed.length < fileData.length * 0.9) { // Only use if saves at least 10%
                                    processedData = compressed;
                                    isCompressed = true;
                                }
                            } catch (IOException e) {
                                // Compression failed, use original data
                                System.err.println("Warning: Compression failed for " + relativePath + ": " + e.getMessage());
                            }
                        }
                        
                        // Create integrity metadata
                        DataIntegrity.IntegrityMetadata integrity = null;
                        if (config.enableIntegrityCheck) {
                            integrity = new DataIntegrity.IntegrityMetadata(
                                DataIntegrity.calculateSHA256(processedData),
                                DataIntegrity.calculateCRC32(processedData),
                                isCompressed,
                                fileData.length,
                                processedData.length
                            );
                        }
                        
                        // Create enhanced serializable file
                        SerializableFile serializableFile = new SerializableFile(file, relativePath);
                        
                        // Write file metadata
                        bso.writeSegment(serializableFile.toByteArray(), BufferedStream.JavaStreamSegmentType.LONG);
                        
                        // Write integrity metadata if enabled
                        if (integrity != null) {
                            bso.writeSegment(integrity.toByteArray(), BufferedStream.JavaStreamSegmentType.SHORT);
                        }
                        
                        // Write compression flag
                        bso.putBoolean(isCompressed);
                        
                        // Apply encryption if enabled
                        if (isPasswordProtected2) {
                            if (processedData.length > 0) {
                                // Process in chunks for large files
                                int chunkSize = Constants.DICTIONARY_MAX_SIZE;
                                int offset = 0;
                                boolean hasMore = true;
                                
                                while (hasMore) {
                                    int currentChunkSize = Math.min(chunkSize, processedData.length - offset);
                                    hasMore = (offset + currentChunkSize) < processedData.length;
                                    
                                    byte[] chunk = Arrays.copyOfRange(processedData, offset, offset + currentChunkSize);
                                    byte[] encryptedChunk = kit2.exec(chunk, CipherKit.CipherMode.ENCRYPT);
                                    
                                    bso.writeSegment(encryptedChunk, BufferedStream.JavaStreamSegmentType.LONG);
                                    bso.putBoolean(hasMore);
                                    
                                    offset += currentChunkSize;
                                }
                            } else {
                                // Empty file
                                bso.putBoolean(false);
                            }
                        } else {
                            // Write unencrypted data directly
                            bso.write(processedData);
                        }
                        
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
            
            if (progressCallback != null) {
                progressCallback.onComplete("create", filesToProcess.size(), finalTotalBytes);
            }
            
            return new ArchiverAPI.OperationResult(true, "Enhanced archive created successfully: " + archivePath);
            
        } catch (Exception e) {
            // Clean up partial archive on error
            try {
                Files.deleteIfExists(Path.of(archivePath));
            } catch (Exception ignored) {}
            throw e;
        }
    }
    
    /**
     * Enhanced file information with compression and integrity data
     */
    public static class EnhancedFileInfo extends ArchiverAPI.ArchiveFileInfo {
        public final boolean isCompressed;
        public final DataIntegrity.IntegrityMetadata integrity;
        
        public EnhancedFileInfo(String name, String path, boolean canRead, boolean canExecute,
                               boolean canWrite, long lastModified, long size, boolean isCompressed,
                               DataIntegrity.IntegrityMetadata integrity) {
            super(name, path, canRead, canExecute, canWrite, lastModified, size);
            this.isCompressed = isCompressed;
            this.integrity = integrity;
        }
        
        @Override
        public String toString() {
            String compression = isCompressed ? " (compressed)" : "";
            String integrityInfo = integrity != null ? " [verified]" : "";
            return String.format("%s (%s) - %s%s%s", name, path, ArchiverAPI.formatSize(size), compression, integrityInfo);
        }
    }
    
    /**
     * Lists enhanced archive contents
     * @param archivePath Path to archive
     * @return List of enhanced file information
     * @throws Exception If archive cannot be read
     */
    public List<EnhancedFileInfo> listArchiveContents(String archivePath) throws Exception {
        if (!archivePath.endsWith(".archivit")) {
            archivePath += ".archivit";
        }
        
        if (!Files.exists(Path.of(archivePath))) {
            throw new FileNotFoundException("Archive not found: " + archivePath);
        }
        
        try (ResourceManager rm = new ResourceManager()) {
            BufferedStream.Input bsi = rm.manage(new BufferedStream.Input(new FileInputStream(archivePath)));
            List<EnhancedFileInfo> files = new ArrayList<>();
            
            // Read and verify signature
            BinaryString signature = new BinaryString();
            signature.fromByteArray(bsi.readNBytes(SIGNATURE.getSize()));
            if (!signature.data.equals(SIGNATURE.data)) {
                throw new IllegalArgumentException("Not an enhanced archive or unsupported format");
            }
            
            // Read format version
            byte version = bsi.readNBytes(1)[0];
            if (version != FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported archive format version: " + version);
            }
            
            // Read file separator
            String fileSeparator = new String(bsi.readNBytes(1), StandardCharsets.UTF_8);
            
            // Read feature flags
            byte flags = bsi.readNBytes(1)[0];
            boolean isPasswordProtected = (flags & 0x01) != 0;
            boolean hasCompression = (flags & 0x02) != 0;
            boolean hasIntegrityCheck = (flags & 0x04) != 0;
            
            // Skip nonce if password protected
            if (isPasswordProtected) {
                bsi.skipNBytes(NONCE_LENGTH);
            }
            
            // Read file entries
            while (bsi.available() > 0) {
                // Read file metadata
                SerializableFile fileInfo = new SerializableFile();
                fileInfo.fromByteArray(bsi.readSegment(BufferedStream.JavaStreamSegmentType.LONG));
                
                // Read integrity metadata if present
                DataIntegrity.IntegrityMetadata integrity = null;
                if (hasIntegrityCheck) {
                    byte[] integrityData = bsi.readSegment(BufferedStream.JavaStreamSegmentType.SHORT);
                    integrity = DataIntegrity.IntegrityMetadata.fromByteArray(integrityData);
                }
                
                // Read compression flag
                boolean isCompressed = bsi.getBoolean();
                
                // Skip file data
                if (isPasswordProtected) {
                    while (bsi.getBoolean()) {
                        bsi.skipNBytes(bsi.getLong());
                    }
                } else {
                    long dataSize = integrity != null ? integrity.compressedSize : fileInfo.size.data;
                    bsi.skipNBytes((int) dataSize);
                }
                
                // Fix file separator if needed
                if (!fileSeparator.equals(getFileSeparator())) {
                    fileInfo.path.data = fileInfo.path.data.replace(fileSeparator, getFileSeparator());
                }
                
                files.add(new EnhancedFileInfo(
                    fileInfo.name.data,
                    fileInfo.path.data,
                    fileInfo.canRead.data,
                    fileInfo.canExecute.data,
                    fileInfo.canWrite.data,
                    fileInfo.lastModified.data,
                    fileInfo.size.data,
                    isCompressed,
                    integrity
                ));
            }
            
            return files;
        }
    }
    
    private String getFileSeparator() {
        return System.getProperty("file.separator");
    }
}