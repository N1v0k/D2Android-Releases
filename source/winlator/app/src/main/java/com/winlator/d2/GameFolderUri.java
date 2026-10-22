package com.winlator.d2;

public final class GameFolderUri {
    public static final String EXTERNAL_STORAGE = "com.android.externalstorage.documents";
    public static final String DOWNLOADS = "com.android.providers.downloads.documents";

    private GameFolderUri() {}

    public static String toPath(String authority, String treeDocumentId, String primaryRoot) {
        if (authority == null || treeDocumentId == null || primaryRoot == null) return null;
        if (authority.equals(DOWNLOADS)) {
            return treeDocumentId.startsWith("raw:/") ? clean(treeDocumentId.substring(4)) : null;
        }
        if (!authority.equals(EXTERNAL_STORAGE)) return null;
        int colon = treeDocumentId.indexOf(':');
        if (colon <= 0) return null;
        String volume = treeDocumentId.substring(0, colon);
        String rest = treeDocumentId.substring(colon + 1);
        String root;
        if (volume.equals("primary")) root = primaryRoot;
        else if (volume.equals("home")) root = primaryRoot + "/Documents";
        else if (volume.matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")) root = "/storage/" + volume;
        else return null;
        return clean(rest.isEmpty() ? root : root + "/" + rest);
    }

    static String clean(String path) {
        if (!path.startsWith("/")) return null;
        StringBuilder out = new StringBuilder();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) return null;
            out.append('/').append(part);
        }
        return out.length() == 0 ? "/" : out.toString();
    }
}
