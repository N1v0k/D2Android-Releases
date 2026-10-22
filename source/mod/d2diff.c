
#define CMD_FRAMES       28
#define FRAMES_HOLD_MS   3000
#define FRAMES_LEN       132
#define ITXT_WCLASS      0xC0
#define ITXT_WCLASS2     0xC4
#define ITXT_REQSTR      0x10A
#define ITXT_REQDEX      0x10C
#define ITXT_2HANDED     0x11C
#define ITXT_LEVELREQ    0x13F
#define IFLAG_NOEQUIP    0x4000
#define STATE_WOLF       139
#define STATE_BEAR       140
#define DIFF_FORMS_VERIFIED 1
#define CAP2_DIFF        0x01

#include "d2diffrules.c"

static const char diffWeaponClasses[14][4] = {"hth", "1hs", "2hs", "1ht", "2ht", "stf", "bow", "xbw", "ht1", "ht2", "1js",
    "1jt", "1ss", "1st"};
static const int diffStatIds[] = {1, 3, 7, 9, 39, 40, 41, 42, 43, 44, 45, 46, 76, 77, 99, 102, 105, 216, 217,
    0, 2, 21, 22, 23, 24, 48, 49, 50, 51, 52, 53, 54, 55, 121, 122};
#define ITXT_STRBONUS 0x106
#define ITXT_DEXBONUS 0x108
#define ITXT_2HMINDAM_DIFF 0x102

static int diffDamageStat(int id) {
    return (id >= 21 && id <= 24) || (id >= 48 && id <= 55) || id == 121 || id == 122;
}

typedef struct {
    int have, klasse, flags, lifePerVit, manaPerEnergy;
    DiffAnim anim[3][14];
    DWORD until;
} DiffFrames;

static DiffFrames diffFrameTables[3];
static int diffAvailable, diffOn;
static DWORD diffUntil;
static wchar_t diffBlock[DIFF_LINES * (DIFF_LINE_CHARS + 1) + 64];

static void checkDiffSites(void) {
    BYTE* txt = compareAvailable && fGetItemText ? (BYTE*) fGetItemText(0) : NULL;
    int items = readable(txt, ITXT_LEVELREQ + 1) && !memcmp(txt + ITXT_CODE, "hax ", 4) && !memcmp(txt + ITXT_WCLASS, "1hs", 3)
        && !memcmp(txt + ITXT_WCLASS2, "1hs", 3) && txt[ITXT_2HANDED] == 0;
    diffAvailable = compareAvailable && items && fBodyLocItem && fGetUnitStat && fGetBaseStat && fItemIsType;
    logLine(diffAvailable ? "diff sites verified" : "diff sites not as expected (tooltip %d, items.txt %d): no comparison lines",
        compareAvailable, items);
}

static BYTE diffCapabilities2(void) {
    return diffAvailable ? CAP2_DIFF : 0;
}

static void framesCommand(const BYTE* buf, int n) {
    if (n < 8 + FRAMES_LEN || buf[8] != 1 || buf[9] > 6 || buf[10] > 2) {
        static DWORD lastBad;
        if (GetTickCount() - lastBad > 10000) logLine("diff: command 28 of %d bytes not understood", n);
        lastBad = GetTickCount();
        return;
    }
    DiffFrames t;
    memset(&t, 0, sizeof(t));
    t.have = 1;
    t.klasse = buf[9];
    t.flags = buf[11];
    t.lifePerVit = buf[12];
    t.manaPerEnergy = buf[13];
    const BYTE* p = buf + 14;
    for (int mode = 0; mode < 3; mode++)
        for (int w = 0; w < 14; w++, p += 3) {
            t.anim[mode][w].fpd = p[0];
            t.anim[mode][w].speed = p[1] | p[2] << 8;
        }
    DiffFrames* slot = &diffFrameTables[buf[10]];
    if (!slot->have || slot->klasse != t.klasse || memcmp(slot->anim, t.anim, sizeof(t.anim)) || slot->flags != t.flags)
        logLine("diff: frames for class %d form %d (flags %d)", t.klasse, buf[10], t.flags);
    t.until = GetTickCount() + FRAMES_HOLD_MS;
    *slot = t;
}

static int diffWanted(DWORD now) {
    diffOn = diffAvailable && (int) (now - diffUntil) < 0;
    return diffOn;
}

static void diffItemStats(void* item, int* out, int damage) {
    void* data = RDP(item, U_DATA);
    if (!readable(data, 0x20) || (RD32(data, IDATA_FLAGS) & IFLAG_NOEQUIP)) return;
    for (unsigned i = 0; i < sizeof(diffStatIds) / sizeof(diffStatIds[0]); i++)
        if (damage || !diffDamageStat(diffStatIds[i]))
            out[diffStatIds[i]] += (int) fGetUnitStat(item, (DWORD) diffStatIds[i], 0);
}

