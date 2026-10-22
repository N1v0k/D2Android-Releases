package com.winlator.d2;

final class D2NpcRing {
    private D2NpcRing() {}

    static boolean closeOnMenuGone(boolean linesShown, boolean menuOpen) {
        return linesShown && !menuOpen;
    }

    static boolean isUnit(Object entry) {
        return entry instanceof D2ModState.Monster || entry instanceof D2ModState.Thing;
    }

    static Object highlightTarget(Object ringChoice) {
        return isUnit(ringChoice) ? ringChoice : null;
    }
}
