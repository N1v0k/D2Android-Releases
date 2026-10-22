package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class D2MpqAttributes {
    private static final int HASH_OFFSET = 0, HASH_NAME_A = 1, HASH_NAME_B = 2, HASH_FILE_KEY = 3;
    static final int BLOCK_EMPTY = 0xFFFFFFFF, BLOCK_DELETED = 0xFFFFFFFE;
    private static final int[] TABLE = cryptTable();

    public enum Result { HIDDEN, NOT_PRESENT }

    private D2MpqAttributes() {}

    public static Result hide(File archive) throws IOException {
        return scan(archive, true);
    }

    public static boolean present(File archive) throws IOException {
        return scan(archive, false) == Result.HIDDEN;
    }

    private static Result scan(File archive, boolean write) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(archive, write ? "rw" : "r")) {
            byte[] head = new byte[32];
            f.readFully(head);
            ByteBuffer h = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
            if (h.getInt(0) != 0x1A51504D) throw new IOException("no MPQ header at offset 0: " + archive.getName());
            long hashPos = h.getInt(16) & 0xFFFFFFFFL;
            int hashCount = h.getInt(24);
            if (hashCount <= 0 || (hashCount & (hashCount - 1)) != 0 || hashCount > (1 << 20) || hashPos + hashCount * 16L > f.length()) {
                throw new IOException("implausible hash table: " + archive.getName());
            }
            byte[] raw = new byte[hashCount * 16];
            f.seek(hashPos);
            f.readFully(raw);
            int[] words = new int[hashCount * 4];
            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(words);
            int key = hash("(hash table)", HASH_FILE_KEY);
            int[] table = crypt(words, key, true);

            int name1 = hash("(attributes)", HASH_NAME_A), name2 = hash("(attributes)", HASH_NAME_B);
            int start = hash("(attributes)", HASH_OFFSET) & (hashCount - 1);
            int hit = -1;
            for (int i = start; ; ) {
                int block = table[i * 4 + 3];
                if (block == BLOCK_EMPTY) break;
                if (table[i * 4] == name1 && table[i * 4 + 1] == name2 && block != BLOCK_DELETED) {
                    hit = i;
                    break;
                }
                i = (i + 1) & (hashCount - 1);
                if (i == start) break;
            }
            if (hit < 0) return Result.NOT_PRESENT;
            if (!write) return Result.HIDDEN;
            table[hit * 4 + 3] = BLOCK_DELETED;
            ByteBuffer out = ByteBuffer.allocate(raw.length).order(ByteOrder.LITTLE_ENDIAN);
            out.asIntBuffer().put(crypt(table, key, false));
            f.seek(hashPos);
            f.write(out.array());
            return Result.HIDDEN;
        }
    }

    public static int hash(String s, int kind) {
        int seed1 = 0x7FED7FED, seed2 = 0xEEEEEEEE;
        for (byte b : s.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)) {
            int ch = b & 0xFF;
            seed1 = TABLE[kind * 0x100 + ch] ^ (seed1 + seed2);
            seed2 = ch + seed1 + seed2 + (seed2 << 5) + 3;
        }
        return seed1;
    }

    public static int[] crypt(int[] words, int key, boolean decrypt) {
        int seed = 0xEEEEEEEE;
        int[] out = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            seed += TABLE[0x400 + (key & 0xFF)];
            int w = words[i];
            int ch = w ^ (key + seed);
            out[i] = ch;
            int plain = decrypt ? ch : w;
            key = ((~key << 0x15) + 0x11111111) | (key >>> 0x0B);
            seed = plain + seed + (seed << 5) + 3;
        }
        return out;
    }

    private static int[] cryptTable() {
        int[] t = new int[0x500];
        long seed = 0x00100001;
        for (int i = 0; i < 0x100; i++) {
            int idx = i;
            for (int j = 0; j < 5; j++) {
                seed = (seed * 125 + 3) % 0x2AAAAB;
                int a = (int) ((seed & 0xFFFF) << 16);
                seed = (seed * 125 + 3) % 0x2AAAAB;
                int b = (int) (seed & 0xFFFF);
                t[idx] = a | b;
                idx += 0x100;
            }
        }
        return t;
    }
}
