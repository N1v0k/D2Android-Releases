package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;

















public final class D2AutoItems {

    public static final int POTION_RESERVE = D2PotionShop.RESERVE_DEFAULT;
    public static final int SCROLLS_WITHOUT_TOME = 3;

    static final String KEY_PICK_POTIONS = "pickPotions", KEY_PICK_SCROLLS = "pickScrolls", KEY_PICK_RUNES = "pickRunes",
        KEY_PICK_GEMS = "pickGems", KEY_PICK_JEWELRY = "pickJewelry", KEY_BELT_REFILL = "beltRefill";


    public boolean gold, potions, scrolls, runes, gems, jewelry, beltRefill;

    public int[] reserve = {POTION_RESERVE, POTION_RESERVE};

    public void load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE);
        gold = D2Settings.autoGold(context);
        potions = prefs.getBoolean(KEY_PICK_POTIONS, true);
        scrolls = prefs.getBoolean(KEY_PICK_SCROLLS, true);
        runes = prefs.getBoolean(KEY_PICK_RUNES, true);
        gems = prefs.getBoolean(KEY_PICK_GEMS, true);
        jewelry = prefs.getBoolean(KEY_PICK_JEWELRY, true);
        beltRefill = prefs.getBoolean(KEY_BELT_REFILL, true);
        reserve = new int[]{D2Settings.shopReserve(context, false), D2Settings.shopReserve(context, true)};
    }

    public static boolean get(Context context, String key) {
        return context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).getBoolean(key, true);
    }


    public boolean any() {
        return gold || potions || scrolls || runes || gems || jewelry;
    }





    public boolean wants(D2ModState s, D2ModState.Thing thing, D2ModItems items) {
        if (thing.kind == D2ModState.ITEM_GOLD) return gold && !s.goldFull;
        if (items == null) return false;
        D2ModItems.Item item = items.find(thing.id);
        if (item == null) return false;
        boolean cell = items.fits(Math.max(1, item.width), Math.max(1, item.height));
        switch (item.category) {
            case D2ModItems.CAT_HEAL:
            case D2ModItems.CAT_MANA: {
                if (!potions) return false;
                int kind = D2ModItems.potionKind(item.category);
                boolean beltRoom = beltRoom(s, items, kind);
                return beltRoom || (items.inventory[kind] < reserve[kind] && cell);
            }
            case D2ModItems.CAT_REJUV:
                return potions && (beltRoom(s, items, D2ModItems.KIND_REJUV) || cell);
            case D2ModItems.CAT_TP_SCROLL:
                if (!scrolls) return false;
                return items.tpTome >= 0 ? items.tpTome < items.tpTomeLimit() : items.tpScrolls < SCROLLS_WITHOUT_TOME && cell;
            case D2ModItems.CAT_RUNE:
                return runes && cell;
            case D2ModItems.CAT_GEM:
                return gems && cell;
            case D2ModItems.CAT_JEWEL:
            case D2ModItems.CAT_RING:
            case D2ModItems.CAT_AMULET:
                return jewelry && cell;
            default:
                return false;
        }
    }





    static int beltKind(int potionKind) {
        return potionKind + 1;
    }


    static boolean beltRoom(D2ModState s, D2ModItems items, int potionKind) {
        int rows = Math.max(1, Math.min(4, items.beltRows));
        for (int column = 0; column < 4; column++) {
            int used = 0;
            for (int row = 0; row < rows; row++) if (s.belt[column + 4 * row] != D2ModState.BELT_NONE) used++;
            if (used == 0) return true;
            if (s.belt[column] == beltKind(potionKind) && used < rows) return true;
        }
        return false;
    }




    static final long REFILL_INTERVAL_MS = 700;





    static final long REFILL_REST_MS = 1000;


    private final int[] columnKind = new int[4];

    private long refillAt;




    private final long[] refillTriedAt = new long[3], refillRestUntil = new long[3];
    private final int[] refillInventory = new int[3], refillMisses = new int[3];
    private String lastRefill = "–";


    public void newGame() {
        java.util.Arrays.fill(columnKind, 0);
        refillAt = 0;
        java.util.Arrays.fill(refillTriedAt, 0);
        java.util.Arrays.fill(refillRestUntil, 0);
        java.util.Arrays.fill(refillInventory, 0);
        java.util.Arrays.fill(refillMisses, 0);
        lastRefill = "–";
    }


    public int[] columnKinds() {
        return columnKind.clone();
    }


    public String lastRefill() {
        return lastRefill;
    }






    public int beltRefill(D2ModState s, D2ModItems items, long now) {
        if (items == null) return -1;
        int rows = Math.max(1, Math.min(4, items.beltRows));
        for (int column = 0; column < 4; column++) if (s.belt[column] != D2ModState.BELT_NONE) columnKind[column] = s.belt[column];
        if (!beltRefill || !items.beltRefillPossible || now - refillAt < REFILL_INTERVAL_MS) return -1;
        for (int column = 0; column < 4; column++) {
            int kind = columnKind[column] - 1;
            if (kind < 0 || kind > 2 || now < refillRestUntil[kind]) continue;
            int used = 0;
            for (int row = 0; row < rows; row++) if (s.belt[column + 4 * row] != D2ModState.BELT_NONE) used++;
            if (used >= rows || items.inventory[kind] == 0) continue;

            if (now - refillTriedAt[kind] < 3000 && items.inventory[kind] >= refillInventory[kind]) {
                if (++refillMisses[kind] >= 2) {
                    refillMisses[kind] = 0;
                    refillRestUntil[kind] = now + REFILL_REST_MS;
                    lastRefill = "Gürtel nimmt " + kind + " nicht, " + REFILL_REST_MS + " ms Pause";
                    continue;
                }
            }
            else refillMisses[kind] = 0;
            refillAt = refillTriedAt[kind] = now;
            refillInventory[kind] = items.inventory[kind];
            lastRefill = "Spalte " + (column + 1) + " ← " + (kind == 0 ? "Heil" : kind == 1 ? "Mana" : "Verjüngung");
            return kind;
        }
        return -1;
    }
}
