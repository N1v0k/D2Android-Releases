package com.winlator.d2;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;













public final class D2RezeptKatalog {

    public static final int CAT_RUNES = 0, CAT_GEMS = 1, CAT_CRAFT = 2, CAT_UPGRADE = 3, CAT_QUEST = 4, CAT_RUNEWORD = 5;
    public static final int CUBE_CATEGORIES = 5;

    public static final int FAM_NONE = -1, FAM_BLOOD = 0, FAM_CASTER = 1, FAM_HIT = 2, FAM_SAFETY = 3;

    public static final int C_WHITE = 0, C_RED = 1, C_GREEN = 2, C_BLUE = 3, C_GOLD = 4, C_GREY = 5, C_TAN = 7, C_ORANGE = 8,
        C_YELLOW = 9;


    public interface Texte {
        String t(int what, Object... args);
    }


    public interface Spieltexte {
        String id(int id);

        String key(String key);
    }


    public static final class Zutat {
        public final int count;

        final String[] codes;
        final int type;
        final int quality, flags;

        public final String picture;
        public final String label;

        final int[] baseTypes;
        public final int baseSockets;

        Zutat(int count, String[] codes, int type, int quality, int flags, String picture, String label, int[] baseTypes,
              int baseSockets) {
            this.count = count;
            this.codes = codes;
            this.type = type;
            this.quality = quality;
            this.flags = flags;
            this.picture = picture;
            this.label = label;
            this.baseTypes = baseTypes;
            this.baseSockets = baseSockets;
        }


        public String fixedCode() {
            return codes != null && codes.length == 1 && quality == D2Rezepte.Q_ANY && flags == D2Rezepte.IN_ITEM ? codes[0] : null;
        }

        public boolean isBase() {
            return baseTypes != null;
        }
    }


    public static final class Zeile {
        public final String text;
        public final int colour;
        public final boolean big;

        Zeile(String text, int colour, boolean big) {
            this.text = text;
            this.colour = colour;
            this.big = big;
        }
    }


    public static final class Eintrag {
        public final int category, family, row;
        public final boolean ladder;
        public final String name, info;
        public final int colour;
        public final List<Zutat> zutaten;

        public final String resultPicture;

        public final String resultCode;
        public final List<Zeile> preview;

        final String search;

        public final String[] runes;

        public final boolean machbar;

        Eintrag(int category, int family, int row, boolean ladder, String name, String info, int colour, List<Zutat> zutaten,
                String resultPicture, String resultCode, List<Zeile> preview, String search, String[] runes, boolean machbar) {
            this.machbar = machbar;
            this.category = category;
            this.family = family;
            this.row = row;
            this.ladder = ladder;
            this.name = name;
            this.info = info;
            this.colour = colour;
            this.zutaten = zutaten;
            this.resultPicture = resultPicture;
            this.resultCode = resultCode;
            this.preview = preview;
            this.search = search;
            this.runes = runes;
        }

        public boolean isRuneword() {
            return category == CAT_RUNEWORD;
        }
    }


    public static final int T_CAT_RUNES = 100, T_CAT_GEMS = 101, T_CAT_CRAFT = 102, T_CAT_UPGRADE = 103, T_CAT_QUEST = 104,
        T_FAM_BLOOD = 110, T_FAM_CASTER = 111, T_FAM_HIT = 112, T_FAM_SAFETY = 113, T_CRAFT_NAME = 114, T_CRAFT_KIND = 115,
        T_CRAFT_RANDOM = 116, T_CRAFT_LEVEL = 117, T_Q_LOW = 120, T_Q_NORMAL = 121, T_Q_MAGIC = 122, T_Q_RARE = 123,
        T_Q_UNIQUE = 124, T_Q_SET = 125, T_F_NOSOCKET = 126, T_F_SOCKETED = 127, T_F_ETH = 128, T_F_NOETH = 129,
        T_F_BASIC = 130, T_F_EXC = 131, T_F_ELITE = 132, T_ANY = 133, T_UP_SOCKETS = 140, T_UP_EXC = 141, T_UP_ELITE = 142,
        T_UP_MAGIC = 143, T_UP_RARE = 144, T_UP_NORMAL = 145, T_UP_REPAIR = 146, T_UP_RECHARGE = 147, T_UP_UNSOCKET = 148,
        T_UP_KEEPS = 149, T_PORTAL_COW = 150, T_PORTAL_PANDEMONIUM = 151, T_PORTAL_FINALE = 152, T_RW_BASE = 160,
        T_RW_LEVEL = 161, T_LADDER = 162, T_SOCKETS = 163, T_WEAPON = 164, T_ARMOR = 165, T_SHIELD = 166, T_UP_LEVELREQ = 167,
        T_TYPE_BASE = 200;

