package com.winlator.d2.test;

public final class Kampfwahl {
    public static final String ANGRIFF = "attack";
    public static final int ZAUBERIN = 1, TOTENBESCHWOERER = 2, BARBAR = 4;
    public static final int FEUERBLITZ = 36, EISBLITZ = 39, FROSTRUESTUNG = 40, FROSTNOVA = 44, FEUERBALL = 47, SHIVER = 50, CHILLING = 60;
    public static final int ZAEHNE = 67, KNOCHENSPEER = 84;
    public static final int HIEB = 126, DOPPELSCHWUNG = 133, SCHREI = 138, KAMPFBEFEHLE = 149;
    static final long HALTEN_MS = 1500, ELEMENT_WECHSEL_MS = 6000;
    static final int HAUPT_RESERVE = 4;
    static final int NOVA_MINDESTENS = 3;
    static final double NOVA_ABSTAND = 200;
    private static final int FEUER = 1, KAELTE = 2;

    public static final class Rollen {
        public final int klasse;
        public final int[] haupt, stark, nova;
        public final int[] puffer;

        public Rollen(int klasse, int[] haupt, int[] stark, int[] nova, int[] puffer) {
            this.klasse = klasse;
            this.haupt = haupt;
            this.stark = stark;
            this.nova = nova;
            this.puffer = puffer;
        }
    }

    private Rollen rollen;
    private int[] rollenHaupt = new int[0], rollenStark = new int[0];
    private int rollenKlasse = -2;

    public void rollen(Rollen r) {
        if (r == rollen) return;
        rollen = r;
        rollenKlasse = r != null ? r.klasse : -2;
        rollenHaupt = r != null ? vereint(r.haupt, haupt(r.klasse)) : new int[0];
        rollenStark = r != null ? vereint(vereint(r.stark, r.nova), stark(r.klasse)) : new int[0];
        if (r != null) puffer.zusatz(r.klasse, r.puffer);
        else puffer.zusatz(-2, null);
    }

    int[] hauptFuer(int klasse) {
        return klasse == rollenKlasse ? rollenHaupt : haupt(klasse);
    }

    int[] starkFuer(int klasse) {
        return klasse == rollenKlasse ? rollenStark : stark(klasse);
    }

    boolean umDieFigurFuer(int skill) {
        if (umDieFigur(skill)) return true;
        if (rollen != null) for (int s : rollen.nova) if (s == skill) return true;
        return false;
    }

    private static int[] vereint(int[] a, int[] b) {
        java.util.LinkedHashSet<Integer> out = new java.util.LinkedHashSet<>();
        for (int v : a) out.add(v);
        for (int v : b) out.add(v);
        int[] r = new int[out.size()];
        int i = 0;
        for (int v : out) r[i++] = v;
        return r;
    }

    static int[] haupt(int klasse) {
        switch (klasse) {
            case ZAUBERIN: return new int[]{FEUERBLITZ, EISBLITZ};
            case TOTENBESCHWOERER: return new int[]{KNOCHENSPEER, ZAEHNE};
            default: return new int[0];
        }
    }

    static int[] stark(int klasse) {
        switch (klasse) {
            case ZAUBERIN: return new int[]{FROSTNOVA, FEUERBALL};
            case BARBAR: return new int[]{DOPPELSCHWUNG, HIEB};
            default: return new int[0];
        }
    }

    public static boolean eigeneListe(int klasse) {
        return haupt(klasse).length > 0;
    }

    static boolean hauptIstAngriff(int klasse) {
        return klasse == BARBAR;
    }

    static boolean umDieFigur(int skill) {
        return skill == FROSTNOVA;
    }

    static int element(int skill) {
        return skill == FEUERBLITZ ? FEUER : skill == EISBLITZ ? KAELTE : 0;
    }

    private Fertigkeiten tabelle;
    public final Puffer puffer = new Puffer();
    private String gewaehlt = ANGRIFF;
    private int gewaehltFertigkeit = -1;
    private long gewaehltSeit;
    private boolean gewaehltStark;
    public double reserveStark;
    private boolean ersteWahl = true;
    private long wechselZiel = -1;
    private int wechselElement;
    public boolean manaFehlt;
    public double gewuenschteKosten;
    public boolean elementGewechselt;
    public String grund = "";