static void diffWeaponInfo(void* weapon, int* out) {
    BYTE* txt = weapon ? (BYTE*) fGetItemText(RD32(weapon, U_TXTNO)) : NULL;
    if (!readable(txt, ITXT_DEXBONUS + 2)) return;
    out[DS_W_IS] = 1;
    out[DS_W_TWO] = txt[ITXT_2HMINDAM_DIFF] != 0 || txt[ITXT_2HMINDAM_DIFF + 1] != 0;
    out[DS_W_STRB] = *(WORD*) (txt + ITXT_STRBONUS);
    out[DS_W_DEXB] = *(WORD*) (txt + ITXT_DEXBONUS);
}

static int diffWeaponClass(void* item, int twoHands) {
    BYTE* txt = item ? (BYTE*) fGetItemText(RD32(item, U_TXTNO)) : NULL;
    if (!readable(txt, ITXT_WCLASS2 + 4)) return 0;
    const BYTE* c = txt + (twoHands ? ITXT_WCLASS2 : ITXT_WCLASS);
    for (int i = 0; i < 14; i++) if (!memcmp(c, diffWeaponClasses[i], 3)) return i;
    return 0;
}

static int diffTwoHanded(void* item) {
    BYTE* txt = item ? (BYTE*) fGetItemText(RD32(item, U_TXTNO)) : NULL;
    return readable(txt, ITXT_2HANDED + 1) && txt[ITXT_2HANDED] != 0;
}

static int diffScore(const DiffResult* r) {
    int s = (r->lifeAfter - r->lifeBefore) * 2 + (r->manaAfter - r->manaBefore);
    for (int i = 0; i < 4; i++) s += 2 * (r->resAfter[i] - r->resBefore[i]);
    for (int k = 0; k < 3; k++)
        if (r->framesBefore[k] >= 0 && r->framesAfter[k] >= 0) s += 20 * (r->framesBefore[k] - r->framesAfter[k]);
    return s;
}

static int diffRequirement(void* player, void* item, int* need) {
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    void* data = RDP(item, U_DATA);
    if (!readable(txt, ITXT_LEVELREQ + 1) || !readable(data, 0x20)) return 0;
    int ethereal = (RD32(data, IDATA_FLAGS) & IFLAG_ETHEREAL) != 0;
    int str = *(WORD*) (txt + ITXT_REQSTR) - (ethereal ? 10 : 0), dex = *(WORD*) (txt + ITXT_REQDEX) - (ethereal ? 10 : 0);
    int level = txt[ITXT_LEVELREQ];
    void* sockets = RDP(item, U_INVENTORY);
    int guard = 0;
    for (void* s = readable(sockets, 0x10) ? RDP(sockets, INV_FIRST) : NULL; readable(s, 0x18) && guard < 8; guard++) {
        BYTE* st = (BYTE*) fGetItemText(RD32(s, U_TXTNO));
        if (readable(st, ITXT_LEVELREQ + 1) && st[ITXT_LEVELREQ] > level) level = st[ITXT_LEVELREQ];
        void* sd = RDP(s, U_DATA);
        s = readable(sd, 0x70) ? RDP(sd, IDATA_NEXTINV) : NULL;
    }
    if ((int) fGetUnitStat(player, 0, 0) < str) { *need = str; return 1; }
    if ((int) fGetUnitStat(player, 2, 0) < dex) { *need = dex; return 2; }
    if ((int) fGetUnitStat(player, 12, 0) < level) { *need = level; return 3; }
    return (RD32(data, IDATA_FLAGS) & IFLAG_NOEQUIP) ? 4 : 0;
}