    public static final String[] TYPE_CODES = {"weap", "armo", "tors", "helm", "shld", "any", "gem0", "gem1", "gem2", "gem3",
        "gem4", "amul", "ring", "axe", "blun", "knif", "rod", "spea", "staf", "swor", "hpot", "mpot", "club", "h2h", "hamm",
        "mace", "mele", "miss", "pala", "pole", "scep", "wand", "glov", "boot", "belt"};

    static final String[][] TYPE_PICTURES = {{"weap", "lsd"}, {"armo", "qui"}, {"tors", "qui"}, {"helm", "cap"}, {"shld", "lrg"},
        {"gem0", "gcv"}, {"gem1", "gfv"}, {"gem2", "gsv"}, {"gem3", "gzv"}, {"gem4", "gpv"}, {"amul", "amu"}, {"ring", "rin"},
        {"axe", "axe"}, {"blun", "mac"}, {"knif", "dgr"}, {"rod", "wnd"}, {"spea", "spr"}, {"staf", "sst"}, {"swor", "lsd"},
        {"hpot", "hp1"}, {"mpot", "mp1"}, {"club", "clb"}, {"h2h", "ktr"}, {"hamm", "whm"}, {"mace", "mac"}, {"mele", "lsd"},
        {"miss", "sbw"}, {"pala", "pa1"}, {"pole", "vou"}, {"scep", "scp"}, {"wand", "wnd"}};




    static final String[] FAMILY_SEARCH = {"blut blood", "zauberer caster", "treffer hit power hitpower", "sicherheit safety"};
    static final String[] CATEGORY_SEARCH = {"runen runes", "steine edelsteine gems", "handwerk crafting crafted",
        "verbessern upgrade", "quest"};


    static final String[] TYPE_SEARCH = {"waffe weapon", "rustung armor armour", "rustung body armor", "helm", "schild shield",
        "beliebig any", "ladierter edelstein chipped gem", "fehlerhafter edelstein flawed gem", "edelstein gem",
        "makelloser edelstein flawless gem", "perfekter edelstein perfect gem", "amulett amulet", "ring", "axt axe",
        "hiebwaffe blunt", "dolch dagger knife", "stab zauberstab staff wand", "speer spear", "stab staff", "schwert sword",
        "heiltrank healing potion", "manatrank mana potion", "keule club", "klaue claw", "hammer", "streitkolben mace",
        "nahkampfwaffe melee", "bogen armbrust bow crossbow", "paladinschild paladin shield", "stangenwaffe polearm",
        "zepter scepter", "zauberstab wand", "handschuhe gloves", "stiefel boots", "gurtel belt"};


    static final String[] HIDDEN_OUTPUTS = {"amu", "rin", "jav", "tax", "aqv", "cqv", "yps"};

    static final String[] FAMILY_GEMS = {"gpr", "gpv", "gpb", "gpg"};

    static final String[] SLOT_TYPES = {"helm", "tors", "shld", "glov", "boot", "belt", "amul", "ring", "weap"};

    private final List<Eintrag> entries;

