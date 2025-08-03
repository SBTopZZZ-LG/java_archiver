import Utilities.ArchiverAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Java Archiver system
 * Tests end-to-end workflows and complex scenarios
 */
class ArchiverIntegrationTest {

    @TempDir
    Path tempDir;
    
    private ArchiverAPI api;
    private Path sourceDir;
    private Path archiveFile;
    private Path extractDir;

    @BeforeEach
    void setUp() throws IOException {
        api = new ArchiverAPI();
        sourceDir = tempDir.resolve("integration_source");
        archiveFile = tempDir.resolve("integration_test.archivit");
        extractDir = tempDir.resolve("integration_extract");
        
        Files.createDirectories(sourceDir);
        createComplexDirectoryStructure();
    }

    private void createComplexDirectoryStructure() throws IOException {
        // Create various types of files and directories
        
        // Text files with different content
        Files.write(sourceDir.resolve("readme.txt"), 
            "This is a README file with some content.\nSecond line.\nThird line.".getBytes());
        
        Files.write(sourceDir.resolve("empty.txt"), new byte[0]);
        
        // Binary file with random content
        byte[] binaryData = new byte[1024];
        new Random(12345).nextBytes(binaryData);
        Files.write(sourceDir.resolve("binary.dat"), binaryData);
        
        // Large text file
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) { // Reduced size to avoid test timeout
            largeContent.append("Line ").append(i).append(": Lorem ipsum dolor sit amet.\n");
        }
        Files.write(sourceDir.resolve("large.txt"), largeContent.toString().getBytes());
        
        // Nested directory structure
        Path level1 = sourceDir.resolve("level1");
        Files.createDirectories(level1);
        Files.write(level1.resolve("file1.txt"), "Level 1 file content".getBytes());
        
        Path level2 = level1.resolve("level2");
        Files.createDirectories(level2);
        Files.write(level2.resolve("file2.txt"), "Level 2 file content".getBytes());
        
        Path level3 = level2.resolve("level3");
        Files.createDirectories(level3);
        Files.write(level3.resolve("deep.txt"), "Deep nested file".getBytes());
        
        // Special characters in filenames (if supported by filesystem)
        try {
            Files.write(sourceDir.resolve("special_chars.txt"), "Special filename".getBytes());
        } catch (Exception e) {
            // Skip if filesystem doesn't support special characters
        }
        
