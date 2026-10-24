package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;






public final class D2InstallPlan {

    public static final String ROOT_FOLDER = "D2Android";

    public static final String INSTALLER_FOLDER = "Installer";

    public static final String GAME_FOLDER = "Diablo2";





    public static final String UNFINISHED_MARKER = ".d2android-installing";

    static final String DOWNLOADER_URL = "https://downloader.battle.net/download/getLegacy?os=win&locale=%s&product=%s";






    public enum Language {
        EN_US("enUS", true),
        DE_DE("deDE", true),
        FR_FR("frFR", true),
        ES_ES("esES", true),
        IT_IT("itIT", true);

        public final String locale;
        public final boolean available;

        Language(String locale, boolean available) {
            this.locale = locale;
            this.available = available;
        }
    }


    public enum Product {
        CLASSIC("D2DV", "D2"),
        LOD("D2XP", "LoD");

        public final String code;

        public final String folder;

        Product(String code, String folder) {
            this.code = code;
            this.folder = folder;
        }
    }


    private static final Pattern DOWNLOADER_TITLE = Pattern.compile("^\\s*(\\d{1,3})\\s*%\\s*[-\u2013\u2014]\\s*Blizzard Downloader.*");

    private D2InstallPlan() {}



    public static final int NO_START_SECONDS = 180;







    public static String helperState(java.util.List<String> lines) {
        String state = null;
        for (String line : lines) {
            String[] parts = line.trim().split(" ");
            if (parts.length < 3) continue;
            if (parts[1].equals("progress")) return "progress";
            if (parts[1].equals("no-progress") || parts[1].equals("start-menu")) state = parts[1];
        }
        return state;
    }








    public static String helperResult(java.util.List<String> lines) {
        String last = "no end in the helper log";
        for (String line : lines) {
            String[] parts = line.trim().split(" ");
            if (parts.length < 3) continue;
            String what = parts[1], value = parts[2];
            if (what.equals("wrong-key")) return what;
            if (what.equals("child-ended")) last = value.equals("0") ? null : what + " " + value;
            else if (what.startsWith("pipe-") || what.equals("start-failed")) last = what + " " + value;
        }
        return last;
    }

    public static int downloaderProgress(String title) {
        if (title == null) return -1;
        Matcher m = DOWNLOADER_TITLE.matcher(title);
        if (!m.matches()) return -1;
        int percent = Integer.parseInt(m.group(1));
        return percent <= 100 ? percent : -1;
    }


    public static Language defaultLanguage(Locale device) {
        String tag = device == null ? "" : device.getLanguage();
        for (Language l : Language.values()) {
            if (l.available && l.locale.startsWith(tag) && !tag.isEmpty()) return l;
        }
        for (Language l : Language.values()) if (l.available) return l;
        throw new IllegalStateException("no installable language");
    }


    public static String formatGb(long bytes, Locale locale) {
        double gb = Math.floor(Math.max(0, bytes) / (1024.0 * 1024 * 1024) * 10) / 10;
        return String.format(locale, "%.1f", gb);
    }

    public static String downloaderUrl(Language language, Product product) {
        if (!language.available) throw new IllegalArgumentException(language.locale + " is not available yet");
        return String.format(Locale.ROOT, DOWNLOADER_URL, language.locale, product.code);
    }

    public static File root(File downloadDir) {
        return new File(downloadDir, ROOT_FOLDER);
    }

    public static File installerDir(File downloadDir, Product product) {
        return new File(new File(root(downloadDir), INSTALLER_FOLDER), product.folder);
    }

    public static File gameDir(File downloadDir) {
        return new File(root(downloadDir), GAME_FOLDER);
    }







    public static File freeGameDir(File downloadDir) {
        return freeGameDir(downloadDir, null);
    }





    public static File freeGameDir(File downloadDir, File chosen) {
        File first = gameDir(downloadDir);
        if (isFree(first, chosen)) return first;
        for (int i = 2; ; i++) {
            File next = new File(root(downloadDir), GAME_FOLDER + "-" + i);
            if (isFree(next, chosen)) return next;
        }
    }

