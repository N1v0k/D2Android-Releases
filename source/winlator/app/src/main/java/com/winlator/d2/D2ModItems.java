package com.winlator.d2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;







public final class D2ModItems {
    public static final int CAT_OTHER = 0, CAT_GOLD = 1, CAT_HEAL = 2, CAT_MANA = 3, CAT_REJUV = 4, CAT_POTION_OTHER = 5,
        CAT_TP_SCROLL = 6, CAT_ID_SCROLL = 7, CAT_TP_TOME = 8, CAT_ID_TOME = 9, CAT_RUNE = 10, CAT_GEM = 11,
        CAT_JEWEL = 12, CAT_RING = 13, CAT_AMULET = 14, CAT_CHARM = 15, CAT_WEAPON = 16, CAT_ARMOR = 17, CAT_AMMO = 18,
        CAT_KEY = 19, CAT_QUEST = 20;

    public static final int QUALITY_LOW = 1, QUALITY_NORMAL = 2, QUALITY_SUPERIOR = 3, QUALITY_MAGIC = 4, QUALITY_SET = 5,
        QUALITY_RARE = 6, QUALITY_UNIQUE = 7, QUALITY_CRAFTED = 8;
    public static final int KIND_HEAL = 0, KIND_MANA = 1, KIND_REJUV = 2;
    public static final int INVENTORY_WIDTH = 10, INVENTORY_HEIGHT = 4;

    public static final int TOME_MAX = 20;

    public static final class Item {
        public final long id;

        public final String code;
        public final int category, tier, quality, sockets, type, width, height, quantity;
        public final boolean ethereal, socketed, identified, runeword;

        public final boolean hiddenByFilter;

        public final String name;

        Item(long id, String code, int category, int tier, int quality, int flags, int sockets, int type, int size,
            int quantity, String name) {
            this.id = id;
            this.code = code;
            this.category = category;
            this.tier = tier;
            this.quality = quality;
            this.ethereal = (flags & 1) != 0;
            this.socketed = (flags & 2) != 0;
            this.identified = (flags & 4) != 0;
            this.runeword = (flags & 8) != 0;
            this.hiddenByFilter = (flags & 16) != 0;
            this.sockets = sockets;
            this.type = type;
            this.width = size >> 4;
            this.height = size & 15;
            this.quantity = quantity;
            this.name = name;
        }

        public boolean isPotion() {
            return category == CAT_HEAL || category == CAT_MANA || category == CAT_REJUV;
        }


        public String oneLine() {
            return name.replace('\n', ' ').trim();
        }
    }


    public final boolean namesKnown, beltRefillPossible;

    public final boolean labelFilterPossible, newLootVisible;

    public final boolean takePossible;

    public final boolean shopPossible;

    public final boolean sellPossible;

    public final D2PotionShop.Job shopJob;

    public final int shopWares;

    public boolean shopSellsPotions() {
        return (shopWares & 3) != 0;
    }


    public final int beltMoves;

    public final int beltRows;

    public final int[] belt = new int[3], inventory = new int[3];

    public final int tpTome, tpScrolls, idTome, idScrolls;

    public final int tpTomeMax, idTomeMax, keyMax;

    public final long usedCells;




    private int gridWidth = INVENTORY_WIDTH, gridHeight = INVENTORY_HEIGHT;
    private long usedLow, usedHigh;
    public final List<Item> items;

    private D2ModItems(ByteBuffer b) {
        int flags = u8(b);
        namesKnown = (flags & 1) != 0;
        beltRefillPossible = (flags & 2) != 0;
        labelFilterPossible = (flags & 4) != 0;
        newLootVisible = (flags & 8) != 0;
        takePossible = (flags & 16) != 0;
        shopPossible = (flags & 32) != 0;
        sellPossible = (flags & 64) != 0;
        beltMoves = u8(b);
        beltRows = u8(b);
        for (int k = 0; k < 3; k++) {
            belt[k] = u8(b);
            inventory[k] = u8(b);
        }
        int tp = u8(b), tps = u8(b), id = u8(b), ids = u8(b);
        tpTome = tp == 0xFF ? -1 : tp;
        tpScrolls = tps;
        idTome = id == 0xFF ? -1 : id;
        idScrolls = ids;
        long used = 0;
        for (int i = 0; i < 5; i++) used |= (long) u8(b) << (8 * i);
        usedCells = used;
        usedLow = used;
        int count = u8(b);
        List<Item> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long itemId = b.getInt() & 0xFFFFFFFFL;
            byte[] code = new byte[4];
            b.get(code);
            int category = u8(b), tier = u8(b), quality = u8(b), itemFlags = u8(b), sockets = u8(b);
            int type = u16(b), size = u8(b), quantity = u16(b);
            byte[] name = new byte[u8(b)];
            b.get(name);
            list.add(new Item(itemId, new String(code, StandardCharsets.US_ASCII).trim(), category, tier, quality, itemFlags,
                sockets, type, size, quantity, new String(name, StandardCharsets.UTF_8)));
        }
        items = Collections.unmodifiableList(list);

