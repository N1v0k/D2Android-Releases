


















#define RVA_GamePacketTable   0xFA698
#define RVA_GameWaypointPkt   0x6CA70
#define RVA_GameSendStat      0x68260
#define ORD_SetUnitStat       10590
#define ORD_GetMaxLife        10574

#define SRV_PACKET            0x49
#define SRV_MARK              0xFFFFFFFFu
#define SRV_CODE_HEAL         0xA0D20001u
#define SRV_CODE_EFFECTS      0xA0D20152u
#define SRV_CODE_DAMAGE       0xA0D20089u




#define STAT_LIFE             6

typedef int (__fastcall *PacketHandler_t)(void* game, void* unit, BYTE* packet, DWORD size);
typedef void (__fastcall *SendStat_t)(void* unit, DWORD stat, DWORD value, void* player);
typedef void (__stdcall *SetUnitStat_t)(void*, DWORD, DWORD, DWORD);
typedef DWORD (__stdcall *GetMaxLife_t)(void*);

static PacketHandler_t srvOriginal;
static SendStat_t srvSendStat;
static SetUnitStat_t fSetUnitStat;
static GetMaxLife_t fGetMaxLife;
static DWORD srvGame;
static int srvState;














#define SRV_QUEUE    8
#define SRV_FREE     0
#define SRV_WAITING  1
#define SRV_BUSY     2
#define SRV_DONE     3
#define SRV_CANCELLED 4
typedef struct {
    volatile LONG state;
    volatile LONG seq;
    DWORD code;
    LONG arg, arg2;
    LONG answer, count;
} SrvSlot;
static SrvSlot srvQueue[SRV_QUEUE];
static LONG srvLastSeq;

static LONG srvRun(SrvSlot* s, void* game, void* unit);
static LONG damagePatchServer(void);


static SrvSlot* srvSlot(LONG seq) {
    if (seq <= 0) return NULL;
    for (int i = 0; i < SRV_QUEUE; i++) if (srvQueue[i].seq == seq) return &srvQueue[i];
    return NULL;
}


static LONG srvSubmit(DWORD code, LONG arg, LONG arg2) {
    SrvSlot* s = NULL;
    for (int i = 0; i < SRV_QUEUE && !s; i++) if (srvQueue[i].state == SRV_FREE) s = &srvQueue[i];
    if (!s) return 0;
    LONG seq = srvLastSeq + 1;
    if (seq <= 0) seq = 1;
    srvLastSeq = seq;
    s->code = code;
    s->arg = arg;
    s->arg2 = arg2;
    s->answer = s->count = 0;
    InterlockedExchange(&s->seq, seq);
    InterlockedExchange(&s->state, SRV_WAITING);
    BYTE packet[9] = {SRV_PACKET};
    *(DWORD*) (packet + 1) = SRV_MARK;
    *(LONG*) (packet + 5) = seq;
    fSendPacket(9, 1, packet);
    return seq;
}


static int srvTake(LONG seq, LONG* answer, LONG* count) {
    SrvSlot* s = srvSlot(seq);

    if (!s || InterlockedCompareExchange(&s->state, SRV_DONE, SRV_DONE) != SRV_DONE) return 0;
    *answer = s->answer;
    if (count) *count = s->count;
    InterlockedExchange(&s->state, SRV_FREE);
    return 1;
}






static int srvCancel(LONG seq) {
    SrvSlot* s = srvSlot(seq);
    if (!s) return 1;
    return InterlockedCompareExchange(&s->state, SRV_FREE, SRV_WAITING) == SRV_WAITING;
}

static DWORD srvResets;


static void srvReset(void) {
    srvResets++;




    for (int i = 0; i < SRV_QUEUE; i++) {
        volatile LONG* st = &srvQueue[i].state;
        for (;;) {
            LONG now = InterlockedCompareExchange(st, SRV_FREE, SRV_FREE);
            if (now == SRV_FREE || now == SRV_CANCELLED) break;
            if (InterlockedCompareExchange(st, now == SRV_BUSY ? SRV_CANCELLED : SRV_FREE, now) == now) break;
        }
    }
}


