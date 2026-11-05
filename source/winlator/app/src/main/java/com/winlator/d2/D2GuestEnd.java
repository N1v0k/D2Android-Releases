package com.winlator.d2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

















public final class D2GuestEnd {
    private D2GuestEnd() {}


    public static final long GONE_MS = 500;

    public static final long SAVE_MS = 1500;





    public static final long HUNG_GONE_MS = 150, HUNG_SAVE_MS = 400;


    public static long longestMs(long goneMs, long serverMs) {
        return goneMs + Math.max(serverMs, goneMs) + goneMs;
    }


    public interface Signals {
        void kill(int pid);

        void term(int pid);

        void cont(int pid);
    }





    public static String run(File procDir, File rootfs, int uid, int selfPid, long goneMs, long serverMs, Signals signals) {
        List<D2GuestProcs.Proc> found = D2GuestProcs.find(procDir, rootfs, uid, selfPid, null);
        if (found.isEmpty()) return "";
        long start = System.currentTimeMillis();
        List<Integer> servers = new ArrayList<>(), others = new ArrayList<>();
        for (D2GuestProcs.Proc p : found) (p.server ? servers : others).add(p.pid);
        StringBuilder line = new StringBuilder("guests: ");
        if (!others.isEmpty()) {
            for (int pid : others) send(signals, pid, 'K');
            List<Integer> left = waitGone(procDir, others, start + goneMs);
            line.append("killed ").append(names(found, others)).append(left.isEmpty()
                ? " (gone in " + (System.currentTimeMillis() - start) + " ms)" : " (" + left + " not gone within " + goneMs + " ms)");
        }
        if (!servers.isEmpty()) {
            if (!others.isEmpty()) line.append("; ");
            long saveStart = System.currentTimeMillis();
            if (serverMs > 0) {
                for (int pid : servers) {
                    send(signals, pid, 'T');
                    send(signals, pid, 'C');
                }
                List<Integer> left = waitGone(procDir, servers, saveStart + serverMs);
                line.append(left.isEmpty() ? "wineserver " + servers + " saved and ended in " + (System.currentTimeMillis() - saveStart) + " ms"
                    : "wineserver " + left + " did not end within " + serverMs + " ms of SIGTERM");
            }
            else {
                for (int pid : servers) send(signals, pid, 'K');
                waitGone(procDir, servers, saveStart + goneMs);
                line.append("wineserver ").append(servers).append(" killed");
            }
        }

        List<D2GuestProcs.Proc> rest = D2GuestProcs.find(procDir, rootfs, uid, selfPid, null);
        if (!rest.isEmpty()) {
            List<Integer> pids = new ArrayList<>();
            for (D2GuestProcs.Proc p : rest) {
                pids.add(p.pid);
                send(signals, p.pid, 'K');
            }
            List<Integer> left = waitGone(procDir, pids, System.currentTimeMillis() + goneMs);
            line.append("; then killed ").append(rest);
            if (!left.isEmpty()) line.append(", still alive after ").append(goneMs).append(" ms: ").append(names(rest, left));
        }
        return line.toString();
    }

    private static void send(Signals signals, int pid, char which) {
        try {
            if (which == 'K') signals.kill(pid);
            else if (which == 'T') signals.term(pid);
            else signals.cont(pid);
        }
        catch (RuntimeException ignored) {

        }
    }


    private static List<Integer> waitGone(File procDir, Collection<Integer> pids, long endMs) {
        List<Integer> left = new ArrayList<>();
        for (int pid : pids) {
            while (D2GuestProcs.alive(procDir, pid) && System.currentTimeMillis() < endMs) {
                try {
                    Thread.sleep(20);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (D2GuestProcs.alive(procDir, pid)) left.add(pid);
        }
        return left;
    }


    private static String names(List<D2GuestProcs.Proc> found, Collection<Integer> pids) {
        List<D2GuestProcs.Proc> out = new ArrayList<>();
        for (D2GuestProcs.Proc p : found) if (pids.contains(p.pid)) out.add(p);
        return out.toString();
    }
}
