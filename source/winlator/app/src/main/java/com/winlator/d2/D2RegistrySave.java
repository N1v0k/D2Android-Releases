package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;













public final class D2RegistrySave {
    private D2RegistrySave() {}


    public static final long WAIT_MS = 1500;


    public interface Signals {
        void term(int pid);

        void cont(int pid);
    }





    public static String run(File procDir, File rootfs, int uid, int selfPid, long maxMs, Signals signals) {
        List<Integer> servers = new ArrayList<>();
        for (D2GuestProcs.Proc p : D2GuestProcs.find(procDir, rootfs, uid, selfPid, null)) if (p.server) servers.add(p.pid);
        if (servers.isEmpty()) return "";
        long start = System.currentTimeMillis();
        for (int pid : servers) {
            try {
                signals.term(pid);
                signals.cont(pid);
            }
            catch (RuntimeException ignored) {

            }
        }
        long end = start + maxMs;
        List<Integer> left = new ArrayList<>();
        for (int pid : servers) {
            while (alive(procDir, pid) && System.currentTimeMillis() < end) {
                try {
                    Thread.sleep(20);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (alive(procDir, pid)) left.add(pid);
        }
        long ms = System.currentTimeMillis() - start;
        return left.isEmpty() ? "registry: wineserver " + servers + " saved and ended in " + ms + " ms"
            : "registry: wineserver " + left + " did not end within " + maxMs + " ms (" + servers + " got SIGTERM)";
    }


    public static boolean alive(File procDir, int pid) {
        try {
            String stat = new String(Files.readAllBytes(new File(procDir, pid + "/stat").toPath()), StandardCharsets.ISO_8859_1);
            int close = stat.lastIndexOf(')');
            return close < 0 || close + 2 >= stat.length() || stat.charAt(close + 2) != 'Z';
        }
        catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
