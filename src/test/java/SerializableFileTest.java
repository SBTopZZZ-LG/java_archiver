import Utilities.SerializableFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SerializableFile functionality
 */
class SerializableFileTest {

    @TempDir
    Path tempDir;
    
    private Path testFile;
    private String testContent = "Test file content for serialization";

    @BeforeEach
    void setUp() throws IOException {
        testFile = tempDir.resolve("test.txt");
        Files.write(testFile, testContent.getBytes());
    }

    @Test
    @DisplayName("SerializableFile should serialize file metadata correctly")
    void testSerializableFileCreation() {
        SerializableFile sf = new SerializableFile(testFile.toString(), "relative/test.txt");
        
        assertThat(sf.name.data).isEqualTo("test.txt");
        assertThat(sf.path.data).isEqualTo("relative/test.txt");
        assertThat(sf.size.data).isEqualTo(testContent.length());
        assertThat(sf.canRead.data).isTrue();
        assertThat(sf.lastModified.data).isGreaterThan(0);
    }

    @Test
    @DisplayName("SerializableFile should handle files without relative path")
    void testSerializableFileWithoutRelativePath() {
        SerializableFile sf = new SerializableFile(testFile.toString(), null);
        
        assertThat(sf.name.data).isEqualTo("test.txt");
        assertThat(sf.path.data).isEqualTo(testFile.toString());
        assertThat(sf.size.data).isEqualTo(testContent.length());
    }

    @Test
    @DisplayName("SerializableFile should reject nonexistent files")
    void testSerializableFileNonexistentFile() {
        String nonexistentPath = tempDir.resolve("nonexistent.txt").toString();
        
        assertThatThrownBy(() -> new SerializableFile(nonexistentPath, "relative/path"))
            .isInstanceOf(InvalidPathException.class);
    }

    @Test
    @DisplayName("SerializableFile should reject directories")
    void testSerializableFileDirectory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Files.createDirectories(dir);
        