    private static boolean isFree(File dir, File chosen) {

        if (chosen != null && dir.getAbsoluteFile().equals(chosen.getAbsoluteFile())) return chosenReusable(dir);
        if (!dir.exists()) return true;
        String[] content = dir.list();
        return dir.isDirectory() && content != null && (content.length == 0 || isUnfinished(dir) && !hasSaves(dir));
    }

    private static boolean isUnfinished(File dir) {
        File marker = dir != null ? new File(dir, UNFINISHED_MARKER) : null;
        return marker != null && marker.isFile() && !markerPassed(marker);
    }


    static final String MARKER_PASSED = "passed";

    private static boolean markerPassed(File marker) {
        if (marker.length() != MARKER_PASSED.length()) return false;
        try {
            return MARKER_PASSED.equals(new String(Files.readAllBytes(marker.toPath()), java.nio.charset.StandardCharsets.US_ASCII));
        }
        catch (IOException e) {
            return false;
        }
    }





    public static boolean unfinishedInstall(File dir) {
        return isUnfinished(dir);
    }






    public static boolean hasSaves(File dir) {
        File[] entries = dir != null ? dir.listFiles() : null;
        if (entries == null) return dir != null && dir.exists();
        for (File e : entries) {
            if (!e.getName().equalsIgnoreCase("save")) continue;
            if (!e.isDirectory() || Files.isSymbolicLink(e.toPath())) return true;
            String[] content = e.list();
            if (content == null || content.length > 0) return true;
        }
        return false;
    }










    public static boolean stoppedWithSaves(File chosen) {
        return isUnfinished(chosen) && hasSaves(chosen);
    }

    public static boolean chosenReusable(File chosen) {
        return isUnfinished(chosen) && !hasSaves(chosen);
    }


    public static final String INSTALLED_MARKER = ".d2android-installed";






    public static void markInstalled(File dir, Language language) throws IOException {
        writeLanguage(new File(dir, INSTALLED_MARKER), language);
    }


    public static Language folderLanguage(File dir) {
        if (dir == null) return null;
        Language l = readLanguage(new File(dir, INSTALLED_MARKER));
        return l != null ? l : readLanguage(new File(dir, UNFINISHED_MARKER));
    }


    public static final String INSTALLER_LANGUAGE = "language";

    public static void markInstallerLanguage(File downloadDir, Language language) throws IOException {
        File dir = new File(root(downloadDir), INSTALLER_FOLDER);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        writeLanguage(new File(dir, INSTALLER_LANGUAGE), language);
    }


    public static Language installerLanguage(File downloadDir) {
        if (!installerFilesThere(downloadDir)) return null;
        return readLanguage(new File(new File(root(downloadDir), INSTALLER_FOLDER), INSTALLER_LANGUAGE));
    }

