package com.winlator.d2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;








public final class D2BugReport {
    public static final String NEW_ISSUE = "https://github.com/N1v0k/D2Android-Releases/issues/new";

    public static final String TEMPLATE = "bug.yml";
    public static final String FIELD_PHONE = "phone", FIELD_ANDROID = "android", FIELD_VERSION = "version";

    static final int MAX_VALUE = 64;

    static final int MAX_ENCODED = 192;

    public static final int MAX_URL = NEW_ISSUE.length() + 32 + 3 * (MAX_ENCODED + 10);

    private D2BugReport() {}





    public static String url(String appVersion, String manufacturer, String model, String androidRelease, int sdk) {
        StringBuilder b = new StringBuilder(NEW_ISSUE).append("?template=").append(encode(TEMPLATE));
        param(b, FIELD_VERSION, appVersion);
        param(b, FIELD_PHONE, phone(manufacturer, model));
        param(b, FIELD_ANDROID, android(androidRelease, sdk));
        return b.toString();
    }


    static String phone(String manufacturer, String model) {
        String maker = clean(manufacturer), name = clean(model);
        if (maker.isEmpty()) return name;
        if (name.isEmpty()) return maker;
        if (name.regionMatches(true, 0, maker, 0, maker.length())) return name;
        return maker + " " + name;
    }



    static String android(String release, int sdk) {
        String r = clean(release);
        String api = sdk > 0 ? "API " + sdk : "";
        if (r.isEmpty()) return api;
        return api.isEmpty() ? "Android " + r : "Android " + r + " (" + api + ")";
    }



    static String clean(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isISOControl(cp) || Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                space = b.length() > 0;
                continue;
            }
            if (space) b.append(' ');
            space = false;
            b.appendCodePoint(cp);
        }
        return b.toString();
    }


    static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }


    static String fit(String value) {
        String v = clean(value);
        if (v.codePointCount(0, v.length()) > MAX_VALUE) v = v.substring(0, v.offsetByCodePoints(0, MAX_VALUE)).trim();
        while (!v.isEmpty() && encode(v).length() > MAX_ENCODED) {
            v = v.substring(0, v.offsetByCodePoints(0, v.codePointCount(0, v.length()) - 1)).trim();
        }
        return v;
    }

    private static void param(StringBuilder b, String field, String value) {
        String v = fit(value);
        if (!v.isEmpty()) b.append('&').append(field).append('=').append(encode(v));
    }
}
