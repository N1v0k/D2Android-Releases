package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

final class D2StallSnapshot {
    static final int MAX_THREADS = 20;
    static final int PER_LINE = 4;

    private D2StallSnapshot() {}

    static List<String> threads(File proc, int pid) {
        File[] tasks = new File(proc, pid + "/task").listFiles();
        List<String> out = new ArrayList<>();
        if (tasks == null) return out;
        List<long[]> order = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (File task : tasks) {
            String stat = read(new File(task, "stat"));
            if (stat == null) continue;
            int open = stat.indexOf('('), close = stat.lastIndexOf(')');
            if (open < 0 || close < open || close + 2 >= stat.length()) continue;
            String comm = stat.substring(open + 1, close);
            String[] rest = stat.substring(close + 2).trim().split("\\s+");
            String state = rest.length > 0 ? rest[0] : "?";
            long ticks = 0;
            try {
                if (rest.length > 12) ticks = Long.parseLong(rest[11]) + Long.parseLong(rest[12]);
            }
            catch (NumberFormatException ignored) {
            }
            String wchan = read(new File(task, "wchan"));
            wchan = wchan == null || wchan.trim().isEmpty() ? "-" : wchan.trim();
            order.add(new long[] {ticks, lines.size()});
            lines.add(task.getName() + " " + comm + " " + state + " " + wchan + " cpu " + ticks);
        }
        order.sort((a, b) -> Long.compare(b[0], a[0]));
        for (long[] o : order) {
            if (out.size() >= MAX_THREADS) break;
            out.add(lines.get((int) o[1]));
        }
        return out;
    }

    static List<String> packed(List<String> threads) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < threads.size(); i += PER_LINE) {
            out.add(String.join(" | ", threads.subList(i, Math.min(threads.size(), i + PER_LINE))));
        }
        return out;
    }

    static int gamePid(File proc, int[] pids) {
        for (int pid : pids) {
            String stat = read(new File(proc, pid + "/stat"));
            if (stat == null) continue;
            int open = stat.indexOf('('), close = stat.lastIndexOf(')');
            if (open >= 0 && close > open && stat.substring(open + 1, close).equalsIgnoreCase("Game.exe")) return pid;
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
