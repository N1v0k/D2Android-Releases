package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class D2FolderLock {
    static final String FILE = ".d2android-lock";
    static final long BEAT_MS = 20_000, STALE_MS = 90_000;

    private D2FolderLock() {}

    static final class Holder {
        final String pkg, label;
        final long since, beat;

        Holder(String pkg, String label, long since, long beat) {
            this.pkg = pkg;
            this.label = label;
            this.since = since;
            this.beat = beat;
        }
    }

    static final class Decision {
        final boolean foreign, byOs, byBeat, unusable;
        final String label;
        final long since;

        Decision(boolean byOs, boolean byBeat, String label, long since) {
            this(byOs, byBeat, label, since, false);
        }

        private Decision(boolean byOs, boolean byBeat, String label, long since, boolean unusable) {
            this.foreign = byOs || byBeat;
            this.byOs = byOs;
            this.byBeat = byBeat;
            this.label = label;
            this.since = since;
            this.unusable = unusable && !foreign;
        }

        String levels() {
            return byOs && byBeat ? "os+beat" : byOs ? "os" : byBeat ? "beat" : "none";
        }
    }

    static final Decision FREE = new Decision(false, false, null, 0);
    static final Decision UNUSABLE = new Decision(false, false, null, 0, true);

    static String format(Holder h) {
        return "package=" + h.pkg + "\nlabel=" + h.label.replace('\n', ' ').replace('\r', ' ') + "\nsince=" + h.since
            + "\nbeat=" + h.beat + "\n";
    }

    static Holder parse(String text) {
        if (text == null) return null;
        String pkg = null, label = "";
        long since = 0, beat = -1;
        for (String line : text.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq), value = line.substring(eq + 1).trim();
            try {
                switch (key) {
                    case "package": pkg = value; break;
                    case "label": label = value; break;
                    case "since": since = Long.parseLong(value); break;
                    case "beat": beat = Long.parseLong(value); break;
                    default: break;
                }
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        return pkg == null || pkg.isEmpty() || beat < 0 ? null : new Holder(pkg, label, since, beat);
    }

    static Decision decide(Holder content, long now, String ownPackage, boolean osHeldElsewhere) {
        boolean other = content != null && !content.pkg.equals(ownPackage);
        boolean byBeat = other && Math.abs(now - content.beat) <= STALE_MS;
        return new Decision(osHeldElsewhere, byBeat, other ? content.label : null, other ? content.since : 0);
    }


    private static final class Held {
        final File file;
        final RandomAccessFile raf;
        final FileLock lock;
        final Holder me;
        int users = 1;
        Runnable beat;

        Held(File file, RandomAccessFile raf, FileLock lock, Holder me) {
            this.file = file;
            this.raf = raf;
            this.lock = lock;
            this.me = me;
        }
    }

    private static final Map<String, Held> held = new HashMap<>();
    private static ScheduledExecutorService beats;

    static synchronized Decision acquire(String pkg, String label, File gameDir, String why) {
        File file = lockFile(gameDir);
        if (file == null) return UNUSABLE;
        Held mine = held.get(file.getPath());
        if (mine != null) {
            mine.users++;
            return FREE;
        }
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "rw");
        }
        catch (IOException | RuntimeException e) {
            Decision decision = readOnly(file, pkg, System.currentTimeMillis());
            D2Trace.log("folder lock: " + why + ", file not writable (" + e + "): " + (decision.foreign
                ? "refused, held by " + decision.label + " (" + decision.levels() + ")" : "unusable"));
            return decision;
        }
        FileLock lock = null;
        try {
            boolean osElsewhere = false;
            try {
                lock = raf.getChannel().tryLock();
                osElsewhere = lock == null;
            }
            catch (IOException | OverlappingFileLockException | UnsupportedOperationException e) {
                D2Trace.log("folder lock: os lock not supported here, heartbeat only (" + e + ")");
            }
            Decision decision = decide(parse(read(raf)), System.currentTimeMillis(), pkg, osElsewhere);
            if (decision.foreign) {
                D2Trace.log("folder lock: " + why + " refused, held by " + decision.label + " (" + decision.levels() + ")");
                if (lock != null) lock.release();
                raf.close();
                return decision;
            }
            long now = System.currentTimeMillis();
            Holder me = new Holder(pkg, label, now, now);
            write(raf, me);
            Held h = new Held(file, raf, lock, me);
            held.put(file.getPath(), h);
            startBeat(h);
            D2Trace.log("folder lock: held for " + why + " (os lock " + (lock != null ? "ok" : "not held") + ")");
            return FREE;
        }
        catch (IOException | RuntimeException e) {
            D2Trace.log("folder lock: " + why + " without a lock, unusable (" + e + ")");
            try {
                if (lock != null) lock.release();
                raf.close();
            }
            catch (IOException ignored) {
            }
            return UNUSABLE;
        }
    }

    static Decision readOnly(File file, String pkg, long now) {
        try {
            Decision decision = decide(parse(new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)),
                now, pkg, false);
            return decision.foreign ? decision : UNUSABLE;
        }
        catch (IOException | RuntimeException e) {
            return UNUSABLE;
        }
    }

    static synchronized void release(File gameDir) {
        File file = lockFile(gameDir);
        if (file == null) return;
        Held h = held.get(file.getPath());
        if (h == null || --h.users > 0) return;
        close(h);
    }

    static synchronized void releaseAll() {
        for (Held h : new java.util.ArrayList<>(held.values())) close(h);
    }

    private static void close(Held h) {
        held.remove(h.file.getPath());
        try {
            h.raf.getChannel().truncate(0);
            if (h.lock != null) h.lock.release();
            h.raf.close();
            D2Trace.log("folder lock: released");
        }
        catch (IOException e) {
            D2Trace.log("folder lock: release failed (" + e + "), stale in " + STALE_MS / 1000 + " s");
        }
    }

    private static File lockFile(File gameDir) {
        if (gameDir == null) return null;
        File saveDir = D2Saves.saveDir(gameDir);
        if (!saveDir.isDirectory() && !saveDir.mkdirs()) {
            D2Trace.log("folder lock: no save folder " + saveDir + ", no lock");
            return null;
        }
        return new File(saveDir, FILE);
    }

    private static String read(RandomAccessFile raf) throws IOException {
        FileChannel channel = raf.getChannel();
        long size = Math.min(channel.size(), 4096);
        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        while (buffer.hasRemaining() && channel.read(buffer, buffer.position()) > 0) {   }
        return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
    }

    private static void write(RandomAccessFile raf, Holder h) throws IOException {
        byte[] data = format(h).getBytes(StandardCharsets.UTF_8);
        FileChannel channel = raf.getChannel();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long at = 0;
        while (buffer.hasRemaining()) at += channel.write(buffer, at);
        channel.truncate(data.length);
        channel.force(false);
    }

    private static void startBeat(Held h) {
        if (beats == null) {
            beats = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "d2-folder-lock");
                t.setDaemon(true);
                return t;
            });
        }
        h.beat = () -> {
            synchronized (D2FolderLock.class) {
                if (held.get(h.file.getPath()) != h) return;
                try {
                    write(h.raf, new Holder(h.me.pkg, h.me.label, h.me.since, System.currentTimeMillis()));
                }
                catch (IOException e) {
                    D2Trace.log("folder lock: heartbeat failed (" + e + ")");
                }
            }
            synchronized (D2FolderLock.class) {
                if (held.get(h.file.getPath()) == h) beats.schedule(h.beat, BEAT_MS, TimeUnit.MILLISECONDS);
            }
        };
        beats.schedule(h.beat, BEAT_MS, TimeUnit.MILLISECONDS);
    }
}
