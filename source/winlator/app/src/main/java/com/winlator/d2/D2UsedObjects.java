package com.winlator.d2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

















final class D2UsedObjects {

    static final long USED_OBJECT_MS = 3000;

    static final long FAILED_TRY_MS = 4000;

    static final long NONE = -1;

    private final Map<Long, Long> until = new HashMap<>();





    static long untilAfterTry(int kind, boolean stillThere, long now) {
        if (kind == D2ModState.OBJECT_SHRINE) return now + USED_OBJECT_MS;
        if (kind == D2ModState.OBJECT_CHEST && stillThere) return now + FAILED_TRY_MS;
        return NONE;
    }






    static boolean isReported(D2ModState s, int unitType, long id) {
        if (s == null) return false;
        if (unitType == D2ModState.UNIT_ITEM) {
            for (D2ModState.Thing item : s.items) if (item.id == id) return true;
            return false;
        }
        if (unitType == D2ModState.UNIT_MONSTER) {
            for (D2ModState.Monster monster : s.monsters) if (monster.id == id) return true;
            return false;
        }
        for (D2ModState.Thing object : s.objects) if (object.id == id && unitType(object.kind) == unitType) return true;
        return false;
    }


    static int unitType(int kind) {
        if (kind == D2ModState.OBJECT_ENTRANCE) return D2ModState.UNIT_TILE;
        if (kind == D2ModState.OBJECT_CORPSE) return D2ModState.UNIT_PLAYER;
        return D2ModState.UNIT_OBJECT;
    }

    private static long key(int kind, long id) {
        return ((long) unitType(kind) << 32) | (id & 0xFFFFFFFFL);
    }


    void use(int kind, long id, long untilMs) {
        until.put(key(kind, id), untilMs);
    }


    long until(int kind, long id) {
        Long u = until.get(key(kind, id));
        return u != null ? u : NONE;
    }


    boolean leftOut(int kind, long id, long now) {
        Long u = until.get(key(kind, id));
        return u != null && now < u;
    }


    void expire(long now) {
        for (Iterator<Map.Entry<Long, Long>> it = until.entrySet().iterator(); it.hasNext(); ) if (now >= it.next().getValue()) it.remove();
    }


    void clear() {
        until.clear();
    }





    Set<Long> objectIds(long now) {
        Set<Long> ids = new HashSet<>();
        for (Map.Entry<Long, Long> e : until.entrySet()) {
            if (now < e.getValue() && (int) (e.getKey() >>> 32) == D2ModState.UNIT_OBJECT) ids.add(e.getKey() & 0xFFFFFFFFL);
        }
        return ids;
    }
}
