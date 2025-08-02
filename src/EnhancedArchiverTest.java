import Utilities.EnhancedArchiverAPI;
import Utilities.ArchiverAPI;
import java.util.List;

/**
 * Test the enhanced archiver with compression and integrity features
 */
public class EnhancedArchiverTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Enhanced Archiver with Compression and Integrity");
        System.out.println("========================================================");
        
        EnhancedArchiverAPI enhancedAPI = new EnhancedArchiverAPI();
        boolean allTestsPassed = true;
        
        // Test 1: Create enhanced archive with compression and integrity
        System.out.println("\n1. Testing enhanced archive creation...");
        EnhancedArchiverAPI.CreateArchiveConfig enhancedConfig = 
            new EnhancedArchiverAPI.CreateArchiveConfig(
                "/home/runner/work/java_archiver/java_archiver/test_data/sample_dir",
                "/home/runner/work/java_archiver/java_archiver/enhanced_archive_test",
                null, // no password
                true, // enable compression
                true  // enable integrity check
            );
        
        ArchiverAPI.OperationResult createResult = enhancedAPI.createArchive(enhancedConfig, 
            new ArchiverAPI.ProgressCallback() {
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
                    System.err.println("  Error: " + fileName + " - " + error.getMessage());
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
        
        // Test 2: List enhanced archive contents
        System.out.println("\n2. Testing enhanced archive listing...");
        try {
            List<EnhancedArchiverAPI.EnhancedFileInfo> files = 
                enhancedAPI.listArchiveContents("enhanced_archive_test.archivit");
            
            System.out.println("✓ Enhanced archive contains " + files.size() + " files:");
            for (EnhancedArchiverAPI.EnhancedFileInfo file : files) {
                System.out.println("  - " + file.toString());
                if (file.integrity != null) {
                    System.out.println("    SHA-256: " + file.integrity.sha256Hash.substring(0, 16) + "...");
                    System.out.println("    CRC32: " + Long.toHexString(file.integrity.crc32Checksum));
                    if (file.isCompressed) {
                        double compressionRatio = (1.0 - (double)file.integrity.compressedSize / file.integrity.originalSize) * 100;
                        System.out.printf("    Compression: %.1f%% saved\n", compressionRatio);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to list enhanced archive: " + e.getMessage());
            e.printStackTrace();
            allTestsPassed = false;
        }
        
        // Test 3: Create password-protected enhanced archive
        System.out.println("\n3. Testing enhanced archive with password...");
        EnhancedArchiverAPI.CreateArchiveConfig passwordConfig = 
            new EnhancedArchiverAPI.CreateArchiveConfig(
                "/home/runner/work/java_archiver/java_archiver/test_data/sample_dir",
                "/home/runner/work/java_archiver/java_archiver/enhanced_encrypted_test",
                "testpass123",
                true, // enable compression
                true  // enable integrity check
            );
        
        ArchiverAPI.OperationResult passwordResult = enhancedAPI.createArchive(passwordConfig, null);
        
        if (passwordResult.success) {
            System.out.println("✓ " + passwordResult.message);
        } else {
            System.err.println("✗ " + passwordResult.message);
            if (passwordResult.error != null) {
                passwordResult.error.printStackTrace();
            }
            allTestsPassed = false;
        }
        
        // Test 4: Compare archive sizes
        System.out.println("\n4. Comparing archive sizes...");
        try {
            java.io.File originalArchive = new java.io.File("test_output.archivit");
            java.io.File enhancedArchive = new java.io.File("enhanced_archive_test.archivit");
            
            if (originalArchive.exists() && enhancedArchive.exists()) {
                long originalSize = originalArchive.length();
                long enhancedSize = enhancedArchive.length();
                
                System.out.println("  Original archive: " + ArchiverAPI.formatSize(originalSize));
                System.out.println("  Enhanced archive: " + ArchiverAPI.formatSize(enhancedSize));
                
                if (enhancedSize <= originalSize) {
                    double savings = (1.0 - (double)enhancedSize / originalSize) * 100;
                    System.out.printf("  ✓ Enhanced archive is %.1f%% smaller\n", savings);
                } else {
                    double overhead = ((double)enhancedSize / originalSize - 1.0) * 100;
                    System.out.printf("  Note: Enhanced archive is %.1f%% larger (due to integrity metadata)\n", overhead);
                }
            } else {
                System.out.println("  Cannot compare - one or both archives missing");
            }
        } catch (Exception e) {
            System.err.println("  Error comparing sizes: " + e.getMessage());
        }
        
        // Summary
        System.out.println("\n" + "=".repeat(60));
        if (allTestsPassed) {
            System.out.println("✓ All enhanced archiver tests passed!");
        } else {
            System.err.println("✗ Some enhanced archiver tests failed");
            System.exit(1);
        }
        
        // Cleanup
        System.out.println("\nCleaning up test files...");
        cleanup("enhanced_archive_test.archivit");
        cleanup("enhanced_encrypted_test.archivit");
    }
    
    private static void cleanup(String path) {
        java.io.File file = new java.io.File(path);
        if (file.exists() && file.delete()) {
            System.out.println("  Removed: " + path);
        }
    }
}