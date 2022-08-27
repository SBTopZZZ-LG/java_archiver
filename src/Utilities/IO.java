package Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class IO {
    public static Pair<String, Integer>[] getFilesAndDirs(String path) {
        List<Pair<String, Integer>> files = new ArrayList<>();
        File dir = new File(path);

        for (final File dirFile : Objects.requireNonNull(dir.listFiles()))
            if (dirFile.isDirectory()) {
                files.add(new Pair<>(dirFile.getPath(), 1));
                files.addAll(Arrays.stream(getFilesAndDirs(dirFile.getPath())).toList());
            } else {
                files.add(new Pair<>(dirFile.getPath(), 0));
            }

        Pair<String, Integer>[] list = new Pair[files.size()];
        list = files.toArray(list);

        return list;
    }

    public interface OnRetrieve {
        void onFileRetrieve(String file);
        void onFolderRetrieve(String folder);

        void onProgress(int progress);
    }
    public static int getFilesAndDirs(String path, OnRetrieve onRetrieve) {
        File dir = new File(path);

        int count = 0;
        for (final File dirFile : Objects.requireNonNull(dir.listFiles()))
            if (dirFile.isDirectory()) {
                onRetrieve.onFolderRetrieve(dirFile.getPath());
                count += getFilesAndDirs(dirFile.getPath(), onRetrieve);
            } else {
                onRetrieve.onFileRetrieve(dirFile.getPath());
                onRetrieve.onProgress(++count);
            }

        return count;
    }
}
