package com.winlator.d2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;







final class D2GuestWatch {

    static final long WATCH_MS = 2000, POLL_MS = 2, END_WAIT_MS = 300;

    static final String WINESERVER = "wineserver";

    private D2GuestWatch() {}


    static final class Status {
        String state;
        long sigPnd, shdPnd, sigBlk, sigIgn;

        final List<String> threadsPending = new ArrayList<>();
        int threads;


        boolean unusual() {
            return state == null || !state.startsWith("T") || sigPnd != 0 || shdPnd != 0 || !threadsPending.isEmpty();
        }
    }


    static Status read(File proc, int pid, boolean threads) {
        Status s = new Status();
        if (!fill(new File(proc, pid + "/status"), s)) return null;
        if (threads) {
            String[] tids = new File(proc, pid + "/task").list();
            if (tids != null) {

                java.util.Arrays.sort(tids, (a, b) -> Long.compare(number(a), number(b)));
                for (String tid : tids) {
                    Status t = new Status();
                    if (!fill(new File(proc, pid + "/task/" + tid + "/status"), t)) continue;
                    s.threads++;
                    if (t.sigPnd != 0) s.threadsPending.add(tid + ":" + hex(t.sigPnd));
                }
            }
        }
        return s;
    }

    private static long number(String s) {
        try {
            return Long.parseLong(s);
        }
        catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean fill(File file, Status s) {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon), value = line.substring(colon + 1).trim();
                switch (key) {
                    case "State": s.state = value; break;
                    case "SigPnd": s.sigPnd = mask(value); break;
                    case "ShdPnd": s.shdPnd = mask(value); break;
                    case "SigBlk": s.sigBlk = mask(value); break;
                    case "SigIgn": s.sigIgn = mask(value); break;
                    default: break;
                }
            }
            return true;
        }
        catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static long mask(String hex) {
        return Long.parseUnsignedLong(hex, 16);
    }

    private static String hex(long mask) {
        return String.format(Locale.ROOT, "%016x", mask);
    }


    static String signals(long mask) {
        List<String> out = new ArrayList<>();
        for (int bit = 0; bit < 64; bit++) {
            if ((mask & (1L << bit)) == 0) continue;
            int signal = bit + 1;
            String name = name(signal);
            out.add(name != null ? signal + " " + name : String.valueOf(signal));
        }
        return String.join(", ", out);
    }


    private static final String[] NAMES = {null, "HUP", "INT", "QUIT", "ILL", "TRAP", "ABRT", "BUS", "FPE", "KILL", "USR1",
        "SEGV", "USR2", "PIPE", "ALRM", "TERM", "STKFLT", "CHLD", "CONT", "STOP", "TSTP", "TTIN", "TTOU", "URG", "XCPU",
        "XFSZ", "VTALRM", "PROF", "WINCH", "IO", "PWR", "SYS"};

    private static String name(int signal) {
        return signal > 0 && signal < NAMES.length ? NAMES[signal] : null;
    }


    static String traceLine(int pid, String name, Status s) {
        if (s == null) return pid + " " + name + ": gone";
        return pid + " " + name + ": " + s.state + " pnd " + hex(s.sigPnd) + " shd " + hex(s.shdPnd) + " blk " + hex(s.sigBlk)
            + " ign " + hex(s.sigIgn) + (s.threads > 0 ? " threads " + s.threads + (s.threadsPending.isEmpty() ? "" : " pending " + s.threadsPending) : "");
    }


    static String unusualText(int pid, String name, Status s) {
        List<String> what = new ArrayList<>();
        if (s.state == null || !s.state.startsWith("T")) what.add("Zustand " + (s.state != null ? s.state : "?"));
        if (s.sigPnd != 0) what.add("SigPnd " + signals(s.sigPnd));
        if (s.shdPnd != 0) what.add("ShdPnd " + signals(s.shdPnd));
        if (!s.threadsPending.isEmpty()) what.add("Threads mit SigPnd " + s.threadsPending);
        return name + " (pid " + pid + "): " + String.join(", ", what);
    }


    static String unusualLine(List<String> unusual) {
        return unusual.isEmpty() ? null : "Vor dem Fortsetzen auffällig: " + String.join("; ", unusual);
    }





    static char state(File proc, int pid, String comm) {
        return new StatReader().state(new File(proc, pid + "/stat").getPath(), comm != null ? latin1(comm) : null);
    }


    static String comm(File proc, int pid) {
        return new StatReader().comm(new File(proc, pid + "/stat").getPath());
    }

    static byte[] latin1(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }







    static final class StatReader {
        private final byte[] buffer = new byte[256];
        private int length, open, close;


        private boolean load(String path) {
            length = 0;
            try (java.io.FileInputStream in = new java.io.FileInputStream(path)) {
                int n;
                while (length < buffer.length && (n = in.read(buffer, length, buffer.length - length)) > 0) length += n;
            }
            catch (IOException | RuntimeException e) {
                return false;
            }
            open = -1;
            close = -1;
            for (int i = 0; i < length; i++) {
                if (buffer[i] == '(' && open < 0) open = i;
                if (buffer[i] == ')') close = i;
            }
            return open >= 0 && close > open && close + 2 < length;
        }


        char state(String path, byte[] comm) {
            if (!load(path)) return 0;
            if (comm != null) {
                if (close - open - 1 != comm.length) return 0;
                for (int i = 0; i < comm.length; i++) if (buffer[open + 1 + i] != comm[i]) return 0;
            }
            return (char) (buffer[close + 2] & 0xff);
        }

        String comm(String path) {
            if (!load(path)) return null;
            return new String(buffer, open + 1, close - open - 1, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }





    static final class Gone {
        final String name;
        final int pid;
        final int round;
        final long nanos;
        final boolean zombie;

        Gone(String name, int pid, int round, long nanos, boolean zombie) {
            this.name = name;
            this.pid = pid;
            this.round = round;
            this.nanos = nanos;
            this.zombie = zombie;
        }
    }











    static String goneLine(List<Gone> gone, int watched, long watchedMs, boolean gameEnded) {
        StringBuilder b = new StringBuilder("Nach dem Fortsetzen (").append(watched).append(" Spielprozesse, ")
            .append(watchedMs).append(" ms beobachtet").append(gameEnded ? ", Spiel endete" : "").append("): ");
        if (gone.isEmpty()) return b.append("keiner beendet").toString();
        List<String> groups = new ArrayList<>();
        for (int i = 0; i < gone.size(); ) {
            int j = i;
            List<String> parts = new ArrayList<>();
            while (j < gone.size() && gone.get(j).round == gone.get(i).round) {
                Gone g = gone.get(j++);
                parts.add(String.format(Locale.ROOT, "%s (pid %d) %s nach %.1f ms", g.name, g.pid, g.zombie ? "Zombie" : "weg", g.nanos / 1e6));
            }
            groups.add(String.join(", ", parts) + (parts.size() > 1 ? " (gleicher Durchlauf, Reihenfolge offen)" : ""));
            i = j;
        }
        b.append(String.join(", ", groups));
        int left = watched - gone.size();
        return b.append(left > 0 ? "; " + left + " noch da" : "; alle beendet").toString();
    }


    static String contLine(int pid, String name, long sinceFirstNanos) {
        return String.format(Locale.ROOT, "cont %d %s +%.2f ms", pid, name, sinceFirstNanos / 1e6);
    }





    static long deepSleepMs(long elapsedMs, long uptimeMs) {
        if (elapsedMs < 0 || uptimeMs < 0) return -1;
        return Math.max(0, elapsedMs - uptimeMs);
    }
}
