package com.winlator.d2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;










final class D2UsedObjects {
    private final Map<Long, Long> until = new HashMap<>();


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
