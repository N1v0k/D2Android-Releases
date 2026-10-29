package com.winlator.d2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;





















public final class D2Werte {

    public interface Texte {
        String get(int id);

        String get(String key);
    }


    public static final class Wert {
        public final int property, param, min, max;

        public Wert(int property, int param, int min, int max) {
            this.property = property;
            this.param = param;
            this.min = min;
            this.max = max;
        }
    }


    public static final class Zeile {
        public final String text;
        public final int priority;
        public final boolean raw;

        Zeile(String text, int priority, boolean raw) {
            this.text = text;
            this.priority = priority;
            this.raw = raw;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static final int STAT_RECORD = 324, PROPERTY_RECORD = 46, CHARSTATS_RECORD = 196;
    static final int S_PRIO = 0x34, S_FUNC = 0x36, S_VAL = 0x37, S_POS = 0x38, S_NEG = 0x3A, S_STR2 = 0x3C, S_DGRP = 0x3E,
        S_DGRPFUNC = 0x40, S_DGRPVAL = 0x41, S_DGRPPOS = 0x42, S_DGRPNEG = 0x44, S_DGRPSTR2 = 0x46, S_OP = 0x54,
        S_OPPARAM = 0x55;
    static final int P_SET = 0x02, P_VAL = 0x0A, P_FUNC = 0x18, P_STAT = 0x20, P_SLOTS = 7;
    static final int SK_CLASS = 0x0C, SK_DESC = 0x194, SD_NAME = 0x08;
    static final int C_ALLSKILLS = 0x52, C_TAB = 0x54, C_ONLY = 0x5A;

    static final int M_RECORD = 424, M_NAME = 0x06;

    static final int ST_STRENGTH = 0, ST_ENERGY = 1, ST_DEXTERITY = 2, ST_VITALITY = 3, ST_MAXDAMAGE_PERCENT = 17,
        ST_MINDAMAGE_PERCENT = 18, ST_MINDAMAGE = 21, ST_MAXDAMAGE = 22, ST_FIREMIN = 48, ST_FIREMAX = 49,
        ST_LIGHTMIN = 50, ST_LIGHTMAX = 51, ST_MAGICMIN = 52, ST_MAGICMAX = 53, ST_COLDMIN = 54, ST_COLDMAX = 55,
        ST_COLDLENGTH = 56, ST_POISONMIN = 57, ST_POISONMAX = 58, ST_POISONLENGTH = 59, ST_INDESTRUCTIBLE = 152,
        ST_SOCKETS = 194;

    static final int ST_ENHANCED = -2;

    static final int F_SET = 1, F_ARMOR_PERCENT = 2, F_SAME = 3, F_MINDAMAGE = 5, F_MAXDAMAGE = 6, F_DAMAGE_PERCENT = 7,
        F_SPEED = 8, F_SKILLTAB = 10, F_EVENT_SKILL = 11, F_SOCKETS = 14, F_MIN = 15, F_MAX = 16, F_PARAM = 17,
        F_CHARGES = 19, F_INDESTRUCTIBLE = 20, F_VAL_PARAM = 21, F_SKILL = 22, F_ETHEREAL = 23, F_MONSTER = 24;

    private static final class Stat {
        int prio, func, val, pos, neg, str2, dgrp, dgrpFunc, dgrpVal, dgrpPos, dgrpNeg, dgrpStr2, op, opParam;
    }

    private static final class Property {
        final int[] set = new int[P_SLOTS], val = new int[P_SLOTS], func = new int[P_SLOTS], stat = new int[P_SLOTS];
    }


    private static final class StatWert {
        final int stat, param, min, max;

        StatWert(int stat, int param, int min, int max) {
            this.stat = stat;
            this.param = param;
            this.min = min;
            this.max = max;
        }
    }

    private final Stat[] stats;
    private final Property[] properties;
    private final int[] skillClass, skillName;
    private final int[][] classTexts;
    private final int[] monsterName;
    private final Texte texte;

    private D2Werte(Stat[] stats, Property[] properties, int[] skillClass, int[] skillName, int[][] classTexts,
        int[] monsterName, Texte texte) {
        this.monsterName = monsterName;
        this.stats = stats;
        this.properties = properties;
        this.skillClass = skillClass;
        this.skillName = skillName;
        this.classTexts = classTexts;
        this.texte = texte;
    }





    public static D2Werte lesen(byte[] itemstatcost, byte[] props, byte[] skills, byte[] skilldesc, byte[] charstats,
        byte[] monstats, Texte texte) {
        int ns = count(itemstatcost, STAT_RECORD), np = count(props, PROPERTY_RECORD);
        if (ns <= ST_SOCKETS || np <= 0 || texte == null) return null;
        Stat[] st = new Stat[ns];
        for (int i = 0; i < ns; i++) {
            int at = 4 + i * STAT_RECORD;
            Stat s = new Stat();
            s.prio = D2Tabellen.u16(itemstatcost, at + S_PRIO);
            s.func = itemstatcost[at + S_FUNC] & 0xFF;
            s.val = itemstatcost[at + S_VAL] & 0xFF;
            s.pos = D2Tabellen.u16(itemstatcost, at + S_POS);
            s.neg = D2Tabellen.u16(itemstatcost, at + S_NEG);
            s.str2 = D2Tabellen.u16(itemstatcost, at + S_STR2);
            s.dgrp = D2Tabellen.u16(itemstatcost, at + S_DGRP);
            s.dgrpFunc = itemstatcost[at + S_DGRPFUNC] & 0xFF;
            s.dgrpVal = itemstatcost[at + S_DGRPVAL] & 0xFF;
            s.dgrpPos = D2Tabellen.u16(itemstatcost, at + S_DGRPPOS);
            s.dgrpNeg = D2Tabellen.u16(itemstatcost, at + S_DGRPNEG);
            s.dgrpStr2 = D2Tabellen.u16(itemstatcost, at + S_DGRPSTR2);
            s.op = itemstatcost[at + S_OP] & 0xFF;
            s.opParam = itemstatcost[at + S_OPPARAM] & 0xFF;
            st[i] = s;
        }
        Property[] pr = new Property[np];
        for (int i = 0; i < np; i++) {
            int at = 4 + i * PROPERTY_RECORD;
            Property p = new Property();
            for (int k = 0; k < P_SLOTS; k++) {
                p.set[k] = props[at + P_SET + k] & 0xFF;
                p.val[k] = (short) D2Tabellen.u16(props, at + P_VAL + 2 * k);
                p.func[k] = props[at + P_FUNC + k] & 0xFF;
                p.stat[k] = D2Tabellen.u16(props, at + P_STAT + 2 * k);
            }
            pr[i] = p;
        }
        int nk = count(skills, -1), nd = count(skilldesc, -1);
        int[] skCls = new int[Math.max(0, nk)], skName = new int[Math.max(0, nk)];

        java.util.Arrays.fill(skName, -1);
        if (nk > 0 && nd > 0) {
            int size = (skills.length - 4) / nk, dsize = (skilldesc.length - 4) / nd;
            for (int id = 0; id < nk; id++) {
                int at = 4 + id * size;
                skCls[id] = size > SK_CLASS ? skills[at + SK_CLASS] : -1;
                int desc = size > SK_DESC + 1 ? D2Tabellen.u16(skills, at + SK_DESC) : -1;
                skName[id] = desc >= 0 && desc < nd && dsize > SD_NAME + 1 ? D2Tabellen.u16(skilldesc, 4 + desc * dsize + SD_NAME) : -1;
            }
        }
        int nc = count(charstats, CHARSTATS_RECORD);
        int[][] cls = new int[Math.max(0, nc)][];
        for (int c = 0; c < nc; c++) {
            int at = 4 + c * CHARSTATS_RECORD;
            cls[c] = new int[]{D2Tabellen.u16(charstats, at + C_ALLSKILLS), D2Tabellen.u16(charstats, at + C_TAB),
                D2Tabellen.u16(charstats, at + C_TAB + 2), D2Tabellen.u16(charstats, at + C_TAB + 4),
                D2Tabellen.u16(charstats, at + C_ONLY)};
        }
        int nm = count(monstats, M_RECORD);
        int[] mon = new int[Math.max(0, nm)];
        for (int m = 0; m < nm; m++) mon[m] = D2Tabellen.u16(monstats, 4 + m * M_RECORD + M_NAME);
        return new D2Werte(st, pr, skCls, skName, cls, mon, texte);
    }


    public int stat(int property) {
        Property p = property >= 0 && property < properties.length ? properties[property] : null;
        if (p == null) return -1;
        for (int k = 0; k < P_SLOTS; k++) if (p.func[k] != 0) return p.func[k] == F_SOCKETS ? ST_SOCKETS : p.stat[k];
        return -1;
    }


    public List<Zeile> zeilen(List<Wert> werte, int level) {
        List<StatWert> sw = new ArrayList<>();
        List<Zeile> out = new ArrayList<>();
        for (Wert w : werte) expand(w, sw, out, level);
        sum(sw);
        enhancedLine(sw, out);
        damageLines(sw, out);
        groupLines(sw, out);
        for (StatWert s : sw) {
            Zeile z = statLine(s, level);
            if (z != null) out.add(z);
        }
        List<Zeile> sorted = new ArrayList<>(out);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.priority, a.priority));
        return sorted;
    }