    private static void writeLanguage(File file, Language language) throws IOException {
        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.US_ASCII)) {
            if (language != null) w.write(language.locale);
        }
    }

    private static Language readLanguage(File file) {
        if (!file.isFile()) return null;
        try {
            String text = new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.US_ASCII).trim();
            for (Language l : Language.values()) if (l.locale.equals(text)) return l;
        }
        catch (IOException ignored) {

        }
        return null;
    }






    public static File findFinished(File downloadDir, File chosen) {
        File[] dirs = root(downloadDir).listFiles();
        if (dirs == null) return null;
        java.util.Arrays.sort(dirs);
        for (File d : dirs) {
            if (!d.getName().startsWith(GAME_FOLDER)) continue;
            if (chosen != null && d.getAbsoluteFile().equals(chosen.getAbsoluteFile())) continue;
            if (isFinishedOwn(d)) return d;
        }
        return null;
    }

    private static boolean isFinishedOwn(File d) {
        return !isUnfinished(d) && new File(d, INSTALLED_MARKER).isFile() && new File(d, "Game.exe").isFile()
            && new File(d, "d2data.mpq").isFile() && new File(d, "d2exp.mpq").isFile();
    }

    private static boolean anyFinishedOwn(File downloadDir) {
        File[] dirs = root(downloadDir).listFiles();
        if (dirs != null) for (File d : dirs) if (d.getName().startsWith(GAME_FOLDER) && isFinishedOwn(d)) return true;
        return false;
    }


    private static boolean installerFilesThere(File downloadDir) {
        for (Product p : Product.values()) {
            String[] files = installerDir(downloadDir, p).list();
            if (files != null) for (String f : files) if (!f.toLowerCase(java.util.Locale.ROOT).startsWith("downloader_")) return true;
        }
        return false;
    }


    public static long installerBytes(File downloadDir) {
        return treeBytes(new File(root(downloadDir), INSTALLER_FOLDER));
    }


    public static long reusableBytes(File dir) {
        return isUnfinished(dir) ? treeBytes(dir) : 0;
    }

    private static long treeBytes(File f) {
        if (f.isFile()) return f.length();
        File[] children = f.isDirectory() && !Files.isSymbolicLink(f.toPath()) ? f.listFiles() : null;
        long sum = 0;
        if (children != null) for (File c : children) sum += treeBytes(c);
        return sum;
    }


    public static boolean awaitsPatch(File dir) {
        return isUnfinished(dir) && new File(dir, INSTALLED_MARKER).isFile() && new File(dir, "d2data.mpq").isFile()
            && new File(dir, "d2exp.mpq").isFile();
    }





    public static File findAwaitingPatch(File downloadDir, File chosen) {
        File[] dirs = root(downloadDir).listFiles();
        if (dirs == null) return null;
        java.util.Arrays.sort(dirs);
        for (File d : dirs) {
            if (!d.getName().startsWith(GAME_FOLDER)) continue;
            if (chosen != null && d.getAbsoluteFile().equals(chosen.getAbsoluteFile()) && !chosenReusable(d)) continue;
            if (hasSaves(d)) continue;
            if (awaitsPatch(d)) return d;
        }
        return null;
    }






    public static boolean interrupted(File downloadDir) {
        File[] dirs = root(downloadDir).listFiles();

        if (dirs != null) for (File d : dirs) if (d.getName().startsWith(GAME_FOLDER) && isUnfinished(d) && !hasSaves(d)) return true;
        return installerFilesThere(downloadDir) && !anyFinishedOwn(downloadDir);
    }


    public static void discardAll(File downloadDir) {
        File[] dirs = root(downloadDir).listFiles();
        if (dirs != null) for (File d : dirs) if (d.getName().startsWith(GAME_FOLDER)) discardUnfinished(d);
        File installers = new File(root(downloadDir), INSTALLER_FOLDER);
        deleteContent(installers);
        installers.delete();
    }





    public static void beginInstall(File dir, Language language) throws IOException {

        if (isUnfinished(dir) && hasSaves(dir)) throw new IOException("saves in " + dir + ", not emptied");
        if (isUnfinished(dir)) deleteContent(dir);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        writeLanguage(new File(dir, UNFINISHED_MARKER), language);
    }


    public static void beginInstall(File dir) throws IOException {
        beginInstall(dir, null);
    }








    public static boolean installFinished(File dir) {
        File marker = new File(dir, UNFINISHED_MARKER);
        if (!marker.exists() || marker.delete()) return true;
        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(marker), java.nio.charset.StandardCharsets.US_ASCII)) {
            w.write(MARKER_PASSED);
        }
        catch (IOException e) {
            return false;
        }
        return !isUnfinished(dir);
    }


    public static boolean discardUnfinished(File dir) {
        if (!isUnfinished(dir) || hasSaves(dir)) return false;
        deleteContent(dir);
        return dir.delete();
    }


    private static void deleteContent(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory() && !Files.isSymbolicLink(child.toPath())) deleteContent(child);
            child.delete();
        }
    }
}