static int srvServe(LONG seq, void* game, void* unit) {
    SrvSlot* s = srvSlot(seq);
    if (!s || InterlockedCompareExchange(&s->state, SRV_BUSY, SRV_WAITING) != SRV_WAITING) return 0;
    s->answer = srvRun(s, game, unit);

    if (InterlockedCompareExchange(&s->state, SRV_DONE, SRV_BUSY) == SRV_CANCELLED) InterlockedExchange(&s->state, SRV_FREE);
    return 1;
}



static LONG srvHeal(void* unit) {

    if (!unit || RD32(unit, U_TYPE) != 0 || RD32(unit, U_MODE) == 0 || RD32(unit, U_MODE) == 17
        || fGetUnitStat(unit, STAT_LIFE, 0) >= 256) return 2;
    DWORD max = fGetMaxLife(unit);
    if (max < 256) return 2;

    fSetUnitStat(unit, STAT_LIFE, max, 0);
    srvSendStat(unit, STAT_LIFE, max, unit);
    return 1;
}










#define SU_STATS          0x5C
#define GAME_FRAME        0xA8
#define SL_STATE          0x14
#define SL_EXPIRE         0x18
#define SL_PREVLINK       0x2C
#define EFFECT_MAX_FRAMES (25u * 60u * 30u)
static DWORD srvEffectLeft[256];


static DWORD srvPointsLeft, srvPointsMost;
static DWORD srvEffectFrame;









#define SL_SKILL          0x1C
#define RVA_DataTables    0xA33F0
#define DT_SKILLS         0xB98
#define DT_SKILL_COUNT    0xBA0
#define SKILL_RECORD      0x23C
#define SKILL_FLAGS       0x04
#define SKILL_FLAG_AURA   (1u << 5)
static WORD srvEffectAura[256];

static int srvSkillIsAura(DWORD skill) {
    BYTE* tables = common && readable((void*) (common + RVA_DataTables), 4) ? RDP(common, RVA_DataTables) : NULL;
    if (!readable(tables, DT_SKILL_COUNT + 4) || skill >= RD32(tables, DT_SKILL_COUNT)) return 0;
    BYTE* record = (BYTE*) RDP(tables, DT_SKILLS) + skill * SKILL_RECORD;
    return readable(record, SKILL_FLAGS + 4) && RD32(record, 0) == skill && (RD32(record, SKILL_FLAGS) & SKILL_FLAG_AURA);
}

static LONG srvEffects(void* game, void* unit) {
    memset(srvEffectLeft, 0, sizeof(srvEffectLeft));
    memset(srvEffectAura, 0, sizeof(srvEffectAura));
    srvPointsLeft = srvPointsMost = 0;
    if (!readable(game, GAME_FRAME + 4) || !readable(unit, SU_STATS + 4) || RD32(unit, U_TYPE) != 0) return 0;
    srvPointsLeft = fGetUnitStat(unit, STAT_BONEARMOR, 0);
    srvPointsMost = fGetUnitStat(unit, STAT_BONEARMORMAX, 0);
    void* stats = RDP(unit, SU_STATS);
    if (!readable(stats, 0x44) || !(RD32(stats, 0x10) & 0x80000000u)) return 0;
    DWORD frame = srvEffectFrame = RD32(game, GAME_FRAME);
    LONG found = 0;
    for (int head = 0x3C; head <= 0x40; head += 4) {
        void* list = RDP(stats, head);
        for (int n = 0; n < 256 && readable(list, SL_PREVLINK + 4); n++, list = RDP(list, SL_PREVLINK)) {
            DWORD state = RD32(list, SL_STATE), frames = RD32(list, SL_EXPIRE) - frame;
            if (!state || state >= 256 || !RD32(list, SL_EXPIRE) || (int) frames <= 0 || frames > EFFECT_MAX_FRAMES) continue;
            if (srvSkillIsAura(RD32(list, SL_SKILL))) {
                srvEffectAura[state] = (WORD) (RD32(list, SL_SKILL) + 1);
                continue;
            }
            if (frames * 40u <= srvEffectLeft[state]) continue;
            if (!srvEffectLeft[state]) found++;
            srvEffectLeft[state] = frames * 40u;
        }
    }
    return found;
}












