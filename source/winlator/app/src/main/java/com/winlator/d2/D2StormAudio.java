package com.winlator.d2;

/*
 * Ported from StormLib (src/huffman/huff.cpp, src/adpcm/adpcm.cpp), Copyright (c) 1999-2013 Ladislav Zezula, MIT
 * License (its text on the app's page "Lizenzen", tools/collect-licenses.sh)
 */
final class D2StormAudio {
    private D2StormAudio() {}


    private static final String[] WEIGHTS = {
        "0a0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000002",
        "5416160d0c08060506050603040403050e0b141313090b0605040302030202020d070906060403020403030303030202"
            + "090604040404030203020202020302040803040709050303030302020203020203020202020202020201010102010202"
            + "060a0808060704030404020204020303040307070906040303020102020202020a020203020201010202020603050203"
            + "0201010101010101010101020301010102010101010101020404040709080c0201010101010101010101010102010103"
            + "040102040501010101010101020101010401010101010201010101010101010102010101010101010301010101010101"
            + "0201010101010102020101020202064b",
        "0000000000000000000327000023000000000000000000000000000000000000ff0101010101010102020101060e1004"
            + "060805040403030202030301010201010104020402020201010401010203030203010306040101010101010201020101"
            + "0129071612400a0a112501031710262a100123232f10060702090101010101",
        "ff0b07050b020202060202010402010309010101030401010201010102010101050101010d0101010101010101010101"
            + "020101030101010101010102010101010a04020106030201010101010301010105020304030303020101010201020303"
            + "01030101020501010403050103010303020104030a06010101010101010101010202010a020501010207021701050101"
            + "0e0101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101"
            + "060201040501010201010101020101010101010101010101010101010101010101010101010101010701010201010101"
            + "02010101010101010201010101010111",
        "fffb989a848563643e3e222213131817",
        "fff19d9e9a9b9a9793938c8e868880827c7c7273696b5f6055564a4b404137372f2f272721211b1c1717131310100d0d"
            + "0b0b0909080807070605050404041918",
        "c3cbf541ff7bf72100000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000bfccf240fd7cf722000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000007a46",
        "c3d9ef3df97ce91efdabf12cfc5bfe170000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000bdd9ec3df57de81dfbaef02cfb5cff1800000000000000000000000000000000"
            + "0000000000000000000000000000000000000000000000000000000000000000706c",
        "bac5da33e36dd818e594da23df4ad110eeafe42cea5ade15f487e921f643fc1200000000000000000000000000000000"
            + "00000000000000000000000000000000b0c7d833e36bd618e795d823db49d011e9b2e22be85cdd15f187e720f744ff13"
            + "00000000000000000000000000000000000000000000000000000000000000005f9e",
    };

    private static final int ITEMS = 515, HEAD = ITEMS, NONE = -1;

    private static final class Tree {
        final int[] next = new int[ITEMS + 1], prev = new int[ITEMS + 1], value = new int[ITEMS + 1],
            weight = new int[ITEMS + 1], parent = new int[ITEMS + 1], childLo = new int[ITEMS + 1];
        final int[] byByte = new int[258];
        int used;

        Tree() {
            next[HEAD] = prev[HEAD] = HEAD;
            java.util.Arrays.fill(byByte, NONE);
        }

        void remove(int item) {
            if (next[item] != NONE) {
                next[prev[item]] = next[item];
                prev[next[item]] = prev[item];
                next[item] = prev[item] = NONE;
            }
        }

        void link(int item1, int item2) {
            next[item2] = next[item1];
            prev[item2] = prev[next[item1]];
            prev[next[item1]] = item2;
            next[item1] = item2;
        }

        int higherOrEqual(int item, int w) {
            if (item != NONE) {
                while (item != HEAD) {
                    if (weight[item] >= w) return item;
                    item = prev[item];
                }
            }
            return HEAD;
        }

        int create(int v, int w, boolean after) {
            if (used >= ITEMS) return NONE;
            int item = used++;
            next[item] = prev[item] = NONE;
            if (after) link(HEAD, item);
            else link(prev[HEAD], item);
            value[item] = v;
            weight[item] = w;
            parent[item] = NONE;
            childLo[item] = NONE;
            return item;
        }

        int fixup(int item, int maxWeight) {
            if (weight[item] < maxWeight) {
                int higher = higherOrEqual(prev[HEAD], weight[item]);
                remove(item);
                link(higher, item);
                return maxWeight;
            }
            return weight[item];
        }

        boolean build(int type) {
            type &= 0x0F;
            if (type >= WEIGHTS.length) return false;
            String table = WEIGHTS[type];
            int maxWeight = 0;
            for (int i = 0; i < 0x100; i++) {
                int w = 2 * i + 2 <= table.length() ? Integer.parseInt(table.substring(2 * i, 2 * i + 2), 16) : 0;
                if (w == 0) continue;
                byByte[i] = create(i, w, true);
                maxWeight = fixup(byByte[i], maxWeight);
            }
            byByte[0x100] = create(0x100, 1, false);
            byByte[0x101] = create(0x101, 1, false);
            int lo = prev[HEAD];
            while (lo != HEAD) {
                int hi = prev[lo];
                if (hi == HEAD) break;
                int p = create(0, weight[hi] + weight[lo], true);
                if (p == NONE) return false;
                parent[lo] = p;
                parent[hi] = p;
                childLo[p] = lo;
                maxWeight = fixup(p, maxWeight);
                lo = prev[hi];
            }
            return true;
        }