    public void tabelle(Fertigkeiten f) {
        tabelle = f;
    }

    public Fertigkeiten tabelle() {
        return tabelle;
    }

    public String gewaehlt() {
        return gewaehlt;
    }

    public int fertigkeit() {
        return gewaehltFertigkeit;
    }

    public String knopf(Zustand z, Zustand.Layout l, long ziel, long ohneWirkungMs, long now) {
        return knopf(z, l, ziel, ohneWirkungMs, null, now);
    }

    public String knopf(Zustand z, Zustand.Layout l, long ziel, long ohneWirkungMs, Gruppen.Gruppe gruppe, long now) {
        manaFehlt = false;
        gewuenschteKosten = 0;
        elementGewechselt = false;
        Fertigkeiten t = tabelle;
        String wahl = ANGRIFF;
        int wahlFertigkeit = -1;
        boolean wahlStark = false;
        reserveStark = 0;
        if (t != null && z.fkBekannt && z.klasse >= 0) {
            double reserve = puffer.reserve(z, l, t, now);
            int[] liste = hauptListe(z, l, t, hauptFuer(z.klasse), starkFuer(z.klasse));
            double hauptKosten = Double.MAX_VALUE;
            for (int skill : liste) {
                int slot = slot(z, l, t, skill);
                if (slot >= 0) hauptKosten = Math.min(hauptKosten, kosten(z, slot, t.get(skill)));
            }
            reserveStark = reserve + (hauptKosten == Double.MAX_VALUE ? 0 : HAUPT_RESERVE * hauptKosten);
            if (gruppe != null && gruppe.stark()) {
                for (int skill : starkFuer(z.klasse)) {
                    int slot = slot(z, l, t, skill);
                    if (slot < 0 || contains(liste, skill)) continue;
                    if (umDieFigurFuer(skill) && gruppe.nahe(z, NOVA_ABSTAND) < NOVA_MINDESTENS) continue;
                    Fertigkeiten.Fertigkeit f = t.get(skill);
                    double cost = kosten(z, slot, f);
                    if (z.mana - cost >= (f.angriffOhneMana ? reserve : reserveStark)) {
                        wahl = "F" + (slot + 1);
                        wahlFertigkeit = skill;
                        wahlStark = true;
                        break;
                    }
                }
            }
            if (ziel >= 0 && ohneWirkungMs > ELEMENT_WECHSEL_MS && ziel != wechselZiel) {
                int aktuell = element(gewaehltFertigkeit);
                if (aktuell != 0 && hatElement(z, l, t, liste, aktuell == FEUER ? KAELTE : FEUER)) {
                    wechselZiel = ziel;
                    wechselElement = aktuell == FEUER ? KAELTE : FEUER;
                    elementGewechselt = true;
                    if (wahlStark) {
                        wahl = ANGRIFF;
                        wahlFertigkeit = -1;
                        wahlStark = false;
                    }
                }
            }
            if (ziel != wechselZiel) wechselElement = 0;
            boolean firstWanted = true;
            for (int pass = 0; pass < 2 && wahlFertigkeit < 0; pass++) {
                for (int skill : liste) {
                    if (pass == 0 && (wechselElement == 0 || element(skill) != wechselElement)) continue;
                    int slot = slot(z, l, t, skill);
                    if (slot < 0) continue;
                    Fertigkeiten.Fertigkeit f = t.get(skill);
                    double cost = kosten(z, slot, f);
                    if (firstWanted) {
                        gewuenschteKosten = cost;
                        firstWanted = false;
                    }
                    if (f.angriffOhneMana || z.mana - cost >= reserve) {
                        wahl = "F" + (slot + 1);
                        wahlFertigkeit = skill;
                        break;
                    }
                    manaFehlt = true;
                }
            }
            double angriff = wahlFertigkeit < 0 ? angriffKosten(l, t) : 0;
            if (angriff > 0 && z.mana < angriff) {
                wahl = null;
                manaFehlt = true;
                if (gewuenschteKosten == 0) gewuenschteKosten = angriff;
            }
        }
        if (!ersteWahl && !java.util.Objects.equals(wahl, gewaehlt) && now - gewaehltSeit < HALTEN_MS && !elementGewechselt
            && gewaehlt != null && bezahlbar(z, l, gewaehltFertigkeit, gewaehltStark, gruppe, now)) {
            grund = gewaehlt.equals(ANGRIFF) ? "Angriff" : gewaehlt;
            return gewaehlt;
        }
        if (ersteWahl || !java.util.Objects.equals(wahl, gewaehlt) || gewaehltFertigkeit != wahlFertigkeit) {
            ersteWahl = false;
            gewaehlt = wahl;
            gewaehltFertigkeit = wahlFertigkeit;
            gewaehltSeit = now;
        }
        gewaehltStark = wahlStark;
        grund = wahl == null ? "Mana fehlt, Angriff kostet Mana: warten"
            : wahl.equals(ANGRIFF) ? (manaFehlt ? "Mana fehlt: Angriff" : "Angriff")
            : wahl + " (Fertigkeit " + wahlFertigkeit + (wahlStark ? ", stark" : "") + (elementGewechselt ? ", anderes Element" : "") + ")";
        return wahl;
    }

