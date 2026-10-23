package com.winlator.d2;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;







public final class D2Mpq implements Closeable {
    private static final int FLAG_IMPLODE = 0x00000100, FLAG_COMPRESS = 0x00000200, FLAG_ENCRYPTED = 0x00010000,
        FLAG_FIX_KEY = 0x00020000, FLAG_SINGLE_UNIT = 0x01000000, FLAG_EXISTS = 0x80000000;
    private static final int[] CRYPT = new int[0x500];

    static {
        int seed = 0x00100001;
        for (int i = 0; i < 0x100; i++) {
            for (int j = i, k = 0; k < 5; k++, j += 0x100) {
                seed = (seed * 125 + 3) % 0x2AAAAB;
                int high = (seed & 0xFFFF) << 16;
                seed = (seed * 125 + 3) % 0x2AAAAB;
                CRYPT[j] = high | (seed & 0xFFFF);
            }
        }
    }

    private final RandomAccessFile file;

    private static final int MAX_FILE = 128 << 20;
    private final long base;
    private final int sectorSize;
    private final int[] hashTable, blockTable;

    public D2Mpq(File path) throws IOException {
        file = new RandomAccessFile(path, "r");
        long start = -1;
        for (long at = 0; at < Math.min(file.length(), 1 << 20); at += 512) {
            file.seek(at);
            if (readIntLE() == 0x1A51504D) {
                start = at;
                break;
            }
        }
        if (start < 0) {
            file.close();
            throw new IOException("no MPQ: " + path);
        }
        base = start;
        byte[] header = read(base, 32);
        sectorSize = 512 << (u16(header, 14));
        int hashPos = s32(header, 16), blockPos = s32(header, 20), hashCount = s32(header, 24), blockCount = s32(header, 28);
        hashTable = readTable(hashPos, hashCount, hash("(hash table)", 3));
        blockTable = readTable(blockPos, blockCount, hash("(block table)", 3));
    }


