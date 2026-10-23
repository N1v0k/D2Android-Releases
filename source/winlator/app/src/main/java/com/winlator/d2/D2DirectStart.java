package com.winlator.d2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;










public final class D2DirectStart {

    public static final String MARKER = "d2android-direct";

    public static final int PKT_DIRECT = 15;

    public static final String EXTRA = "d2_direct";


    public static final int RUNNING = 1, IN_GAME = 2, STOPPED = 3;

    public static final int LEFT = 4;

    public static final int REASON_MARKER = 1, REASON_VERSION = 2, REASON_NOT_LISTED = 3, REASON_DIFFICULTY = 4,
        REASON_NO_PROGRESS = 5, REASON_WINDOW = 6, REASON_NOT_PLAYABLE = 7;

    private D2DirectStart() {}


    public static final class Request {
        public final String file;
        public final int difficulty;

        public Request(String file, int difficulty) {
            this.file = file;
            this.difficulty = difficulty;
        }


        public static final Request NEW = new Request(":neu", 0);

        public boolean isNew() {
            return NEW.file.equals(file);
        }


        public static Request parse(String extra) {
            if (extra == null) return null;
            if (NEW.toExtra().equals(extra)) return NEW;
            int colon = extra.lastIndexOf(':');
            if (colon < 0 || colon != extra.length() - 2) return null;
            String file = extra.substring(0, colon);
            char d = extra.charAt(colon + 1);
            if (d < '0' || d > '2' || !validFile(file)) return null;
            return new Request(file, d - '0');
        }

        public String toExtra() {
            return file + ":" + difficulty;
        }

        String markerText() {
            return file + "\n" + difficulty + "\n";
        }
    }


    static boolean validFile(String file) {
        if (file == null || file.length() < 2 || file.length() > 15) return false;
        for (int i = 0; i < file.length(); i++) {
            char c = file.charAt(i);
            if (c < 0x20 || c == '\\' || c == '/' || c == ':' || c == '.' || c > 0xFF) return false;
        }
        return true;
    }


    public static void apply(File startDir, Request request) throws IOException {
        File marker = new File(startDir, MARKER);
        if (request == null) {
            if (marker.exists() && !marker.delete()) throw new IOException("cannot delete " + marker);
            return;
        }
        try (OutputStream out = new FileOutputStream(marker)) {
            out.write(request.markerText().getBytes(StandardCharsets.ISO_8859_1));
        }
    }


    public static final class Report {
        public final int state, step, reason;

        Report(int state, int step, int reason) {
            this.state = state;
            this.step = step;
            this.reason = reason;
        }


        public static Report parse(byte[] data, int length) {
            if (data == null || length < 11 || data.length < 11) return null;
            if (data[0] != 'D' || data[1] != '2' || data[2] != 'A' || data[3] != 'M' || (data[5] & 0xFF) != PKT_DIRECT) return null;
            return new Report(data[8] & 0xFF, data[9] & 0xFF, data[10] & 0xFF);
        }

        @Override
        public String toString() {
            return "state " + state + " step " + step + " reason " + reason;
        }
    }


    public enum Outcome {

        WAIT,

        IN_GAME,

        STOPPED,

        NO_REPORT,

        TOO_LONG,

        CANCELLED
    }








    public static final class Watch {
        public static final long NO_REPORT_MS = 10_000, NO_MOD_MS = 90_000, LIMIT_MS = 180_000;

        static final long MAX_STEP_MS = 1000;
        private long shownMs, modMs;
        private boolean reported;





        public Outcome update(Report report, boolean gameState, boolean modAlive, long frontMs) {
            long step = frontMs > 0 ? Math.min(frontMs, MAX_STEP_MS) : 0;
            shownMs += step;
            if (modAlive) modMs += step;
            if (gameState) return Outcome.IN_GAME;
            if (report != null) {
                reported = true;
                if (report.state == IN_GAME) return Outcome.IN_GAME;
                if (report.state == STOPPED) return Outcome.STOPPED;
            }
            if (!reported && (modMs >= NO_REPORT_MS || shownMs >= NO_MOD_MS)) return Outcome.NO_REPORT;
            if (shownMs >= LIMIT_MS) return Outcome.TOO_LONG;
            return Outcome.WAIT;
        }

        long shownMs() {
            return shownMs;
        }
    }
}