    private void expand(Wert w, List<StatWert> sw, List<Zeile> out, int level) {
        Property p = w.property >= 0 && w.property < properties.length ? properties[w.property] : null;
        if (p == null) {
            out.add(raw(w));
            return;
        }
        boolean any = false;
        for (int k = 0; k < P_SLOTS; k++) {
            int f = p.func[k], stat = p.stat[k];
            if (f == 0) continue;
            any = true;
            switch (f) {
                case F_SET: case F_ARMOR_PERCENT: case F_SAME: case F_SPEED:
                    if (validStat(stat)) sw.add(new StatWert(stat, w.param, w.min, w.max));
                    break;
                case F_MINDAMAGE:
                    sw.add(new StatWert(ST_MINDAMAGE, 0, w.min, w.max));
                    break;
                case F_MAXDAMAGE:
                    sw.add(new StatWert(ST_MAXDAMAGE, 0, w.min, w.max));
                    break;
                case F_DAMAGE_PERCENT:

                    sw.add(new StatWert(ST_ENHANCED, 0, w.min, w.max));
                    break;
                case F_SKILLTAB:
                    sw.add(new StatWert(validStat(stat) ? stat : -1, w.param, w.min, w.max));
                    break;
                case F_EVENT_SKILL: case F_CHARGES: case F_SKILL:
                    if (validStat(stat)) sw.add(new StatWert(stat, w.param, w.min, w.max));
                    break;
                case F_SOCKETS: {

                    String t = text("Socketable");

                    out.add(t != null ? new Zeile(t + " (" + range(w.min, w.max) + ")", 0, false) : raw(w));
                    break;
                }
                case F_MIN:
                    if (validStat(stat)) sw.add(new StatWert(stat, 0, w.min, w.min));
                    break;
                case F_MAX:
                    if (validStat(stat)) sw.add(new StatWert(stat, 0, w.max, w.max));
                    break;
                case F_PARAM:

                    if (validStat(stat)) sw.add(w.param != 0 ? new StatWert(stat, 0, w.param, w.param) : new StatWert(stat, 0, w.min, w.max));
                    break;
                case F_INDESTRUCTIBLE:
                    sw.add(new StatWert(ST_INDESTRUCTIBLE, 0, 1, 1));
                    break;
                case F_VAL_PARAM:

                    if (validStat(stat)) sw.add(new StatWert(stat, p.val[k], w.min, w.max));
                    break;
                case F_ETHEREAL:
                    break;
                case F_MONSTER:

                    if (validStat(stat)) sw.add(new StatWert(stat, w.param, w.min, w.max));
                    break;
                default:
                    out.add(raw(w));
                    return;
            }
        }
        if (!any) out.add(raw(w));
    }