static int diffBuild(void* player, void* item) {
    void* inventory = RDP(player, U_INVENTORY);
    if (!inventory) return 0;
    int form = DIFF_FORMS_VERIFIED && fGetUnitState
        ? (fGetUnitState(player, STATE_WOLF) ? 1 : fGetUnitState(player, STATE_BEAR) ? 2 : 0) : 0;
    DiffFrames* t = &diffFrameTables[form];
    if (!t->have || (int) (GetTickCount() - t->until) >= 0 || t->klasse != (int) RD32(player, U_TXTNO)) return 0;
    void* partners[2] = {NULL, NULL};
    int count = compareSlots(item, inventory, partners);
    int itemIsWeapon = fItemIsType(item, ITYPE_WEAPON), itemTwoHands = itemIsWeapon && diffTwoHanded(item);
    static const DWORD wornTypes[] = {ITYPE_HELM, ITYPE_AMULET, ITYPE_ARMOR, ITYPE_BELT, ITYPE_BOOTS, ITYPE_GLOVES,
        ITYPE_RING, ITYPE_WEAPON, ITYPE_SHIELD, ITYPE_BOWQUIVER, ITYPE_XBOWQUIVER};
    int wearable = 0;
    for (unsigned i = 0; i < sizeof(wornTypes) / sizeof(wornTypes[0]); i++) wearable |= fItemIsType(item, wornTypes[i]) != 0;
    if (!wearable) return 0;
    int ringFree = fItemIsType(item, ITYPE_RING) && count < 2;
    void* right = fBodyLocItem(inventory, 4), *left = fBodyLocItem(inventory, 5);
    void* wornTwoHands = right && fItemIsType(right, ITYPE_WEAPON) && diffTwoHanded(right) ? right
        : left && fItemIsType(left, ITYPE_WEAPON) && diffTwoHanded(left) ? left : NULL;
    int itemIsOffhand = !itemIsWeapon && (fItemIsType(item, ITYPE_SHIELD) || fItemIsType(item, ITYPE_BOWQUIVER)
        || fItemIsType(item, ITYPE_XBOWQUIVER));
    if (itemIsOffhand && wornTwoHands) return 0;
    void* removed[2] = {NULL, NULL};
    int choices = 1;
    if (itemTwoHands) {
        removed[0] = right;
        removed[1] = left;
    }
    else if (count == 2) choices = 2;
    else if (!ringFree) removed[0] = partners[0];
    DiffResult best;
    memset(&best, 0, sizeof(best));
    int bestScore = 0, have = 0;
    for (int c = 0; c < choices; c++) {
        void* gone[2] = {removed[0], removed[1]};
        if (choices == 2) gone[0] = partners[c], gone[1] = NULL;
        static DiffFigure f;
        memset(&f, 0, sizeof(f));
        f.level = (int) fGetUnitStat(player, 12, 0);
        f.difficulty = questSite == 1 ? RD8(client, RVA_QuestDifficulty) : 0;
        if (f.difficulty > 2) f.difficulty = 0;
        f.baseLife = (long long) fGetBaseStat(player, 7, 0);
        f.baseMana = (long long) fGetBaseStat(player, 9, 0);
        f.lifePerVit = t->lifePerVit;
        f.manaPerEnergy = t->manaPerEnergy;
        for (unsigned i = 0; i < sizeof(diffStatIds) / sizeof(diffStatIds[0]); i++) {
            int id = diffStatIds[i];
            if (id != 1 && id != 3 && id != 7 && id != 9) f.stats[id] = (int) fGetBaseStat(player, (DWORD) id, 0);
        }
        for (DWORD loc = 1; loc <= 10; loc++) {
            void* worn = fBodyLocItem(inventory, loc);
            if (!worn || worn == gone[0] || worn == gone[1]) continue;
            diffItemStats(worn, f.stats, !fItemIsType(worn, ITYPE_WEAPON));
        }
        int guard = 0;
        for (void* it = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; readable(it, 0x18) && guard < 512; guard++) {
            void* d = RDP(it, U_DATA);
            if (!readable(d, 0x70)) break;
            const char* code = itemCode(it);
            if (RD8(d, IDATA_LOCATION) == 0 && RD8(d, IDATA_NODEPAGE) == 1 && code[0] == 'c' && code[1] == 'm'
                && code[2] >= '1' && code[2] <= '3')
                diffItemStats(it, f.stats, 1);
            it = RDP(d, IDATA_NEXTINV);
        }
        static int wornStats[DIFF_STATS], itemStats[DIFF_STATS];
        memset(wornStats, 0, sizeof(wornStats));
        memset(itemStats, 0, sizeof(itemStats));
        void* wornWeapon = NULL;
        for (int g = 0; g < 2; g++) {
            if (!gone[g]) continue;
            int compared = itemIsWeapon && !wornWeapon && fItemIsType(gone[g], ITYPE_WEAPON);
            if (compared) wornWeapon = gone[g];
            diffItemStats(gone[g], wornStats, compared || !fItemIsType(gone[g], ITYPE_WEAPON));
        }
        if (wornWeapon) {
            diffWeaponInfo(wornWeapon, wornStats);
            diffWeaponInfo(item, itemStats);
        }
        for (unsigned i = 0; i < sizeof(diffStatIds) / sizeof(diffStatIds[0]); i++)
            itemStats[diffStatIds[i]] += (int) fGetUnitStat(item, (DWORD) diffStatIds[i], 0);
        void* weapon = itemIsWeapon ? item : (right && fItemIsType(right, ITYPE_WEAPON) ? right : NULL);
        int wclass = weapon && form == 0 ? diffWeaponClass(weapon, diffTwoHanded(weapon)) : 0;
        void* offhand = itemIsOffhand ? item : (itemTwoHands ? NULL : left);
        int shield = offhand && fItemIsType(offhand, ITYPE_SHIELD);
        int frames = t->klasse != 5 || DIFF_FORMS_VERIFIED;
        f.anim[0] = (t->flags & 1) || !frames ? (DiffAnim) {0, 0} : t->anim[0][wclass];
        f.anim[1] = frames ? t->anim[1][wclass] : (DiffAnim) {0, 0};
        f.anim[2] = shield && form == 0 && frames ? t->anim[2][wclass] : (DiffAnim) {0, 0};
        DiffResult r;
        diffCompute(&f, wornStats, itemStats, &r);
        int score = diffScore(&r);
        if (!have || score > bestScore) {
            best = r;
            bestScore = score;
            have = 1;
        }
    }
    if (!have) return 0;
    static wchar_t lines[DIFF_LINES][DIFF_LINE_CHARS];
    int german = gameGerman();
    int n = diffLines(&best, german, lines);
    int need = 0, why = diffRequirement(player, item, &need);
    if (why && n < DIFF_LINES) {
        static const wchar_t* const de[5] = {L"", L"Str", L"Ges", L"Stufe", L""};
        static const wchar_t* const en[5] = {L"", L"Str", L"Dex", L"Level", L""};
        lines[n][0] = 0;
        diffPut(lines[n], L"\x00FF" L"c5(");
        if (why == 4) diffPut(lines[n], german ? L"nicht anlegbar" : L"not usable");
        else {
            diffPut(lines[n], german ? de[why] : en[why]);
            diffPut(lines[n], L" ");
            diffNumber(lines[n], need, 0);
        }
        diffPut(lines[n], L")");
        n++;
    }
    if (!n) return 0;
    diffBlock[0] = 0;
    int length = 0;
    for (int i = n - 1; i >= 0; i--) {
        int l = lstrlenW(lines[i]);
        if (length + l + 2 >= (int) (sizeof(diffBlock) / sizeof(diffBlock[0]))) break;
        memcpy(diffBlock + length, lines[i], l * sizeof(wchar_t));
        length += l;
        diffBlock[length++] = L'\n';
        diffBlock[length] = 0;
    }
    static void* lastItem;
    if (item != lastItem) {
        trace("diff: item %.3s, %d line(s): life %d -> %d, mana %d -> %d, frames %d->%d %d->%d %d->%d", itemCode(item), n,
            best.lifeBefore, best.lifeAfter, best.manaBefore, best.manaAfter, best.framesBefore[0], best.framesAfter[0],
            best.framesBefore[1], best.framesAfter[1], best.framesBefore[2], best.framesAfter[2]);
        lastItem = item;
    }
    return length;
}

