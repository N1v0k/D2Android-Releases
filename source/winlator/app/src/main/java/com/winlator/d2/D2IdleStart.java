package com.winlator.d2;

import com.winlator.d2.test.Quests;






final class D2IdleStart {
    enum Sperre { NUR_EINZELSPIELER, FIGUR_LAEDT, HC_UNBEKANNT, HARDCORE, QUEST_NUR_AKT1, QUEST_UNBEKANNT, QUEST_KEINE, TOT, FENSTER }

    private D2IdleStart() {}






    static Sperre pruefen(boolean einzelspieler, String name, Boolean hardcore, boolean stadt, int gebiet, int[] worte,
                          boolean lebt, boolean fenster) {
        if (!einzelspieler) return Sperre.NUR_EINZELSPIELER;
        if (name == null) return Sperre.FIGUR_LAEDT;
        if (hardcore == null) return Sperre.HC_UNBEKANNT;
        if (hardcore) return Sperre.HARDCORE;
        if (stadt) {
            switch (Quests.start(true, gebiet, worte)) {
                case NUR_AKT1: return Sperre.QUEST_NUR_AKT1;
                case UNBEKANNT: return Sperre.QUEST_UNBEKANNT;
                case KEINE: return Sperre.QUEST_KEINE;
                default: break;
            }
        }
        if (!lebt) return Sperre.TOT;
        if (fenster) return Sperre.FENSTER;
        return null;
    }


    static final long PAUSE_LIFT_MS = 3000;







    static boolean pauseEndet(boolean pauseWanted, boolean statePaused, boolean preparing, long seitStart) {
        if (pauseWanted) return true;
        return statePaused && (!preparing || seitStart > PAUSE_LIFT_MS);
    }
}
