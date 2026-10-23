package com.winlator.d2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;










public final class D2ExitGuard {

    public static final long TEARDOWN_MS = 4000;


    public interface Host {
        void hangs(String step, long ms);
    }

    private final CountDownLatch over = new CountDownLatch(1);
    private final long startMs = System.currentTimeMillis();
    private volatile String step = "start";

    private D2ExitGuard() {}


    public static D2ExitGuard arm(long limitMs, Host host) {
        D2ExitGuard guard = new D2ExitGuard();
        Thread watcher = new Thread(() -> {
            try {
                if (!guard.over.await(limitMs, TimeUnit.MILLISECONDS)) host.hangs(guard.step, guard.elapsedMs());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "d2-exit-guard");
        watcher.setDaemon(true);
        watcher.start();
        return guard;
    }


    public void step(String name) {
        step = name;
    }


    public long done() {
        over.countDown();
        return elapsedMs();
    }

    long elapsedMs() {
        return System.currentTimeMillis() - startMs;
    }
}
