package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;











public final class D2InstallManifest {


    static final List<String> CHECKED = Collections.unmodifiableList(Arrays.asList("Installer.exe", "Installer Tome.mpq"));


    static final int TAIL_BYTES = 4 * 1024 * 1024;
    private static final byte[] START = "d8:announce".getBytes(StandardCharsets.US_ASCII);


    public static final class Entry {
        public final String path;
        public final long length, offset;

        Entry(String path, long length, long offset) {
            this.path = path;
            this.length = length;
            this.offset = offset;
        }
    }


    public static final class Manifest {

        public final String name;
        public final String locale;
        public final String infoHash;
        public final int pieceLength;
        final byte[] pieces;
        public final List<Entry> files;
        public final long total;

        Manifest(String name, String locale, String infoHash, int pieceLength, byte[] pieces, List<Entry> files, long total) {
            this.name = name;
            this.locale = locale;
            this.infoHash = infoHash;
            this.pieceLength = pieceLength;
            this.pieces = pieces;
            this.files = files;
            this.total = total;
        }

        public Entry file(String path) {
            for (Entry e : files) if (e.path.equals(path)) return e;
            return null;
        }
    }

    private D2InstallManifest() {}


    public static Manifest read(File downloader) throws IOException {
        long size = downloader.length();
        int n = (int) Math.min(size, TAIL_BYTES);
        byte[] tail = new byte[n];
        try (RandomAccessFile f = new RandomAccessFile(downloader, "r")) {
            f.seek(size - n);
            f.readFully(tail);
        }
        return parse(tail);
    }


    public static Manifest parse(byte[] data) throws IOException {
        int at = lastIndexOf(data, START);
        if (at < 0) throw new D2InstallFetch.Refused("no manifest in the downloader");
        Bencode b = new Bencode(data);
        Object top;
        try {
            top = b.value(at);
        }
        catch (RuntimeException e) {
            throw new D2InstallFetch.Refused("broken manifest");
        }
        if (!(top instanceof Map) || b.infoStart < 0) throw new D2InstallFetch.Refused("broken manifest");
        Map<?, ?> root = (Map<?, ?>) top;
        Object infoValue = root.get("info");
        if (!(infoValue instanceof Map)) throw new D2InstallFetch.Refused("broken manifest");
        Map<?, ?> info = (Map<?, ?>) infoValue;
        String name = text(info.get("name"));
        String locale = text(root.get("locale"));
        Object pieceLength = info.get("piece length"), pieces = info.get("pieces"), files = info.get("files");
        if (name == null || !(pieceLength instanceof Long) || !(pieces instanceof byte[]) || !(files instanceof List)) {
            throw new D2InstallFetch.Refused("broken manifest");
        }
        long pl = (Long) pieceLength;
        if (pl < 16 * 1024 || pl > 16 * 1024 * 1024 || ((byte[]) pieces).length % 20 != 0) throw new D2InstallFetch.Refused("broken manifest");
        List<Entry> entries = new ArrayList<>();
        long offset = 0;
        for (Object o : (List<?>) files) {
            if (!(o instanceof Map)) throw new D2InstallFetch.Refused("broken manifest");
            Object length = ((Map<?, ?>) o).get("length"), path = ((Map<?, ?>) o).get("path");
            if (!(length instanceof Long) || (Long) length < 0 || !(path instanceof List) || ((List<?>) path).isEmpty()) {
                throw new D2InstallFetch.Refused("broken manifest");
            }
            StringBuilder p = new StringBuilder();
            for (Object part : (List<?>) path) {
                String s = text(part);

                if (s == null || s.isEmpty() || s.equals(".") || s.equals("..") || s.contains("/") || s.contains("\\")) {
                    throw new D2InstallFetch.Refused("broken manifest");
                }
                if (p.length() > 0) p.append('/');
                p.append(s);
            }
            entries.add(new Entry(p.toString(), (Long) length, offset));
            offset += (Long) length;
        }
        long count = (offset + pl - 1) / pl;
        if (count != ((byte[]) pieces).length / 20) throw new D2InstallFetch.Refused("broken manifest");
        String hash = hex(sha1(data, b.infoStart, b.infoEnd - b.infoStart));
        return new Manifest(name, locale, hash, (int) pl, (byte[]) pieces, Collections.unmodifiableList(entries), offset);
    }


