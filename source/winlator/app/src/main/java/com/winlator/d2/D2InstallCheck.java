package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;







public final class D2InstallCheck {

    static final List<String> VARYING = Arrays.asList("d2data.mpq", "d2char.mpq", "d2sfx.mpq");

    static final String PATCH_ARCHIVE = "patch_d2.mpq";


    static final long VARYING_MIN_BYTES = 16L * 1024 * 1024;

    private D2InstallCheck() {}


    public static String verify(File gameDir, Map<String, String> reference) throws IOException {
        return verify(gameDir, reference, false);
    }








    public static String verifyLanguageNeutral(File gameDir, Map<String, String> enUS) throws IOException {
        return verify(gameDir, enUS, true);
    }

    private static String verify(File gameDir, Map<String, String> reference, boolean languageFree) throws IOException {
        int hashed = 0;
        for (Map.Entry<String, String> e : reference.entrySet()) {
            String name = e.getKey();
            File f = new File(gameDir, name);
            if (!f.isFile()) return name + ": missing";
            if (VARYING.contains(name.toLowerCase(Locale.ROOT)) || languageFree && structural(name)) {
                String why = structure(f);
                if (why != null) return name + ": " + why;
                continue;
            }
            if (!GameVersion.sha256(f).equalsIgnoreCase(e.getValue())) return name + ": checksum differs";
            hashed++;
        }
        return hashed == 0 ? "no reference" : null;
    }


    public static boolean structural(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mpq") && !lower.equals(PATCH_ARCHIVE);
    }

    static String structure(File archive) throws IOException {
        if (archive.length() < VARYING_MIN_BYTES) return "too small";
        try (RandomAccessFile f = new RandomAccessFile(archive, "r")) {
            if (f.readInt() != 0x4D50511A) return "no MPQ header";
        }
        return D2MpqAttributes.present(archive) ? "(attributes) not hidden" : null;
    }
}
