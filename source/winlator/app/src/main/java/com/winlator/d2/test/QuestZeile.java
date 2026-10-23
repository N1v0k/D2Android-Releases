package com.winlator.d2.test;





public final class QuestZeile {

    public enum Zieltyp { GEBIET_LEEREN, MONSTER_TOETEN, OBJEKT_BENUTZEN, GEGENSTAND_AUFHEBEN, NPC_ANSPRECHEN }


    public final int nummer, akt;

    public final int wort;

    public final String name, ort;

    public final int[] zielGebiete;
    public final Zieltyp zieltyp;

    public final int ziel;

    public final String abgabe;

    public final int mindeststufe;
    public final int levelGebiet;

    public final String levelName;

    public final int stadt;

    public QuestZeile(int akt, int nummer, int wort, String name, String ort, int[] zielGebiete, Zieltyp zieltyp, int ziel,
                      String abgabe, int mindeststufe, int levelGebiet, String levelName, int stadt) {
        this.akt = akt;
        this.nummer = nummer;
        this.wort = wort;
        this.name = name;
        this.ort = ort;
        this.zielGebiete = zielGebiete.clone();
        this.zieltyp = zieltyp;
        this.ziel = ziel;
        this.abgabe = abgabe;
        this.mindeststufe = mindeststufe;
        this.levelGebiet = levelGebiet;
        this.levelName = levelName;
        this.stadt = stadt;
    }


    public int zielGebiet() {
        return zielGebiete[0];
    }


    public boolean kann() {
        return zieltyp == Zieltyp.GEBIET_LEEREN;
    }

    @Override
    public String toString() {
        return "Q" + nummer + " " + name;
    }
}
