
#define ORD_GetBaseStat     10216
#define RVA_SkillTab        0xF6EE0
#define RVA_SkillTabCheck   0x30A07
#define RVA_SkillColumnX    0x2F100
#define RVA_SkillRowY       0x2F150
#define RVA_ScreenW         0xF7034
#define RVA_ScreenH         0xF7038
#define RVA_PanelOffX       0x11D354
#define RVA_PanelOffY       0x11D358

#define CAP_POINTS          0x01
#define CAP_COMPARE         0x02
#define CAP_DAMAGE          0x08
#define CAP_LOOT            0x20
#define CAP_TELEPORT        0x80
#define CAP_VALUE           0x40
#define CAP2_TELE_HOVER     0x02
static BYTE diffCapabilities2(void);
static DWORD damageDps(void);
static int teleHoverCapable(void);
static BYTE effectsCapabilities2(void);
static const DWORD XP_LEVEL[99] = {
    0u, 500u, 1500u, 3750u, 7875u, 14175u,
    22680u, 32886u, 44396u, 57715u, 72144u, 90180u,
    112725u, 140906u, 176132u, 220165u, 275207u, 344008u,
    430010u, 537513u, 671891u, 839864u, 1049830u, 1312287u,
    1640359u, 2050449u, 2563061u, 3203826u, 3902260u, 4663553u,
    5493363u, 6397855u, 7383752u, 8458379u, 9629723u, 10906488u,
    12298162u, 13815086u, 15468534u, 17270791u, 19235252u, 21376515u,
    23710491u, 26254525u, 29027522u, 32050088u, 35344686u, 38935798u,
    42850109u, 47116709u, 51767302u, 56836449u, 62361819u, 68384473u,
    74949165u, 82104680u, 89904191u, 98405658u, 107672256u, 117772849u,
    128782495u, 140783010u, 153863570u, 168121381u, 183662396u, 200602101u,
    219066380u, 239192444u, 261129853u, 285041630u, 311105466u, 339515048u,
    370481492u, 404234916u, 441026148u, 481128591u, 524840254u, 572485967u,
    624419793u, 681027665u, 742730244u, 809986056u, 883294891u, 963201521u,
    1050299747u, 1145236814u, 1248718217u, 1361512946u, 1484459201u, 1618470619u,
    1764543065u, 1923762030u, 2097310703u, 2286478756u, 2492671933u, 2717422497u,
    2962400612u, 3229426756u, 3520485254u,
};

static void putExperience(Packet* p, void* hero) {
    DWORD xp = hero ? fGetUnitStat(hero, 13, 0) : 0, level = hero ? fGetUnitStat(hero, 12, 0) : 0;
    DWORD from = 0xFFFFFFFF, to = 0xFFFFFFFF;
    if (level >= 1 && level <= 99) {
        from = XP_LEVEL[level - 1];
        to = level < 99 ? XP_LEVEL[level] : 0xFFFFFFFF;
        if (xp < from || (level < 99 && xp >= to)) from = to = 0xFFFFFFFF;
    }
    putU32(p, xp);
    putU32(p, from);
    putU32(p, to);
}

static BYTE capabilities2(void) {
    return diffCapabilities2()
        | (teleHoverCapable() ? CAP2_TELE_HOVER : 0)
        | effectsCapabilities2()
        | 8
        | 16;
}

#define ITYPE_BOW           27
#define ITYPE_CROSSBOW      35
#define ITYPE_THROWPOTION   38
#define STATE_WEREWOLF      139
#define STATE_WEREBEAR      140
#define ATTACK_STRIKES      0
#define ATTACK_SHOOTS       1
#define ATTACK_UNKNOWN      0xFF
static BYTE attackNow = ATTACK_UNKNOWN;

