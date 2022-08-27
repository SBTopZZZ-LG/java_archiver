package Utilities;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

public class IO {
    public static final String[] EXCLUSIONS = new String[] {
            ".url",
            ".URL",
    };

    public interface OnRetrieve {
        void onFileRetrieve(String file);
        void onFolderRetrieve(String folder);
        void onSymLinkFileRetrieve(String symLinkPath);
        void onExclusion(String file);
    }
    public static void getFilesAndDirs(String path, OnRetrieve onRetrieve) {
        File dir = new File(path);

        for (final File dirFile : Objects.requireNonNull(dir.listFiles()))
            if (dirFile.isDirectory()) {
                onRetrieve.onFolderRetrieve(dirFile.getPath());
                getFilesAndDirs(dirFile.getPath(), onRetrieve);
            } else {
                String extension = FilenameUtils.getExtension(dirFile.getName());
                if (!extension.equals("") && Arrays.stream(EXCLUSIONS).noneMatch(extension::equals)) {
                    if (Files.isSymbolicLink(dirFile.toPath()))
                        onRetrieve.onSymLinkFileRetrieve(dirFile.getPath());
                    else
                        onRetrieve.onFileRetrieve(dirFile.getPath());
                } else
                    onRetrieve.onExclusion(dirFile.getPath());
            }
    }
}
