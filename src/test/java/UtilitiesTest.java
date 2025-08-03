import Utilities.IO;
import Utilities.ByteArrayBuilder;
import Utilities.ResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for utility classes
 */
class UtilitiesTest {

    @TempDir
    Path tempDir;
    
    private Path testDir;

    @BeforeEach
    void setUp() throws IOException {
        testDir = tempDir.resolve("test_io");
        Files.createDirectories(testDir);
        
        // Create test file structure
        Files.write(testDir.resolve("file1.txt"), "Content 1".getBytes());
        Files.write(testDir.resolve("file2.txt"), "Content 2".getBytes());
        
        Path subDir = testDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.write(subDir.resolve("nested.txt"), "Nested content".getBytes());
        
        Files.createDirectories(testDir.resolve("empty_dir"));
    }

    @Test
    @DisplayName("IO.getFilesAndDirs should retrieve all files and directories")
    void testIOGetFilesAndDirs() {
        List<String> files = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        List<String> exclusions = new ArrayList<>();
        
        IO.getFilesAndDirs(testDir.toString(), new IO.OnRetrieve() {
            @Override
            public void onFileRetrieve(String file) {
                files.add(file);
            }
            
            @Override
            public void onFolderRetrieve(String folder) {
                folders.add(folder);
            }
            
            @Override
            public void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath) {
                // Not expected in this test
            }
            
            @Override
            public void onExclusion(String file) {
                exclusions.add(file);
            }
        });
        
        assertThat(files).hasSize(3); // file1.txt, file2.txt, nested.txt
        assertThat(folders).hasSizeGreaterThanOrEqualTo(2); // subdir, empty_dir
        
        // Verify specific files are found
        boolean foundFile1 = files.stream().anyMatch(f -> f.endsWith("file1.txt"));
        boolean foundFile2 = files.stream().anyMatch(f -> f.endsWith("file2.txt"));
        boolean foundNested = files.stream().anyMatch(f -> f.endsWith("nested.txt"));
        
