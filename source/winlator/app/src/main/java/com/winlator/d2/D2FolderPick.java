package com.winlator.d2;

import java.io.File;







final class D2FolderPick {
    private D2FolderPick() {}

    enum Outcome {

        REFUSED,

        CHOSEN,

        CHOSEN_BELOW,

        UNCHANGED,

        NO_PATH,

        SEVERAL
    }





    static java.util.List<File> gamesBelow(File picked) {
        java.util.List<File> games = new java.util.ArrayList<>();
        File[] subs = picked != null ? picked.listFiles(File::isDirectory) : null;
        if (subs == null) return games;
        java.util.Arrays.sort(subs);
        for (File sub : subs) {
            String[] names = sub.list();
            if (names != null) for (String n : names) if (n.equalsIgnoreCase("game.exe")) { games.add(sub); break; }
        }
        return games;
    }





    static Outcome outcome(File picked, boolean usable, File found, String before, java.util.List<File> below) {
        Outcome plain = outcome(picked, usable, found, before);
        if (plain != Outcome.REFUSED || below == null || below.size() < 2) return plain;
        for (File game : below) if (game.getAbsolutePath().equals(before)) return Outcome.UNCHANGED;
        return Outcome.SEVERAL;
    }








    static final class LineKeeper {
        private boolean fresh;


        void written() {
            fresh = true;
        }


        boolean dropOnRestart() {
            return !fresh;
        }


        void resumed() {
            fresh = false;
        }
    }





    static Outcome outcome(File picked, boolean usable, File found, String before) {
        if (!usable || found == null) return Outcome.REFUSED;
        String now = found.getAbsolutePath();
        if (now.equals(before)) return Outcome.UNCHANGED;
        return found.getAbsoluteFile().equals(picked.getAbsoluteFile()) ? Outcome.CHOSEN : Outcome.CHOSEN_BELOW;
    }


    enum Title { NO_GAME, WRONG_VERSION, INCOMPLETE, NOT_WRITABLE, NO_PATH, CHOSEN, UNCHANGED, SEVERAL }

    static Title title(Outcome outcome, GameVersion.Status status) {
        switch (outcome) {
            case NO_PATH: return Title.NO_PATH;
            case UNCHANGED: return Title.UNCHANGED;
            case CHOSEN:
            case CHOSEN_BELOW: return Title.CHOSEN;
            case SEVERAL: return Title.SEVERAL;
            default:
                if (status == GameVersion.Status.WRONG_VERSION) return Title.WRONG_VERSION;
                if (status == GameVersion.Status.MISSING) return Title.INCOMPLETE;
                if (status == GameVersion.Status.NOT_WRITABLE) return Title.NOT_WRITABLE;
                return Title.NO_GAME;
        }
    }





    static String traceName(String path) {
        if (path == null) return "null";
        String p = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        int slash = p.lastIndexOf('/');
        return "…/" + (slash >= 0 ? p.substring(slash + 1) : p);
    }





    static boolean reloadStartPage(String shown, String chosen, boolean pageShown) {

        return pageShown && shown != null && !normal(shown).equals(normal(chosen));
    }


    static String normal(String path) {
        if (path == null) return "";
        java.util.ArrayDeque<String> parts = new java.util.ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (!parts.isEmpty()) parts.removeLast();
            }
            else parts.addLast(part);
        }
        StringBuilder out = new StringBuilder();
        for (String part : parts) out.append('/').append(part);
        return path.startsWith("/") ? (out.length() == 0 ? "/" : out.toString()) : out.length() == 0 ? "" : out.substring(1);
    }


    static String shortPath(String path, String storageRoot) {
        if (path == null) return "";
        return storageRoot != null && path.startsWith(storageRoot + "/") ? path.substring(storageRoot.length() + 1) : path;
    }
}
