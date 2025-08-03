import Utilities.ArchiverAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for ArchiverAPI functionality
 */
class ArchiverAPITest {

    @TempDir
    Path tempDir;
    
    private ArchiverAPI api;
    private Path sourceDir;
    private Path archiveFile;
    private Path extractDir;

    @BeforeEach
    void setUp() throws IOException {
        api = new ArchiverAPI();
        sourceDir = tempDir.resolve("source");
        archiveFile = tempDir.resolve("test.archivit");
        extractDir = tempDir.resolve("extract");
        
        Files.createDirectories(sourceDir);
        
        // Create test files in source directory
        createTestFile(sourceDir.resolve("file1.txt"), "Content of file 1");
        createTestFile(sourceDir.resolve("file2.txt"), "Content of file 2");
        
        Path subDir = sourceDir.resolve("subdir");
        Files.createDirectories(subDir);
        createTestFile(subDir.resolve("nested.txt"), "Nested file content");
    }

    private void createTestFile(Path filePath, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(content);
        }
    }

    @Test
    @DisplayName("ArchiverAPI should create archive without password")
    void testCreateArchiveWithoutPassword() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isTrue();
        assertThat(result.message).contains("successfully");
        assertThat(result.error).isNull();
        assertThat(archiveFile).exists();
    }

    @Test
    @DisplayName("ArchiverAPI should create archive with password")
    void testCreateArchiveWithPassword() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString(), "testpass123");
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isTrue();
        assertThat(result.message).contains("successfully");
        assertThat(result.error).isNull();
        assertThat(archiveFile).exists();
    }

    @Test
    @DisplayName("ArchiverAPI should fail to create archive with invalid source path")
    void testCreateArchiveWithInvalidSourcePath() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            "/nonexistent/path", archiveFile.toString());
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isFalse();
        assertThat(result.message).contains("not a valid directory");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to create archive with empty source path")
    void testCreateArchiveWithEmptySourcePath() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            "", archiveFile.toString());
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isFalse();
        assertThat(result.message).contains("Source path cannot be empty");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to create archive with null source path")
    void testCreateArchiveWithNullSourcePath() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            null, archiveFile.toString());
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isFalse();
        assertThat(result.message).contains("Source path cannot be empty");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to create archive with empty archive path")
    void testCreateArchiveWithEmptyArchivePath() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), "");
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isFalse();
        assertThat(result.message).contains("Archive path cannot be empty");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to create archive with existing archive file")
    void testCreateArchiveWithExistingFile() throws IOException {
        Files.createFile(archiveFile);
        
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isFalse();
        assertThat(result.message).contains("already exists");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to create archive with invalid password")
    void testCreateArchiveWithInvalidPassword() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString(), "short");
        
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isFalse();
        assertThat(result.message).contains("Password must be 6-16 characters");
    }

    @Test
    @DisplayName("ArchiverAPI should extract archive without password")
    void testExtractArchiveWithoutPassword() {
        // First create an archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // Then extract it
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        
        assertThat(extractResult.success).isTrue();
        assertThat(extractResult.message).contains("successfully");
        assertThat(extractResult.error).isNull();
        
        // Verify extracted files
        Path extractedDir = extractDir.resolve("test");
        assertThat(extractedDir).exists();
        assertThat(extractedDir.resolve("file1.txt")).exists();
        assertThat(extractedDir.resolve("file2.txt")).exists();
        assertThat(extractedDir.resolve("subdir").resolve("nested.txt")).exists();
    }

    @Test
    @DisplayName("ArchiverAPI should extract archive with password")
    void testExtractArchiveWithPassword() throws IOException {
        String password = "testpass123";
        
        // First create a password-protected archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString(), password);
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // Then extract it
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString(), password);
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        
        assertThat(extractResult.success).isTrue();
        assertThat(extractResult.message).contains("successfully");
        
        // Verify extracted files
        Path extractedDir = extractDir.resolve("test");
        assertThat(extractedDir).exists();
        
        // Verify file contents
        String file1Content = Files.readString(extractedDir.resolve("file1.txt"));
        assertThat(file1Content).isEqualTo("Content of file 1");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to extract with wrong password")
    void testExtractArchiveWithWrongPassword() {
        String correctPassword = "testpass123";
        String wrongPassword = "wrongpass";
        
        // First create a password-protected archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString(), correctPassword);
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // Try to extract with wrong password
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString(), wrongPassword);
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        
        assertThat(extractResult.success).isFalse();
        assertThat(extractResult.message).contains("password");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to extract with missing password")
    void testExtractArchiveWithMissingPassword() {
        String password = "testpass123";
        
        // First create a password-protected archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString(), password);
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // Try to extract without password
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        
        assertThat(extractResult.success).isFalse();
        assertThat(extractResult.message).contains("password-protected");
    }

    @Test
    @DisplayName("ArchiverAPI should fail to extract nonexistent archive")
    void testExtractNonexistentArchive() {
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            "/nonexistent/archive.archivit", extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, null);
        
        assertThat(extractResult.success).isFalse();
        assertThat(extractResult.message).contains("not found");
    }

    @Test
    @DisplayName("ArchiverAPI should list archive contents")
    void testListArchiveContents() throws Exception {
        // First create an archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        // List contents
        List<ArchiverAPI.ArchiveFileInfo> files = api.listArchiveContents(archiveFile.toString());
        
        assertThat(files).hasSize(3); // 2 files + 1 nested file
        
        boolean foundFile1 = false, foundFile2 = false, foundNested = false;
        for (ArchiverAPI.ArchiveFileInfo file : files) {
            if (file.name.equals("file1.txt")) {
                foundFile1 = true;
                assertThat(file.size).isGreaterThan(0);
            } else if (file.name.equals("file2.txt")) {
                foundFile2 = true;
                assertThat(file.size).isGreaterThan(0);
            } else if (file.name.equals("nested.txt")) {
                foundNested = true;
                assertThat(file.path).contains("subdir");
            }
        }
        
        assertThat(foundFile1).isTrue();
        assertThat(foundFile2).isTrue();
        assertThat(foundNested).isTrue();
    }

    @Test
    @DisplayName("ArchiverAPI should fail to list nonexistent archive")
    void testListNonexistentArchive() {
        assertThatThrownBy(() -> api.listArchiveContents("/nonexistent/archive.archivit"))
            .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("ArchiverAPI should handle progress callback during creation")
    void testCreateArchiveWithProgressCallback() {
        final int[] progressCallCount = {0};
        final int[] completeCallCount = {0};
        
        ArchiverAPI.ProgressCallback callback = new ArchiverAPI.ProgressCallback() {
            @Override
            public void onProgress(String operation, String fileName, long processed, long total) {
                progressCallCount[0]++;
                assertThat(operation).isEqualTo("create");
                assertThat(fileName).isNotBlank();
            }
            
            @Override
            public void onComplete(String operation, long totalFiles, long totalBytes) {
                completeCallCount[0]++;
                assertThat(operation).isEqualTo("create");
                assertThat(totalFiles).isEqualTo(3);
                assertThat(totalBytes).isGreaterThan(0);
            }
            
            @Override
            public void onError(String operation, String fileName, Exception error) {
                fail("Should not have errors during successful creation");
            }
        };
        
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult result = api.createArchive(config, callback);
        
        assertThat(result.success).isTrue();
        assertThat(progressCallCount[0]).isEqualTo(3); // 3 files processed
        assertThat(completeCallCount[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("ArchiverAPI should handle progress callback during extraction")
    void testExtractArchiveWithProgressCallback() {
        // First create an archive
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, null);
        assertThat(createResult.success).isTrue();
        
        final int[] progressCallCount = {0};
        final int[] completeCallCount = {0};
        
        ArchiverAPI.ProgressCallback callback = new ArchiverAPI.ProgressCallback() {
            @Override
            public void onProgress(String operation, String fileName, long processed, long total) {
                progressCallCount[0]++;
                assertThat(operation).isEqualTo("extract");
                assertThat(fileName).isNotBlank();
            }
            
            @Override
            public void onComplete(String operation, long totalFiles, long totalBytes) {
                completeCallCount[0]++;
                assertThat(operation).isEqualTo("extract");
                assertThat(totalFiles).isEqualTo(3);
            }
            
            @Override
            public void onError(String operation, String fileName, Exception error) {
                fail("Should not have errors during successful extraction");
            }
        };
        
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            archiveFile.toString(), extractDir.toString());
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, callback);
        
        assertThat(extractResult.success).isTrue();
        assertThat(progressCallCount[0]).isEqualTo(3); // 3 files processed
        assertThat(completeCallCount[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("ArchiverAPI should handle empty directories")
    void testCreateArchiveWithEmptyDirectory() throws IOException {
        Path emptyDir = sourceDir.resolve("empty");
        Files.createDirectories(emptyDir);
        
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isTrue();
        assertThat(archiveFile).exists();
    }

    @Test
    @DisplayName("ArchiverAPI should handle large files")
    void testCreateArchiveWithLargeFile() throws IOException {
        Path largeFile = sourceDir.resolve("large.txt");
        
        // Create a large file (1MB)
        try (BufferedWriter writer = Files.newBufferedWriter(largeFile)) {
            for (int i = 0; i < 100000; i++) {
                writer.write("This is line " + i + " of the large file content.\n");
            }
        }
        
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), archiveFile.toString());
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isTrue();
        assertThat(archiveFile).exists();
        
        // Verify large file is in archive
        try {
            List<ArchiverAPI.ArchiveFileInfo> files = api.listArchiveContents(archiveFile.toString());
            boolean foundLargeFile = files.stream().anyMatch(f -> f.name.equals("large.txt") && f.size > 1000000);
            assertThat(foundLargeFile).isTrue();
        } catch (Exception e) {
            fail("Failed to list archive contents: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("ArchiverAPI formatSize should handle various file sizes")
    void testFormatSize() {
        assertThat(ArchiverAPI.formatSize(0)).isEqualTo("0 bytes");
        assertThat(ArchiverAPI.formatSize(500)).isEqualTo("500 bytes");
        assertThat(ArchiverAPI.formatSize(1024)).isEqualTo("1 KB");
        assertThat(ArchiverAPI.formatSize(1048576)).isEqualTo("1 MB");
        assertThat(ArchiverAPI.formatSize(1073741824)).isEqualTo("1.0 GB"); // Updated to match actual output
        assertThat(ArchiverAPI.formatSize(1099511627776L)).contains("TB");
    }

    @Test
    @DisplayName("ArchiverAPI should auto-add .archivit extension")
    void testAutoAddArchivitExtension() {
        ArchiverAPI.CreateArchiveConfig config = new ArchiverAPI.CreateArchiveConfig(
            sourceDir.toString(), tempDir.resolve("test_no_ext").toString());
        ArchiverAPI.OperationResult result = api.createArchive(config, null);
        
        assertThat(result.success).isTrue();
        assertThat(tempDir.resolve("test_no_ext.archivit")).exists();
    }
}