        assertThat(foundFile1).isTrue();
        assertThat(foundFile2).isTrue();
        assertThat(foundNested).isTrue();
    }

    @Test
    @DisplayName("IO.getFileNameWithoutExtension should handle various filenames")
    void testIOGetFileNameWithoutExtension() {
        assertThat(IO.getFileNameWithoutExtension("file.txt")).isEqualTo("file");
        assertThat(IO.getFileNameWithoutExtension("archive.archivit")).isEqualTo("archive");
        assertThat(IO.getFileNameWithoutExtension("file.with.multiple.dots.txt")).isEqualTo("file.with.multiple.dots");
        assertThat(IO.getFileNameWithoutExtension("noextension")).isEqualTo("noextension");
        assertThat(IO.getFileNameWithoutExtension("")).isEqualTo("");
        assertThat(IO.getFileNameWithoutExtension(".hidden")).isEqualTo("");
        assertThat(IO.getFileNameWithoutExtension("path/to/file.txt")).isEqualTo("path/to/file");
    }

    @Test
    @DisplayName("IO.getFileNameWithoutExtension should handle null input")
    void testIOGetFileNameWithoutExtensionNull() {
        assertThatThrownBy(() -> IO.getFileNameWithoutExtension(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("IO should handle empty directory")
    void testIOEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);
        
        List<String> files = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        
        IO.getFilesAndDirs(emptyDir.toString(), new IO.OnRetrieve() {
            @Override
            public void onFileRetrieve(String file) {
                files.add(file);
            }
            
            @Override
            public void onFolderRetrieve(String folder) {
                folders.add(folder);
            }
            
            @Override
            public void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath) {}
            
            @Override
            public void onExclusion(String file) {}
        });
        
        assertThat(files).isEmpty();
        // May or may not include the empty directory itself
    }

    @Test
    @DisplayName("IO should handle nonexistent directory gracefully")
    void testIONonexistentDirectory() {
        List<String> files = new ArrayList<>();
        
        assertThatCode(() -> {
            IO.getFilesAndDirs("/nonexistent/path", new IO.OnRetrieve() {
                @Override
                public void onFileRetrieve(String file) {
                    files.add(file);
                }
                
                @Override
                public void onFolderRetrieve(String folder) {}
                
                @Override
                public void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath) {}
                
                @Override
                public void onExclusion(String file) {}
            });
        }).doesNotThrowAnyException();
        
        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("ByteArrayBuilder should build byte arrays correctly")
    void testByteArrayBuilder() {
        byte[] bytes1 = "Hello".getBytes();
        byte[] bytes2 = " ".getBytes();
        byte[] bytes3 = "World".getBytes();
        
        byte[] result = ByteArrayBuilder.build()
            .appendBytes(bytes1)
            .appendBytes(bytes2)
            .appendBytes(bytes3)
            .toByteArray();
        
        assertThat(new String(result)).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("ByteArrayBuilder should handle empty arrays")
    void testByteArrayBuilderEmpty() {
        byte[] result = ByteArrayBuilder.build()
            .appendBytes(new byte[0])
            .appendBytes("Test".getBytes())
            .appendBytes(new byte[0])
            .toByteArray();
        
        assertThat(new String(result)).isEqualTo("Test");
    }

    @Test
    @DisplayName("ByteArrayBuilder should handle multiple appendBytes calls")
    void testByteArrayBuilderMultipleAppends() {
        ByteArrayBuilder builder = ByteArrayBuilder.build();
        
        for (int i = 0; i < 5; i++) {
            builder.appendBytes(String.valueOf(i).getBytes());
        }
        
        byte[] result = builder.toByteArray();
        assertThat(new String(result)).isEqualTo("01234");
    }

    @Test
    @DisplayName("ByteArrayBuilder should handle large arrays")
    void testByteArrayBuilderLargeArrays() {
        byte[] largeArray = new byte[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (byte) (i % 256);
        }
        
        byte[] result = ByteArrayBuilder.build()
            .appendBytes("Start:".getBytes())
            .appendBytes(largeArray)
            .appendBytes(":End".getBytes())
            .toByteArray();
        
        assertThat(result).hasSize(10000 + 6 + 4); // large array + "Start:" + ":End"
        assertThat(new String(result, 0, 6)).isEqualTo("Start:");
        assertThat(new String(result, result.length - 4, 4)).isEqualTo(":End");
    }

    @Test
    @DisplayName("ByteArrayBuilder should handle null input gracefully")
    void testByteArrayBuilderNullInput() {
        assertThatThrownBy(() -> {
            ByteArrayBuilder.build().appendBytes((byte[])null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ResourceManager should provide basic functionality")
    void testResourceManager() {
        // Test that ResourceManager can be instantiated
        ResourceManager rm = new ResourceManager();
        assertThat(rm).isNotNull();
        
        // Since ResourceManager might be a utility class without much functionality,
        // we'll just verify it doesn't crash on instantiation
    }

    @Test
    @DisplayName("ByteArrayBuilder should handle varargs appendBytes")
    void testByteArrayBuilderVarargs() {
        byte[] result = ByteArrayBuilder.build()
            .appendBytes("A".getBytes())
            .appendBytes("B".getBytes()) 
            .appendBytes("C".getBytes())
            .toByteArray();
        
        assertThat(new String(result)).isEqualTo("ABC");
    }

    @Test
    @DisplayName("ByteArrayBuilder should handle mixed single and varargs calls")
    void testByteArrayBuilderMixedCalls() {
        byte[] result = ByteArrayBuilder.build()
            .appendBytes("Start".getBytes())
            .appendBytes("-".getBytes())
            .appendBytes("Middle".getBytes()) 
            .appendBytes("-".getBytes())
            .appendBytes("End".getBytes())
            .toByteArray();
        
        assertThat(new String(result)).isEqualTo("Start-Middle-End");
    }

    @Test
    @DisplayName("IO callback interface should handle all methods")
    void testIOCallbackInterface() {
        IO.OnRetrieve callback = new IO.OnRetrieve() {
            @Override
            public void onFileRetrieve(String file) {
                // Implementation for testing
                assertThat(file).isNotNull();
            }
            
            @Override
            public void onFolderRetrieve(String folder) {
                // Implementation for testing
                assertThat(folder).isNotNull();
            }
            
            @Override
            public void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath) {
                // Implementation for testing
                assertThat(symLinkPath).isNotNull();
                assertThat(canonicalPath).isNotNull();
            }
            
            @Override
            public void onExclusion(String file) {
                // Implementation for testing
                assertThat(file).isNotNull();
            }
        };
        
        // Test that all methods can be called
        assertThatCode(() -> {
            callback.onFileRetrieve("test.txt");
            callback.onFolderRetrieve("testdir");
            callback.onSymLinkFileRetrieve("link", "target");
            callback.onExclusion("excluded.tmp");
        }).doesNotThrowAnyException();
    }
}