#define SRV_CODE_WARP       0xA0D20121u
#define SRV_CODE_PLACE      0xA0D20122u
#define SRV_CODE_INVUL_ON   0xA0D20123u
#define SRV_CODE_INVUL_OFF  0xA0D20124u
#define SRV_CODE_DIFFICULTY 0xA0D20125u
#define RVA_LevelWarpUnit   0x5DFC0
#define RVA_PlaceUnit       0x6F3A0
#define GAME_DIFFICULTY     0x6D
#define SU_FLAGS            0xC4
#define SU_ATTACKABLE       0xCu

static LONG srvLivingPlayer(void* unit) {
    return unit && readable(unit, SU_FLAGS + 4) && RD32(unit, U_TYPE) == 0 && RD32(unit, U_MODE) != 0 && RD32(unit, U_MODE) != 17;
}


static DWORD srvCallWarp(DWORD fn, void* game, void* unit, DWORD level, DWORD tile) {
    DWORD eax = (DWORD) unit, ecx = tile, edx = fn;
    __asm__ volatile ("push %%ecx\n\tpush %%esi\n\tcall *%%edx"
                      : "+a"(eax), "+c"(ecx), "+d"(edx)
                      : "D"(level), "S"(game)
                      : "memory", "cc");
    return eax;
}


static const BYTE WAYPOINT_LEVELS[] = {1, 40, 75, 103, 109, 3, 4, 5, 6, 27, 29, 32, 35, 48, 42, 57, 43, 44, 52, 76, 77, 78,
    79, 80, 81, 83, 101, 106, 107, 111, 112, 113, 115, 117, 118, 129, 123};

static int isWaypointLevel(DWORD level) {
    for (unsigned i = 0; i < sizeof(WAYPOINT_LEVELS); i++) if (WAYPOINT_LEVELS[i] == level) return 1;
    return 0;
}

static const BYTE warpStart[] = {0x83, 0xEC, 0x0C, 0x53, 0x55, 0x8B, 0x6C, 0x24, 0x18, 0x56, 0x57, 0x8B, 0xF0};
static const BYTE placeStart[] = {0x83, 0xEC, 0x2C, 0x53, 0x55, 0x8B, 0x6C, 0x24, 0x3C, 0x85, 0xED, 0x56, 0x57, 0x8B, 0xD8, 0x8B, 0xF1, 0x8B, 0xFA};


static int resumeFunctionsKnown(void) {
    DWORD game = (DWORD) GetModuleHandleA("D2Game.dll");
    BYTE* warp = (BYTE*) (game + RVA_LevelWarpUnit);
    BYTE* place = (BYTE*) (game + RVA_PlaceUnit);
    return game && readable(warp, sizeof(warpStart)) && memcmp(warp, warpStart, sizeof(warpStart)) == 0
        && readable(place, sizeof(placeStart)) && memcmp(place, placeStart, sizeof(placeStart)) == 0;
}




static LONG srvWarp(void* game, void* unit, LONG level, LONG maxFrame, LONG* frame) {
    BYTE* fn = (BYTE*) (srvGame + RVA_LevelWarpUnit);
    if (!readable(fn, sizeof(warpStart)) || memcmp(fn, warpStart, sizeof(warpStart)) != 0) return 3;
    if (!srvLivingPlayer(unit) || !readable(game, GAME_FRAME + 4) || !isWaypointLevel((DWORD) level)) return 2;

    *frame = (LONG) RD32(game, GAME_FRAME);
    if (maxFrame > 0 && (DWORD) *frame > (DWORD) maxFrame) return 4;
    int town = level == 1 || level == 40 || level == 75 || level == 103 || level == 109;
    srvCallWarp((DWORD) fn, game, unit, (DWORD) level, town ? 13 : 0);
    return 1;
}