    private static void sum(List<StatWert> sw) {
        for (int i = 0; i < sw.size(); i++)
            for (int j = sw.size() - 1; j > i; j--) {
                StatWert a = sw.get(i), b = sw.get(j);
                if (a.stat != b.stat || a.param != b.param || a.stat == ST_COLDLENGTH || a.stat == ST_POISONLENGTH) continue;
                sw.set(i, new StatWert(a.stat, a.param, a.min + b.min, a.max + b.max));
                sw.remove(j);
            }
    }


    private void enhancedLine(List<StatWert> sw, List<Zeile> out) {
        StatWert ed = find(sw, ST_ENHANCED);
        if (ed == null) return;
        sw.remove(ed);
        String t = text("strModEnhancedDamage");
        out.add(new Zeile("+" + range(ed.min, ed.max) + "% " + (t != null ? t : ""), prio(ST_MAXDAMAGE_PERCENT), t == null));
    }


    private void damageLines(List<StatWert> sw, List<Zeile> out) {
        int[][] pairs = {{ST_FIREMIN, ST_FIREMAX}, {ST_LIGHTMIN, ST_LIGHTMAX}, {ST_MAGICMIN, ST_MAGICMAX},
            {ST_COLDMIN, ST_COLDMAX}, {ST_MINDAMAGE, ST_MAXDAMAGE}};
        String[] keys = {"strModFireDamageRange", "strModLightningDamageRange", "strModMagicDamageRange",
            "strModColdDamageRange", "strModMinDamageRange"};
        String[] single = {"strModFireDamage", "strModLightningDamage", "strModMagicDamage", "strModColdDamage",
            "strModMinDamage"};
        for (int i = 0; i < pairs.length; i++) {
            StatWert mn = find(sw, pairs[i][0]), mx = find(sw, pairs[i][1]);
            if (mn == null || mx == null) continue;

            if (mn.min != mn.max || mx.min != mx.max) continue;
            boolean one = mn.min == mx.min;
            String f = text(one ? single[i] : keys[i]);
            if (f == null || !f.contains("%d")) continue;
            out.add(new Zeile(one ? format(f, String.valueOf(mn.min)) : format(f, String.valueOf(mn.min), String.valueOf(mx.min)),
                prio(pairs[i][0]), false));
            sw.remove(mn);
            sw.remove(mx);
            if (pairs[i][0] == ST_COLDMIN) {
                StatWert len = find(sw, ST_COLDLENGTH);
                if (len != null) sw.remove(len);
            }
        }
        StatWert pmin = find(sw, ST_POISONMIN), pmax = find(sw, ST_POISONMAX);


        int lengths = 0, sources = 0;
        for (StatWert s : sw) {
            if (s.stat != ST_POISONLENGTH) continue;
            lengths += s.min;
            sources++;
        }
        int len = sources > 0 ? lengths / sources : 0;
        if (pmin != null && pmax != null && len > 0) {


            int a = (pmin.min * len + 128) >> 8, b = (pmax.max * len + 128) >> 8, sec = Math.max(1, len / 25);
            String key = a == b ? "strModPoisonDamage" : "strModPoisonDamageRange";
            String f = text(key);
            if (f != null && f.contains("%d")) {
                out.add(new Zeile(a == b ? format(f, String.valueOf(a), String.valueOf(sec))
                    : format(f, String.valueOf(a), String.valueOf(b), String.valueOf(sec)), prio(ST_POISONMIN), false));
                sw.remove(pmin);
                sw.remove(pmax);
            }
        }

        sw.removeIf(s -> s.stat == ST_COLDLENGTH || s.stat == ST_POISONLENGTH);
    }


