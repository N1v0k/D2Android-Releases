package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;









public final class D2IdleTraceGate {
    public static final long FENSTER_MS = 30_000;
    static final long LANGE_SUCHE_MS = 100;
    private static final Pattern ZAHLEN = Pattern.compile("\\d+");
    private static final Pattern SUCHE = Pattern.compile("untersucht in (\\d+) ms");

    private String art;
    private long seit;
    private int ausgelassen;
    private String letzte;


    public List<String> pass(String text, long now) {
        List<String> out = new ArrayList<>(2);
        String a = ZAHLEN.matcher(text).replaceAll("#");
        if (a.equals(art) && now - seit < FENSTER_MS && !wichtig(text)) {
            ausgelassen++;
            letzte = text;
            return out;
        }
        String z = zusammenfassung(now);
        if (z != null) out.add(z);
        out.add(text);
        art = a;
        seit = now;
        return out;
    }


    public String flush(long now) {
        String z = zusammenfassung(now);
        art = null;
        return z;
    }

    private String zusammenfassung(long now) {
        if (ausgelassen == 0) return null;
        String z = "(" + ausgelassen + " gleichartige in " + Math.max(0, (now - seit) / 1000) + " s, zuletzt: " + letzte + ")";
        ausgelassen = 0;
        letzte = null;
        return z;
    }


    static boolean wichtig(String text) {
        Matcher m = SUCHE.matcher(text);
        while (m.find()) {
            try {
                if (Long.parseLong(m.group(1)) >= LANGE_SUCHE_MS) return true;
            }
            catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }
}
