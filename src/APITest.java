import Utilities.ArchiverAPI;
import java.io.File;
import java.util.List;

/**
 * Test class to demonstrate and validate the non-interactive ArchiverAPI
 */
public class APITest {
    
    public static void main(String[] args) {
        System.out.println("Testing Non-Interactive Archiver API");
        System.out.println("====================================");
        
        ArchiverAPI api = new ArchiverAPI();
        boolean allTestsPassed = true;
        
        // Test 1: Create archive without password
        System.out.println("\n1. Testing archive creation (no password)...");
        ArchiverAPI.CreateArchiveConfig createConfig = new ArchiverAPI.CreateArchiveConfig(
            "/home/runner/work/java_archiver/java_archiver/test_data/sample_dir",
            "/home/runner/work/java_archiver/java_archiver/api_test_output"
        );
        
        ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, new ArchiverAPI.ProgressCallback() {
            @Override
            public void onProgress(String operation, String fileName, long processed, long total) {
                System.out.println("  Processing: " + fileName + " (" + processed + "/" + total + ")");
            }
            
            @Override
            public void onComplete(String operation, long totalFiles, long totalBytes) {
                System.out.println("  Completed: " + totalFiles + " files, " + totalBytes + " bytes");
            }
            
            @Override
            public void onError(String operation, String fileName, Exception error) {
                System.err.println("  Error processing " + fileName + ": " + error.getMessage());
            }
        });
        
        if (createResult.success) {
            System.out.println("✓ " + createResult.message);
        } else {
            System.err.println("✗ " + createResult.message);
            if (createResult.error != null) {
                createResult.error.printStackTrace();
            }
            allTestsPassed = false;
        }
        
        // Test 2: List archive contents
        System.out.println("\n2. Testing archive listing...");
        try {
            List<ArchiverAPI.ArchiveFileInfo> files = api.listArchiveContents("api_test_output.archivit");
            System.out.println("✓ Archive contains " + files.size() + " files:");
            for (ArchiverAPI.ArchiveFileInfo file : files) {
                System.out.println("  - " + file.toString());
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to list archive: " + e.getMessage());
            allTestsPassed = false;
        }
        
        // Test 3: Extract archive
        System.out.println("\n3. Testing archive extraction...");
        ArchiverAPI.ExtractArchiveConfig extractConfig = new ArchiverAPI.ExtractArchiveConfig(
            "/home/runner/work/java_archiver/java_archiver/api_test_output.archivit",
            "/home/runner/work/java_archiver/java_archiver/api_extract_test"
        );
        
        ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig, new ArchiverAPI.ProgressCallback() {
            @Override
            public void onProgress(String operation, String fileName, long processed, long total) {
                System.out.println("  Extracting: " + fileName);
            }
            
            @Override
            public void onComplete(String operation, long totalFiles, long totalBytes) {
                System.out.println("  Extracted: " + totalFiles + " files");
            }
            
            @Override
            public void onError(String operation, String fileName, Exception error) {
                System.err.println("  Error extracting " + fileName + ": " + error.getMessage());
            }
        });
        
        if (extractResult.success) {
            System.out.println("✓ " + extractResult.message);
        } else {
            System.err.println("✗ " + extractResult.message);
            if (extractResult.error != null) {
                extractResult.error.printStackTrace();
            }
            allTestsPassed = false;
        }
        
        // Test 4: Create password-protected archive
        System.out.println("\n4. Testing password-protected archive creation...");
        ArchiverAPI.CreateArchiveConfig passwordConfig = new ArchiverAPI.CreateArchiveConfig(
            "/home/runner/work/java_archiver/java_archiver/test_data/sample_dir",
            "/home/runner/work/java_archiver/java_archiver/api_test_encrypted",
            "testpass123"
        );
        
        ArchiverAPI.OperationResult passwordCreateResult = api.createArchive(passwordConfig, null);
        
        if (passwordCreateResult.success) {
            System.out.println("✓ " + passwordCreateResult.message);
            
            // Test 5: Extract password-protected archive with correct password
            System.out.println("\n5. Testing encrypted archive extraction...");
            ArchiverAPI.ExtractArchiveConfig passwordExtractConfig = new ArchiverAPI.ExtractArchiveConfig(
                "/home/runner/work/java_archiver/java_archiver/api_test_encrypted.archivit",
                "/home/runner/work/java_archiver/java_archiver/api_extract_encrypted",
                "testpass123"
            );
            
            ArchiverAPI.OperationResult passwordExtractResult = api.extractArchive(passwordExtractConfig, null);
            
            if (passwordExtractResult.success) {
                System.out.println("✓ " + passwordExtractResult.message);
            } else {
                System.err.println("✗ " + passwordExtractResult.message);
                allTestsPassed = false;
            }
            
            // Test 6: Try to extract with wrong password
            System.out.println("\n6. Testing wrong password rejection...");
            ArchiverAPI.ExtractArchiveConfig wrongPasswordConfig = new ArchiverAPI.ExtractArchiveConfig(
                "/home/runner/work/java_archiver/java_archiver/api_test_encrypted.archivit",
                "/home/runner/work/java_archiver/java_archiver/api_extract_wrong",
                "wrongpassword"
            );
            
            ArchiverAPI.OperationResult wrongPasswordResult = api.extractArchive(wrongPasswordConfig, null);
            
            if (!wrongPasswordResult.success && wrongPasswordResult.message.contains("password")) {
                System.out.println("✓ Wrong password correctly rejected");
            } else {
                System.err.println("✗ Wrong password was not rejected properly");
                allTestsPassed = false;
            }
        } else {
            System.err.println("✗ " + passwordCreateResult.message);
            allTestsPassed = false;
        }
        
        // Test 7: Error handling - invalid source path
        System.out.println("\n7. Testing error handling (invalid source)...");
        ArchiverAPI.CreateArchiveConfig invalidConfig = new ArchiverAPI.CreateArchiveConfig(
            "/nonexistent/path",
            "/home/runner/work/java_archiver/java_archiver/should_not_exist"
        );
        
        ArchiverAPI.OperationResult invalidResult = api.createArchive(invalidConfig, null);
        
        if (!invalidResult.success) {
            System.out.println("✓ Invalid source path correctly rejected: " + invalidResult.message);
        } else {
            System.err.println("✗ Invalid source path was not rejected");
            allTestsPassed = false;
        }
        
        // Summary
        System.out.println("\n" + "=".repeat(50));
        if (allTestsPassed) {
            System.out.println("✓ All API tests passed successfully!");
        } else {
            System.err.println("✗ Some API tests failed");
            System.exit(1);
        }
        
        // Cleanup test files (optional)
        System.out.println("\nCleaning up test files...");
        cleanupFile("api_test_output.archivit");
        cleanupFile("api_test_encrypted.archivit");
        cleanupDirectory("api_extract_test");
        cleanupDirectory("api_extract_encrypted");
        cleanupDirectory("api_extract_wrong");
    }
    
    private static void cleanupFile(String path) {
        File file = new File(path);
        if (file.exists() && file.delete()) {
            System.out.println("  Removed: " + path);
        }
    }
    
    private static void cleanupDirectory(String path) {
        File dir = new File(path);
        if (dir.exists()) {
            deleteRecursively(dir);
            System.out.println("  Removed: " + path);
        }
    }
    
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}