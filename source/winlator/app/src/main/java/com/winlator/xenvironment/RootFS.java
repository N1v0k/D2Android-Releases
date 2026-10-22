package com.winlator.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * D2Android: the Linux root of the runtime (files/rootfs, see D2Runtime). Winlator's image
 * version files, Wine selection and user paths are gone.
 */
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