static DWORD srvCallPlace(DWORD fn, void* game, void* unit, DWORD x, DWORD y) {
    DWORD eax = y, ecx = x, edx = 0;
    __asm__ volatile ("push $0\n\tpush $0\n\tpush %%esi\n\tpush %%edi\n\tcall *%%ebx"
                      : "+a"(eax), "+c"(ecx), "+d"(edx)
                      : "D"(game), "S"(unit), "b"(fn)
                      : "memory", "cc");
    return eax;
}


static LONG srvPlace(void* game, void* unit, LONG x, LONG y) {
    BYTE* fn = (BYTE*) (srvGame + RVA_PlaceUnit);
    if (!readable(fn, sizeof(placeStart)) || memcmp(fn, placeStart, sizeof(placeStart)) != 0) return 3;
    if (!srvLivingPlayer(unit) || x <= 0 || y <= 0 || x > 0xFFFF || y > 0xFFFF) return 2;
    return srvCallPlace((DWORD) fn, game, unit, (DWORD) x, (DWORD) y) ? 1 : 0;
}


static LONG srvInvulnerable(void* unit, int on, LONG* count) {
    if (!srvLivingPlayer(unit)) return 0;
    DWORD* flags = (DWORD*) ((BYTE*) unit + SU_FLAGS);
    *count = (LONG) (*flags & SU_ATTACKABLE);
    if (on) *flags &= ~SU_ATTACKABLE;
    else *flags |= SU_ATTACKABLE;
    return 1;
}


#define SRV_CODE_STASH_DEPOSIT  0xA0D20871u
#define SRV_CODE_STASH_WITHDRAW 0xA0D20872u
#define SRV_CODE_STASH_ALL      0xA0D20873u
static LONG stashServer(DWORD code, LONG arg1, LONG arg2, void* game, void* unit, LONG* moved);

static LONG srvRun(SrvSlot* s, void* game, void* unit) {
    switch (s->code) {
    case SRV_CODE_HEAL: return srvHeal(unit);
    case SRV_CODE_EFFECTS: return srvEffects(game, unit);
    case SRV_CODE_DAMAGE: return damagePatchServer();
    case SRV_CODE_STASH_DEPOSIT:
    case SRV_CODE_STASH_WITHDRAW:
    case SRV_CODE_STASH_ALL: return stashServer(s->code, s->arg, s->arg2, game, unit, &s->count);

    case SRV_CODE_DIFFICULTY: return readable(game, GAME_DIFFICULTY + 1) ? *((BYTE*) game + GAME_DIFFICULTY) : -1;
    case SRV_CODE_WARP: return srvWarp(game, unit, s->arg, s->arg2, &s->count);
    case SRV_CODE_PLACE: return srvPlace(game, unit, s->arg, s->arg2);
    case SRV_CODE_INVUL_ON: return srvInvulnerable(unit, 1, &s->count);
    case SRV_CODE_INVUL_OFF: return srvInvulnerable(unit, 0, &s->count);
    default: return 0;
    }
}


static int __fastcall srvPacket(void* game, void* unit, BYTE* packet, DWORD size) {
    if (size != 9 || *(DWORD*) (packet + 1) != SRV_MARK) return srvOriginal(game, unit, packet, size);
    srvServe(*(LONG*) (packet + 5), game, unit);
    return 0;
}


