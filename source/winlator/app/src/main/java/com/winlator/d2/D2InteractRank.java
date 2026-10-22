package com.winlator.d2;

public final class D2InteractRank {
    private D2InteractRank() {}

    public static final int CORPSE = 900, TRAVEL = 800, PERSON = 700, ITEM_TOP = 600, ITEM_RARE = 500, ITEM_MAGIC = 400,
        ITEM_USEFUL = 300, ITEM_OTHER = 200, CONTAINER = 100, OTHER = 0;

    public static int object(int kind) {
        switch (kind) {
            case D2ModState.OBJECT_CORPSE: return CORPSE;
            case D2ModState.OBJECT_PORTAL:
            case D2ModState.OBJECT_WAYPOINT:
            case D2ModState.OBJECT_ENTRANCE: return TRAVEL;
            case D2ModState.OBJECT_CHEST:
            case D2ModState.OBJECT_SHRINE:
            case D2ModState.OBJECT_WELL: return CONTAINER;
            default: return OTHER;
        }
    }

    public static int person() {
        return PERSON;
    }

    public static int item(int category, int quality, int stateKind, int stateQuality) {
        int q = quality >= 0 ? quality : stateQuality;
        if (category == D2ModItems.CAT_RUNE || (category < 0 && stateKind == D2ModState.ITEM_RUNE)) return ITEM_TOP;
        if (q == D2ModItems.QUALITY_UNIQUE || q == D2ModItems.QUALITY_SET) return ITEM_TOP;
        if (q == D2ModItems.QUALITY_RARE || q == D2ModItems.QUALITY_CRAFTED) return ITEM_RARE;
        if (q == D2ModItems.QUALITY_MAGIC) return ITEM_MAGIC;
        if (category >= 0) {
            switch (category) {
                case D2ModItems.CAT_GOLD:
                case D2ModItems.CAT_HEAL:
                case D2ModItems.CAT_MANA:
                case D2ModItems.CAT_REJUV:
                case D2ModItems.CAT_POTION_OTHER:
                case D2ModItems.CAT_TP_SCROLL:
                case D2ModItems.CAT_GEM:
                    return ITEM_USEFUL;
                default:
                    return ITEM_OTHER;
            }
        }
        return stateKind == D2ModState.ITEM_GOLD || stateKind == D2ModState.ITEM_POTION || stateKind == D2ModState.ITEM_GEM
            ? ITEM_USEFUL : ITEM_OTHER;
    }

    public static double key(int rank, double distance) {
        return -rank * 100000.0 + Math.max(0, Math.min(distance, 99999));
    }

    public static int[] page(int count, int perPage, int index) {
        int pages = Math.max(1, (count + perPage - 1) / perPage);
        int p = ((index % pages) + pages) % pages;
        int from = p * perPage;
        return new int[]{from, Math.min(count, from + perPage), pages};
    }
}
