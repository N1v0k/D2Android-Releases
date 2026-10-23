package com.winlator.d2;














public final class D2SkillTargets {
    static final int RECORD = 572, FLAGS = 0x04, RANGE = 0x14;

    static final int PASSIVE = 1 << 4, AURA = 1 << 5, TARGETABLE_ONLY = 1 << 20, SEARCH_ENEMY_XY = 1 << 21,
        SEARCH_ENEMY_NEAR = 1 << 22, SEARCH_OPEN_XY = 1 << 23, TARGET_CORPSE = 1 << 24, TARGET_ITEM = 1 << 27,
        TGT_PLACE_CHECK = 1 << 29, ITEM_EFFECT = 1 << 30;

    static final int AIMS = ITEM_EFFECT | TARGETABLE_ONLY | SEARCH_ENEMY_XY | SEARCH_ENEMY_NEAR | SEARCH_OPEN_XY;

    static final int NEVER = PASSIVE | AURA | TGT_PLACE_CHECK | TARGET_CORPSE | TARGET_ITEM;

    static final int FIRE_BOLT = 36, FROST_NOVA = 44;

    private D2SkillTargets() {}


    public static boolean aims(int flags, int range) {
        return ((flags & AIMS) != 0 || range != 0) && (flags & NEVER) == 0;
    }





    public static boolean[] read(byte[] bin) {
        if (bin == null || bin.length < 4) return null;
        int count = s32(bin, 0);
        if (count <= FROST_NOVA || (long) count * RECORD + 4 != bin.length) return null;
        boolean[] aims = new boolean[count];
        for (int id = 0; id < count; id++) {
            int at = 4 + id * RECORD;
            aims[id] = aims(s32(bin, at + FLAGS), bin[at + RANGE] & 0xFF);
        }
        return aims[FIRE_BOLT] && !aims[FROST_NOVA] ? aims : null;
    }


    static final int WARMTH = 37;






    public static boolean[] passives(byte[] bin) {
        if (bin == null || bin.length < 4) return null;
        int count = s32(bin, 0);
        if (count <= WARMTH || (long) count * RECORD + 4 != bin.length) return null;
        boolean[] passive = new boolean[count];
        for (int id = 0; id < count; id++) passive[id] = (s32(bin, 4 + id * RECORD + FLAGS) & PASSIVE) != 0;
        return passive[WARMTH] && !passive[FIRE_BOLT] ? passive : null;
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
