import Utilities.ArchiverAPI;
import Utilities.SimpleASCIITable;

import java.util.List;
import java.util.Scanner;

/**
 * Enhanced main class that provides both interactive and non-interactive modes
 * Improved version with better error handling and resource management
 */
public class MainEnhanced {
    final static Scanner sc = new Scanner(System.in);
    final static ArchiverAPI api = new ArchiverAPI();

    public static void main(String[] args) {
        // Check if non-interactive mode is requested
        if (args.length > 0) {
            runNonInteractive(args);
        } else {
            runInteractive();
        }
    }
    
    /**
     * Non-interactive mode - allows programmatic usage
     * Usage examples:
     *   java MainEnhanced create /path/to/source /path/to/archive.archivit
     *   java MainEnhanced create /path/to/source /path/to/archive.archivit password123
     *   java MainEnhanced extract /path/to/archive.archivit /path/to/extract
     *   java MainEnhanced extract /path/to/archive.archivit /path/to/extract password123
     *   java MainEnhanced list /path/to/archive.archivit
     */
    public static void runNonInteractive(String[] args) {
        try {
            if (args.length < 2) {
                printUsage();
                System.exit(1);
            }
            
            String command = args[0].toLowerCase();
            
            switch (command) {
                case "create":
                    if (args.length < 3) {
                        System.err.println("Error: create command requires source and destination paths");
                        printUsage();
                        System.exit(1);
                    }
                    
                    String sourcePath = args[1];
                    String archivePath = args[2];
                    String createPassword = args.length > 3 ? args[3] : null;
                    
                    ArchiverAPI.CreateArchiveConfig createConfig = createPassword != null ?
                        new ArchiverAPI.CreateArchiveConfig(sourcePath, archivePath, createPassword) :
                        new ArchiverAPI.CreateArchiveConfig(sourcePath, archivePath);
                    
                    ArchiverAPI.OperationResult createResult = api.createArchive(createConfig, 
                        new ProgressPrinter("Creating archive"));
                    
                    if (createResult.success) {
                        System.out.println("SUCCESS: " + createResult.message);
                        System.exit(0);
                    } else {
                        System.err.println("ERROR: " + createResult.message);
                        if (createResult.error != null) {
                            createResult.error.printStackTrace();
                        }
                        System.exit(1);
                    }
                    break;
                    
                case "extract":
                    if (args.length < 3) {
                        System.err.println("Error: extract command requires archive and destination paths");
                        printUsage();
                        System.exit(1);
                    }
                    
                    String extractArchivePath = args[1];
                    String extractPath = args[2];
                    String extractPassword = args.length > 3 ? args[3] : null;
                    
                    ArchiverAPI.ExtractArchiveConfig extractConfig = extractPassword != null ?
                        new ArchiverAPI.ExtractArchiveConfig(extractArchivePath, extractPath, extractPassword) :
                        new ArchiverAPI.ExtractArchiveConfig(extractArchivePath, extractPath);
                    
                    ArchiverAPI.OperationResult extractResult = api.extractArchive(extractConfig,
                        new ProgressPrinter("Extracting archive"));
                    
                    if (extractResult.success) {
                        System.out.println("SUCCESS: " + extractResult.message);
                        System.exit(0);
                    } else {
                        System.err.println("ERROR: " + extractResult.message);
                        if (extractResult.error != null) {
                            extractResult.error.printStackTrace();
                        }
                        System.exit(1);
                    }
                    break;
                    
                case "list":
                    if (args.length < 2) {
                        System.err.println("Error: list command requires archive path");
                        printUsage();
                        System.exit(1);
                    }
                    
                    String listArchivePath = args[1];
                    
                    try {
                        List<ArchiverAPI.ArchiveFileInfo> files = api.listArchiveContents(listArchivePath);
                        printArchiveContents(files);
                        System.exit(0);
                    } catch (Exception e) {
                        System.err.println("ERROR: Failed to list archive contents: " + e.getMessage());
                        e.printStackTrace();
                        System.exit(1);
                    }
                    break;
                    
                default:
                    System.err.println("Error: Unknown command '" + command + "'");
                    printUsage();
                    System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Interactive mode - original menu-driven interface
     */
    public static void runInteractive() {
        System.out.print("Archivit v3 Enhanced\n1] Create Archive\n2] Extract an archive\n3] List archive contents\n> ");
        int option = sc.nextInt();
        sc.nextLine();

        try {
            if (option == 1)
                createArchiveInteractive();
            else if (option == 2)
                extractArchiveInteractive();
            else if (option == 3)
                listArchiveInteractive();
            else
                System.out.println("Invalid option " + option);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pause();
        }
    }
    
    private static void createArchiveInteractive() {
        System.out.println("Enter folder path to archive: ");
        String folderPath = sc.nextLine();
        
        System.out.println("Enter destination archive path: ");
        String archivePath = sc.nextLine();
        
        String password = null;
        System.out.println("Add password protection? (Y/n): ");
        char option = sc.next().toLowerCase().charAt(0);
        if (option == 'y') {
            sc.nextLine(); // consume newline
            do {
                System.out.print("Enter password (6-16 characters): ");
                password = sc.nextLine();
                if (password.length() < 6 || password.length() > 16 || password.trim().length() == 0) {
                    System.out.println("Password must be 6-16 characters long and not whitespace-only");
                    password = null;
                }
            } while (password == null);
        }
        
        ArchiverAPI.CreateArchiveConfig config = password != null ?
            new ArchiverAPI.CreateArchiveConfig(folderPath, archivePath, password) :
            new ArchiverAPI.CreateArchiveConfig(folderPath, archivePath);
        
        ArchiverAPI.OperationResult result = api.createArchive(config, new ProgressPrinter("Creating archive"));
        
        if (result.success) {
            System.out.println("SUCCESS: " + result.message);
        } else {
            System.err.println("ERROR: " + result.message);
            if (result.error != null) {
                result.error.printStackTrace();
            }
        }
    }
    
    private static void extractArchiveInteractive() {
        System.out.println("Enter archive path: ");
        String archivePath = sc.nextLine();
        
        System.out.println("Enter extraction path (a folder with the archive name will be created): ");
        String extractPath = sc.nextLine();
        
        // First, check if the archive is password protected by trying to list it
        String password = null;
        try {
            api.listArchiveContents(archivePath);
        } catch (Exception e) {
            if (e.getMessage().contains("password") || e.getMessage().contains("decrypt")) {
                System.out.print("This archive is password-protected!\nEnter password: ");
                password = sc.nextLine();
            }
        }
        
        ArchiverAPI.ExtractArchiveConfig config = password != null ?
            new ArchiverAPI.ExtractArchiveConfig(archivePath, extractPath, password) :
            new ArchiverAPI.ExtractArchiveConfig(archivePath, extractPath);
        
        ArchiverAPI.OperationResult result = api.extractArchive(config, new ProgressPrinter("Extracting archive"));
        
        if (result.success) {
            System.out.println("SUCCESS: " + result.message);
        } else {
            System.err.println("ERROR: " + result.message);
            if (result.error != null) {
                result.error.printStackTrace();
            }
        }
    }
    
    private static void listArchiveInteractive() {
        System.out.println("Enter archive path: ");
        String archivePath = sc.nextLine();
        
        try {
            List<ArchiverAPI.ArchiveFileInfo> files = api.listArchiveContents(archivePath);
            printArchiveContents(files);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to list archive contents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printArchiveContents(List<ArchiverAPI.ArchiveFileInfo> files) {
        System.out.println("Archive contains " + files.size() + " files:\n");
        
        String[][] dataSet = new String[files.size()][];
        for (int i = 0; i < files.size(); i++) {
            ArchiverAPI.ArchiveFileInfo file = files.get(i);
            dataSet[i] = new String[] {
                file.name,
                file.path,
                file.canRead ? "Yes" : "No",
                file.canExecute ? "Yes" : "No",
                file.canWrite ? "Yes" : "No",
                String.valueOf(file.lastModified),
                Utilities.ArchiverAPI.formatSize(file.size)
            };
        }
        
        SimpleASCIITable inst = SimpleASCIITable.getInstance();
        inst.printTable(new String[] {
            "Name", "Path", "Readable", "Executable", "Writable", "Last modified", "Size"
        }, dataSet);
    }
    
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  Interactive mode: java MainEnhanced");
        System.out.println("  Non-interactive mode:");
        System.out.println("    java MainEnhanced create <source_path> <archive_path> [password]");
        System.out.println("    java MainEnhanced extract <archive_path> <extract_path> [password]");
        System.out.println("    java MainEnhanced list <archive_path>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java MainEnhanced create /home/user/documents my_backup.archivit");
        System.out.println("  java MainEnhanced create /home/user/documents my_backup.archivit secretpass");
        System.out.println("  java MainEnhanced extract my_backup.archivit /tmp/restore");
        System.out.println("  java MainEnhanced list my_backup.archivit");
    }

    /**
     * Pauses the console (awaits user prompt)
     */
    private static void pause() {
        System.out.println("\nPress any key to continue...");
        try {
            System.in.read();
        } catch (Exception ignored) {}
    }
    
    /**
     * Progress callback that prints progress to console
     */
    private static class ProgressPrinter implements ArchiverAPI.ProgressCallback {
        private final String operation;
        
        public ProgressPrinter(String operation) {
            this.operation = operation;
        }
        
        @Override
        public void onProgress(String op, String fileName, long processed, long total) {
            if (total > 0) {
                System.out.printf("\r%s: %s (%d/%d)", operation, fileName, processed, total);
            } else {
                System.out.printf("\r%s: %s", operation, fileName);
            }
        }
        
        @Override
        public void onComplete(String op, long totalFiles, long totalBytes) {
            System.out.printf("\n%s completed: %d files", operation, totalFiles);
            if (totalBytes > 0) {
                System.out.printf(", %d bytes", totalBytes);
            }
            System.out.println();
        }
        
        @Override
        public void onError(String op, String fileName, Exception error) {
            System.err.printf("\nError during %s: %s - %s\n", operation, fileName, error.getMessage());
        }
    }
}