static BYTE attackKind(void* player) {
    void* inventory = player ? RDP(player, U_INVENTORY) : NULL;
    if (!fItemIsType || !readable(inventory, 0x10)) return ATTACK_UNKNOWN;
    if (fGetUnitState && (fGetUnitState(player, STATE_WEREWOLF) || fGetUnitState(player, STATE_WEREBEAR)))
        return ATTACK_STRIKES;
    int steps = 0;
    for (void* item = RDP(inventory, INV_FIRST); item && steps < 512; steps++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) return ATTACK_UNKNOWN;
        BYTE location = RD8(data, IDATA_BODYLOC);
        if (RD8(data, IDATA_NODEPAGE) == 3 && (location == 4 || location == 5) && !(RD32(data, IDATA_FLAGS) & 0x4000)
            && (fItemIsType(item, ITYPE_BOW) || fItemIsType(item, ITYPE_CROSSBOW) || fItemIsType(item, ITYPE_THROWPOTION)))
            return ATTACK_SHOOTS;
        item = RDP(data, IDATA_NEXTINV);
    }
    return ATTACK_STRIKES;
}

static int damageCapable(void);
static int valueCapable(void);
#define PTS_SKILLTREE       0x01
#define PTS_BASESTATS       0x02
#define PTS_STAT            0x04
#define PTS_SKILL           0x08
#define PTS_KEY             0x10
#define PTS_STATSPAGE       0x20

#define CMD_POINTS          19
#define RVA_StatSite        0xC06D1
#define RVA_SkillSite       0x30B21
#define RVA_Send3           0xB6B00
#define RVA_HotkeySite      0xA5EE7
#define RVA_SetHotkey       0xA5A20
#define RVA_HotkeyItem      0x1014B0
#define RVA_CharScreenCall  0x1D548
#define RVA_CharScreenPage  0xBF640
#define RVA_PlugYPrintPage  0x19AE0
#define RVA_PlugYRealm      0x31FCA
#define RVA_PlugYPage       0x32014
#define RVA_PlugYLastPage   0x32018
#define STATSPAGE_UNKNOWN   0xFF

static int singlePlayerGame(void);

typedef DWORD (__stdcall *GetBaseStat_t)(void*, DWORD, DWORD);
static GetBaseStat_t fGetBaseStat;
static int skillTreeKnown;
static BYTE pointOps;

typedef struct {
    BYTE level, tab;
    WORD statPoints, skillPoints, stats[4], first;
    BYTE levels[30];
    BYTE statsPage, spare;
} PointsState;
static PointsState pointsNow;
static BYTE pointsCounter;
static int pointsHave;

static int sameAbs(const BYTE* at, DWORD rva) {
    return *(const DWORD*) at == client + rva;
}