    public static Map<String, String> parsePins(String text) {
        Map<String, String> pins = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int gap = line.indexOf("  ");
            if (gap != 40 || !line.substring(0, 40).matches("[0-9a-fA-F]{40}")) continue;
            pins.put(line.substring(42).trim(), line.substring(0, 40).toLowerCase(Locale.ROOT));
        }
        return pins;
    }






    public static void verifyProduct(File productDir, File downloader, Map<String, String> pins, String code, String locale) throws IOException {
        String pinned = pins.get(code);
        if (pinned == null) throw new D2InstallFetch.Refused("no manifest pin for " + code);
        if (!downloader.isFile()) throw new D2InstallFetch.Refused("missing " + downloader.getName());
        Manifest m = read(downloader);
        if (!pinned.equals(m.infoHash)) throw new D2InstallFetch.Refused("manifest differs: " + code);
        if (locale != null && !locale.equals(m.locale)) throw new D2InstallFetch.Refused("manifest of another language: " + m.locale);
        File dir = new File(productDir, m.name);
        verifyFiles(dir, m, CHECKED);


        for (String name : CHECKED) {
            File picked = D2InstallFetch.find(productDir, name, 1, locale);
            if (picked == null || !picked.getAbsoluteFile().equals(new File(dir, name).getAbsoluteFile())) {
                throw new D2InstallFetch.Refused("another " + name + " beside the checked one");
            }
        }
    }


    static void verifyFiles(File dir, Manifest m, List<String> names) throws IOException {
        if (m.name.contains("/") || m.name.contains("\\") || m.name.equals("..") || m.name.equals(".")) throw new D2InstallFetch.Refused("broken manifest");
        MessageDigest sha1 = digest();
        byte[] buf = new byte[m.pieceLength];
        for (String name : names) {
            Entry e = m.file(name);
            if (e == null) throw new D2InstallFetch.Refused("not in the manifest: " + name);
            File f = new File(dir, e.path);
            if (!f.isFile()) throw new D2InstallFetch.Refused("missing " + e.path);
            if (f.length() != e.length) throw new D2InstallFetch.Refused("size differs: " + e.path);
            if (e.length == 0) continue;
            long first = e.offset / m.pieceLength, last = (e.offset + e.length - 1) / m.pieceLength;
            for (long p = first; p <= last; p++) {
                long start = p * m.pieceLength, end = Math.min(start + m.pieceLength, m.total);
                int got = readRange(dir, m, start, end, buf, e.path);
                sha1.reset();
                sha1.update(buf, 0, got);
                byte[] want = Arrays.copyOfRange(m.pieces, (int) p * 20, (int) p * 20 + 20);
                if (!MessageDigest.isEqual(sha1.digest(), want)) throw new D2InstallFetch.Refused("checksum differs: " + e.path);
            }
        }
    }


    private static int readRange(File dir, Manifest m, long start, long end, byte[] buf, String checking) throws IOException {
        int filled = 0;
        for (Entry e : m.files) {
            long from = Math.max(start, e.offset), to = Math.min(end, e.offset + e.length);
            if (from >= to) continue;
            File f = new File(dir, e.path);
            if (!f.isFile() || f.length() != e.length) throw new D2InstallFetch.Refused("missing " + e.path + " (next to " + checking + ")");
            try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
                r.seek(from - e.offset);
                r.readFully(buf, (int) (from - start), (int) (to - from));
            }
            filled += (int) (to - from);
        }
        if (filled != end - start) throw new D2InstallFetch.Refused("broken manifest");
        return filled;
    }

    private static String text(Object o) {
        return o instanceof byte[] ? new String((byte[]) o, StandardCharsets.UTF_8) : null;
    }

    private static int lastIndexOf(byte[] data, byte[] what) {
        outer:
        for (int i = data.length - what.length; i >= 0; i--) {
            for (int j = 0; j < what.length; j++) if (data[i + j] != what[j]) continue outer;
            return i;
        }
        return -1;
    }

    private static MessageDigest digest() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static byte[] sha1(byte[] data, int from, int length) throws IOException {
        MessageDigest md = digest();
        md.update(data, from, length);
        return md.digest();
    }

    static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) s.append(String.format(Locale.ROOT, "%02x", b));
        return s.toString();
    }


    private static final class Bencode {
        private final byte[] d;
        private int pos, depth;

        int infoStart = -1, infoEnd = -1;

        Bencode(byte[] d) {
            this.d = d;
        }

        Object value(int at) {
            pos = at;
            return next();
        }

        private Object next() {
            if (pos >= d.length) throw new IllegalStateException("end");
            byte c = d[pos];
            if (c == 'i') {
                int e = find('e');
                long v = Long.parseLong(new String(d, pos + 1, e - pos - 1, StandardCharsets.US_ASCII));
                pos = e + 1;
                return v;
            }
            if (c == 'l' || c == 'd') {
                if (++depth > 16) throw new IllegalStateException("too deep");
                pos++;
                Object result;
                if (c == 'l') {
                    List<Object> list = new ArrayList<>();
                    while (peek() != 'e') list.add(next());
                    result = list;
                }
                else {
                    Map<String, Object> map = new LinkedHashMap<>();
                    while (peek() != 'e') {
                        Object key = next();
                        if (!(key instanceof byte[])) throw new IllegalStateException("key");
                        String k = new String((byte[]) key, StandardCharsets.UTF_8);
                        int valueStart = pos;
                        map.put(k, next());
                        if (depth == 1 && k.equals("info")) {
                            infoStart = valueStart;
                            infoEnd = pos;
                        }
                    }
                    result = map;
                }
                pos++;
                depth--;
                return result;
            }
            if (c >= '0' && c <= '9') {
                int colon = find(':');
                int n = Integer.parseInt(new String(d, pos, colon - pos, StandardCharsets.US_ASCII));
                if (n < 0 || colon + 1 + n > d.length) throw new IllegalStateException("length");
                byte[] s = Arrays.copyOfRange(d, colon + 1, colon + 1 + n);
                pos = colon + 1 + n;
                return s;
            }
            throw new IllegalStateException("byte " + c);
        }

        private byte peek() {
            if (pos >= d.length) throw new IllegalStateException("end");
            return d[pos];
        }

        private int find(char ch) {
            for (int i = pos; i < d.length && i < pos + 24; i++) if (d[i] == ch) return i;
            throw new IllegalStateException("no " + ch);
        }
    }


    static Map<String, String> readPins(File file) throws IOException {
        return parsePins(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }
}
