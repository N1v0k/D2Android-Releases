package com.winlator.d2;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.ProcessHelper;
import com.winlator.xenvironment.EnvironmentComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class D2GuestComponent extends EnvironmentComponent {
    private final File rootDir;
    private final String guestCommand;
    private final EnvVars envVars;
    private final Callback<Integer> terminationCallback;
    private final String stdinText;
    private static int pid = -1;
    private static final Object lock = new Object();
    private String stopReason;
    private static String[] runReason;
    private static volatile String endReason;
    private static List<Integer> stoppedPids;
    private static List<String> stoppedNames;
    private static long stoppedAt, resumedAt;
    private static long stoppedUptime;
    private static int interruptionsAtPause;
    private static volatile Watch watch;
    private final File outputFile;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkOutput = new Runnable() {
        @Override
        public void run() {
            limitOutput("running");
            if (running()) handler.postDelayed(this, D2GuestOutput.CHECK_MS);
        }
    };
    private static volatile long endSinceResumeMs = -1;

    public D2GuestComponent(File rootDir, String guestCommand, EnvVars envVars, Callback<Integer> terminationCallback) {
        this(rootDir, guestCommand, envVars, terminationCallback, null);
    }

    public D2GuestComponent(File rootDir, String guestCommand, EnvVars envVars, Callback<Integer> terminationCallback, String stdinText) {
        this(rootDir, guestCommand, envVars, terminationCallback, stdinText, null);
    }

    public D2GuestComponent(File rootDir, String guestCommand, EnvVars envVars, Callback<Integer> terminationCallback, String stdinText, File outputFile) {
        this.outputFile = outputFile;
        this.rootDir = rootDir;
        this.guestCommand = guestCommand;
        this.envVars = envVars;
        this.terminationCallback = terminationCallback;
        this.stdinText = stdinText;
    }

    @Override
    public void start() {
        synchronized (lock) {
            kill(D2ExitText.BY_NEW_RUN);
            new File(rootDir, "tmp/shm").mkdirs();
            final String[] reason = new String[1];
            runReason = reason;
            stoppedPids = null;
            resumedAt = 0;
            watch = null;
            pid = ProcessHelper.exec(rootDir + "/usr/local/bin/box64 " + guestCommand, envVars, rootDir, (status) -> {
                synchronized (lock) {
                    pid = -1;
                    endReason = reason[0];
                    endSinceResumeMs = resumedAt != 0 ? SystemClock.elapsedRealtime() - resumedAt : -1;
                }
                Watch w = watch;
                if (w != null) w.gameEnded();
                if (terminationCallback != null) terminationCallback.call(status);
            }, stdinText, outputFile);
            if (outputFile != null) handler.postDelayed(checkOutput, D2GuestOutput.CHECK_MS);
        }
    }

    private void limitOutput(String when) {
        if (outputFile != null && D2GuestOutput.limit(outputFile, D2GuestOutput.MAX_BYTES, when)) {
            D2Trace.log("guest output: " + D2GuestOutput.FILE + " over " + D2GuestOutput.MAX_BYTES + " bytes, emptied (" + when + ")");
        }
    }

    @Override
    public void stop() {
        handler.removeCallbacks(checkOutput);
        synchronized (lock) {
            String reason = stopReason != null ? stopReason : D2ExitText.BY_UNSAID;
            stopReason = null;
            kill(reason);
        }
    }

    public void setStopReason(String reason) {
        synchronized (lock) {
            stopReason = reason;
        }
    }

    public static boolean guestsStopped() {
        synchronized (lock) {
            return stoppedPids != null;
        }
    }

    public static boolean running() {
        synchronized (lock) {
            return pid != -1;
        }
    }

    public static String endReason() {
        return endReason;
    }

    public static long endSinceResumeMs() {
        return endSinceResumeMs;
    }

    private static void kill(String reason) {
        if (pid != -1) {
            if (runReason != null) runReason[0] = reason;
            Process.killProcess(pid);
            pid = -1;
        }
    }

    @Override
    public void onPause() {
        synchronized (lock) {
            if (pid == -1) return;
            D2GuestList.Result found = guestList();
            List<ProcessHelper.PStat> processes = found.processes;
            int guests = 0, stopped = 0;
            boolean note = stoppedPids == null;
            if (note) {
                stoppedPids = new ArrayList<>();
                stoppedNames = new ArrayList<>();
                stoppedAt = SystemClock.elapsedRealtime();
                stoppedUptime = SystemClock.uptimeMillis();
                interruptionsAtPause = com.winlator.xconnector.XConnectorEpoll.interruptionCount();
            }
            for (int i = processes.size() - 1; i >= 0; i--) {
                ProcessHelper.PStat process = processes.get(i);
                if (process.guestProcess) guests++;
                if (process.guestProcess && note && process.state != ProcessHelper.PState.ZOMBIE
                    && process.state != ProcessHelper.PState.DEAD) {
                    stoppedPids.add(process.pid);
                    stoppedNames.add(D2ExitText.shortName(process.name));
                }
                if (process.guestProcess && process.state != ProcessHelper.PState.STOPPED) {
                    ProcessHelper.suspendProcess(process.pid);
                    stopped++;
                }
            }
            D2Trace.log("guest pause: " + stopped + " of " + guests + " guest processes stopped (" + processes.size() + " listed"
                + (found.onlyByRuntime > 0 ? ", " + found.onlyByRuntime + " only by the runtime check" : "") + ")");
        }
        limitOutput("pause");
    }

    @Override
    public void onResume() {
        synchronized (lock) {
            if (pid == -1) return;
            D2GuestList.Result found = guestList();
            List<ProcessHelper.PStat> processes = found.processes;
            long sleepMs = stoppedPids != null ? D2GuestWatch.deepSleepMs(SystemClock.elapsedRealtime() - stoppedAt,
                SystemClock.uptimeMillis() - stoppedUptime) : -1;
            long stoppedMs = stoppedPids != null ? SystemClock.elapsedRealtime() - stoppedAt : -1;
            int interruptions = com.winlator.xconnector.XConnectorEpoll.interruptionCount();
            int interruptionsInPause = stoppedPids != null ? interruptions - interruptionsAtPause : -1;
            noteMissingGuests(processes, sleepMs);
            noteStatus(processes, stoppedMs);
            int guests = 0, continued = 0;
            Watch w = new Watch();
            long first = 0;
            List<String> conts = new ArrayList<>();
            for (ProcessHelper.PStat process : processes) {
                if (process.guestProcess) guests++;
                if (process.guestProcess && process.state == ProcessHelper.PState.STOPPED) {
                    String comm = D2GuestWatch.comm(PROC, process.pid);
                    long now = System.nanoTime();
                    ProcessHelper.resumeProcess(process.pid);
                    if (continued == 0) first = now;
                    String name = D2ExitText.shortName(process.name);
                    conts.add(D2GuestWatch.contLine(process.pid, name, now - first));
                    w.add(process.pid, name, comm);
                    continued++;
                }
            }
            if (continued > 0) w.begin(first);
            resumedAt = SystemClock.elapsedRealtime();
            D2Trace.log("guest resume: " + continued + " of " + guests + " guest processes continued (" + processes.size() + " listed"
                + (found.onlyByRuntime > 0 ? ", " + found.onlyByRuntime + " only by the runtime check" : "") + ")"
                + (sleepMs >= 0 ? ", deep sleep " + sleepMs / 1000 + " s of " + stoppedMs / 1000 + " s" : "")
                + ", " + (interruptionsInPause >= 0 ? interruptionsInPause + " connector interruptions survived in the pause, " : "")
                + interruptions + " since the start");
            for (String line : conts) D2Trace.log(line);
        }
        limitOutput("resume");
    }

    private static final File PROC = new File("/proc");

    private D2GuestList.Result guestList() {
        return D2GuestList.find(PROC, rootDir, Process.myUid(), Process.myPid());
    }

    private static void noteStatus(List<ProcessHelper.PStat> processes, long stoppedMs) {
        List<String> unusual = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        int read = 0;
        for (ProcessHelper.PStat process : processes) {
            if (!process.guestProcess || process.state == ProcessHelper.PState.ZOMBIE || process.state == ProcessHelper.PState.DEAD) continue;
            if (stoppedMs < 0 && process.state != ProcessHelper.PState.STOPPED) continue;
            String name = D2ExitText.shortName(process.name);
            D2GuestWatch.Status status = D2GuestWatch.read(PROC, process.pid, D2GuestWatch.WINESERVER.equals(name));
            read++;
            lines.add("status " + D2GuestWatch.traceLine(process.pid, name, status));
            if (status != null && status.unusual()) unusual.add(D2GuestWatch.unusualText(process.pid, name, status));
        }
        String line = D2GuestWatch.unusualLine(unusual);
        if (line != null) {
            D2ExitLog.note(line);
            D2Trace.log(line);
        }
        if (line != null || stoppedMs >= D2ExitText.RESUME_WORTH_NOTING_MS) for (String l : lines) D2Trace.log(l);
        else if (read > 0) D2Trace.log("guest status before resume: " + read + " read, all stopped, no signal pending");
    }

    private static final class Watch implements Runnable {
        private final List<Integer> pids = new ArrayList<>();
        private final List<String> names = new ArrayList<>(), comms = new ArrayList<>();
        private final D2GuestWatch.StatReader reader = new D2GuestWatch.StatReader();
        private final List<D2GuestWatch.Gone> gone = new ArrayList<>();
        private long firstNanos;
        private boolean reported;
        private Thread thread;

        void add(int pid, String name, String comm) {
            pids.add(pid);
            names.add(name);
            comms.add(comm);
        }

        void begin(long firstNanos) {
            this.firstNanos = firstNanos;
            watch = this;
            thread = new Thread(this, "guest-watch");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void run() {
            int count = pids.size();
            boolean[] ended = new boolean[count];
            String[] paths = new String[count];
            byte[][] commBytes = new byte[count][];
            for (int i = 0; i < count; i++) {
                paths[i] = "/proc/" + pids.get(i) + "/stat";
                commBytes[i] = comms.get(i) != null ? D2GuestWatch.latin1(comms.get(i)) : null;
            }
            int left = count, round = 0;
            while (left > 0 && elapsedMs() < D2GuestWatch.WATCH_MS) {
                round++;
                long roundNanos = System.nanoTime() - firstNanos;
                for (int i = 0; i < count; i++) {
                    if (ended[i]) continue;
                    char state = reader.state(paths[i], commBytes[i]);
                    if (state == 0 || state == 'Z' || state == 'X') {
                        ended[i] = true;
                        left--;
                        synchronized (this) {
                            gone.add(new D2GuestWatch.Gone(names.get(i), pids.get(i), round, roundNanos, state != 0));
                        }
                    }
                }
                try {
                    Thread.sleep(D2GuestWatch.POLL_MS);
                }
                catch (InterruptedException e) {
                    break;
                }
            }
            report(false);
            if (watch == this) watch = null;
        }

        void gameEnded() {
            try {
                long left = Math.min(D2GuestWatch.END_WAIT_MS, D2GuestWatch.WATCH_MS - elapsedMs());
                if (left > 0) thread.join(left);
            }
            catch (InterruptedException ignored) {}
            report(true);
        }

        private void report(boolean gameEnded) {
            String line;
            boolean any;
            synchronized (this) {
                if (reported) return;
                reported = true;
                any = !gone.isEmpty();
                line = D2GuestWatch.goneLine(new ArrayList<>(gone), pids.size(), elapsedMs(), gameEnded);
            }
            D2Trace.log(line);
            if (gameEnded || any) D2ExitLog.note(line);
        }

        private long elapsedMs() {
            return (System.nanoTime() - firstNanos) / 1_000_000;
        }
    }

    private static void noteMissingGuests(List<ProcessHelper.PStat> processes, long sleepMs) {
        if (stoppedPids == null) return;
        long stoppedMs = SystemClock.elapsedRealtime() - stoppedAt;
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < stoppedPids.size(); i++) {
            int stoppedPid = stoppedPids.get(i);
            String name = stoppedNames.get(i);
            ProcessHelper.PStat now = null;
            for (ProcessHelper.PStat process : processes) if (process.pid == stoppedPid) now = process;
            boolean gone = now == null || (!name.equals(D2ExitText.shortName(now.name)) && now.state != ProcessHelper.PState.ZOMBIE);
            if (gone) missing.add(D2ExitText.guest(name, stoppedPid, -1));
            else if (now.state == ProcessHelper.PState.ZOMBIE || now.state == ProcessHelper.PState.DEAD) {
                missing.add(D2ExitText.guest(name, stoppedPid, zombieStatus(stoppedPid)));
            }
        }
        String line = D2ExitText.resumed(stoppedMs, sleepMs, stoppedPids.size(), missing);
        if (line != null) {
            D2ExitLog.note(line);
            D2Trace.log(line);
        }
        stoppedPids = null;
    }

    private static int zombieStatus(int pid) {
        try {
            String stat;
            try (BufferedReader in = new BufferedReader(new FileReader("/proc/" + pid + "/stat"))) {
                stat = in.readLine();
            }
            String[] fields = stat.substring(stat.lastIndexOf(')') + 2).trim().split(" ");
            return fields.length > 49 ? Integer.parseInt(fields[49]) : -1;
        }
        catch (Exception e) {
            return -1;
        }
    }
}
