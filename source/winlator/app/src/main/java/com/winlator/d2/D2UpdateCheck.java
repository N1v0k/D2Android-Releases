package com.winlator.d2;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;






public final class D2UpdateCheck {

    public static final String LATEST_URL = "https://api.github.com/repos/N1v0k/D2Android-Releases/releases/latest";
    public static final String RELEASES_PAGE = "https://github.com/N1v0k/D2Android-Releases/releases";
    public static final long DAY_MS = 24L * 60 * 60 * 1000;

    private D2UpdateCheck() {}


    public static final class Release {
        public final String tag, version, url, body;

        public Release(String tag, String url, String body) {
            this.tag = tag;
            this.version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            this.url = url != null && url.startsWith(RELEASES_PAGE) ? url : RELEASES_PAGE;
            this.body = body != null ? body : "";
        }
    }


    public static Release parse(String json) {
        try {
            JSONObject o = new JSONObject(json);
            String tag = text(o, "tag_name").trim();
            if (tag.isEmpty() || o.optBoolean("draft", false) || o.optBoolean("prerelease", false)) return null;
            if (numbers(tag).isEmpty()) return null;
            return new Release(tag, o.isNull("html_url") ? null : o.optString("html_url", null), text(o, "body"));
        }
        catch (JSONException e) {
            return null;
        }
    }


    private static String text(JSONObject o, String key) {
        return o.isNull(key) ? "" : o.optString(key, "");
    }


    static List<Long> numbers(String version) {
        List<Long> list = new ArrayList<>();
        long n = -1;
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c >= '0' && c <= '9') n = (n < 0 ? 0 : n) * 10 + (c - '0');
            else if (n >= 0) {
                list.add(n);
                n = -1;
            }
            if (n > 1_000_000_000L) return new ArrayList<>();
        }
        if (n >= 0) list.add(n);
        return list;
    }


    public static int compare(String a, String b) {
        List<Long> x = numbers(a), y = numbers(b);
        for (int i = 0; i < Math.max(x.size(), y.size()); i++) {
            long p = i < x.size() ? x.get(i) : 0, q = i < y.size() ? y.get(i) : 0;
            if (p != q) return p < q ? -1 : 1;
        }
        return 0;
    }


    public static boolean isNewer(Release release, String installedVersion) {
        if (release == null || installedVersion == null || numbers(installedVersion).isEmpty()) return false;
        return compare(release.version, installedVersion) > 0;
    }


    public static boolean due(long lastCheck, long now) {
        return lastCheck <= 0 || now < lastCheck || now - lastCheck >= DAY_MS;
    }


    public static String preview(String body, int maxLines) {
        StringBuilder out = new StringBuilder();
        int lines = 0;
        for (String line : body.split("\r?\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("<!--")) continue;
            if (lines == maxLines) {
                out.append("\n…");
                break;
            }
            if (out.length() > 0) out.append('\n');
            out.append(t.startsWith("- ") || t.startsWith("* ") ? "• " + t.substring(2) : t);
            lines++;
        }
        return out.toString();
    }
}
