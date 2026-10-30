package com.winlator.xenvironment;  // Modified by D2Android (changes 2026-10-09 to 2026-10-30).

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;





public class RootFS {
    private final File rootDir;

    private RootFS(File rootDir) {
        this.rootDir = rootDir;
    }

    public static RootFS find(Context context) {
        return new RootFS(new File(context.getFilesDir(), "rootfs"));
    }

    public File getRootDir() {
        return rootDir;
    }

    public File getTmpDir() {
        return new File(rootDir, "tmp");
    }

    @NonNull
    @Override
    public String toString() {
        return rootDir.getPath();
    }
}
