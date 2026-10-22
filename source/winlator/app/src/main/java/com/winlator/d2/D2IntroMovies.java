package com.winlator.d2;

import java.io.File;
import java.io.IOException;

public final class D2IntroMovies {
    public static final String MARKER = "d2android-skipintro";
    public static final boolean SKIP_BY_DEFAULT = true;

    private D2IntroMovies() {}

    public static void apply(File startDir, boolean skip) throws IOException {
        File marker = new File(startDir, MARKER);
        if (skip) {
            if (!marker.isFile() && !marker.createNewFile()) throw new IOException("cannot create " + marker);
        }
        else if (marker.exists() && !marker.delete()) throw new IOException("cannot delete " + marker);
    }
}