        assertThatThrownBy(() -> new SerializableFile(dir.toString(), "relative/dir"))
            .isInstanceOf(InvalidPathException.class);
    }

    @Test
    @DisplayName("SerializableFile should serialize and deserialize correctly")
    void testSerializableFileSerializationRoundTrip() {
        SerializableFile original = new SerializableFile(testFile.toString(), "relative/test.txt");
        
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotEmpty();
        
        SerializableFile deserialized = new SerializableFile();
        deserialized.fromByteArray(serialized);
        
        assertThat(deserialized.name.data).isEqualTo(original.name.data);
        assertThat(deserialized.path.data).isEqualTo(original.path.data);
        assertThat(deserialized.size.data).isEqualTo(original.size.data);
        assertThat(deserialized.canRead.data).isEqualTo(original.canRead.data);
        assertThat(deserialized.canExecute.data).isEqualTo(original.canExecute.data);
        assertThat(deserialized.canWrite.data).isEqualTo(original.canWrite.data);
        assertThat(deserialized.lastModified.data).isEqualTo(original.lastModified.data);
    }

    @Test
    @DisplayName("SerializableFile should handle files with special characters in name")
    void testSerializableFileSpecialCharacters() throws IOException {
        Path specialFile = tempDir.resolve("test file with spaces & symbols!.txt");
        Files.write(specialFile, "Special content".getBytes());
        
        SerializableFile sf = new SerializableFile(specialFile.toString(), "relative/special.txt");
        
        assertThat(sf.name.data).isEqualTo("test file with spaces & symbols!.txt");
        assertThat(sf.path.data).isEqualTo("relative/special.txt");
        
        // Test serialization round trip
        byte[] serialized = sf.toByteArray();
        SerializableFile deserialized = new SerializableFile();
        deserialized.fromByteArray(serialized);
        
        assertThat(deserialized.name.data).isEqualTo(sf.name.data);
        assertThat(deserialized.path.data).isEqualTo(sf.path.data);
    }

    @Test
    @DisplayName("SerializableFile should handle empty files")
    void testSerializableFileEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.write(emptyFile, new byte[0]);
        
        SerializableFile sf = new SerializableFile(emptyFile.toString(), "relative/empty.txt");
        
        assertThat(sf.name.data).isEqualTo("empty.txt");
        assertThat(sf.size.data).isEqualTo(0);
        
        byte[] serialized = sf.toByteArray();
        SerializableFile deserialized = new SerializableFile();
        deserialized.fromByteArray(serialized);
        
        assertThat(deserialized.size.data).isEqualTo(0);
    }

    @Test
    @DisplayName("SerializableFile should calculate size correctly")
    void testSerializableFileGetSize() {
        SerializableFile sf = new SerializableFile(testFile.toString(), "relative/test.txt");
        
        int size = sf.getSize();
        assertThat(size).isGreaterThan(0);
        
        // Size should include metadata sizes plus 4 bytes for length indicators
        int expectedMinSize = sf.name.getSize() + sf.path.getSize() + 
                             sf.canRead.getSize() + sf.canExecute.getSize() + sf.canWrite.getSize() +
                             sf.lastModified.getSize() + sf.size.getSize() + 4;
        
        assertThat(size).isEqualTo(expectedMinSize);
    }

    @Test
    @DisplayName("SerializableFile should handle very long filenames")
    void testSerializableFileLongFilename() throws IOException {
        // Create a file with a very long name (but within filesystem limits)
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longName.append("VeryLongFileName");
        }
        longName.append(".txt");
        
        try {
            Path longFile = tempDir.resolve(longName.toString());
            Files.write(longFile, "Long filename content".getBytes());
            
            SerializableFile sf = new SerializableFile(longFile.toString(), "relative/long.txt");
            
            assertThat(sf.name.data).isEqualTo(longName.toString());
            
            // Test serialization
            byte[] serialized = sf.toByteArray();
            SerializableFile deserialized = new SerializableFile();
            deserialized.fromByteArray(serialized);
            
            assertThat(deserialized.name.data).isEqualTo(longName.toString());
        } catch (Exception e) {
            // Skip if filesystem doesn't support such long names
            System.out.println("Skipping long filename test: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("SerializableFile should create file with callback")
    void testSerializableFileCreateFileWithCallback() throws IOException {
        SerializableFile sf = new SerializableFile(testFile.toString(), "relative/test.txt");
        
        Path targetFile = tempDir.resolve("created_file.txt");
        sf.path.data = targetFile.toString();
        
        boolean result = sf.createFile(new SerializableFile.CreateFileCallback() {
            @Override
            public void writeBinaryData(File file) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write("New file content".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        assertThat(result).isTrue();
        assertThat(targetFile).exists();
        assertThat(Files.readString(targetFile)).isEqualTo("New file content");
    }

    @Test
    @DisplayName("SerializableFile createFile should handle callback exceptions gracefully")
    void testSerializableFileCreateFileWithFailingCallback() throws IOException {
        SerializableFile sf = new SerializableFile(testFile.toString(), "relative/test.txt");
        
        Path targetFile = tempDir.resolve("callback_fail.txt");
        sf.path.data = targetFile.toString();
        
        assertThatThrownBy(() -> {
            sf.createFile(new SerializableFile.CreateFileCallback() {
                @Override
                public void writeBinaryData(File file) {
                    throw new RuntimeException("Callback failed intentionally");
                }
            });
        }).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Callback failed intentionally");
    }

    @Test
    @DisplayName("SerializableFile should handle default constructor")
    void testSerializableFileDefaultConstructor() {
        SerializableFile sf = new SerializableFile();
        
        assertThat(sf.name).isNotNull();
        assertThat(sf.path).isNotNull();
        assertThat(sf.canRead).isNotNull();
        assertThat(sf.canExecute).isNotNull();
        assertThat(sf.canWrite).isNotNull();
        assertThat(sf.lastModified).isNotNull();
        assertThat(sf.size).isNotNull();
        
        // Default values should be empty/false/zero
        assertThat(sf.name.data).isNull();
        assertThat(sf.path.data).isNull();
        assertThat(sf.canRead.data).isFalse();
        assertThat(sf.canExecute.data).isFalse();
        assertThat(sf.canWrite.data).isFalse();
        assertThat(sf.lastModified.data).isEqualTo(0);
        assertThat(sf.size.data).isEqualTo(0);
    }
}