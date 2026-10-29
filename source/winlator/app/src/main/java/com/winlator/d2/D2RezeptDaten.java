package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;








public final class D2RezeptDaten {
    public final D2RezeptKatalog katalog;
    public final D2Gegenstaende items;
    private final Map<String, int[]> crops;

    private D2RezeptDaten(D2RezeptKatalog katalog, D2Gegenstaende items, Map<String, int[]> crops) {
        this.katalog = katalog;
        this.items = items;
        this.crops = crops;
    }


    public int[] crop(String file) {
        return crops.get(file);
    }

    public static D2RezeptDaten load(File gameDir, D2RezeptKatalog.Texte app) {
        try (D2Sprites s = new D2Sprites(gameDir)) {
            return load(s, app);
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static D2RezeptDaten load(D2Sprites s, D2RezeptKatalog.Texte app) throws IOException {
        String x = "data\\global\\excel\\";
        D2Rezepte rezepte = D2Rezepte.lesen(s.read(x + "cubemain.bin"));
        D2Gegenstaende items = D2Gegenstaende.lesen(s.read(x + "weapons.bin"), s.read(x + "armor.bin"), s.read(x + "misc.bin"));
        if (rezepte == null || items == null || !items.mitTypen(s.read(x + "itemtypes.bin"))) return null;
        D2Runenwoerter runen = D2Runenwoerter.lesen(s.read(x + "runes.bin"), s.read(x + "gems.bin"));
        D2Strings de = new D2Strings(s), en = new D2Strings(s, "eng"), deu = new D2Strings(s, "deu");
        D2Werte werte = D2Werte.lesen(s.read(x + "itemstatcost.bin"), s.read(x + "properties.bin"), s.read(x + "skills.bin"),
            s.read(x + "skilldesc.bin"), s.read(x + "charstats.bin"), s.read(x + "monstats.bin"), texte(de));

        D2RezeptKatalog k = D2RezeptKatalog.bauen(rezepte, items, runen, werte, spieltexte(de), both(spieltexte(en), spieltexte(deu)), app,
            uniqueKeys(s.read(x + "uniqueitems.bin")));
        if (k == null || k.all().isEmpty()) return null;
        TreeSet<String> files = new TreeSet<>();
        files.add("invbox");
        for (D2RezeptKatalog.Eintrag e : k.all()) {
            if (e.resultPicture != null) files.add(e.resultPicture);
            for (D2RezeptKatalog.Zutat z : e.zutaten) if (z.picture != null) files.add(z.picture);
        }
        Map<String, int[]> crops = new HashMap<>();
        for (String f : files) {
            D2Sprites.Frame[] fr = s.dc6("data\\global\\items\\" + f + ".dc6");
            if (fr != null && fr.length > 0) crops.put(f, D2LayerArt.crop(fr[0]));
        }
        return new D2RezeptDaten(k, items, crops);
    }


    static String[] uniqueKeys(byte[] b) {
        int n = b != null && b.length >= 4 ? D2Tabellen.s32(b, 0) : -1;
        if (n <= 0 || (long) n * 332 + 4 != b.length) return null;
        String[] keys = new String[n];
        for (int i = 0; i < n; i++) keys[i] = D2Gegenstaende.cString(b, 4 + i * 332 + 2, 32);
        return keys;
    }

    static D2Werte.Texte texte(D2Strings s) {
        return new D2Werte.Texte() {
            @Override public String get(int id) { return s.get(id); }

            @Override public String get(String key) { return s.get(key); }
        };
    }


    static D2RezeptKatalog.Spieltexte both(D2RezeptKatalog.Spieltexte a, D2RezeptKatalog.Spieltexte b) {
        return new D2RezeptKatalog.Spieltexte() {
            @Override public String id(int id) { return join(D2RezeptKatalog.lastLine(a.id(id)), D2RezeptKatalog.lastLine(b.id(id))); }

            @Override public String key(String key) { return join(D2RezeptKatalog.lastLine(a.key(key)), D2RezeptKatalog.lastLine(b.key(key))); }
        };
    }

    private static String join(String a, String b) {
        if (a == null) return b;
        if (b == null || b.equals(a)) return a;
        return a + " " + b;
    }

    static D2RezeptKatalog.Spieltexte spieltexte(D2Strings s) {
        return new D2RezeptKatalog.Spieltexte() {
            @Override public String id(int id) { return s.get(id); }

            @Override public String key(String key) { return s.get(key); }
        };
    }
}