    public boolean stark() {
        return gewaehltStark;
    }

    private int[] hauptListe(Zustand z, Zustand.Layout l, Fertigkeiten t, int[] haupt, int[] stark) {
        for (int skill : haupt) if (slot(z, l, t, skill) >= 0) return haupt;
        if (hauptIstAngriff(z.klasse)) return new int[0];
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (int skill : stark) if (!umDieFigurFuer(skill)) out.add(skill);
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    private static boolean contains(int[] a, int v) {
        for (int x : a) if (x == v) return true;
        return false;
    }

    static int slot(Zustand z, Zustand.Layout l, Fertigkeiten t, int skill) {
        Fertigkeiten.Fertigkeit f = t.get(skill);
        if (f == null || f.klasse != z.klasse) return -1;
        if (f.zweiWaffen() && z.waffen < 2) return -1;
        for (int i = 0; i < 4; i++) {
            if (z.fkFertigkeit[i] != skill || z.fkLinks[i]) continue;
            if (!f.kostenOhneStufe() && z.fkStufe[i] <= 0) continue;
            String id = "F" + (i + 1);
            if (l != null && (!l.knoepfe.containsKey(id) || !l.sichtbar(id))) continue;
            return i;
        }
        return -1;
    }

    private static boolean hatElement(Zustand z, Zustand.Layout l, Fertigkeiten t, int[] liste, int element) {
        for (int skill : liste) if (element(skill) == element && slot(z, l, t, skill) >= 0) return true;
        return false;
    }

    static double kosten(Zustand z, int slot, Fertigkeiten.Fertigkeit f) {
        int stufe = slot >= 0 && slot < 16 ? z.fkStufe[slot] : 0;
        return f.kosten(stufe > 0 ? stufe : 1);
    }

    private boolean bezahlbar(Zustand z, Zustand.Layout l, int skill, boolean stark, Gruppen.Gruppe gruppe, long now) {
        Fertigkeiten t = tabelle;
        if (skill < 0) return t == null || z.mana >= angriffKosten(l, t);
        int slot = t == null ? -1 : slot(z, l, t, skill);
        if (slot < 0) return false;
        Fertigkeiten.Fertigkeit f = t.get(skill);
        double reserve = puffer.reserve(z, l, t, now);
        if (stark && umDieFigurFuer(skill) && (gruppe == null || gruppe.nahe(z, NOVA_ABSTAND) < NOVA_MINDESTENS)) return false;
        if (f.angriffOhneMana) return !stark || z.mana - kosten(z, slot, f) >= reserve;
        return z.mana - kosten(z, slot, f) >= (stark ? reserveStark : reserve);
    }

    static double angriffKosten(Zustand.Layout l, Fertigkeiten t) {
        if (l == null || l.angriffFertigkeit <= 0) return 0;
        Fertigkeiten.Fertigkeit f = t.get(l.angriffFertigkeit);
        return f == null || f.angriffOhneMana ? 0 : f.kosten(1);
    }
}
