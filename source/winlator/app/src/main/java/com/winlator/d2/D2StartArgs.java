package com.winlator.d2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;










public final class D2StartArgs {
    public static final String D2DX = "-3dfx", DIRECT = "-direct", TXT = "-txt", NO_SOUND = "-ns";
    static final int MAX_TOKEN = 32, MAX_TOKENS = 12;


    public static final int WARN_WINDOW = 1, WARN_RENDERER = 2, WARN_BATTLENET = 3, WARN_FIXED = 4;

    private D2StartArgs() {}


    public static final class Parsed {
        public final List<String> taken, dropped;

        public final List<Object[]> warnings;

        Parsed(List<String> taken, List<String> dropped, List<Object[]> warnings) {
            this.taken = Collections.unmodifiableList(taken);
            this.dropped = Collections.unmodifiableList(dropped);
            this.warnings = Collections.unmodifiableList(warnings);
        }
    }


    public static Parsed parse(String free) {
        List<String> taken = new ArrayList<>(), dropped = new ArrayList<>();
        List<Object[]> warnings = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (free != null) {
            for (String token : free.trim().split("\\s+")) {
                if (token.isEmpty()) continue;
                if (!plain(token) || taken.size() >= MAX_TOKENS) {
                    dropped.add(token);
                    continue;
                }
                String key = token.toLowerCase(Locale.ROOT);
                if (key.equals(D2DX) || key.equals(DIRECT) || key.equals(TXT) || key.equals(NO_SOUND)) {
                    warnings.add(new Object[]{token, WARN_FIXED});
                    continue;
                }
                if (!seen.add(key)) continue;
                taken.add(token);
                int warn = warning(key);
                if (warn != 0) warnings.add(new Object[]{token, warn});
            }
        }
        return new Parsed(taken, dropped, warnings);
    }


    public static String commandLine(boolean direct, boolean txt, boolean noSound, String free) {
        StringBuilder sb = new StringBuilder(D2DX);
        if (direct) sb.append(' ').append(DIRECT);
        if (txt) sb.append(' ').append(TXT);
        if (noSound) sb.append(' ').append(NO_SOUND);
        for (String token : parse(free).taken) sb.append(' ').append(token);
        return sb.toString();
    }


    static boolean plain(String token) {
        if (token.isEmpty() || token.length() > MAX_TOKEN) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '.' || c == '_' || c == ':' || c == '=' || c == '+' || c == '-' || (i == 0 && c == '/');
            if (!ok) return false;
        }

        for (int i = 0; i < token.length(); i++) if (Character.isLetterOrDigit(token.charAt(i))) return true;
        return false;
    }






    static int warning(String key) {
        String k = key.startsWith("/") ? "-" + key.substring(1) : key;
        switch (k) {
            case "-w": case "-window": case "-windowed":
                return WARN_WINDOW;
            case "-d3d": case "-opengl": case "-glide": case "-ddraw": case "-gdi": case "-rave":
                return WARN_RENDERER;
            case "-skiptobnet": case "-bnet": case "-realm":
                return WARN_BATTLENET;
            default:
                return 0;
        }
    }
}
