package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;














public final class D2BigInventory {

    public static final String FILE = "d2android-biginv";

    static final String[] OWN_PANEL_LANGUAGES = {"jpn", "kor", "sin", "chi"};

    private D2BigInventory() {}


    public static String file(boolean on) {
        return on ? "# D2Android grosses Inventar\r\nversion=1\r\ninventory=1\r\n" : null;
    }


    public static void apply(File startDir, boolean on) throws IOException {
        File file = new File(startDir, FILE);
        String text = file(on);
        if (text == null) {
            if (file.exists() && !file.delete()) throw new IOException("cannot delete " + file);
            return;
        }
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.US_ASCII));
        }
    }








    public static boolean guarded(boolean modOwnTable) {
        return !modOwnTable;
    }


    public static boolean languageOk(String language) {
        if (language == null) return true;
        for (String l : OWN_PANEL_LANGUAGES) if (l.equals(language)) return false;
        return true;
    }
}