static void checkPointSites(void) {
    fGetBaseStat = (GetBaseStat_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetBaseStat);
    static const BYTE col1[] = {0x3C, 0x01, 0x75, 0x11, 0xA1};
    static const BYTE col2[] = {0x2B, 0x05};
    static const BYTE col3[] = {0x2D, 0x31, 0x01, 0x00, 0x00, 0xC3};
    static const BYTE row1[] = {0x3C, 0x01, 0x75, 0x13, 0xA1};
    static const BYTE row2[] = {0x8B, 0x0D};
    static const BYTE row3[] = {0x8D, 0x84, 0x01, 0x5E, 0xFE, 0xFF, 0xFF, 0xC3};
    const BYTE* c = (const BYTE*) (client + RVA_SkillColumnX);
    const BYTE* r = (const BYTE*) (client + RVA_SkillRowY);
    const BYTE* t = (const BYTE*) (client + RVA_SkillTabCheck);
    skillTreeKnown = readable(c, 21) && readable(r, 23) && readable(t, 6)
        && memcmp(c, col1, 5) == 0 && sameAbs(c + 5, RVA_ScreenW) && memcmp(c + 9, col2, 2) == 0
        && sameAbs(c + 11, RVA_PanelOffX) && memcmp(c + 15, col3, 6) == 0
        && memcmp(r, row1, 5) == 0 && sameAbs(r + 5, RVA_ScreenH) && memcmp(r + 9, row2, 2) == 0
        && sameAbs(r + 11, RVA_PanelOffY) && memcmp(r + 15, row3, 8) == 0
        && t[0] == 0x39 && t[1] == 0x0D && sameAbs(t + 2, RVA_SkillTab);
    static const BYTE stat[] = {0xBB, 0x03, 0x00, 0x00, 0x00, 0xC6, 0x44, 0x24, 0x14, 0x3A};
    static const BYTE skill[] = {0x8B, 0xCE, 0xB0, 0x3B, 0xE8};
    static const BYTE key[] = {0xBB, 0x09, 0x00, 0x00, 0x00, 0xC6, 0x44, 0x24, 0x0C, 0x51};
    const BYTE* a = (const BYTE*) (client + RVA_StatSite);
    const BYTE* b = (const BYTE*) (client + RVA_SkillSite);
    const BYTE* k = (const BYTE*) (client + RVA_HotkeySite);
    pointOps = 0;
    if (fSendPacket && readable(a, sizeof(stat)) && memcmp(a, stat, sizeof(stat)) == 0) pointOps |= PTS_STAT;
    if (fSendPacket && readable(b, 9) && memcmp(b, skill, sizeof(skill)) == 0
        && (DWORD) b + 9 + *(const DWORD*) (b + 5) == client + RVA_Send3) pointOps |= PTS_SKILL;
    const BYTE* h = (const BYTE*) (client + RVA_SetHotkey);
    int setter = readable(h, 28) && h[0] == 0x89 && h[1] == 0x0C && h[2] == 0x85 && sameAbs(h + 3, RVA_HotkeySkill)
        && h[7] == 0x8B && h[8] == 0x4C && h[9] == 0x24 && h[10] == 0x04 && h[11] == 0x89 && h[12] == 0x14
        && h[13] == 0x85 && sameAbs(h + 14, RVA_HotkeyLeft) && h[18] == 0x89 && h[19] == 0x0C && h[20] == 0x85
        && sameAbs(h + 21, RVA_HotkeyItem) && h[25] == 0xC2 && h[26] == 0x04 && h[27] == 0x00;
    if (fSendPacket && setter && readable(k, sizeof(key)) && memcmp(k, key, sizeof(key)) == 0) pointOps |= PTS_KEY;
    logLine("points: skill tree sites %s, GetBaseStat %s, packet sites:%s%s%s", skillTreeKnown ? "verified" : "not as expected",
        fGetBaseStat ? "found" : "missing", pointOps & PTS_STAT ? " stat" : "", pointOps & PTS_SKILL ? " skill" : "",
        pointOps & PTS_KEY ? " hotkey" : "");
}

static int statsPageState;
static DWORD statsPageHook;
static BYTE* statsPagePlugY;

