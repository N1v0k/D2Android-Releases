package com.winlator.d2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;





public final class D2ModInfo {
    static final String FILES = "files", SAVES = "save", CACHE = "cache";

    private D2ModInfo() {}


    public static final class Mod {
        public final String id, name, file, sha256, kind;
        public final long installed, size;
        public final int files;

        public final boolean direct, txt;

        public final boolean inventory, archives;
        public File dir;

        Mod(String id, String name, String file, String sha256, String kind, long installed, long size, int files,
            boolean direct, boolean txt, boolean inventory, boolean archives) {
            this.id = id;
            this.name = name;
            this.file = file;
            this.sha256 = sha256;
            this.kind = kind;
            this.installed = installed;
            this.size = size;
            this.files = files;
            this.direct = direct;
            this.txt = txt;
            this.inventory = inventory;
            this.archives = archives;
        }

        public File files() {
            return new File(dir, FILES);
        }

        public File saves() {
            return new File(dir, SAVES);
        }

        public File cache() {
            return new File(dir, CACHE);
        }


        public boolean originalGrid() {
            return !inventory && !archives;
        }

        String toJson() {
            try {
                return new JSONObject().put("id", id).put("name", name).put("file", file).put("sha256", sha256)
                    .put("kind", kind).put("source", "file").put("installed", installed).put("size", size)
                    .put("files", files).put("direct", direct).put("txt", txt).put("inventory", inventory)
                    .put("archives", archives).toString(2);
            }
            catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }

        static Mod fromJson(String text) throws JSONException {
            JSONObject o = new JSONObject(text);
            String id = o.getString("id");
            if (!validId(id)) throw new JSONException("bad id " + id);
            return new Mod(id, o.optString("name", id), o.optString("file", ""), o.optString("sha256", ""),
                o.optString("kind", "data"), o.optLong("installed"), o.optLong("size"), o.optInt("files"),
                o.optBoolean("direct"), o.optBoolean("txt"), o.optBoolean("inventory"), o.optBoolean("archives"));
        }
    }




    static boolean validId(String id) {
        if (id == null || id.isEmpty() || id.length() > 48 || id.startsWith(".")) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.')) return false;
        }
        return true;
    }


    static String newId(String fileName, Set<String> taken) {
        String base = displayName(fileName);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base.length() && sb.length() < 40; i++) {
            char c = base.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
            if (ok) sb.append(c);
            else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') sb.append('-');
        }
        while (sb.length() > 0 && (sb.charAt(sb.length() - 1) == '-' || sb.charAt(sb.length() - 1) == '.')) sb.setLength(sb.length() - 1);
        while (sb.length() > 0 && sb.charAt(0) == '.') sb.deleteCharAt(0);
        String id = sb.length() == 0 ? "mod" : sb.toString();
        Set<String> lower = new HashSet<>();
        for (String t : taken) lower.add(t.toLowerCase(Locale.ROOT));
        String out = id;
        for (int n = 2; lower.contains(out.toLowerCase(Locale.ROOT)); n++) out = id + "-" + n;
        return out;
    }


    static String displayName(String fileName) {
        String n = fileName == null ? "" : fileName.trim();
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) n = n.substring(slash + 1);
        String lower = n.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".zip") || lower.endsWith(".7z")) n = n.substring(0, n.lastIndexOf('.'));
        n = n.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length() && sb.length() < 40; i++) {
            char c = n.charAt(i);
            sb.append(c < 0x20 || c == 0x7F ? ' ' : c);
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "Mod" : out;
    }

}
