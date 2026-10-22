package com.winlator.d2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class D2SaveHeader {
    static final int SIGNATURE = 0xAA55AA55, VERSION = 0x60, STATUS_AT = 36, HARDCORE = 0x04, HEAD = 44;
    static final int NAME_AT = 20, NAME_LEN = 16, PROGRESS_AT = 37, CLASS_AT = 40, LEVEL_AT = 43, LAST_AT = 168,
        FULL_HEAD = 171, DIED = 0x08, EXPANSION = 0x20;
    enum Refusal { NONE, DEAD_HARDCORE, NAME_NOT_FILE }

    private D2SaveHeader() {}

    static Boolean hardcore(byte[] head, int length) {
        if (head == null || length < HEAD || head.length < HEAD) return null;
        if (u32(head, 0) != SIGNATURE || u32(head, 4) != VERSION) return null;
        return (head[STATUS_AT] & HARDCORE) != 0;
    }

    private static int u32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    static File characterFile(File dir, String name) {
        if (dir == null || name == null || name.isEmpty()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) if (f.isFile() && f.getName().equalsIgnoreCase(name + ".d2s")) return f;
        return null;
    }

    static Boolean hardcore(File dir, String name) {
        File file = characterFile(dir, name);
        if (file == null) return null;
        byte[] head = new byte[HEAD];
        int n = 0;
        try (InputStream in = new FileInputStream(file)) {
            for (int r; n < HEAD && (r = in.read(head, n, HEAD - n)) > 0; ) n += r;
        }
        catch (IOException e) {
            return null;
        }
        return hardcore(head, n);
    }

    static final class Info {
        final String file;
        final String name;
        final int status, progress, characterClass, level;
        final int lastDifficulty;
        final int lastAct;
        final long modified;

        Info(String file, String name, int status, int progress, int characterClass, int level, int lastDifficulty, long modified) {
            this(file, name, status, progress, characterClass, level, lastDifficulty, 0, modified);
        }

        Info(String file, String name, int status, int progress, int characterClass, int level, int lastDifficulty, int lastAct,
             long modified) {
            this.file = file;
            this.name = name;
            this.status = status;
            this.progress = progress;
            this.characterClass = characterClass;
            this.level = level;
            this.lastDifficulty = lastDifficulty;
            this.lastAct = Math.max(0, Math.min(4, lastAct));
            this.modified = modified;
        }

        boolean hardcore() { return (status & HARDCORE) != 0; }
        boolean expansion() { return (status & EXPANSION) != 0; }
        boolean deadHardcore() { return (status & (HARDCORE | DIED)) == (HARDCORE | DIED); }

        int openDifficulty() {
            return Math.min(2, (progress & 0x1F) / (expansion() ? 5 : 4));
        }

        int defaultDifficulty() {
            return lastDifficulty >= 0 && lastDifficulty <= openDifficulty() ? lastDifficulty : 0;
        }

        Refusal refusal() {
            if (deadHardcore()) return Refusal.DEAD_HARDCORE;
            if (!name.equals(file) || !D2DirectStart.validFile(file)) return Refusal.NAME_NOT_FILE;
            return Refusal.NONE;
        }
    }

    static Info parse(byte[] head, int length, String file, long modified) {
        if (head == null || length < FULL_HEAD || head.length < FULL_HEAD) return null;
        if (u32(head, 0) != SIGNATURE || u32(head, 4) != VERSION) return null;
        int end = NAME_AT;
        while (end < NAME_AT + NAME_LEN && head[end] != 0) end++;
        String name = new String(head, NAME_AT, end - NAME_AT, StandardCharsets.ISO_8859_1);
        int last = -1, act = 0;
        for (int d = 0; d < 3; d++) if ((head[LAST_AT + d] & 0x80) != 0) {
            last = d;
            act = head[LAST_AT + d] & 0x07;
        }
        return new Info(file, name, head[STATUS_AT] & 0xFF, head[PROGRESS_AT] & 0xFF, head[CLASS_AT] & 0xFF,
            head[LEVEL_AT] & 0xFF, last, act, modified);
    }

    static Info read(File file) {
        String n = file.getName();
        if (!n.toLowerCase(java.util.Locale.ROOT).endsWith(".d2s")) return null;
        byte[] head = new byte[FULL_HEAD];
        int got = 0;
        try (InputStream in = new FileInputStream(file)) {
            for (int r; got < FULL_HEAD && (r = in.read(head, got, FULL_HEAD - got)) > 0; ) got += r;
        }
        catch (IOException e) {
            return null;
        }
        return parse(head, got, n.substring(0, n.length() - 4), file.lastModified());
    }

    static List<Info> list(File dir) {
        List<Info> out = new ArrayList<>();
        File[] files = dir != null ? dir.listFiles() : null;
        if (files == null) return out;
        for (File f : files) {
            Info info = f.isFile() ? read(f) : null;
            if (info != null) out.add(info);
        }
        Collections.sort(out, (a, b) -> Long.compare(b.modified, a.modified));
        return out;
    }
}