    private D2RezeptKatalog(List<Eintrag> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    public List<Eintrag> all() {
        return entries;
    }


    static D2RezeptKatalog of(List<Eintrag> entries) {
        return new D2RezeptKatalog(new ArrayList<>(entries));
    }





    public static D2RezeptKatalog bauen(D2Rezepte rezepte, D2Gegenstaende items, D2Runenwoerter runen, D2Werte werte,
                                        Spieltexte de, Spieltexte en, Texte app) {
        return bauen(rezepte, items, runen, werte, de, en, app, null);
    }


    public static D2RezeptKatalog bauen(D2Rezepte rezepte, D2Gegenstaende items, D2Runenwoerter runen, D2Werte werte,
                                        Spieltexte de, Spieltexte en, Texte app, String[] uniqueKeys) {
        if (rezepte == null || items == null || de == null || app == null) return null;
        if (en == null) en = de;
        List<Eintrag> list = new ArrayList<>();
        for (D2Rezepte.Rezept r : rezepte.all()) {
            Eintrag e = recipe(r, items, runen, werte, de, en, app, uniqueKeys);
            if (e != null) list.add(e);
        }
        if (runen != null) for (D2Runenwoerter.Runenwort w : runen.all()) {
            Eintrag e = runeword(w, items, runen, werte, de, en, app);
            if (e != null) list.add(e);
        }
        return new D2RezeptKatalog(list);
    }




    static boolean visible(D2Rezepte.Rezept r, D2Gegenstaende items) {
        D2Rezepte.Ausgabe o = r.output();
        if (!r.enabled || o == null || r.inputs.isEmpty()) return false;
        if (o.prefix != 0 || o.suffix != 0) return false;
        if (o.kind == D2Rezepte.KIND_ITEM || o.kind == D2Rezepte.KIND_TYPE) {
            D2Gegenstaende.Item i = items.get(o.item);
            if (i != null) for (String h : HIDDEN_OUTPUTS) if (h.equals(i.code)) return false;
        }
        return true;
    }

    static int category(D2Rezepte.Rezept r, D2Gegenstaende items) {
        D2Rezepte.Ausgabe o = r.output();
        if (o.portal()) return CAT_QUEST;
        if (o.kind == D2Rezepte.KIND_ITEM) {
            D2Gegenstaende.Item i = items.get(o.item);
            String c = i != null ? i.code : "";
            if (isRune(c)) return CAT_RUNES;
            if (isGem(c) || c.equals("rvs") || c.equals("rvl")) return CAT_GEMS;
            return CAT_QUEST;
        }
        if (o.kind == D2Rezepte.KIND_USETYPE && o.quality == D2Rezepte.Q_CRAFTED) return CAT_CRAFT;
        return CAT_UPGRADE;
    }

    static boolean isRune(String c) {
        return c.length() == 3 && c.charAt(0) == 'r' && Character.isDigit(c.charAt(1)) && Character.isDigit(c.charAt(2));
    }

    static boolean isGem(String c) {
        return c.length() == 3 && (c.charAt(0) == 'g' && "cfslzp".indexOf(c.charAt(1)) >= 0 && "vybgrw".indexOf(c.charAt(2)) >= 0
            || c.startsWith("sk") && "cfulz".indexOf(c.charAt(2)) >= 0);
    }

    private static Eintrag recipe(D2Rezepte.Rezept r, D2Gegenstaende items, D2Runenwoerter runen, D2Werte werte,
                                  Spieltexte de, Spieltexte en, Texte app, String[] uniqueKeys) {
        if (!visible(r, items)) return null;
        D2Rezepte.Ausgabe o = r.output();
        int cat = category(r, items);
        List<Zutat> zutaten = new ArrayList<>();
        StringBuilder search = new StringBuilder();
        for (D2Rezepte.Eingabe in : r.inputs) {
            Zutat z = zutat(in, items, de, app, uniqueKeys);
            if (z == null) return null;
            zutaten.add(z);
            search.append(' ').append(z.label);
            String enLabel = inputName(in, items, en, uniqueKeys);
            if (enLabel != null) search.append(' ').append(enLabel);

            D2Gegenstaende.Item base = !in.isType() && !in.isAny() ? items.get(in.item) : null;
            if (base != null && in.has(D2Rezepte.IN_UPGRADED)) {
                for (String c : new String[]{base.uberCode, base.ultraCode}) {
                    D2Gegenstaende.Item v = items.get(c);
                    if (v == null || v == base) continue;
                    String n1 = itemName(v, de), n2 = itemName(v, en);
                    if (n1 != null) search.append(' ').append(n1);
                    if (n2 != null) search.append(' ').append(n2);
                }
            }
            if (in.isType()) search.append(' ').append(typeSearch(items.typeCode(in.item)));
        }
        int family = FAM_NONE;
        String name, info = null, resultPicture = null, resultCode = null, enName = null;
        int colour = C_WHITE;
        List<Zeile> preview = new ArrayList<>();
        D2Gegenstaende.Item result = o.kind == D2Rezepte.KIND_ITEM ? items.get(o.item) : null;
        D2Gegenstaende.Item first = firstItem(r, items);
        if (cat == CAT_CRAFT) {
            family = family(r, items);
            int slot = slot(first, items);
            String fam = app.t(T_FAM_BLOOD + Math.max(0, family)), slotName = slotName(slot, app);
            name = app.t(T_CRAFT_NAME, fam, slotName);
            enName = null;
            colour = C_ORANGE;
            resultPicture = first != null ? first.invFile : null;
            info = slotName;
            preview.add(new Zeile(name, C_ORANGE, true));
            preview.add(new Zeile(app.t(T_CRAFT_KIND, fam), C_GREY, false));
            addLines(preview, werte, o.mods, C_BLUE);
            preview.add(new Zeile(app.t(T_CRAFT_RANDOM), C_GREY, false));
            preview.add(new Zeile(app.t(T_CRAFT_LEVEL), C_GREY, false));
            search.append(' ').append(fam).append(' ').append(app.t(T_CAT_CRAFT)).append(' ').append(FAMILY_SEARCH[Math.max(0, family)]);
        }
        else if (o.portal()) {
            name = app.t(o.kind == D2Rezepte.KIND_COW ? T_PORTAL_COW : o.kind == D2Rezepte.KIND_PANDEMONIUM ? T_PORTAL_PANDEMONIUM
                : T_PORTAL_FINALE);
            colour = C_WHITE;
            D2Gegenstaende.Item tome = items.get("tbk");
            resultPicture = tome != null ? tome.invFile : null;
            preview.add(new Zeile(name, C_WHITE, true));
        }
        else if (result != null) {
            name = itemName(result, de);
            enName = itemName(result, en);
            colour = isRune(result.code) ? C_ORANGE : C_WHITE;
            resultPicture = result.invFile;
            resultCode = result.code;
            preview.add(new Zeile(name, colour, true));
            D2Runenwoerter.Steckteil s = runen != null ? runen.steckteil(result.index) : null;
            if (s != null && werte != null) {
                addGroup(preview, werte, app.t(T_WEAPON), s.waffe);
                addGroup(preview, werte, app.t(T_ARMOR), s.ruestung);
                addGroup(preview, werte, app.t(T_SHIELD), s.schild);
            }
            if (result.levelReq > 0) preview.add(new Zeile(app.t(T_RW_LEVEL, result.levelReq), C_WHITE, false));
        }
        else {

            name = upgradeName(o, werte, app);
            colour = C_WHITE;
            resultPicture = first != null ? first.invFile : null;
            preview.add(new Zeile(name, C_WHITE, true));
            for (String line : upgradeLines(o, werte, app)) preview.add(new Zeile(line, C_GREY, false));
            addLines(preview, werte, socketless(werte, o.mods), C_BLUE);
        }
        search.append(' ').append(name);
        if (enName != null) search.append(' ').append(enName);
        search.append(' ').append(app.t(T_CAT_RUNES + cat)).append(' ').append(CATEGORY_SEARCH[cat]);
        if (cat == CAT_UPGRADE) search.append(' ').append(upgradeSearch(o, werte));
        boolean machbar = !o.has(D2Rezepte.OUT_REPAIR) && !o.has(D2Rezepte.OUT_RECHARGE);
        return new Eintrag(cat, family, r.row, r.ladder, name, info, colour, zutaten, resultPicture, resultCode, preview,
            fold(search.toString()), null, machbar);
    }


    private static D2Gegenstaende.Item firstItem(D2Rezepte.Rezept r, D2Gegenstaende items) {
        D2Rezepte.Eingabe in = r.inputs.get(0);
        if (!in.isType() && !in.isAny()) return items.get(in.item);
        String t = in.isType() ? items.typeCode(in.item) : null;
        String pic = t != null ? typePicture(t) : null;
        return pic != null ? items.get(pic) : null;
    }

    static int family(D2Rezepte.Rezept r, D2Gegenstaende items) {
        for (D2Rezepte.Eingabe in : r.inputs) {
            D2Gegenstaende.Item i = in.isType() ? null : items.get(in.item);
            if (i == null) continue;
            for (int f = 0; f < FAMILY_GEMS.length; f++) if (FAMILY_GEMS[f].equals(i.code)) return f;
        }
        return FAM_BLOOD;
    }


    static int slot(D2Gegenstaende.Item i, D2Gegenstaende items) {
        if (i == null) return -1;
        for (int s = 0; s < SLOT_TYPES.length; s++) if (items.ist(i.type, SLOT_TYPES[s])) return s;
        return -1;
    }

    private static String slotName(int slot, Texte app) {
        return slot < 0 ? app.t(T_ANY) : typeLabel(SLOT_TYPES[slot], app);
    }

    static String typeLabel(String code, Texte app) {
        for (int i = 0; i < TYPE_CODES.length; i++) if (TYPE_CODES[i].equals(code)) return app.t(T_TYPE_BASE + i);
        return code;
    }

    static String typeSearch(String code) {
        for (int i = 0; i < TYPE_CODES.length; i++) if (TYPE_CODES[i].equals(code)) return TYPE_SEARCH[i];
        return code != null ? code : "";
    }


    private static String upgradeSearch(D2Rezepte.Ausgabe o, D2Werte werte) {
        StringBuilder b = new StringBuilder();
        if (o.has(D2Rezepte.OUT_SOCKETS) || hasSocketMod(werte, o.mods)) b.append(" fassungen sockets socket sockel");
        if (o.has(D2Rezepte.OUT_EXCEPTIONAL)) b.append(" exzeptionell exceptional");
        if (o.has(D2Rezepte.OUT_ELITE)) b.append(" elite");
        if (o.has(D2Rezepte.OUT_REPAIR)) b.append(" reparieren repair");
        if (o.has(D2Rezepte.OUT_RECHARGE)) b.append(" aufladen recharge");
        if (o.has(D2Rezepte.OUT_UNSOCKET)) b.append(" leeren unsocket fassungen sockets");
        if (o.quality == D2Rezepte.Q_MAGIC) b.append(" magisch magic");
        if (o.quality == D2Rezepte.Q_RARE) b.append(" selten rare");
        if (o.quality == D2Rezepte.Q_NORMAL) b.append(" normal");
        return b.toString();
    }

    static String typePicture(String typeCode) {
        for (String[] p : TYPE_PICTURES) if (p[0].equals(typeCode)) return p[1];
        return null;
    }

    private static Zutat zutat(D2Rezepte.Eingabe in, D2Gegenstaende items, Spieltexte de, Texte app, String[] uniqueKeys) {
        String label = in.isAny() || in.isType() ? typeInputLabel(in, items, app) : inputName(in, items, de, uniqueKeys);
        if (label == null) return null;

        String qualifiers = in.has(D2Rezepte.IN_UNIQUE_SET) && in.uniqueSet > 0 ? "" : qualifiers(in, app);
        if (!qualifiers.isEmpty()) label = label + " (" + qualifiers + ")";
        if (in.isAny()) {
            return new Zutat(in.quantity, null, -1, in.quality, in.flags, null, label, null, 0);
        }
        if (in.isType()) {
            String t = items.typeCode(in.item);
            if (t == null) return null;
            String pic = typePicture(t);
            D2Gegenstaende.Item p = pic != null ? items.get(pic) : null;
            return new Zutat(in.quantity, null, in.item, in.quality, in.flags, p != null ? p.invFile : null, label, null, 0);
        }
        D2Gegenstaende.Item i = items.get(in.item);
        if (i == null) return null;
        String[] codes = in.has(D2Rezepte.IN_UPGRADED) && i.tier() != D2Gegenstaende.NO_TIER
            ? new String[]{i.normCode, i.uberCode, i.ultraCode} : new String[]{i.code};
        return new Zutat(in.quantity, codes, -1, in.quality, in.flags, i.invFile, label, null, 0);
    }


    private static String inputName(D2Rezepte.Eingabe in, D2Gegenstaende items, Spieltexte texts, String[] uniqueKeys) {
        if (in.isAny()) return null;
        if (in.isType()) return null;
        if (in.has(D2Rezepte.IN_UNIQUE_SET) && in.uniqueSet > 0 && uniqueKeys != null && in.uniqueSet - 1 < uniqueKeys.length) {
            String u = clean(texts.key(uniqueKeys[in.uniqueSet - 1]));
            if (u != null) return u;
        }
        D2Gegenstaende.Item i = items.get(in.item);
        return i != null ? itemName(i, texts) : null;
    }

    private static String qualifiers(D2Rezepte.Eingabe in, Texte app) {
        List<String> q = new ArrayList<>();
        switch (in.quality) {
            case D2Rezepte.Q_LOW: q.add(app.t(T_Q_LOW)); break;
            case D2Rezepte.Q_NORMAL: q.add(app.t(T_Q_NORMAL)); break;
            case D2Rezepte.Q_MAGIC: q.add(app.t(T_Q_MAGIC)); break;
            case D2Rezepte.Q_RARE: q.add(app.t(T_Q_RARE)); break;
            case D2Rezepte.Q_UNIQUE: q.add(app.t(T_Q_UNIQUE)); break;
            case D2Rezepte.Q_SET: q.add(app.t(T_Q_SET)); break;
            default: break;
        }
        if (in.has(D2Rezepte.IN_BASIC)) q.add(app.t(T_F_BASIC));
        if (in.has(D2Rezepte.IN_EXCEPTIONAL)) q.add(app.t(T_F_EXC));
        if (in.has(D2Rezepte.IN_ELITE)) q.add(app.t(T_F_ELITE));
        if (in.has(D2Rezepte.IN_ETHEREAL)) q.add(app.t(T_F_ETH));
        if (in.has(D2Rezepte.IN_NOT_ETHEREAL)) q.add(app.t(T_F_NOETH));
        if (in.has(D2Rezepte.IN_NOSOCKET)) q.add(app.t(T_F_NOSOCKET));
        if (in.has(D2Rezepte.IN_SOCKETED)) q.add(app.t(T_F_SOCKETED));
        return String.join(", ", q);
    }


    static String typeInputLabel(D2Rezepte.Eingabe in, D2Gegenstaende items, Texte app) {
        return in.isAny() ? app.t(T_ANY) : typeLabel(items.typeCode(in.item), app);
    }

    private static String upgradeName(D2Rezepte.Ausgabe o, D2Werte werte, Texte app) {
        if (o.has(D2Rezepte.OUT_UNSOCKET)) return app.t(T_UP_UNSOCKET);
        if (o.kind == D2Rezepte.KIND_USETYPE && o.quality == D2Rezepte.Q_MAGIC) return app.t(T_UP_MAGIC);
        if (o.kind == D2Rezepte.KIND_USETYPE && o.quality == D2Rezepte.Q_RARE) return app.t(T_UP_RARE);
        if (o.has(D2Rezepte.OUT_ELITE)) return app.t(T_UP_ELITE);
        if (o.has(D2Rezepte.OUT_EXCEPTIONAL)) return app.t(T_UP_EXC);
        if (o.has(D2Rezepte.OUT_RECHARGE)) return app.t(T_UP_RECHARGE);
        if (o.has(D2Rezepte.OUT_REPAIR)) return app.t(T_UP_REPAIR);
        if (o.has(D2Rezepte.OUT_SOCKETS) || hasSocketMod(werte, o.mods)) return app.t(T_UP_SOCKETS);
        if (o.quality == D2Rezepte.Q_MAGIC) return app.t(T_UP_MAGIC);
        if (o.quality == D2Rezepte.Q_RARE) return app.t(T_UP_RARE);
        if (o.quality == D2Rezepte.Q_NORMAL) return app.t(T_UP_NORMAL);
        return app.t(T_CAT_UPGRADE);
    }

    private static List<String> upgradeLines(D2Rezepte.Ausgabe o, D2Werte werte, Texte app) {
        List<String> l = new ArrayList<>();
        if (o.has(D2Rezepte.OUT_EXCEPTIONAL) || o.has(D2Rezepte.OUT_ELITE) || o.has(D2Rezepte.OUT_SOCKETS)
            || o.kind == D2Rezepte.KIND_USEITEM && !o.has(D2Rezepte.OUT_UNSOCKET)) l.add(app.t(T_UP_KEEPS));
        if (o.has(D2Rezepte.OUT_SOCKETS) || hasSocketMod(werte, o.mods)) l.add(app.t(T_SOCKETS, socketRange(werte, o)));
        for (D2Rezepte.Mod m : o.mods) if (isStat(werte, m, STAT_LEVELREQ)) l.add(app.t(T_UP_LEVELREQ, m.min));
        return l;
    }


    static final int STAT_SOCKETS = 194, STAT_LEVELREQ = 92;
    private static boolean isStat(D2Werte werte, D2Rezepte.Mod m, int stat) {
        return werte != null && werte.stat(m.property) == stat;
    }

    private static boolean hasSocketMod(D2Werte werte, List<D2Rezepte.Mod> mods) {
        for (D2Rezepte.Mod m : mods) if (isStat(werte, m, STAT_SOCKETS)) return true;
        return false;
    }

    private static String socketRange(D2Werte werte, D2Rezepte.Ausgabe o) {
        for (D2Rezepte.Mod m : o.mods) if (isStat(werte, m, STAT_SOCKETS)) return D2Werte.range(m.min, m.max);
        return o.quantity > 0 && o.quantity < 255 ? String.valueOf(o.quantity) : "1";
    }


    private static List<D2Rezepte.Mod> socketless(D2Werte werte, List<D2Rezepte.Mod> mods) {
        List<D2Rezepte.Mod> l = new ArrayList<>();
        for (D2Rezepte.Mod m : mods) if (!isStat(werte, m, STAT_SOCKETS) && !isStat(werte, m, STAT_LEVELREQ)) l.add(m);
        return l;
    }

    private static void addLines(List<Zeile> out, D2Werte werte, List<D2Rezepte.Mod> mods, int colour) {
        if (werte == null || mods.isEmpty()) return;
        List<D2Werte.Wert> w = new ArrayList<>();
        for (D2Rezepte.Mod m : mods) w.add(new D2Werte.Wert(m.property, m.param, m.min, m.max));
        for (D2Werte.Zeile z : werte.zeilen(w, 0)) out.add(new Zeile(z.text, z.raw ? C_GREY : colour, false));
    }

    private static void addGroup(List<Zeile> out, D2Werte werte, String title, List<D2Werte.Wert> w) {
        if (w == null || w.isEmpty()) return;
        List<D2Werte.Zeile> z = werte.zeilen(w, 0);
        if (z.isEmpty()) return;
        StringBuilder b = new StringBuilder(title).append(": ");
        for (int i = 0; i < z.size(); i++) b.append(i > 0 ? ", " : "").append(z.get(i).text);
        out.add(new Zeile(b.toString(), C_BLUE, false));
    }



    private static Eintrag runeword(D2Runenwoerter.Runenwort w, D2Gegenstaende items, D2Runenwoerter runen, D2Werte werte,
                                    Spieltexte de, Spieltexte en, Texte app) {
        if (!w.complete || w.runes.length == 0) return null;
        String name = clean(de.key(w.nameKey)), enName = clean(en.key(w.nameKey));
        if (name == null) return null;
        List<Zutat> zutaten = new ArrayList<>();
        StringBuilder order = new StringBuilder(), search = new StringBuilder(name);
        if (enName != null) search.append(' ').append(enName);
        String[] codes = new String[w.runes.length];
        int level = 0;
        for (int k = 0; k < w.runes.length; k++) {
            D2Gegenstaende.Item rune = items.get(w.runes[k]);
            if (rune == null) return null;
            codes[k] = rune.code;
            level = Math.max(level, rune.levelReq);
            String rn = itemName(rune, de);
            zutaten.add(new Zutat(1, new String[]{rune.code}, -1, D2Rezepte.Q_ANY, D2Rezepte.IN_ITEM, rune.invFile, rn, null, 0));
            order.append(order.length() > 0 ? " " : "").append(shortRune(rn));
            search.append(' ').append(rn);
            String ren = itemName(rune, en);
            if (ren != null) search.append(' ').append(ren);
        }
        List<Integer> types = new ArrayList<>();
        StringBuilder base = new StringBuilder();
        String basePicture = null;
        for (int t : w.types) {
            if (t <= 0) continue;
            types.add(t);
            String code = items.typeCode(t);
            if (code == null) continue;
            String label = typeLabel(code, app);
            base.append(base.length() > 0 ? "/" : "").append(label);
            search.append(' ').append(label).append(' ').append(typeSearch(code));
            if (basePicture == null) {
                D2Gegenstaende.Item p = typePicture(code) != null ? items.get(typePicture(code)) : null;
                if (p != null) basePicture = p.invFile;
            }
        }
        int[] baseTypes = new int[types.size()];
        for (int i = 0; i < baseTypes.length; i++) baseTypes[i] = types.get(i);
        String baseLabel = app.t(T_RW_BASE, base.toString(), w.runes.length);
        zutaten.add(new Zutat(1, null, -1, D2Rezepte.Q_ANY, 0, basePicture, baseLabel, baseTypes, w.runes.length));
        List<Zeile> preview = new ArrayList<>();
        preview.add(new Zeile(name, C_GOLD, true));
        preview.add(new Zeile("'" + order + "'", C_GOLD, false));
        preview.add(new Zeile(baseLabel, C_GREY, false));
        preview.add(new Zeile(app.t(T_RW_LEVEL, level), C_GREY, false));


        if (werte != null) {


            java.util.TreeSet<Integer> groups = new java.util.TreeSet<>();
            for (int t : baseTypes) groups.add(baseGroup(t, items));
            List<D2Werte.Wert> all = new ArrayList<>(w.werte);
            if (groups.size() == 1) all.addAll(runeBonuses(w, runen, groups.first()));
            for (D2Werte.Zeile z : werte.zeilen(all, 0)) preview.add(new Zeile(z.text, z.raw ? C_GREY : C_BLUE, false));
            if (groups.size() > 1) for (int g : groups) {
                addGroup(preview, werte, app.t(g == 0 ? T_WEAPON : g == 2 ? T_SHIELD : T_ARMOR), runeBonuses(w, runen, g));
            }
        }
        if (w.ladder) preview.add(new Zeile(app.t(T_LADDER), C_GREY, false));
        return new Eintrag(CAT_RUNEWORD, FAM_NONE, w.row, w.ladder, name, baseLabel, C_GOLD, zutaten, basePicture, null, preview,
            fold(search.toString()), codes, true);
    }


    static int baseGroup(int type, D2Gegenstaende items) {
        if (items.ist(type, "weap")) return 0;
        if (items.ist(type, "shld")) return 2;
        return 1;
    }


    private static List<D2Werte.Wert> runeBonuses(D2Runenwoerter.Runenwort w, D2Runenwoerter runen, int group) {
        List<D2Werte.Wert> out = new ArrayList<>();
        for (int r : w.runes) {
            D2Runenwoerter.Steckteil s = runen != null ? runen.steckteil(r) : null;
            if (s != null) out.addAll(group == 0 ? s.waffe : group == 2 ? s.schild : s.ruestung);
        }
        return out;
    }


    static String shortRune(String name) {
        if (name == null) return null;

        for (String suffix : new String[]{"-Rune", " Rune", "-rune", " rune"}) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) return name.substring(0, name.length() - suffix.length());
        }

        for (String prefix : new String[]{"Runa ", "Rune "}) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) return name.substring(prefix.length());
        }

        return name;
    }



    static String itemName(D2Gegenstaende.Item i, Spieltexte texts) {
        String n = lastLine(texts.id(i.nameId));
        return n != null ? n : clean(lastLine(texts.key(i.code)));
    }


    static String lastLine(String s) {
        if (s == null) return null;
        String[] lines = s.split("\n");
        for (int k = lines.length - 1; k >= 0; k--) {
            String t = clean(lines[k]);
            if (t != null) return t;
        }
        return null;
    }


    static String clean(String s) {
        if (s == null) return null;
        String t = D2ItemLabel.plainBase(s);
        if (t == null) return null;
        t = t.replace('\n', ' ').trim();
        return t.isEmpty() ? null : t;
    }


    public static String fold(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder b = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
            if (c == 'ß') b.append("ss");
            else b.append(c);
        }
        return b.toString();
    }


    public static boolean matches(Eintrag e, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        for (String word : fold(query).trim().split("\\s+")) if (!e.search.contains(word)) return false;
        return true;
    }
}
