package com.winlator.d2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;











public final class D2ItemDetails {

    public static final int MARKER = 1;

    public static final int NORMAL = 0, EXCEPTIONAL = 1, ELITE = 2, UNKNOWN = 255;


    public static final class Item {
        public final int sockets, filled, tier, level;

        Item(int sockets, int filled, int tier, int level) {
            this.sockets = sockets;
            this.filled = filled;
            this.tier = tier;
            this.level = level;
        }
    }

    private D2ItemDetails() {}


    public static Item[] parse(byte[] data, int length) {
        if (data == null || length > data.length) return null;
        try {
            ByteBuffer b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN);

            b.position(8 + 2 + 4 + 16 + 12 + 1 + 8 + 4);
            int places = b.get() & 0xFF;
            b.position(b.position() + places * 11);
            b.position(b.position() + 5);
            int carried = b.get() & 0xFF;
            b.position(b.position() + carried * 16);
            int shop = b.get() & 0xFF;
            b.position(b.position() + shop * 16);
            if (!b.hasRemaining()) return null;
            b.get();

            if (b.remaining() < 1 + 4 * carried || (b.get() & 0xFF) != MARKER) return null;
            Item[] items = new Item[carried];
            for (int i = 0; i < carried; i++) items[i] = new Item(b.get() & 0xFF, b.get() & 0xFF, b.get() & 0xFF, b.get() & 0xFF);
            return items;
        }
        catch (RuntimeException e) {
            return null;
        }
    }
}
