package com.winlator.d2;

import java.util.HashSet;
import java.util.Set;












final class D2OfferCheck {

    static final int CAP = 30;

    private final Set<String> logged = new HashSet<>();





    static String unexpectedUse(int kind, long untilMs, long now) {
        long limit;
        if (kind == D2ModState.OBJECT_CHEST) limit = D2UsedObjects.FAILED_TRY_MS;
        else if (kind == D2ModState.OBJECT_SHRINE) limit = D2UsedObjects.USED_OBJECT_MS;
        else return "used";
        return untilMs - now > limit ? "used for " + (untilMs - now) + " ms" : null;
    }





    String leftOutByUse(int kind, long id, long untilMs, long now) {
        String why = unexpectedUse(kind, untilMs, now);
        if (why == null || logged.size() >= CAP) return null;

        if (!logged.add(D2UsedObjects.unitType(kind) + ":" + id + ":" + (why.startsWith("used for") ? "long" : why))) return null;

        return "Nehmen: left out unexpectedly: kind " + kind + " id " + id + " why " + why;

    }


    void clear() {
        logged.clear();
    }
}