static int diffLinesAdd(void) {
    if (!diffOn || !hoverText || !hoverText[0]) return 0;
    if (RD32(client, RVA_HoverGrid) != 1 || RD32(client, RVA_HoverBody) != 0) return 0;
    void* item = RDP(client, RVA_HoverItem);
    void* player = fGetPlayerUnit();
    if (!player || !readable(item, 0x18) || RD32(item, U_TYPE) != UNIT_ITEM || RD32(item, U_MODE) != 0) return 0;
    static void* cachedItem;
    static DWORD cachedId, cachedAt;
    static int cachedLength;
    DWORD now = GetTickCount();
    if (item != cachedItem || RD32(item, U_ID) != cachedId || now - cachedAt >= 250) {
        cachedLength = diffBuild(player, item);
        cachedItem = item;
        cachedId = RD32(item, U_ID);
        cachedAt = now;
    }
    int n = cachedLength;
    int length = lstrlenW(hoverText);
    if (n <= 0 || length + n >= HOVER_CHARS) return 0;
    memmove(hoverText + n, hoverText, (length + 1) * sizeof(wchar_t));
    memcpy(hoverText, diffBlock, n * sizeof(wchar_t));
    return n;
}

static void diffLinesRemove(int added) {
    if (added <= 0 || memcmp(hoverText, diffBlock, added * sizeof(wchar_t))) return;
    memmove(hoverText, hoverText + added, (lstrlenW(hoverText + added) + 1) * sizeof(wchar_t));
}
