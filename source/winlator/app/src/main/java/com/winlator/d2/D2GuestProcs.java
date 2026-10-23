package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;











public final class D2GuestProcs {
    private D2GuestProcs() {}


    public static final class Proc {
        public final int pid;
        public final String name;
        public final boolean server;

        Proc(int pid, String name, boolean server) {
            this.pid = pid;
            this.name = name;
            this.server = server;
        }

        @Override
        public String toString() {
            return name + " " + pid;
        }
    }


    public static List<Proc> find(File procDir, File rootfs, int uid, int selfPid, Collection<Integer> skip) {
        List<Proc> out = new ArrayList<>();
        String[] names = procDir.list();
        if (names == null) return out;
        Set<String> roots = prefixes(rootfs);

        String tail = "/" + (rootfs.getParentFile() == null ? "" : rootfs.getParentFile().getName() + "/") + rootfs.getName() + "/";
        for (String name : names) {
            if (!name.matches("\\d+")) continue;
            int pid;
            try {
                pid = Integer.parseInt(name);
            }
            catch (NumberFormatException e) {
                continue;
            }
            if (pid == selfPid || skip != null && skip.contains(pid)) continue;
            File dir = new File(procDir, name);
            if (uid(dir) != uid) continue;
            String stat = read(new File(dir, "stat"));
            if (stat == null) continue;
            int open = stat.indexOf('('), close = stat.lastIndexOf(')');
            if (open < 0 || close < open) continue;
            if (close + 2 < stat.length() && stat.charAt(close + 2) == 'Z') continue;
            String cmd = read(new File(dir, "cmdline"));
            cmd = cmd == null ? "" : cmd.replace('\0', ' ');
            if (!under(exe(dir), roots) && !environInRoot(dir, roots) && !inCommand(cmd, roots, tail)) continue;
            String comm = stat.substring(open + 1, close);
            out.add(new Proc(pid, comm, cmd.contains("wineserver") || "wineserver".equals(comm)));
        }
        return out;
    }


    static Set<String> prefixes(File rootfs) {
        Set<String> out = new LinkedHashSet<>();
        out.add(rootfs.getAbsolutePath());
        try {
            out.add(rootfs.getCanonicalPath());
        }
        catch (IOException ignored) {

        }
        for (String p : new ArrayList<>(out)) {
            if (p.startsWith("/data/user/0/")) out.add("/data/data/" + p.substring("/data/user/0/".length()));
            else if (p.startsWith("/data/data/")) out.add("/data/user/0/" + p.substring("/data/data/".length()));
        }
        return out;
    }

    private static boolean under(String path, Set<String> roots) {
        if (path == null) return false;
        for (String root : roots) if (path.equals(root) || path.startsWith(root + "/")) return true;
        return false;
    }

    private static boolean inCommand(String cmd, Set<String> roots, String tail) {
        for (String root : roots) if (cmd.contains(root + "/")) return true;
        return cmd.contains(tail);
    }


    private static boolean environInRoot(File dir, Set<String> roots) {
        String env = read(new File(dir, "environ"));
        if (env == null) return false;
        for (String entry : env.split("\0")) {
            if (entry.startsWith("WINEPREFIX=") && under(entry.substring("WINEPREFIX=".length()), roots)) return true;
        }
        return false;
    }


    private static String exe(File dir) {
        try {
            String target = Files.readSymbolicLink(new File(dir, "exe").toPath()).toString();
            return target.endsWith(" (deleted)") ? target.substring(0, target.length() - " (deleted)".length()) : target;
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }


    private static int uid(File dir) {
        String status = read(new File(dir, "status"));
        if (status == null) return -1;
        for (String line : status.split("\n")) {
            if (!line.startsWith("Uid:")) continue;
            String[] fields = line.substring(4).trim().split("\\s+");
            try {
                return Integer.parseInt(fields[0]);
            }
            catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static String read(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