static BYTE statsPage(void) {
    const BYTE* c = (const BYTE*) (client + RVA_CharScreenCall);
    if (!readable(c, 5) || c[0] != 0xE8) return STATSPAGE_UNKNOWN;
    DWORD target = (DWORD) c + 5 + *(const DWORD*) (c + 1);
    if (target == client + RVA_CharScreenPage) return 0;
    if (statsPageHook != target) {
        BYTE* plugy = plugyLoaded ? (BYTE*) GetModuleHandleA("PlugY.dll") : NULL;
        static const BYTE head[] = {0x55, 0x8B, 0xEC, 0x83, 0xE4, 0xF8, 0x80, 0x3D};
        static const BYTE realm[] = {0x00, 0x75, 0x3C, 0xA1};
        static const BYTE pages[] = {0x85, 0xC0, 0x7E, 0x0D, 0x3B, 0xC1, 0x7F, 0x09, 0xE8};
        static const BYTE runewords[] = {0x8D, 0x51, 0x01, 0x3B, 0xC2, 0x75, 0x09, 0xE8};
        static const BYTE newStats[] = {0x83, 0xC1, 0x02, 0x3B, 0xC1, 0x75, 0x09, 0xE8};
        const BYTE* f = (const BYTE*) target;
        int ok = plugy && target == (DWORD) plugy + RVA_PlugYPrintPage && readable(f, 0x43)
            && memcmp(f, head, sizeof(head)) == 0
            && *(const DWORD*) (f + 8) == (DWORD) plugy + RVA_PlugYRealm && memcmp(f + 12, realm, sizeof(realm)) == 0
            && *(const DWORD*) (f + 16) == (DWORD) plugy + RVA_PlugYPage && f[20] == 0x8B && f[21] == 0x0D
            && *(const DWORD*) (f + 22) == (DWORD) plugy + RVA_PlugYLastPage && memcmp(f + 26, pages, sizeof(pages)) == 0
            && memcmp(f + 0x2B, runewords, sizeof(runewords)) == 0 && memcmp(f + 0x3B, newStats, sizeof(newStats)) == 0
            && readable(plugy + RVA_PlugYRealm, 1) && readable(plugy + RVA_PlugYPage, 8);
        statsPageHook = target;
        statsPageState = ok ? 1 : -1;
        statsPagePlugY = plugy;
        logLine(ok ? "points: PlugY's stats pages verified" : "points: PlugY's stats pages not as expected (page unknown)");
    }
    if (statsPageState < 0) return STATSPAGE_UNKNOWN;
    BYTE* plugy = statsPagePlugY;
    if (*(volatile BYTE*) (plugy + RVA_PlugYRealm)) return 0;
    int page = *(volatile int*) (plugy + RVA_PlugYPage), last = *(volatile int*) (plugy + RVA_PlugYLastPage);
    if ((page > 0 && page <= last) || page == last + 1 || page == last + 2) {
        return (BYTE) (page < 1 ? 1 : page > 254 ? 254 : page);
    }
    return 0;
}

static int ownSkill(void* player, WORD skill) {
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) return 0;
    int guard = 0;
    for (void* k = RDP(info, INFO_FIRST); readable(k, SKILL_ITEM + 4) && guard < 400; k = RDP(k, SKILL_NEXT), guard++) {
        if (skillId(k) == skill && RD32(k, SKILL_ITEM) == 0xFFFFFFFF) return 1;
    }
    return 0;
}

static int passiveSkill(void* player, WORD skill) {
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) return 0;
    int guard = 0;
    for (void* k = RDP(info, INFO_FIRST); readable(k, SKILL_ITEM + 4) && guard < 400; k = RDP(k, SKILL_NEXT), guard++) {
        if (skillId(k) != skill || RD32(k, SKILL_ITEM) != 0xFFFFFFFF) continue;
        const BYTE* txt = (const BYTE*) RDP(k, 0);
        return readable(txt, 8) && (*(const DWORD*) (txt + 4) & 0x10) != 0;
    }
    return 0;
}