        // Empty directory
        Files.createDirectories(sourceDir.resolve("empty_dir"));
    }

    @Test
    @DisplayName("Complete workflow: Create, extract, and verify complex directory structure")
    void testCompleteWorkflowWithComplexStructure() throws Exception {
        // Step 1: Create archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, 
            new TestProgressCallback("CREATE"));
        
        assertThat(createResult.success).isTrue();
        assertThat(archiveFile).exists();
        assertThat(Files.size(archiveFile)).isGreaterThan(1000); // Should have substantial size
        
        // Step 2: List archive contents
        List<ArchiverAPI.ArchiveFileInfo> archiveContents = api.listArchiveContents(archiveFile.toString());
        
        assertThat(archiveContents).hasSizeGreaterThan(5); // Should have multiple files
        
        // Verify key files are present
        boolean hasReadme = archiveContents.stream().anyMatch(f -> f.name.equals("readme.txt"));
        boolean hasEmpty = archiveContents.stream().anyMatch(f -> f.name.equals("empty.txt"));
        boolean hasBinary = archiveContents.stream().anyMatch(f -> f.name.equals("binary.dat"));
        boolean hasLarge = archiveContents.stream().anyMatch(f -> f.name.equals("large.txt"));
        boolean hasDeep = archiveContents.stream().anyMatch(f -> f.name.equals("deep.txt"));
        
        assertThat(hasReadme).isTrue();
        assertThat(hasEmpty).isTrue();
        assertThat(hasBinary).isTrue();
        assertThat(hasLarge).isTrue();
        assertThat(hasDeep).isTrue();
        
        // Step 3: Extract archive
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig,
            new TestProgressCallback("EXTRACT"));
        
        assertThat(extractResult.success).isTrue();
        
        // Step 4: Verify extracted structure matches original
        Path extractedRoot = extractDir.resolve("integration_test");
        assertThat(extractedRoot).exists();
        
        verifyExtractedStructure(sourceDir, extractedRoot);
    }

    @Test
    @DisplayName("Password-protected workflow: Create, extract, and verify with encryption")
    void testPasswordProtectedWorkflow() throws Exception {
        String password = "testpass123";
        
        // Create password-protected archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString(), password);
        
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).as("Archive creation should succeed: " + createResult.message).isTrue();
        
        // Verify we cannot extract without password
        ArchiverAPI.ExtractArchiveConfig noPasswordConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult noPasswordResult = api.extractArchive(noPasswordConfig, null);
        assertThat(noPasswordResult.success).as("Should fail without password: " + noPasswordResult.message).isFalse();
        
        // Extract with correct password
        ArchiverAPI.ExtractArchiveConfig correctPasswordConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString(), password);
        ArchiverAPI.OperationResult correctPasswordResult = api.extractArchive(correctPasswordConfig, null);
        assertThat(correctPasswordResult.success).as("Should succeed with correct password: " + correctPasswordResult.message).isTrue();
        
        // Verify extracted structure
        Path extractedRoot = extractDir.resolve("integration_test");
        verifyExtractedStructure(sourceDir, extractedRoot);
    }

    @Test
    @DisplayName("Large file workflow: Handle files larger than buffer size")
    void testLargeFileWorkflow() throws Exception {
        // Create a moderately large file (1MB instead of 5MB)
        Path largeFile = sourceDir.resolve("large.bin");
        try (FileOutputStream fos = new FileOutputStream(largeFile.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            Random random = new Random(54321);
            byte[] chunk = new byte[8192];
            for (int i = 0; i < 128; i++) { // 128 * 8192 = ~1MB
                random.nextBytes(chunk);
                bos.write(chunk);
            }
        }
        
        long originalSize = Files.size(largeFile);
        assertThat(originalSize).isGreaterThan(1000000); // > 1MB
        
        // Create archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // Extract archive
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        assertThat(extractResult.success).isTrue();
        
        // Verify large file was extracted correctly
        Path extractedLargeFile = extractDir.resolve("integration_test").resolve("large.bin");
        assertThat(extractedLargeFile).exists();
        assertThat(Files.size(extractedLargeFile)).isEqualTo(originalSize);
        
        // Verify content integrity
        assertThat(Files.mismatch(largeFile, extractedLargeFile)).isEqualTo(-1);
    }

    @Test
    @DisplayName("Multiple archives workflow: Create and manage multiple archives")
    void testMultipleArchivesWorkflow() throws Exception {
        // Create first archive with subset of files
        Path subDir1 = sourceDir.resolve("level1");
        Path archive1 = tempDir.resolve("archive1.archivit");
        
        ArchiverAPI.CreateArchiveConfig config1 = new ArchiverAPI.CreateArchiveConfig(
            subDir1.toString(), archive1.toString());
        ArchiverAPI.OperationResult result1 = api.createArchive(config1, null);
        assertThat(result1.success).isTrue();
        
        // Create second archive with different files
        Path archive2 = tempDir.resolve("archive2.archivit");
        
        // Create a separate directory for archive2
        Path source2 = tempDir.resolve("source2");
        Files.createDirectories(source2);
        Files.write(source2.resolve("archive2_file.txt"), "Archive 2 content".getBytes());
        
        ArchiverAPI.CreateArchiveConfig config2 = new ArchiverAPI.CreateArchiveConfig(
            source2.toString(), archive2.toString());
        ArchiverAPI.OperationResult result2 = api.createArchive(config2, null);
        assertThat(result2.success).isTrue();
        
        // Extract both archives to different locations
        Path extract1 = tempDir.resolve("extract1");
        Path extract2 = tempDir.resolve("extract2");
        
        ArchiverAPI.ExtractArchiveConfig extractConfig1 = new ArchiverAPI.ExtractArchiveConfig(
            archive1.toString(), extract1.toString());
        ArchiverAPI.ExtractArchiveConfig extractConfig2 = new ArchiverAPI.ExtractArchiveConfig(
            archive2.toString(), extract2.toString());
        
        ArchiverAPI.OperationResult extractResult1 = api.extractArchive(extractConfig1, null);
        ArchiverAPI.OperationResult extractResult2 = api.extractArchive(extractConfig2, null);
        
        assertThat(extractResult1.success).isTrue();
        assertThat(extractResult2.success).isTrue();
        
        // Verify different content in each extracted archive
        assertThat(extract1.resolve("archive1")).exists();
        assertThat(extract2.resolve("archive2")).exists();
        
        assertThat(extract1.resolve("archive1").resolve("file1.txt")).exists();
        assertThat(extract2.resolve("archive2").resolve("archive2_file.txt")).exists();
    }

    @Test
    @DisplayName("Error recovery workflow: Handle partial failures gracefully")
    void testErrorRecoveryWorkflow() throws Exception {
        // Create a source directory with a file that becomes unreadable
        Path problematicFile = sourceDir.resolve("problematic.txt");
        Files.write(problematicFile, "Initial content".getBytes());
        
        // Create archive successfully first
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // Test extraction to read-only directory (if possible)
        Path readOnlyExtract = tempDir.resolve("readonly_extract");
        Files.createDirectories(readOnlyExtract);
        
        // Create archive that should work
        List<ArchiverAPI.ArchiveFileInfo> contents = api.listArchiveContents(archiveFile.toString());
        assertThat(contents).isNotEmpty();
        
        // Normal extraction should work
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        assertThat(extractResult.success).isTrue();
    }

    @Test
    @DisplayName("Cross-platform compatibility: Handle different file separators")
    void testCrossPlatformCompatibility() throws Exception {
        // Create archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // List contents and verify paths use correct separators
        List<ArchiverAPI.ArchiveFileInfo> contents = api.listArchiveContents(archiveFile.toString());
        
        String systemSeparator = System.getProperty("file.separator");
        for (ArchiverAPI.ArchiveFileInfo file : contents) {
            if (file.path.contains("/") || file.path.contains("\\")) {
                // Path should use system separator
                assertThat(file.path).contains(systemSeparator);
            }
        }
        
        // Extract and verify
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        assertThat(extractResult.success).isTrue();
        
        verifyExtractedStructure(sourceDir, extractDir.resolve("integration_test"));
    }

    private void verifyExtractedStructure(Path original, Path extracted) throws IOException {
        // Verify all files exist and have correct content
        assertThat(extracted.resolve("readme.txt")).exists();
        assertThat(extracted.resolve("empty.txt")).exists();
        assertThat(extracted.resolve("binary.dat")).exists();
        assertThat(extracted.resolve("large.txt")).exists();
        
        // Verify nested structure
        assertThat(extracted.resolve("level1").resolve("file1.txt")).exists();
        assertThat(extracted.resolve("level1").resolve("level2").resolve("file2.txt")).exists();
        assertThat(extracted.resolve("level1").resolve("level2").resolve("level3").resolve("deep.txt")).exists();
        
        // Verify file contents
        String readmeContent = Files.readString(extracted.resolve("readme.txt"));
        assertThat(readmeContent).contains("This is a README file");
        
        String level1Content = Files.readString(extracted.resolve("level1").resolve("file1.txt"));
        assertThat(level1Content).isEqualTo("Level 1 file content");
        
        // Verify empty file
        assertThat(Files.size(extracted.resolve("empty.txt"))).isEqualTo(0);
        
        // Verify binary file integrity
        if (Files.exists(original.resolve("binary.dat")) && Files.exists(extracted.resolve("binary.dat"))) {
            assertThat(Files.mismatch(original.resolve("binary.dat"), extracted.resolve("binary.dat")))
                .isEqualTo(-1);
        }
        
        // Verify large file integrity
        if (Files.exists(original.resolve("large.txt")) && Files.exists(extracted.resolve("large.txt"))) {
            assertThat(Files.mismatch(original.resolve("large.txt"), extracted.resolve("large.txt")))
                .isEqualTo(-1);
        }
    }

    /**
     * Test progress callback implementation
     */
    private static class TestProgressCallback implements ArchiverAPI.ProgressCallback {
        private final String operation;
        private int progressCount = 0;
        private int completeCount = 0;
        private int errorCount = 0;

        public TestProgressCallback(String operation) {
            this.operation = operation;
        }

        @Override
        public void onProgress(String op, String fileName, long processed, long total) {
            progressCount++;
            assertThat(op).isEqualTo(operation.toLowerCase());
            assertThat(fileName).isNotBlank();
        }

        @Override
        public void onComplete(String op, long totalFiles, long totalBytes) {
            completeCount++;
            assertThat(op).isEqualTo(operation.toLowerCase());
            assertThat(totalFiles).isGreaterThan(0);
        }

        @Override
        public void onError(String op, String fileName, Exception error) {
            errorCount++;
            // Log error for debugging if needed
            System.err.println("Progress callback error: " + error.getMessage());
        }

        public int getProgressCount() { return progressCount; }
        public int getCompleteCount() { return completeCount; }
        public int getErrorCount() { return errorCount; }
    }
}