        void incWeights(int item) {
            for (; item != NONE; item = parent[item]) {
                weight[item]++;
                int higher = higherOrEqual(prev[item], weight[item]);
                int hi = next[higher];
                if (hi != item) {
                    remove(hi);
                    link(item, hi);
                    remove(item);
                    link(higher, item);
                    int lo = childLo[parent[hi]];
                    int p = parent[item];
                    if (childLo[p] == item) childLo[p] = hi;
                    if (lo == hi) childLo[parent[hi]] = item;
                    p = parent[item];
                    parent[item] = parent[hi];
                    parent[hi] = p;
                }
            }
        }

        boolean newBranch(int value1, int value2) {
            int last = prev[HEAD];
            int hi = create(value1, weight[last], false);
            if (hi == NONE) return false;
            parent[hi] = last;
            byByte[value1] = hi;
            int lo = create(value2, 0, false);
            if (lo == NONE) return false;
            parent[lo] = last;
            childLo[last] = lo;
            byByte[value2] = lo;
            incWeights(lo);
            return true;
        }
    }

    private static final class Bits {
        final byte[] in;
        int at, end, buffer, count;

        Bits(byte[] in, int at, int end) {
            this.in = in;
            this.at = at;
            this.end = end;
        }

        int bit() {
            if (count == 0) {
                if (at >= end) return -1;
                buffer = in[at++] & 0xFF;
                count = 8;
            }
            int b = buffer & 1;
            buffer >>>= 1;
            count--;
            return b;
        }

        int byte8() {
            if (count < 8) {
                if (at >= end) return -1;
                buffer |= (in[at++] & 0xFF) << count;
                count += 8;
            }
            int v = buffer & 0xFF;
            buffer >>>= 8;
            count -= 8;
            return v;
        }
    }

    static byte[] huffman(byte[] in, int at, int end, int max) {
        byte[] out = new byte[max];
        int n = 0;
        Bits bits = new Bits(in, at, end);
        int type = bits.byte8();
        if (type < 0) return new byte[0];
        Tree t = new Tree();
        if (!t.build(type)) return new byte[0];
        boolean sparse = type == 0;
        while (true) {
            int item = t.next[HEAD];
            if (item == HEAD) break;
            boolean error = false;
            while (t.childLo[item] != NONE) {
                int b = bits.bit();
                if (b < 0) {
                    error = true;
                    break;
                }
                item = b != 0 ? t.prev[t.childLo[item]] : t.childLo[item];
            }
            if (error) break;
            int v = t.value[item];
            if (v == 0x100) break;
            if (v == 0x101) {
                v = bits.byte8();
                if (v < 0 || !t.newBranch(t.value[t.prev[HEAD]], v)) break;
                if (!sparse) t.incWeights(t.byByte[v]);
            }
            if (n >= max) break;
            out[n++] = (byte) v;
            if (sparse) t.incWeights(t.byByte[v]);
        }
        return n == max ? out : java.util.Arrays.copyOf(out, n);
    }


    private static final int[] NEXT_STEP = {
        -1, 0, -1, 4, -1, 2, -1, 6, -1, 1, -1, 5, -1, 3, -1, 7, -1, 1, -1, 5, -1, 3, -1, 7, -1, 2, -1, 4, -1, 6, -1, 8};
    private static final int[] STEP_SIZE = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107,
        118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876,
        963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086,
        29794, 32767};

    static int adpcm(byte[] in, int at, int end, byte[] out, int outAt, int max, int channels) {
        int o = outAt, outEnd = outAt + max;
        int[] predicted = new int[2], step = {0x2C, 0x2C};
        if (end - at < 2) return 0;
        at++;
        int shift = in[at++] & 0xFF;
        for (int i = 0; i < channels; i++) {
            if (end - at < 2) return o - outAt;
            int s = (short) ((in[at] & 0xFF) | (in[at + 1] << 8));
            at += 2;
            predicted[i] = s;
            if (outEnd - o < 2) return o - outAt;
            out[o++] = (byte) s;
            out[o++] = (byte) (s >> 8);
        }
        int channel = channels - 1;
        while (at < end) {
            int encoded = in[at++] & 0xFF;
            channel = (channel + 1) % channels;
            if (encoded == 0x80) {
                if (step[channel] != 0) step[channel]--;
                if (outEnd - o < 2) return o - outAt;
                out[o++] = (byte) predicted[channel];
                out[o++] = (byte) (predicted[channel] >> 8);
            }
            else if (encoded == 0x81) {
                step[channel] += 8;
                if (step[channel] > 0x58) step[channel] = 0x58;
                channel = (channel + 1) % channels;
            }
            else {
                int index = step[channel], size = STEP_SIZE[index];
                int difference = size >> shift;
                if ((encoded & 0x01) != 0) difference += size;
                if ((encoded & 0x02) != 0) difference += size >> 1;
                if ((encoded & 0x04) != 0) difference += size >> 2;
                if ((encoded & 0x08) != 0) difference += size >> 3;
                if ((encoded & 0x10) != 0) difference += size >> 4;
                if ((encoded & 0x20) != 0) difference += size >> 5;
                int p = predicted[channel];
                if ((encoded & 0x40) != 0) {
                    p -= difference;
                    if (p <= -32768) p = -32768;
                }
                else {
                    p += difference;
                    if (p >= 32767) p = 32767;
                }
                predicted[channel] = (short) p;
                if (outEnd - o < 2) return o - outAt;
                out[o++] = (byte) p;
                out[o++] = (byte) (p >> 8);
                index += NEXT_STEP[encoded & 0x1F];
                step[channel] = index < 0 ? 0 : Math.min(index, 88);
            }
        }
        return o - outAt;
    }
}
