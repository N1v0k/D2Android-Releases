





















#define SEL_CACHE 512

static struct {
    DWORD key[SEL_CACHE];
    DWORD id[SEL_CACHE];


    void* unit[SEL_CACHE];
    BYTE selectable[SEL_CACHE];
    int next;
} selCache;
static DWORD selCacheSeed;

static DWORD selKey(void* unit) {
    return ((RD32(unit, U_TYPE) & 0xF) << 28 | (RD32(unit, U_MODE) & 0xFF) << 20 | (RD32(unit, U_ID) & 0xFFFFF)) + 1;
}



static int gameSelectable(void* unit) {
    if (selfCheckSite < 0) checkSelfCheckSite();
    if (selfCheckSite != 1) return -1;
    void* act = RDP(unit, U_ACT);
    DWORD seed = readable(act, ACT_SEED + 4) ? RD32(act, ACT_SEED) : 0;
    if (seed != selCacheSeed || selNewGame) {
        memset(&selCache, 0, sizeof(selCache));
        selCacheSeed = seed;
        selNewGame = 0;
    }
    DWORD key = selKey(unit), id = RD32(unit, U_ID);
    if (RD32(client, RVA_PanelSide) == 0) {
        int answer = fUnitSelectable(unit, 0, 0, 0) != 0;
        for (int i = 0; i < SEL_CACHE; i++) {
            if (selCache.key[i] == key && selCache.id[i] == id && selCache.unit[i] == unit) {
                selCache.selectable[i] = (BYTE) answer;
                return answer;
            }
        }
        int i = selCache.next;
        selCache.next = (selCache.next + 1) % SEL_CACHE;
        selCache.key[i] = key;
        selCache.id[i] = id;
        selCache.unit[i] = unit;
        selCache.selectable[i] = (BYTE) answer;
        return answer;
    }
    for (int i = 0; i < SEL_CACHE; i++)
        if (selCache.key[i] == key && selCache.id[i] == id && selCache.unit[i] == unit) return selCache.selectable[i];
    return -1;
}


static int durielAliveNear(void* unit) {
    void* act = RDP(unit, U_ACT);
    int units = 0;
    for (void* room = act ? RDP(act, ACT_ROOM1) : NULL; room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* u = RDP(room, ROOM1_UNITS); u && units < 4096; u = RDP(u, U_LISTNEXT), units++) {
            if (RD32(u, U_TYPE) != UNIT_MONSTER || RD32(u, U_TXTNO) != MONSTER_DURIEL) continue;
            DWORD mode = RD32(u, U_MODE);
            if (mode != 0 && mode != 12) return 1;
        }
    }
    return 0;
}


static int questWord(int word) {
    if (questSite < 0) checkQuestSite();
    if (questSite != 1) return -1;
    const DWORD* flags = *(const DWORD* const*) (client + RVA_QuestFlags);
    if (!readable(flags, 8) || flags[1] != QUEST_BITS) return -1;
    const BYTE* buffer = (const BYTE*) flags[0];
    if (!readable(buffer, QUEST_BITS / 8) || word < 0 || word >= QUEST_WORDS) return -1;
    return buffer[2 * word] | buffer[2 * word + 1] << 8;
}





static const char* exclusionOf(void* unit, DWORD level) {
    DWORD type = RD32(unit, U_TYPE), txt = RD32(unit, U_TXTNO), mode = RD32(unit, U_MODE);
    if (type == UNIT_MONSTER) {
        if (isMercenary(txt) || isSummon(txt)) return "companion";
        if (isAncientStatue(txt)) return "statue of the Ancients";
        if (txt == MONSTER_TYRAEL_LAIR && level == LEVEL_DURIELS_LAIR) {
            int word = questWord(QUEST_WORD_SEVEN_TOMBS);
            if (tyraelWaits(txt, level, word >= 0, word < 0 ? 0 : (unsigned) word, durielAliveNear(unit))) return "Tyrael before Duriel";
        }
    }
    if (type == UNIT_OBJECT && txt == SC_OBJECT_ORIFICE && mode != 0) return "Orifice after the staff";
    return NULL;
}


static int monsterVerdict(void* unit, int town, DWORD level) {
    DWORD mode = RD32(unit, U_MODE);
    if (mode == 0 || mode == 12) return MON_OUT;
    int selectable = gameSelectable(unit);
    if (selectable < 0) return monsterReportOf(unit, town);
    if (!selectable || exclusionOf(unit, level)) return MON_OUT;
    DWORD txt = RD32(unit, U_TXTNO);
    int ally = fGetUnitStat(unit, 172, 0) == 2;
    BYTE* row = monStatsRow(unit);
    return monsterReportSelectable(town, ally, isMercenary(txt) || isSummon(txt), txt, row ? RD32(row, MONSTATS_FLAGS) : 0,
                                   row ? RD16(row, MONSTATS_NAMESTR) : 0);
}


static int objectVerdict(void* unit, const BYTE* txt, DWORD mode, DWORD level) {
    int selectable = gameSelectable(unit);
    if (selectable < 0) return objectReported(txt, mode);
    return selectable && !exclusionOf(unit, level);
}
