package com.winlator.d2.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Quests {
    public static final int ERLEDIGT = 1, BELOHNUNG_OFFEN = 2;

    public static final QuestZeile Q1 = new QuestZeile(1, 1, 1, "Höhle des Bösen", "Höhle",
        new int[]{Gebiete.HOEHLE_DES_BOESEN}, QuestZeile.Zieltyp.GEBIET_LEEREN, 0, "Akara", 4, Gebiete.BLUTMOOR, "Blutmoor",
        Gebiete.LAGER);

    public static final List<QuestZeile> AKT1 = Collections.unmodifiableList(Arrays.asList(Q1));

    private Quests() {}

    public enum Start { LEEREN, QUEST, NUR_AKT1, UNBEKANNT, KEINE }

    public static Start start(boolean stadt, int gebiet, int[] worte) {
        if (!stadt) return Start.LEEREN;
        if (gebiet != Gebiete.LAGER) return Start.NUR_AKT1;
        if (worte == null) return Start.UNBEKANNT;
        return naechsteOffene(worte) != null ? Start.QUEST : Start.KEINE;
    }

    public static boolean worteBekannt(int pakete, long erstesAm, long spielSeit) {
        return pakete >= 2 || (pakete == 1 && erstesAm - spielSeit >= 1000);
    }

    public static QuestZeile akt1(int nummer) {
        for (QuestZeile z : AKT1) if (z.nummer == nummer) return z;
        return null;
    }

    public static QuestZeile naechsteOffene(int[] worte) {
        return naechsteOffene(worte, java.util.Collections.<QuestZeile>emptySet());
    }

    public static QuestZeile naechsteOffene(int[] worte, java.util.Set<QuestZeile> abgeschlossen) {
        if (worte == null) return null;
        for (QuestZeile z : AKT1) {
            if (abgeschlossen.contains(z)) continue;
            if (z.wort >= worte.length) continue;
            if ((worte[z.wort] & ERLEDIGT) != 0) continue;
            if (z.kann()) return z;
        }
        return null;
    }
}