static void pointsCommand(void* player, const BYTE* buf, int n) {
    int kind = buf[8];
    WORD what = n >= 11 ? *(const WORD*) (buf + 9) : 0xFFFF;
    int key = n >= 12 ? buf[11] : -1;
    int move = kind == 2 && n >= 13 && buf[12] == 1;
    const char* why = NULL;
    if (kind > 2) why = "no such kind";
    else if (pauseApplied) why = "paused";
    else if (!singlePlayerGame()) why = "not single player";
    else if (!(pointOps & (PTS_STAT << kind))) why = "site not verified";
    else if (kind == 0 && what > 3) why = "no such stat";
    else if (kind == 0 && fGetUnitStat(player, 4, 0) == 0) why = "no free stat point";
    else if (kind == 1 && (what == 0 || !classSkill(RD32(player, U_TXTNO), what))) why = "not in the class's tree";
    else if (kind == 1 && fGetUnitStat(player, 5, 0) == 0) why = "no free skill point";
    else if (kind == 2 && (key < 0 || key > 15)) why = "no such key";
    else if (kind == 2 && !ownSkill(player, what)) why = "not learnt";
    else if (kind == 2 && passiveSkill(player, what)) why = "passive";
    else if (kind == 2 && !move) {
        DWORD held = RD32(client, RVA_HotkeySkill + key * 4);
        if (held < 0xFFFF && held != what) why = "key holds another skill";
        for (int i = 0; i < 16 && !why; i++) {
            if (i != key && RD32(client, RVA_HotkeySkill + i * 4) == what) why = "already on another key";
        }
    }
    if (why) {
        logLine("points: command 19 kind %d %u key %d refused: %s", kind, what, key, why);
        return;
    }
    if (kind == 0) {
        BYTE packet[3] = {0x3A, (BYTE) what, 0x00};
        fSendPacket(3, 1, packet);
    }
    else if (kind == 1) {
        BYTE packet[3] = {0x3B, (BYTE) what, (BYTE) (what >> 8)};
        fSendPacket(3, 1, packet);
    }
    else {
        DWORD* skills = (DWORD*) (client + RVA_HotkeySkill);
        DWORD* lefts = (DWORD*) (client + RVA_HotkeyLeft);
        DWORD* items = (DWORD*) (client + RVA_HotkeyItem);
        if (move && skills[key] != 0xFFFFFFFF) {
            BYTE clear[9] = {0x51};
            *(DWORD*) (clear + 1) = 0xFFFF | (DWORD) key << 16;
            *(DWORD*) (clear + 5) = 0xFFFFFFFF;
            fSendPacket(9, 1, clear);
        }
        BYTE packet[9] = {0x51};
        *(DWORD*) (packet + 1) = what | (DWORD) key << 16;
        *(DWORD*) (packet + 5) = 0xFFFFFFFF;
        skills[key] = what;
        lefts[key] = 0;
        items[key] = 0xFFFFFFFF;
        fSendPacket(9, 1, packet);
        for (int i = 0; move && i < 16; i++) {
            if (i == key || skills[i] != what || items[i] != 0xFFFFFFFF) continue;
            skills[i] = lefts[i] = items[i] = 0xFFFFFFFF;
            BYTE clear[9] = {0x51};
            *(DWORD*) (clear + 1) = 0xFFFF | (DWORD) i << 16;
            *(DWORD*) (clear + 5) = 0xFFFFFFFF;
            fSendPacket(9, 1, clear);
        }
    }
    logLine("points: command 19 kind %d %u key %d%s sent", kind, what, key, move ? " (move)" : "");
}

static int skillTreeEdge(DWORD rva, int n) {
    DWORD a = (DWORD) n;
    __asm__ volatile ("call *%1" : "+a"(a) : "r"(client + rva) : "ecx", "edx", "memory", "cc");
    return (int) a;
}

static WORD clampWord(DWORD v) {
    return v > 0xFFFF ? 0xFFFF : (WORD) v;
}

static void readPoints(void* player, PointsState* s) {
    static const WORD first[7] = {6, 36, 66, 96, 126, 221, 251};
    memset(s, 0, sizeof(*s));
    DWORD level = fGetUnitStat(player, 12, 0);
    s->level = (BYTE) (level > 255 ? 255 : level);
    s->statPoints = clampWord(fGetUnitStat(player, 4, 0));
    s->skillPoints = clampWord(fGetUnitStat(player, 5, 0));
    for (int i = 0; i < 4 && fGetBaseStat; i++) s->stats[i] = clampWord(fGetBaseStat(player, (DWORD) i, 0));
    DWORD cls = RD32(player, U_TXTNO);
    s->first = cls < 7 ? first[cls] : 0xFFFF;
    void* info = RDP(player, U_INFO);
    if (cls < 7 && readable(info, 0x10)) {
        int guard = 0;
        for (void* k = RDP(info, INFO_FIRST); readable(k, SKILL_ITEM + 4) && guard < 400; k = RDP(k, SKILL_NEXT), guard++) {
            WORD id = skillId(k);
            if (id < s->first || id >= s->first + 30 || RD32(k, SKILL_ITEM) != 0xFFFFFFFF) continue;
            DWORD base = RD32(k, SKILL_LEVEL);
            s->levels[id - s->first] = (BYTE) (base > 255 ? 255 : base);
        }
    }
    DWORD tab = skillTreeKnown ? RD32(client, RVA_SkillTab) : 0;
    s->tab = (BYTE) (tab >= 1 && tab <= 3 ? tab : 0);
    s->statsPage = statsPage();
}

