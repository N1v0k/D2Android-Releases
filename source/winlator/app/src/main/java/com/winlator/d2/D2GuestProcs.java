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
        java.util.Arrays.sort(names);
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

            File look = dir;
            if (close + 2 < stat.length() && stat.charAt(close + 2) == 'Z') {
                look = livingThread(dir, pid);
                if (look == null) continue;
            }
            String cmd = read(new File(look, "cmdline"));
            cmd = cmd == null ? "" : cmd.replace('\0', ' ');
            if (!under(exe(look), roots) && !environInRoot(look, roots) && !inCommand(cmd, roots, tail)) continue;
            String comm = stat.substring(open + 1, close);
            out.add(new Proc(pid, comm, cmd.contains("wineserver") || "wineserver".equals(comm)));
        }
        return out;
    }






    public static List<String> leftovers(File procDir, int uid, int selfPid) {
        List<String> out = new ArrayList<>();
        String[] names = procDir.list();
        if (names == null) return out;
        java.util.Arrays.sort(names);
        for (String name : names) {
            if (!name.matches("\\d+") || name.equals(String.valueOf(selfPid))) continue;
            File dir = new File(procDir, name);
            if (uid(dir) != uid) continue;
            String stat = read(new File(dir, "stat"));
            if (stat == null) continue;
            int open = stat.indexOf('('), close = stat.lastIndexOf(')');
            if (open < 0 || close < open || close + 2 >= stat.length()) continue;
            char state = stat.charAt(close + 2);
            int threads = threads(dir);
            String exe = exe(dir);
            if (exe == null && state == 'Z') {
                File thread = livingThread(dir, Integer.parseInt(name));
                if (thread != null) exe = exe(thread);
            }
            String states = state == 'Z' || threads > 1 ? threadStates(dir, name) : "";
            out.add(stat.substring(open + 1, close) + " " + name + " " + state + ", " + threads + " threads" + states
                + (exe != null ? ", " + exe : ""));
        }
        return out;
    }





    public static boolean alive(File procDir, int pid) {
        File dir = new File(procDir, String.valueOf(pid));
        char state = state(read(new File(dir, "stat")));
        if (state == 0) return false;
        return state != 'Z' && state != 'X' || livingThread(dir, pid) != null;
    }


    private static File livingThread(File dir, int leader) {
        File task = new File(dir, "task");
        String[] tids = task.list();
        if (tids == null) return null;
        java.util.Arrays.sort(tids);
        for (String tid : tids) {
            if (tid.equals(String.valueOf(leader))) continue;
            File t = new File(task, tid);
            char state = state(read(new File(t, "stat")));
            if (state != 0 && state != 'Z' && state != 'X') return t;
        }
        return null;
    }


    private static String threadStates(File dir, String leader) {
        String[] tids = new File(dir, "task").list();
        if (tids == null) return "";
        java.util.Map<Character, Integer> counts = new java.util.TreeMap<>();
        for (String tid : tids) {
            if (tid.equals(leader)) continue;
            char state = state(read(new File(new File(new File(dir, "task"), tid), "stat")));
            if (state != 0) counts.merge(state, 1, Integer::sum);
        }
        if (counts.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (java.util.Map.Entry<Character, Integer> e : counts.entrySet()) parts.add(e.getKey() + " " + e.getValue());
        return " (" + String.join(", ", parts) + ")";
    }


    private static char state(String stat) {
        if (stat == null) return 0;
        int close = stat.lastIndexOf(')');
        return close < 0 || close + 2 >= stat.length() ? 0 : stat.charAt(close + 2);
    }


    private static int threads(File dir) {
        String status = read(new File(dir, "status"));
        if (status == null) return -1;
        for (String line : status.split("\n")) {
            if (!line.regionMatches(true, 0, "threads:", 0, 8)) continue;
            try {
                return Integer.parseInt(line.substring(8).trim());
            }
            catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
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