    private void groupLines(List<StatWert> sw, List<Zeile> out) {
        Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < stats.length; i++) {
            if (stats[i].dgrp == 0) continue;
            List<Integer> g = groups.get(stats[i].dgrp);
            if (g == null) groups.put(stats[i].dgrp, g = new ArrayList<>());
            g.add(i);
        }
        for (List<Integer> g : groups.values()) {
            List<StatWert> found = new ArrayList<>();
            for (int stat : g) {
                StatWert s = find(sw, stat);
                if (s == null) break;
                found.add(s);
            }
            if (found.size() != g.size()) continue;
            StatWert first = found.get(0);
            boolean same = true;
            for (StatWert s : found) same &= s.min == first.min && s.max == first.max;
            if (!same) continue;
            Stat s = stats[first.stat];
            String text = describe(s.dgrpFunc, s.dgrpVal, first.max < 0 ? s.dgrpNeg : s.dgrpPos, s.dgrpStr2, first.min, first.max);
            if (text == null) continue;
            out.add(new Zeile(text, s.prio, false));
            sw.removeAll(found);
        }
    }

    private Zeile statLine(StatWert s, int level) {
        if (s.stat < 0 || s.stat >= stats.length) return new Zeile("#stat " + range(s.min, s.max), 0, true);
        Stat st = stats[s.stat];
        String text = null;
        switch (st.func) {
            case 6: case 7: case 8: case 9: {

                int shift = st.op >= 2 && st.op <= 5 ? st.opParam : 0;
                int lo = level > 0 ? (s.min * level) >> shift : (s.min * 1) >> shift;
                int hi = level > 0 ? (s.max * level) >> shift : (s.max * 99) >> shift;
                text = describe(st.func, st.val, st.pos, st.str2, lo, hi);
                break;
            }
            case 11: {
                String f = text("ModStre9u");
                if (f != null && s.max > 0) text = format(f, "1", String.valueOf(100 / s.max));
                break;
            }
            case 13: {
                String c = classText(s.param, 0);
                if (c != null) text = "+" + range(s.min, s.max) + " " + c;
                break;
            }
            case 14: {

                int cls = s.param / 3, tab = s.param % 3;
                String f = classText(cls, 1 + tab), only = classText(cls, 4);
                if (f != null) text = format(f, range(s.min, s.max)) + (only != null ? " " + only : "");
                break;
            }
            case 15: {
                String f = text(st.pos), skill = skillName(s.param);
                if (f != null && skill != null) text = format(f, String.valueOf(s.min), String.valueOf(s.max), skill);
                break;
            }
            case 16: {
                String f = text(st.pos), skill = skillName(s.param);
                if (f != null && skill != null) text = format(f, range(s.min, s.max), skill);
                break;
            }
            case 24: {

                String lvl = text("ModStre10b"), f = text(st.pos), skill = skillName(s.param);
                if (lvl != null && f != null && skill != null)
                    text = lvl + " " + s.max + " " + skill + " " + format(f, String.valueOf(s.min), String.valueOf(s.min));
                break;
            }
            case 27: case 28: {
                String to = text("to"), skill = skillName(s.param);
                if (to != null && skill != null) {
                    text = "+" + range(s.min, s.max) + " " + to + " " + skill;
                    if (st.func == 27) {
                        int cls = s.param >= 0 && s.param < skillClass.length ? skillClass[s.param] : -1;
                        String only = classText(cls, 4);
                        if (only != null) text += " " + only;
                    }
                }
                break;
            }
            case 23: {
                String f = text(st.pos), mon = s.param >= 0 && s.param < monsterName.length ? text(monsterName[s.param]) : null;
                if (f != null && mon != null) text = range(s.min, s.max) + "% " + f + " " + mon;
                break;
            }
            case 0:
                return null;
            default:
                text = describe(st.func, st.val, s.max < 0 ? st.neg : st.pos, st.str2, s.min, s.max);
        }
        if (text == null) return new Zeile("#" + s.stat + " " + range(s.min, s.max), st.prio, true);
        return new Zeile(text, st.prio, false);
    }


    private String describe(int func, int descval, int strId, int str2Id, int min, int max) {
        String s = text(strId);
        if (s == null) return null;
        String s2 = str2Id > 0 ? text(str2Id) : null;
        String r = range(Math.abs(min), Math.abs(max)), sign = max < 0 ? "-" : "+";
        String v;
        switch (func) {
            case 1: case 6: case 12: v = sign + r; break;
            case 2: case 7: v = (max < 0 ? "-" : "") + r + "%"; break;
            case 3: case 9: v = (max < 0 ? "-" : "") + r; break;
            case 4: case 8: v = sign + r + "%"; break;
            case 5: v = range(min * 100 / 128, max * 100 / 128) + "%"; break;
            case 20: v = "-" + r + "%"; break;
            case 19: return format(s, range(min, max));
            default: return null;
        }
        String line = descval == 0 ? s : descval == 2 ? s + " " + v : v + " " + s;
        if ((func >= 6 && func <= 9) && s2 != null) line += " " + s2;
        return line;
    }


    private String text(int id) {
        return clean(texte.get(id));
    }

    private String text(String key) {
        return clean(texte.get(key));
    }

    private static String clean(String t) {
        return t == null ? null : t.replace('\n', ' ').trim();
    }

    private String classText(int cls, int which) {
        if (cls < 0 || cls >= classTexts.length) return null;
        return text(classTexts[cls][which]);
    }

    private String skillName(int skill) {
        return skill >= 0 && skill < skillName.length && skillName[skill] >= 0 ? text(skillName[skill]) : null;
    }

    private int prio(int stat) {
        return stat >= 0 && stat < stats.length ? stats[stat].prio : 0;
    }

    private boolean validStat(int stat) {
        return stat >= 0 && stat < stats.length;
    }

    private static StatWert find(List<StatWert> sw, int stat) {
        for (StatWert s : sw) if (s.stat == stat) return s;
        return null;
    }

    private static Zeile raw(Wert w) {
        return new Zeile("#" + w.property + " " + range(w.min, w.max), 0, true);
    }


    static String range(int min, int max) {
        return min == max ? String.valueOf(min) : Math.min(min, max) + "-" + Math.max(min, max);
    }





    static String format(String f, String... values) {
        StringBuilder b = new StringBuilder();
        int next = 0;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c != '%' || i + 1 >= f.length()) {
                b.append(c);
                continue;
            }
            char d = f.charAt(i + 1);
            if (d == '%') {
                b.append('%');
                i++;
                continue;
            }
            boolean plus = d == '+';
            int j = plus ? i + 2 : i + 1;
            if (j < f.length() && (f.charAt(j) == 'd' || f.charAt(j) == 's' || f.charAt(j) == 'u' || f.charAt(j) == 'i')) {
                String v = next < values.length ? values[next++] : "";
                if (plus && !v.startsWith("-") && !v.startsWith("+")) b.append('+');
                b.append(v);
                i = j;
            }
            else b.append(c);
        }
        return b.toString();
    }

    static int count(byte[] b, int record) {
        if (b == null || b.length < 4) return -1;
        int n = D2Tabellen.s32(b, 0);
        if (n <= 0) return -1;
        if (record < 0) return (b.length - 4) % n == 0 ? n : -1;
        return (long) n * record + 4 == b.length ? n : -1;
    }
}