static void srvCheck(void) {
    static const BYTE handlerStart[] = {0x51, 0x83, 0x7C, 0x24, 0x0C, 0x09};
    static const BYTE sendStart[] = {0x53, 0x56, 0x8B, 0x74, 0x24, 0x10, 0x85, 0xF6, 0x57, 0x8B, 0xDA, 0x8B, 0xF9};
    DWORD gameBase = (DWORD) GetModuleHandleA("D2Game.dll");
    if (!gameBase) return;
    DWORD* entry = (DWORD*) (gameBase + RVA_GamePacketTable + SRV_PACKET * 8);
    if (srvState == 1 && srvGame == gameBase && readable(entry, 4) && *entry == (DWORD) srvPacket) return;
    srvState = -1;
    srvGame = gameBase;
    BYTE* handler = (BYTE*) (gameBase + RVA_GameWaypointPkt);
    BYTE* send = (BYTE*) (gameBase + RVA_GameSendStat);
    fSetUnitStat = (SetUnitStat_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_SetUnitStat);
    fGetMaxLife = (GetMaxLife_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetMaxLife);
    DWORD old;
    if (!fSetUnitStat || !fGetMaxLife || !fGetUnitStat || !readable(entry, 4) || *entry != (DWORD) handler
        || !readable(handler, sizeof(handlerStart)) || memcmp(handler, handlerStart, sizeof(handlerStart)) != 0
        || !readable(send, sizeof(sendStart)) || memcmp(send, sendStart, sizeof(sendStart)) != 0
        || !VirtualProtect(entry, 4, PAGE_READWRITE, &old)) {
        logLine("server: packet table not as expected, no server repairs");
        return;
    }
    srvOriginal = (PacketHandler_t) handler;
    srvSendStat = (SendStat_t) send;
    InterlockedExchange((volatile LONG*) entry, (LONG) srvPacket);
    VirtualProtect(entry, 4, old, &old);
    srvState = 1;
    logLine("server: packet 0x49 wrapped");
}






static void updateServerRepairs(void* player, DWORD now) {
    static void* lastPlayer;
    static DWORD lastPlayerId, lastSeen;
    static int asked;
    static DWORD askedAt;
    static LONG pending;







    DWORD playerId = player ? RD32(player, U_ID) : 0;
    int newGame = player != lastPlayer || playerId != lastPlayerId || (now - lastSeen > 2000 && outOfGameSeen);
    InterlockedExchange(&outOfGameSeen, 0);
    if (newGame) logLine("server: new game (hero %p id %lu, before %p id %lu, %lu ms without a tick), queue emptied",
                         player, (unsigned long) playerId, lastPlayer, (unsigned long) lastPlayerId,
                         (unsigned long) (lastSeen ? now - lastSeen : 0));
    lastSeen = now;
    if (newGame) {
        lastPlayer = player;
        lastPlayerId = playerId;
        asked = 0;
        pending = 0;
        srvReset();
    }
    LONG answer;
    if (pending && srvTake(pending, &answer, NULL)) {
        pending = 0;
        logLine(answer == 1 ? "server: hero loaded with life 0 healed to full"
                            : "server: life check, nothing to heal");
    }
    if (!player || !singlePlayerGame() || pauseApplied) return;
    DWORD mode = RD32(player, U_MODE);
    if (mode == 0 || mode == 17 || fGetUnitStat(player, STAT_LIFE, 0) >= 256) return;
    if (asked >= 3 || (asked && now - askedAt < 2000)) return;
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1) return;

    if (pending && !srvCancel(pending)) return;
    pending = srvSubmit(SRV_CODE_HEAL, 0, 0);
    if (!pending) return;
    asked++;
    askedAt = now;
    logLine("server: hero alive with life 0, asking the server to heal (%d)", asked);
}