    public synchronized byte[] read(String name) throws IOException {
        int block = find(name);
        if (block < 0) return null;
        int offset = blockTable[block * 4], packed = blockTable[block * 4 + 1], size = blockTable[block * 4 + 2], flags = blockTable[block * 4 + 3];
        if ((flags & FLAG_EXISTS) == 0) return null;
        int key = 0;
        if ((flags & FLAG_ENCRYPTED) != 0) {
            String plain = name.substring(name.lastIndexOf('\\') + 1);
            key = hash(plain, 3);
            if ((flags & FLAG_FIX_KEY) != 0) key = (key + offset) ^ size;
        }

        if (size < 0 || size > MAX_FILE) throw new IOException("bad file size " + size);
        byte[] out = new byte[size];
        boolean compressed = (flags & (FLAG_IMPLODE | FLAG_COMPRESS)) != 0;
        if ((flags & FLAG_SINGLE_UNIT) != 0) {
            byte[] data = read(base + (offset & 0xFFFFFFFFL), packed);
            if ((flags & FLAG_ENCRYPTED) != 0) decrypt(data, data.length, key);
            unpack(data, data.length, out, 0, size, flags);
            return out;
        }
        int sectors = (size + sectorSize - 1) / sectorSize;
        int[] starts;
        if (compressed) {
            byte[] table = read(base + (offset & 0xFFFFFFFFL), (sectors + 1) * 4);
            if ((flags & FLAG_ENCRYPTED) != 0) decrypt(table, table.length, key - 1);
            starts = new int[sectors + 1];
            for (int i = 0; i <= sectors; i++) starts[i] = s32(table, i * 4);
        }
        else {
            starts = new int[sectors + 1];
            for (int i = 0; i <= sectors; i++) starts[i] = Math.min(i * sectorSize, size);
        }
        for (int i = 0; i < sectors; i++) {
            int want = Math.min(sectorSize, size - i * sectorSize);
            int length = starts[i + 1] - starts[i];

            if (length <= 0 || length > want) throw new IOException("bad sector table");
            byte[] data = read(base + (offset & 0xFFFFFFFFL) + starts[i], length);
            if ((flags & FLAG_ENCRYPTED) != 0) decrypt(data, length, key + i);
            if (compressed && length < want) unpack(data, length, out, i * sectorSize, want, flags);
            else System.arraycopy(data, 0, out, i * sectorSize, want);
        }
        return out;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    private int find(String name) {
        int count = hashTable.length / 4;
        int index = hash(name, 0) & (count - 1), a = hash(name, 1), b = hash(name, 2);
        for (int n = 0; n < count; n++) {
            int at = ((index + n) & (count - 1)) * 4;
            int block = hashTable[at + 3];
            if (block == -1) return -1;
            if (hashTable[at] == a && hashTable[at + 1] == b && block != -2) return block;
        }
        return -1;
    }

    private static void unpack(byte[] data, int length, byte[] out, int at, int size, int flags) throws IOException {
        if ((flags & FLAG_IMPLODE) != 0) {
            explode(data, 0, length, out, at, size);
            return;
        }
        int mask = data[0] & 0xFF;
        if (mask == 0x08) explode(data, 1, length, out, at, size);
        else if (mask == 0x02) {
            Inflater inflater = new Inflater();
            inflater.setInput(data, 1, length - 1);
            try {
                inflater.inflate(out, at, size);
            }
            catch (DataFormatException e) {
                throw new IOException(e);
            }
            finally {
                inflater.end();
            }
        }
        else if (mask != 0 && (mask & ~(0x01 | 0x40 | 0x80)) == 0 && (mask & 0xC0) != 0xC0) {

            byte[] in = data;
            int from = 1, to = length;
            if ((mask & 0x01) != 0) {
                in = D2StormAudio.huffman(data, 1, length, size);
                from = 0;
                to = in.length;
                if ((mask & 0xC0) == 0) {
                    if (to != size) throw new IOException("Huffman sector " + to + " of " + size + " bytes");
                    System.arraycopy(in, 0, out, at, size);
                    return;
                }
            }
            int got = D2StormAudio.adpcm(in, from, to, out, at, size, (mask & 0x80) != 0 ? 2 : 1);
            if (got != size) throw new IOException("ADPCM sector " + got + " of " + size + " bytes");
        }
        else throw new IOException(String.format(Locale.ROOT, "compression 0x%02X not supported", mask));
    }

    private int[] readTable(int position, int count, int key) throws IOException {
        byte[] raw = read(base + (position & 0xFFFFFFFFL), count * 16);
        decrypt(raw, raw.length, key);
        int[] table = new int[count * 4];
        for (int i = 0; i < table.length; i++) table[i] = s32(raw, i * 4);
        return table;
    }

    private byte[] read(long position, int length) throws IOException {
        if (length < 0 || position < 0 || position + length > file.length()) throw new IOException("read beyond the archive");
        byte[] data = new byte[length];
        file.seek(position);
        file.readFully(data);
        return data;
    }

    private int readIntLE() throws IOException {
        byte[] b = new byte[4];
        file.readFully(b);
        return s32(b, 0);
    }

    static int hash(String text, int type) {
        int seed1 = 0x7FED7FED, seed2 = 0xEEEEEEEE;
        for (int i = 0; i < text.length(); i++) {
            int ch = Character.toUpperCase(text.charAt(i));
            if (ch == '/') ch = '\\';
            seed1 = CRYPT[(type << 8) + (ch & 0xFF)] ^ (seed1 + seed2);
            seed2 = ch + seed1 + seed2 + (seed2 << 5) + 3;
        }
        return seed1;
    }

    private static void decrypt(byte[] data, int length, int key) {
        int seed = 0xEEEEEEEE;
        for (int i = 0; i + 4 <= length; i += 4) {
            seed += CRYPT[0x400 + (key & 0xFF)];
            int value = s32(data, i) ^ (key + seed);
            key = ((~key << 21) + 0x11111111) | (key >>> 11);
            seed = value + seed + (seed << 5) + 3;
            data[i] = (byte) value;
            data[i + 1] = (byte) (value >>> 8);
            data[i + 2] = (byte) (value >>> 16);
            data[i + 3] = (byte) (value >>> 24);
        }
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }


    // A port of blast.c by Mark Adler (zlib license, zlib/contrib/blast).


    private static final class Huffman {
        final short[] count = new short[14], symbol;

        Huffman(int symbols, int[] rep) {
            symbol = new short[symbols];
            short[] length = new short[256];
            int n = 0;
            for (int r : rep) {
                int left = (r >> 4) + 1, len = r & 15;
                while (left-- > 0) length[n++] = (short) len;
            }
            for (int s = 0; s < n; s++) count[length[s]]++;
            short[] offs = new short[14];
            for (int len = 1; len < 13; len++) offs[len + 1] = (short) (offs[len] + count[len]);
            for (int s = 0; s < n; s++) if (length[s] != 0) symbol[offs[length[s]]++] = (short) s;
        }
    }

    private static final Huffman LIT = new Huffman(256, new int[]{
        11, 124, 8, 7, 28, 7, 188, 13, 76, 4, 10, 8, 12, 10, 12, 10, 8, 23, 8,
        9, 7, 6, 7, 8, 7, 6, 55, 8, 23, 24, 12, 11, 7, 9, 11, 12, 6, 7, 22, 5,
        7, 24, 6, 11, 9, 6, 7, 22, 7, 11, 38, 7, 9, 8, 25, 11, 8, 11, 9, 12,
        8, 12, 5, 38, 5, 38, 5, 11, 7, 5, 6, 21, 6, 10, 53, 8, 7, 24, 10, 27,
        44, 253, 253, 253, 252, 252, 252, 13, 12, 45, 12, 45, 12, 61, 12, 45,
        44, 173});
    private static final Huffman LEN = new Huffman(16, new int[]{2, 35, 36, 53, 38, 23});
    private static final Huffman DIST = new Huffman(64, new int[]{2, 20, 53, 230, 247, 151, 248});
    private static final short[] BASE = {3, 2, 4, 5, 6, 7, 8, 9, 10, 12, 16, 24, 40, 72, 136, 264};
    private static final byte[] EXTRA = {0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8};

    private static final class Bits {
        final byte[] in;
        int pos;
        final int end;
        int buf, cnt;

        Bits(byte[] in, int pos, int end) {
            this.in = in;
            this.pos = pos;
            this.end = end;
        }

        int bits(int need) throws IOException {
            int val = buf;
            while (cnt < need) {
                if (pos >= end) throw new IOException("explode: out of input");
                val |= (in[pos++] & 0xFF) << cnt;
                cnt += 8;
            }
            buf = val >> need;
            cnt -= need;
            return val & ((1 << need) - 1);
        }

        int decode(Huffman h) throws IOException {
            int code = 0, first = 0, index = 0, len = 1, next = 1;
            int bitbuf = buf, left = cnt;
            while (true) {
                while (left-- > 0) {
                    code |= (bitbuf & 1) ^ 1;
                    bitbuf >>= 1;
                    int count = h.count[next++];
                    if (code < first + count) {
                        buf = bitbuf;
                        cnt = (cnt - len) & 7;
                        return h.symbol[index + (code - first)];
                    }
                    index += count;
                    first += count;
                    first <<= 1;
                    code <<= 1;
                    len++;
                }
                left = 14 - len;
                if (left == 0) break;
                if (pos >= end) throw new IOException("explode: out of input");
                bitbuf = in[pos++] & 0xFF;
                if (left > 8) left = 8;
            }
            throw new IOException("explode: bad code");
        }
    }

    static void explode(byte[] in, int pos, int end, byte[] out, int at, int size) throws IOException {
        Bits s = new Bits(in, pos, end);
        int lit = s.bits(8), dict = s.bits(8);
        if (lit > 1 || dict < 4 || dict > 6) throw new IOException("explode: bad header");
        int next = at, limit = at + size;
        while (next < limit) {
            if (s.bits(1) != 0) {
                int symbol = s.decode(LEN);
                int len = BASE[symbol] + s.bits(EXTRA[symbol]);
                if (len == 519) break;
                int shift = len == 2 ? 2 : dict;
                int dist = (s.decode(DIST) << shift) + s.bits(shift) + 1;
                if (dist > next - at) throw new IOException("explode: distance too far back");
                for (int i = 0; i < len && next < limit; i++, next++) out[next] = out[next - dist];
            }
            else out[next++] = (byte) (lit != 0 ? s.decode(LIT) : s.bits(8));
        }
    }
}
