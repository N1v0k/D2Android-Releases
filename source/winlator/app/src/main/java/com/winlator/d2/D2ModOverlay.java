package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;











public final class D2ModOverlay {




    static final String MANIFEST = "d2android-overlay.list";

    static final String[] WRITABLE = {"data", "data/global", "data/global/excel"};
    static final String EXCEL = "data/global/excel";


    public interface Provided {
        boolean is(String name);
    }

    private D2ModOverlay() {}





    public static void clear(File shadow, java.util.function.Function<String, File> cacheOf) throws IOException {
        File manifest = new File(shadow, MANIFEST);
        if (!manifest.isFile()) return;
        List<String> lines = Files.readAllLines(manifest.toPath(), StandardCharsets.UTF_8);
        String id = lines.isEmpty() ? "" : lines.get(0).trim();
        File excel = new File(shadow, EXCEL.replace('/', File.separatorChar));
        File cache = id.isEmpty() || cacheOf == null ? null : cacheOf.apply(id);
        if (cache != null && isRealDir(excel)) {
            File[] written = excel.listFiles();
            if (written != null) {
                File target = new File(cache, "excel");
                for (File f : written) {
                    if (isLink(f) || !f.isFile() || !f.getName().toLowerCase(Locale.ROOT).endsWith(".bin")) continue;
                    if (!target.isDirectory() && !target.mkdirs()) break;
                    File to = new File(target, f.getName());
                    Files.move(f.toPath(), to.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        for (int i = 1; i < lines.size(); i++) {
            String name = lines.get(i).trim();
            if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals("..") || name.equals(".")) continue;
            remove(new File(shadow, name));
        }
        if (!manifest.delete()) throw new IOException("cannot delete " + manifest);
    }


    static void remove(File f) throws IOException {
        if (isLink(f)) {
            Files.delete(f.toPath());
            return;
        }
        if (f.isDirectory()) {
            File[] in = f.listFiles();
            if (in != null) for (File c : in) remove(c);
        }
        if (f.exists() && !f.delete()) throw new IOException("cannot delete " + f);
    }






    public static List<String> lay(File shadow, File modFiles, File gameDir, File cache, String id, Provided provided) throws IOException {
        File[] top = modFiles.listFiles();
        if (top == null) throw new IOException("mod not readable: " + modFiles);
        Map<String, File> game = byLowerName(gameDir);


        List<String> made = new ArrayList<>();
        List<File> from = new ArrayList<>();
        for (File m : byLowerName(modFiles).values()) {
            String name = m.getName();
            if (provided.is(name) || isLink(m)) continue;
            if (isWritable(name.toLowerCase(Locale.ROOT))) name = name.toLowerCase(Locale.ROOT);
            File out = new File(shadow, name);

            if (out.exists() || isLink(out)) continue;
            made.add(name);
            from.add(m);
        }

        File data = new File(shadow, "data");
        boolean dataToo = nameIn(made, "data") == null && !data.exists() && !isLink(data);
        if (dataToo) made.add("data");
        writeManifest(shadow, id, made);
        for (int i = 0; i < from.size(); i++) {
            String name = made.get(i);
            merge(from.get(i), game.get(name.toLowerCase(Locale.ROOT)), new File(shadow, name), name.toLowerCase(Locale.ROOT));
        }
        if (dataToo) merge(null, game.get("data"), data, "data");

        File cached = cache != null ? new File(cache, "excel") : null;
        File excel = new File(shadow, EXCEL.replace('/', File.separatorChar));
        File[] bins = cached != null ? cached.listFiles() : null;
        if (bins != null && isRealDir(excel)) {
            Map<String, File> there = byLowerName(excel);
            for (File b : bins) {
                if (!b.isFile() || there.containsKey(b.getName().toLowerCase(Locale.ROOT))) continue;
                Files.copy(b.toPath(), new File(excel, b.getName()).toPath());
            }
        }
        return made;
    }


    public static String nameIn(List<String> names, String lower) {
        for (String n : names) if (n.toLowerCase(Locale.ROOT).equals(lower)) return n;
        return null;
    }





    static void merge(File m, File g, File out, String path) throws IOException {
        boolean writable = isWritable(path);
        boolean mDir = m != null && m.isDirectory(), gDir = g != null && g.isDirectory();
        if (!writable) {
            if (m != null && !(mDir && gDir)) {
                link(m, out);
                return;
            }
            if (m == null) {
                if (g != null && !(EXCEL.equals(parent(path)) && isBin(path))) link(g, out);
                return;
            }
        }
        if (m != null && !mDir) {

            Files.copy(m.toPath(), out.toPath());
            return;
        }
        if (!out.mkdir()) throw new IOException("cannot create " + out);
        Map<String, File> mods = m != null ? byLowerName(m) : new HashMap<>();
        Map<String, File> games = gDir ? byLowerName(g) : new HashMap<>();
        for (Map.Entry<String, File> e : mods.entrySet()) {
            File child = e.getValue();
            if (isLink(child)) continue;
            String sub = path + "/" + e.getKey();
            File to = new File(out, isWritable(sub) ? e.getKey() : child.getName());
            if (EXCEL.equals(path) && child.isFile() && isBin(sub)) {
                Files.copy(child.toPath(), to.toPath());
                continue;
            }
            merge(child, games.get(e.getKey()), to, sub);
        }
        for (Map.Entry<String, File> e : games.entrySet()) {
            if (mods.containsKey(e.getKey())) continue;
            String sub = path + "/" + e.getKey();
            if (EXCEL.equals(path) && isBin(sub)) continue;
            merge(null, e.getValue(), new File(out, isWritable(sub) ? e.getKey() : e.getValue().getName()), sub);
        }
        for (String w : WRITABLE) {
            if (!parent(w).equals(path)) continue;
            String child = w.substring(w.lastIndexOf('/') + 1);
            if (mods.containsKey(child) || games.containsKey(child)) continue;
            merge(null, null, new File(out, child), w);
        }
    }

    static boolean isWritable(String path) {
        for (String w : WRITABLE) if (w.equals(path)) return true;
        return false;
    }

    private static String parent(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private static boolean isBin(String path) {
        return path.endsWith(".bin");
    }

    private static void link(File target, File link) throws IOException {
        Files.createSymbolicLink(link.toPath(), target.getAbsoluteFile().toPath());
    }

    static boolean isLink(File f) {
        return Files.isSymbolicLink(f.toPath());
    }

    private static boolean isRealDir(File f) {
        return Files.isDirectory(f.toPath(), LinkOption.NOFOLLOW_LINKS);
    }


    static Map<String, File> byLowerName(File dir) {
        Map<String, File> out = new java.util.TreeMap<>();
        File[] all = dir != null ? dir.listFiles() : null;
        if (all != null) {
            java.util.Arrays.sort(all);
            for (File f : all) out.putIfAbsent(f.getName().toLowerCase(Locale.ROOT), f);
        }
        return out;
    }

    private static void writeManifest(File shadow, String id, List<String> made) throws IOException {
        StringBuilder sb = new StringBuilder(id).append('\n');
        for (String n : made) sb.append(n).append('\n');
        Files.write(new File(shadow, MANIFEST).toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