        boolean stacks = (flags & 128) != 0 && b.remaining() >= 3;

        boolean job = b.remaining() - (stacks ? 3 : 0) >= 10;
        shopJob = job ? new D2PotionShop.Job(b.getInt() & 0xFFFFFFFFL, u8(b), u8(b), u8(b), u8(b), u8(b)) : null;
        shopWares = job ? u8(b) : 0;
        int end = b.limit();
        tpTomeMax = stacks ? b.get(end - 3) & 0xFF : -1;
        idTomeMax = stacks ? b.get(end - 2) & 0xFF : -1;
        keyMax = stacks ? b.get(end - 1) & 0xFF : -1;
    }


    public int tpTomeLimit() {
        return tpTomeMax > 0 ? tpTomeMax : TOME_MAX;
    }


    public static D2ModItems parse(byte[] data, int length) {
        if (D2ModState.packetType(data, length) != D2ModState.PKT_ITEMS) return null;
        ByteBuffer b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN);
        b.position(8);
        try {
            return new D2ModItems(b);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    public Item find(long id) {
        for (Item item : items) if (item.id == id) return item;
        return null;
    }





    public void attachGrid(int width, int height, byte[] mask) {
        if (width < 1 || width > 16 || height < 1 || height > 8 || mask == null || mask.length * 8 < width * height) return;
        long low = 0, high = 0;
        for (int i = 0; i < width * height; i++) {
            if ((mask[i / 8] >> (i % 8) & 1) == 0) continue;
            if (i < 64) low |= 1L << i;
            else high |= 1L << (i - 64);
        }
        gridWidth = width;
        gridHeight = height;
        usedLow = low;
        usedHigh = high;
    }


    public int gridWidth() {
        return gridWidth;
    }

    public int gridHeight() {
        return gridHeight;
    }


    public long usedCellsLow() {
        return usedLow;
    }


    public long usedCellsHigh() {
        return usedHigh;
    }

    public boolean cellUsed(int x, int y) {
        int i = y * gridWidth + x;
        if (x < 0 || y < 0 || x >= gridWidth || y >= gridHeight) return true;
        return ((i < 64 ? usedLow >> i : usedHigh >> (i - 64)) & 1) != 0;
    }


    public boolean fits(int width, int height) {
        for (int y = 0; y + height <= gridHeight; y++) {
            for (int x = 0; x + width <= gridWidth; x++) {
                boolean free = true;
                for (int dy = 0; dy < height && free; dy++) for (int dx = 0; dx < width && free; dx++) free = !cellUsed(x + dx, y + dy);
                if (free) return true;
            }
        }
        return false;
    }

    public int freeCells() {
        int cells = gridWidth * gridHeight;
        long lowMask = cells >= 64 ? -1L : (1L << cells) - 1, highMask = cells <= 64 ? 0 : cells >= 128 ? -1L : (1L << (cells - 64)) - 1;
        return cells - Long.bitCount(usedLow & lowMask) - Long.bitCount(usedHigh & highMask);
    }


    public String describe() {
        StringBuilder line = new StringBuilder(String.format(java.util.Locale.ROOT,
            "Mitgeführt: Gürtel %d Reihen, Heil %d+%d, Mana %d+%d, Verjüngung %d+%d, Portal %s+%d, frei %d%s%s  Boden:",
            beltRows, belt[0], inventory[0], belt[1], inventory[1], belt[2], inventory[2], tpTome < 0 ? "–" : String.valueOf(tpTome),
            tpScrolls, freeCells(), namesKnown ? "" : " (ohne Namen)", beltRefillPossible ? "" : " (Gürtel füllen nicht möglich)"));
        for (int i = 0; i < Math.min(4, items.size()); i++) {
            Item item = items.get(i);
            line.append(i > 0 ? " |" : "").append(' ').append(item.code).append(" k").append(item.category).append(" q").append(item.quality)
                .append(' ').append(item.oneLine());
        }
        return line.toString();
    }


    public static int potionKind(int category) {
        return category == CAT_HEAL ? KIND_HEAL : (category == CAT_MANA ? KIND_MANA : (category == CAT_REJUV ? KIND_REJUV : -1));
    }

    private static int u8(ByteBuffer b) {
        return b.get() & 0xFF;
    }

    private static int u16(ByteBuffer b) {
        return b.getShort() & 0xFFFF;
    }
}