static int pointsTick(void* player) {
    BYTE attack = attackKind(player);
    int attackChanged = attack != attackNow;
    if (attackChanged) trace("attack kind %u (0 strikes, 1 shoots, 255 unknown)", attack);
    attackNow = attack;
    PointsState s;
    readPoints(player, &s);
    if (pointsHave && memcmp(&s, &pointsNow, sizeof(s)) == 0) return attackChanged;
    PointsState withoutTab = s;
    withoutTab.tab = pointsNow.tab;
    withoutTab.statsPage = pointsNow.statsPage;
    if (!pointsHave || memcmp(&withoutTab, &pointsNow, sizeof(s)) != 0) {
        pointsCounter++;
        logLine("points: level %u, free %u/%u, base %u/%u/%u/%u (counter %u)", s.level, s.statPoints, s.skillPoints,
            s.stats[0], s.stats[1], s.stats[2], s.stats[3], pointsCounter);
    }
    else trace("points: skill tree tab %u, stats page %u", s.tab, s.statsPage);
    pointsNow = s;
    pointsHave = 1;
    return 1;
}

static void putPointsBlock(Packet* p) {
    int lengthAt = p->len;
    putU8(p, 0);
    putU8(p, (pointOps ? CAP_POINTS : 0) | (compareAvailable ? CAP_COMPARE : 0) | (equipAvailable ? CAP_EQUIP : 0)
        | (damageCapable() ? CAP_DAMAGE : 0) | (equipAvailable ? CAP_SORT : 0)
        | (labelFilterAvailable && labelsAvailable ? CAP_LOOT : 0) | (valueCapable() ? CAP_VALUE : 0) | CAP_TELEPORT);
    putU8(p, pointsCounter);
    putU8(p, (skillTreeKnown ? PTS_SKILLTREE : 0) | (fGetBaseStat ? PTS_BASESTATS : 0) | pointOps
        | (pointsNow.statsPage != STATSPAGE_UNKNOWN ? PTS_STATSPAGE : 0));
    putU8(p, pointsNow.level);
    putU16(p, pointsNow.statPoints);
    putU16(p, pointsNow.skillPoints);
    for (int i = 0; i < 4; i++) putU16(p, pointsNow.stats[i]);
    putU16(p, pointsNow.first);
    for (int i = 0; i < 30; i++) putU8(p, pointsNow.levels[i]);
    putU8(p, pointsNow.tab);
    for (int i = 1; i <= 3; i++) putU16(p, (WORD) (skillTreeKnown ? skillTreeEdge(RVA_SkillColumnX, i) : 0));
    for (int i = 1; i <= 6; i++) putU16(p, (WORD) (skillTreeKnown ? skillTreeEdge(RVA_SkillRowY, i) : 0));
    putU8(p, equipCounter);
    putU8(p, equipResult);
    putU8(p, sortCounter);
    putU8(p, sortStateByte(fGetPlayerUnit()));
    putU8(p, sortMoves);
    putU8(p, pointsNow.statsPage);
    putU8(p, capabilities2());
    putResumeFields(p);
    putU8(p, attackNow);
    void* hero = fGetPlayerUnit();
    putU16(p, (WORD) damageDps());
    putU32(p, hero ? fGetUnitStat(hero, 14, 0) : 0);
    putU32(p, hero ? fGetUnitStat(hero, 15, 0) : 0);
    putExperience(p, hero);
    p->data[lengthAt] = (BYTE) (p->len - lengthAt - 1);
}
