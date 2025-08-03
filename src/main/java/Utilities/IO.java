package Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

public class IO {
    public static final String[] EXCLUSIONS = new String[] {
            ".url",
            ".URL",
    };

    public interface OnRetrieve {
        /**
         * Called when a file is retrieved from the Disk
         * @param file File path (absolute)
         */
        void onFileRetrieve(String file);

        /**
         * Called when a folder is retrieved from the Disk
         * @param folder Folder path (absolute)
         */
        void onFolderRetrieve(String folder);

        /**
         * Called when a file symbolic link is retrieved from the Disk
         * @param symLinkPath File symbolic link path (absolute)
         * @param canonicalPath File symbolic link canonical path
         */
        void onSymLinkFileRetrieve(String symLinkPath, String canonicalPath);

        /**
         * Called when a file which is flagged as exclusive is retrieved from the Disk
         * @param file File path (absolute)
         */
        void onExclusion(String file);
    }

    /**
     * Retrieves a list of all files and folders (recursive) from a specified path
     * @param path Path
     * @param onRetrieve Callback
     */
    public static void getFilesAndDirs(String path, OnRetrieve onRetrieve) {
        File dir = new File(path);

        // Handle NullPointerException safely with guard clause
        final File[] files = dir.listFiles();
        if (files == null)
            return;

        for (final File dirFile : files)
            if (dirFile.isDirectory()) {
                onRetrieve.onFolderRetrieve(dirFile.getPath());
                getFilesAndDirs(dirFile.getPath(), onRetrieve);
            } else {
                String extension = FilenameUtils.getExtension(dirFile.getName());
                if (!extension.equals("") && !extension.startsWith("."))
                    extension = "." + extension;
                if (!extension.equals("") && Arrays.stream(EXCLUSIONS).noneMatch(extension::equals)) {
                    if (Files.isSymbolicLink(dirFile.toPath()))
                        try {
                            onRetrieve.onSymLinkFileRetrieve(dirFile.getPath(), dirFile.getCanonicalPath());
                        } catch (IOException ignored) {
                            onRetrieve.onSymLinkFileRetrieve(dirFile.getPath(), null);
                        }
                    else
                        onRetrieve.onFileRetrieve(dirFile.getPath());
                } else
                    onRetrieve.onExclusion(dirFile.getPath());
            }
    }

    /**
     * Returns the file name without the extension
     * @param fileName File name
     * @return File name without extension
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (!fileName.contains("."))
            return fileName;

        StringBuilder result = new StringBuilder();
        final String[] split = fileName.split("\\.");
        for (int i = 0; i < split.length - 1; i++)
            result.append(split[i]).append(i < split.length - 1 - 1 ? "." : "");

        return result.toString();
    }
}