static void updateServerEffects(void* player, DWORD now) {
    static DWORD askedAt;
    static int logged;
    static LONG pending;
    static DWORD resets;
    static BYTE auraLogged[256];
    LONG answer;
    if (srvResets != resets) {
        resets = srvResets;
        pending = 0;
        memset(auraLogged, 0, sizeof(auraLogged));
    }
    if (pending && srvTake(pending, &answer, NULL)) {
        pending = 0;
        if (!logged++) logLine("server: effect timer answered, frame %lu, %ld found", (unsigned long) srvEffectFrame, (long) answer);
        for (DWORD state = 0; state < 256; state++) if (srvEffectLeft[state]) syncEffect(state, srvEffectLeft[state]);

        for (DWORD state = 0; state < 256; state++)
            if (srvEffectAura[state] && !auraLogged[state]) {
                auraLogged[state] = 1;
                logLine("effect state %lu: aura skill %u, renewed by its pulse, no time", (unsigned long) state,
                        (unsigned) (srvEffectAura[state] - 1));
            }
        effectPointsLeft = srvPointsLeft;
        effectPointsMost = srvPointsMost;
        effectPointsAt = now;
    }
    if (!player || !singlePlayerGame() || worldPaused()) return;
    int any = 0;
    for (DWORD state = 0; state < 256; state++) any |= effectOn[state];
    if (!any || now - askedAt < 500) return;
    if (pending && now - askedAt < 2000) return;

    if (pending && !srvCancel(pending)) return;
    pending = 0;
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1) return;
    pending = srvSubmit(SRV_CODE_EFFECTS, 0, 0);
    if (pending) askedAt = now;
}

#ifdef D2MOD_TESTCMDS




static LONG srvTestSeq[2];
static DWORD srvTestAt;

static void srvTestTwo(DWORD now) {
    if (!singlePlayerGame()) return;
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1) {
        logLine("server queue test: no wrapper");
        return;
    }
    srvTestSeq[0] = srvSubmit(SRV_CODE_HEAL, 0, 0);
    srvTestSeq[1] = srvSubmit(SRV_CODE_EFFECTS, 0, 0);
    srvTestAt = now;
    logLine("server queue test: heal %ld and effect timer %ld sent in one tick", (long) srvTestSeq[0], (long) srvTestSeq[1]);
}


static LONG srvProbeSeq;
static DWORD srvProbeCode, srvProbeAt;

static void srvProbe(DWORD code, LONG arg, LONG arg2, DWORD now) {
    if (!singlePlayerGame()) return;
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1 || srvProbeSeq) return;
    srvProbeSeq = srvSubmit(code, arg, arg2);
    srvProbeCode = code;
    srvProbeAt = now;
    logLine("server probe: code %08lX arg %ld sent as %ld", (unsigned long) code, (long) arg, (long) srvProbeSeq);
}

static void updateServerQueueTest(DWORD now) {
    if (srvTestWanted) {
        srvTestWanted = 0;
        srvTestTwo(now);
    }
    if (srvProbeWanted) {
        static const DWORD codes[] = {0, SRV_CODE_DIFFICULTY, SRV_CODE_WARP, SRV_CODE_INVUL_ON, SRV_CODE_INVUL_OFF, SRV_CODE_PLACE};
        int op = srvProbeWanted & 0xFF;
        if (op == 5) srvProbe(SRV_CODE_PLACE, srvProbeX, srvProbeY, now);
        else if (op >= 1 && op <= 4) srvProbe(codes[op], (srvProbeWanted >> 8) & 0xFF, 0, now);
        srvProbeWanted = 0;
    }
    if (srvProbeSeq) {
        LONG answer, count;
        if (srvTake(srvProbeSeq, &answer, &count)) {
            logLine("server probe: code %08lX answered %ld, number %ld", (unsigned long) srvProbeCode, (long) answer, (long) count);
            srvProbeSeq = 0;
        }
        else if (now - srvProbeAt > 3000 && srvCancel(srvProbeSeq)) {
            logLine("server probe: code %08lX LOST", (unsigned long) srvProbeCode);
            srvProbeSeq = 0;
        }
    }
    for (int i = 0; i < 2; i++) {
        LONG answer, count;
        if (srvTestSeq[i] && srvTake(srvTestSeq[i], &answer, &count)) {
            logLine("server queue test: %s %ld answered %ld", i ? "effect timer" : "heal", (long) srvTestSeq[i], (long) answer);
            srvTestSeq[i] = 0;
        }
        else if (srvTestSeq[i] && now - srvTestAt > 3000 && srvCancel(srvTestSeq[i])) {
            logLine("server queue test: %s %ld LOST", i ? "effect timer" : "heal", (long) srvTestSeq[i]);
            srvTestSeq[i] = 0;
        }
    }
}
#endif
