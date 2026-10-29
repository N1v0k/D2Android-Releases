









































#define RVA_DrawUnitInfoCall 0x1D457
#define RVA_DrawUnitInfo     0x1A730
#define RVA_GameDamage       0x8BCB0
#define RVA_GameDamageLife   0x8BE32
#define RVA_GameDamageSet    0x8BE54
#define RVA_GameSetStatThunk 0xA3C4
#define RVA_GameRegen        0x86390
#define RVA_GameRegenSet     0x8649A
#define RVA_GamePlayerRegen  0x91C60
#define RVA_GamePlayerRegenSet 0x91CC2
#define RVA_GameStateListThunk 0xA49C
#define ORD_GetStatListWithState 10429
#define RVA_GameListStatThunk 0xA97C
#define ORD_GetStatListStat  10190
#define STATE_POISON 2
#define STATE_BURNING 115
#define STATE_OPENWOUNDS 62
#define STATLIST_OWNER_TYPE 0x08
#define STATLIST_OWNER_ID 0x0C
#define DMG_DOT_RING 512
#define DMG_DOT_SLOTS 64
#define DMG_DOT_DX 14
#define DMG_RING 128
#define DMG_FLOATERS 48
#define DMG_RISE_MS 1100
#define DMG_HITS 256
#define DMG_COLOR_SHADOW 6
#define DMG_FONT 1




#define DMG_FONT_LARGE 7
#define DMG_CRIT_BOLD 0

#include "d2damagerules.c"

static int damageSites;
static int damageServerSite;






static volatile LONG dmgShieldId = -1;
static volatile LONG dmgShielded;
static LONG dmgShieldedSeen;
static int damagePatched;
static int damageDrawApplied;
static DWORD damageNumbersUntil, damageDpsUntil, damageElementUntil, damageAppDpsUntil;
static int damageNumbersOn, damageDpsOn, damageStyle;
static DWORD dmgGame;
static LONG dmgRequest;
static DWORD dmgRequestResets;
static volatile LONG dmgRecord;

static volatile struct { DWORD defType, defId, attType, attId, attTxt, attAlign, amount, result; LONG attNpc; LONG phys, fire, cold, light, magic, poison, burn; DWORD total, taken; } dmgRing[DMG_RING];
static volatile LONG dmgWritten;
static LONG dmgRead;
DWORD dmgSetUnitStat;

static volatile struct { DWORD defId, loss, ownerType, ownerId, dead, element; } dmgDotRing[DMG_DOT_RING];
static volatile LONG dmgDotWritten;
static LONG dmgDotRead;
static int poisonSite;
static int poisonPatched;
static int playerRegenSite, playerRegenPatched;
static void* (__stdcall *fGetStatListWithState)(void*, DWORD);
static LONG (__stdcall *fGetStatListStat)(void*, DWORD, DWORD);
static DmgDot dmgDot[DMG_DOT_SLOTS];
static DmgDotOwner dmgDotOwners[32];
static unsigned long dmgDotOwnerNext;
static DmgStream dmgStreams[DMG_DOT_SLOTS];
static struct { DWORD id, value, at, color, large, dx; } dmgFloat[DMG_FLOATERS];
static DWORD dmgFloatNext;
static DmgHit dmgHits[DMG_HITS];
static DWORD dmgHitNext;


static DmgFight dmgFight;
static DWORD dmgDpsNow = DMG_DPS_NONE, dmgDpsMax = DMG_DPS_NONE, dmgDpsGame;



static void noteDamageHit(void* defender, DWORD newLife, void* attacker, BYTE* damage);
__attribute__((force_align_arg_pointer)) DWORD __cdecl noteDamage(void* defender, DWORD newLife, void* attacker, BYTE* damage) {
    LONG shield = dmgShieldId;
    if (shield != -1 && readable(defender, 0x10) && RD32(defender, U_TYPE) == 0 && (LONG) RD32(defender, U_ID) == shield) {


        if (readable(damage, 0x48)) {
            static const int AFTER[] = {0x14, 0x18, 0x28, 0x2C, 0x30, 0x34, 0x3C, 0x40, 0x44};
            for (unsigned i = 0; i < sizeof(AFTER) / sizeof(AFTER[0]); i++) *(LONG*) (damage + AFTER[i]) = 0;
        }
        DWORD old = fGetUnitStat(defender, 6, 0);
        if (newLife < old) {
            InterlockedIncrement(&dmgShielded);
            return old;
        }
    }
    noteDamageHit(defender, newLife, attacker, damage);
    return 0xFFFFFFFF;
}

static void noteDamageHit(void* defender, DWORD newLife, void* attacker, BYTE* damage) {
    if (!dmgRecord || !readable(defender, 0x10) || RD32(defender, U_TYPE) != UNIT_MONSTER || !readable(damage, 0x50)) return;
    DWORD old = fGetUnitStat(defender, 6, 0);
    if (old <= newLife) return;
    LONG k = dmgWritten % DMG_RING;
    dmgRing[k].defType = UNIT_MONSTER;
    dmgRing[k].defId = RD32(defender, U_ID);
    dmgRing[k].attType = readable(attacker, 0x10) ? RD32(attacker, U_TYPE) : 0xFFFFFFFF;
    dmgRing[k].attId = readable(attacker, 0x10) ? RD32(attacker, U_ID) : 0xFFFFFFFF;
    dmgRing[k].attTxt = readable(attacker, 0x10) ? RD32(attacker, U_TXTNO) : 0xFFFFFFFF;

    int monster = readable(attacker, 0x18) && RD32(attacker, U_TYPE) == UNIT_MONSTER;
    BYTE* row = monster ? monStatsRow(attacker) : NULL;
    dmgRing[k].attAlign = monster ? fGetUnitStat(attacker, 172, 0) : 0xFFFFFFFF;
    dmgRing[k].attNpc = row ? ((RD32(row, MONSTATS_FLAGS) & MONSTATS_NPC) != 0) : -1;
    dmgRing[k].amount = (*(DWORD*) (damage + 0x4C) + 128) >> 8;
    dmgRing[k].result = *(DWORD*) (damage + 0x04) & 0xFFFF;
    dmgRing[k].phys = *(LONG*) (damage + 0x08);
    dmgRing[k].fire = *(LONG*) (damage + 0x10);
    dmgRing[k].light = *(LONG*) (damage + 0x1C);
    dmgRing[k].magic = *(LONG*) (damage + 0x20);
    dmgRing[k].cold = *(LONG*) (damage + 0x24);
    dmgRing[k].poison = *(LONG*) (damage + 0x28);
    dmgRing[k].burn = *(LONG*) (damage + 0x14);
    dmgRing[k].total = *(DWORD*) (damage + 0x4C);
    dmgRing[k].taken = old - newLife;
    InterlockedIncrement(&dmgWritten);
}


static void noteOverTime(void* unit, DWORD loss, BYTE* list, DWORD element, int dead) {
    if (!loss) return;
    LONG k = dmgDotWritten % DMG_DOT_RING;
    dmgDotRing[k].element = element;
    dmgDotRing[k].defId = RD32(unit, U_ID);
    dmgDotRing[k].loss = loss;
    dmgDotRing[k].ownerType = list ? RD32(list, STATLIST_OWNER_TYPE) : 0xFFFFFFFF;
    dmgDotRing[k].ownerId = list ? RD32(list, STATLIST_OWNER_ID) : 0xFFFFFFFF;
    dmgDotRing[k].dead = dead;
    InterlockedIncrement(&dmgDotWritten);
}






__attribute__((force_align_arg_pointer)) void __cdecl notePoison(void* unit, DWORD newLife) {
    static const DWORD states[] = {STATE_POISON, STATE_BURNING, STATE_OPENWOUNDS};
    static const DWORD elements[] = {DMG_EL_POISON, DMG_EL_FIRE, DMG_EL_PHYS};
    if (!dmgRecord || !fGetStatListWithState || !readable(unit, 0x10) || RD32(unit, U_TYPE) != UNIT_MONSTER) return;
    DWORD old = fGetUnitStat(unit, 6, 0);
    if (old <= newLife) return;
    DWORD loss = old - newLife;
    int dead = newLife == 0;
    BYTE* list[3];
    LONG rate[3];
    long long total = 0;
    int found = 0, last = -1;
    for (int i = 0; i < 3; i++) {
        list[i] = fGetStatListWithState(unit, states[i]);
        if (!readable(list[i], 0x10)) list[i] = NULL;
        rate[i] = list[i] ? -fGetStatListStat(list[i], 74, 0) : 0;
        if (list[i]) {
            found++;
            if (last < 0) last = i;
        }
        if (rate[i] > 0) total += rate[i];
    }
    if (found > 1 && total > 0) {
        DWORD given = 0;
        int lastRated = -1;
        for (int i = 0; i < 3; i++) if (rate[i] > 0) lastRated = i;
        for (int i = 0; i < 3; i++) {
            if (rate[i] <= 0) continue;
            DWORD part = i == lastRated ? loss - given : (DWORD) ((unsigned long long) loss * (unsigned long long) rate[i] / (unsigned long long) total);
            given += part;
            noteOverTime(unit, part, list[i], elements[i], dead);
        }
        return;
    }
    if (last >= 0) noteOverTime(unit, loss, list[last], elements[last], dead);
    else noteOverTime(unit, loss, NULL, 0xFFFFFFFF, dead);
}

void poisonStub(void);
__asm__(
    ".text\n"
    ".globl _poisonStub\n"
    "_poisonStub:\n"

    "    pushal\n"
    "    pushl 0x2C(%esp)\n"
    "    pushl 0x28(%esp)\n"
    "    call _notePoison\n"
    "    addl $8, %esp\n"
    "    popal\n"
    "    jmp *_dmgSetUnitStat\n"
);







__attribute__((force_align_arg_pointer)) DWORD __cdecl notePlayerRegen(void* unit, DWORD newLife) {
    LONG shield = dmgShieldId;
    if (shield == -1 || !readable(unit, 0x10) || RD32(unit, U_TYPE) != 0 || (LONG) RD32(unit, U_ID) != shield) return 0xFFFFFFFF;
    DWORD old = fGetUnitStat(unit, 6, 0);
    if (newLife >= old) return 0xFFFFFFFF;
    InterlockedIncrement(&dmgShielded);
    return old;
}

void playerRegenStub(void);
__asm__(
    ".text\n"
    ".globl _playerRegenStub\n"
    "_playerRegenStub:\n"

    "    pushal\n"
    "    pushl 0x2C(%esp)\n"
    "    pushl 0x28(%esp)\n"
    "    call _notePlayerRegen\n"
    "    addl $8, %esp\n"
    "    cmpl $-1, %eax\n"
    "    je 1f\n"
    "    movl %eax, 0x2C(%esp)\n"
    "1:  popal\n"
    "    jmp *_dmgSetUnitStat\n"
);

void damageStub(void);
__asm__(
    ".text\n"
    ".globl _damageStub\n"
    "_damageStub:\n"

    "    pushal\n"
    "    pushl %ebp\n"
    "    pushl 0x54(%esp)\n"
    "    pushl 0x34(%esp)\n"
    "    pushl 0x30(%esp)\n"
    "    call _noteDamage\n"
    "    addl $16, %esp\n"
    "    cmpl $-1, %eax\n"
    "    je 1f\n"
    "    movl %eax, 0x2C(%esp)\n"
    "1:  popal\n"
    "    jmp *_dmgSetUnitStat\n"
);


static DWORD dmgCallTarget(DWORD at) {
    const BYTE* p = (const BYTE*) at;
    return readable((void*) p, 5) && p[0] == 0xE8 ? at + 5 + *(const DWORD*) (p + 1) : 0;
}

static void checkDamageSites(void) {
    static const BYTE prologue[] = {0x51, 0x53, 0x8B, 0x5C, 0x24, 0x0C, 0x55, 0x8B, 0x6C, 0x24, 0x1C, 0x56, 0x57, 0x8B, 0x7C, 0x24, 0x1C};
    static const BYTE life[] = {0x8B, 0x7D, 0x4C, 0x85, 0xFF, 0x7E, 0x20, 0x6A, 0x00, 0x6A, 0x06, 0x56, 0xE8};
    static const BYTE set[] = {0x2B, 0xC7, 0x3D, 0x00, 0x01, 0x00, 0x00, 0x7D, 0x02, 0x33, 0xC0, 0x6A, 0x00, 0x50, 0x6A, 0x06, 0x56};
    dmgGame = (DWORD) GetModuleHandleA("D2Game.dll");
    DWORD game = dmgGame;
    BYTE* thunk = (BYTE*) (game + RVA_GameSetStatThunk);
    dmgSetUnitStat = (DWORD) GetProcAddress((HMODULE) common, (LPCSTR) 10590);
    int server = game && bytesAre(game + RVA_GameDamage, prologue, sizeof(prologue))
        && bytesAre(game + RVA_GameDamageLife, life, sizeof(life)) && bytesAre(game + RVA_GameDamageSet - sizeof(set), set, sizeof(set))
        && (callAt(game, RVA_GameDamageSet, RVA_GameSetStatThunk) || dmgCallTarget(game + RVA_GameDamageSet) == (DWORD) damageStub)
        && readable(thunk, 6) && thunk[0] == 0xFF && thunk[1] == 0x25 && dmgSetUnitStat
        && readable(*(void**) (thunk + 2), 4) && **(DWORD**) (thunk + 2) == dmgSetUnitStat;
    int draw = callAt(client, RVA_DrawUnitInfoCall, RVA_DrawUnitInfo) && RD8(client, RVA_DrawUnitInfoCall - 1) == 0x50
        && fDrawText && fSetTextSize && fGetTextWidth;
    damageSites = server && draw;
    damageServerSite = server;
    damagePatched = server && dmgCallTarget(game + RVA_GameDamageSet) == (DWORD) damageStub;
    logLine(damageSites ? "damage numbers sites verified%s" : "damage numbers sites not as expected (server %d, draw %d)",
        damageSites ? (damagePatched ? " (stub already in place)" : "") : "", server, draw);

    static const BYTE regen[] = {0x83, 0xEC, 0x0C, 0x53, 0x56, 0x57, 0x6A, 0x00, 0x8B, 0xF2, 0x6A, 0x4A, 0x8B, 0xF9, 0x56};
    static const BYTE regenSet[] = {0x8B, 0xFD, 0x83, 0xFF, 0x01, 0x7D, 0x02, 0x33, 0xFF, 0x6A, 0x00, 0x57, 0x6A, 0x06, 0x56};
    BYTE* listThunk = (BYTE*) (game + RVA_GameStateListThunk);
    BYTE* statThunk = (BYTE*) (game + RVA_GameListStatThunk);
    void* statFn = (void*) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetStatListStat);
    void* listFn = (void*) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetStatListWithState);
    poisonSite = damageSites && bytesAre(game + RVA_GameRegen, regen, sizeof(regen))
        && bytesAre(game + RVA_GameRegenSet - sizeof(regenSet), regenSet, sizeof(regenSet))
        && (callAt(game, RVA_GameRegenSet, RVA_GameSetStatThunk) || dmgCallTarget(game + RVA_GameRegenSet) == (DWORD) poisonStub)
        && listFn && readable(listThunk, 6) && listThunk[0] == 0xFF && listThunk[1] == 0x25
        && readable(*(void**) (listThunk + 2), 4) && **(void***) (listThunk + 2) == listFn
        && statFn && readable(statThunk, 6) && statThunk[0] == 0xFF && statThunk[1] == 0x25
        && readable(*(void**) (statThunk + 2), 4) && **(void***) (statThunk + 2) == statFn;
    fGetStatListStat = poisonSite ? (LONG (__stdcall *)(void*, DWORD, DWORD)) statFn : NULL;
    fGetStatListWithState = poisonSite ? (void* (__stdcall *)(void*, DWORD)) listFn : NULL;
    poisonPatched = poisonSite && dmgCallTarget(game + RVA_GameRegenSet) == (DWORD) poisonStub;

    static const BYTE playerRegen[] = {0x51, 0x53, 0x56, 0x6A, 0x00, 0x8B, 0xF0, 0x6A, 0x4A, 0x56};
    static const BYTE playerRegenSet[] = {0x81, 0xFF, 0x00, 0x01, 0x00, 0x00, 0x7D, 0x05, 0xBF, 0x00, 0x01, 0x00, 0x00, 0x6A, 0x00,
        0x57, 0x6A, 0x06, 0x56};
    playerRegenSite = damageServerSite && bytesAre(game + RVA_GamePlayerRegen, playerRegen, sizeof(playerRegen))
        && bytesAre(game + RVA_GamePlayerRegenSet - sizeof(playerRegenSet), playerRegenSet, sizeof(playerRegenSet))
        && (callAt(game, RVA_GamePlayerRegenSet, RVA_GameSetStatThunk) || dmgCallTarget(game + RVA_GamePlayerRegenSet) == (DWORD) playerRegenStub);
    playerRegenPatched = playerRegenSite && dmgCallTarget(game + RVA_GamePlayerRegenSet) == (DWORD) playerRegenStub;
    logLine(playerRegenSite ? "shield: player regeneration site verified" : "shield: player regeneration site not as expected");
    if (damageSites) logLine(poisonSite ? "poison numbers site verified%s" : "poison numbers site not as expected: no poison numbers",
        poisonPatched ? " (stub already in place)" : "");
}


static int damagePatchCall(DWORD rva, void (*stub)(void)) {
    BYTE* at = (BYTE*) (dmgGame + rva);
    if (dmgCallTarget((DWORD) at) == (DWORD) stub) return 1;
    if (!callAt(dmgGame, rva, RVA_GameSetStatThunk)) return 0;
    DWORD old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return 0;
    *(DWORD*) (at + 1) = (DWORD) stub - ((DWORD) at + 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    return 1;
}



static LONG damagePatchServer(void) {
    if (!damageServerSite || !damagePatchCall(RVA_GameDamageSet, damageStub)) return 0;
    return 1 | (poisonSite && damagePatchCall(RVA_GameRegenSet, poisonStub) ? 2 : 0)
        | (playerRegenSite && damagePatchCall(RVA_GamePlayerRegenSet, playerRegenStub) ? 4 : 0);
}


static int damageCapable(void) {
    return damageSites && srvState == 1 && singlePlayerGame();
}






static void damageCommand(int numbers, int dps, int element, int appDps, DWORD now) {
    DWORD off = now - 60000;
    damageNumbersUntil = numbers ? now + 2000 : off;
    damageDpsUntil = dps ? now + 2000 : off;
    damageElementUntil = element ? now + 2000 : off;
    damageAppDpsUntil = appDps ? now + 2000 : off;
}


static DWORD damageDps(void) {

    if (!damageDpsOn || !damagePatched || !singlePlayerGame()) return DMG_DPS_NONE;
    return dmgDpsNow;
}


static DWORD damageDpsMax(void) {
    return damageDps() == DMG_DPS_NONE ? DMG_DPS_NONE : dmgDpsMax;
}



static void putDpsFields(Packet* p) {
    putU32(p, damageDps());
    putU32(p, damageDpsMax());
}


static void drawDamage(void) {
    DWORD now = GetTickCount();
    int oldFont = fSetTextSize(DMG_FONT);
    if (damageNumbersOn) {
        View view;
        view.offX = (long) fGetMouseXOffset();
        view.offY = (long) fGetMouseYOffset();
        for (int k = 0; k < DMG_FLOATERS; k++) {
            DWORD age = now - dmgFloat[k].at;
            if (!dmgFloat[k].value || age >= DMG_RISE_MS) continue;
            void* unit = findUnit(dmgFloat[k].id, UNIT_MONSTER);
            if (!unit) continue;
            double x, y;
            unitPosition(unit, &x, &y);
            int sx, sy;
            toScreen(&view, x, y, &sx, &sy);
            sy -= 70 + (int) (age * 40 / DMG_RISE_MS);
            wchar_t text[16];
            wsprintfW(text, L"%lu", (unsigned long) dmgFloat[k].value);
            int large = dmgFloat[k].large != 0, bold = large && DMG_CRIT_BOLD;
            if (large) fSetTextSize(DMG_FONT_LARGE);
            int w = fGetTextWidth(text) + bold;
            sx += (int) dmgFloat[k].dx;
            for (int b = 0; b <= bold; b++) fDrawText(text, sx - w / 2 + 1 + b, sy + 1, DMG_COLOR_SHADOW, 0);
            for (int b = 0; b <= bold; b++) fDrawText(text, sx - w / 2 + b, sy, (int) dmgFloat[k].color, 0);
            if (large) fSetTextSize(DMG_FONT);
        }
    }
    fSetTextSize(oldFont);
}


__attribute__((force_align_arg_pointer)) static void __stdcall unitInfoDrawn(void* selected) {
    ((void (__stdcall *)(void*)) (client + RVA_DrawUnitInfo))(selected);
    drawDamage();
}

static void applyDamageDraw(int on) {
    BYTE* at = (BYTE*) (client + RVA_DrawUnitInfoCall);
    DWORD target = on ? (DWORD) unitInfoDrawn : client + RVA_DrawUnitInfo, old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    *(DWORD*) (at + 1) = target - ((DWORD) at + 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    damageDrawApplied = on;
    logLine(on ? "damage numbers drawing on" : "damage numbers drawing off");
}





static void damageShieldPrepare(void* player) {

    if (player) InterlockedExchange(&dmgShieldId, (LONG) RD32(player, U_ID));
    logLine(damageServerSite ? "damage: the protected hero keeps his life on every hit" : "damage: no shield, the sites are not as expected");
    if (!damageServerSite || damagePatched || srvState != 1) return;
    if (dmgRequest && dmgRequestResets != srvResets) dmgRequest = 0;
    if (dmgRequest) return;
    dmgRequest = srvSubmit(SRV_CODE_DAMAGE, 0, 0);
    dmgRequestResets = srvResets;
    logLine("damage: patch asked for the shield while resuming");
}


static void damageShieldStep(void* player) {
    LONG answer;
    if (dmgRequest && dmgRequestResets != srvResets) dmgRequest = 0;
    if (dmgRequest && !damagePatched && srvTake(dmgRequest, &answer, NULL)) {
        dmgRequest = 0;
        damagePatched = (answer & 1) != 0;
        poisonPatched = (answer & 2) != 0;
        playerRegenPatched = (answer & 4) != 0;
        logLine(damagePatched ? "damage numbers: stub in place (server thread), poison numbers %s, player regeneration %s"
                : "damage numbers: patch refused, no numbers", poisonPatched ? "on" : "off", playerRegenPatched ? "shielded" : "not shielded");
        if (!damagePatched) damageSites = damageServerSite = 0;
    }
    LONG shield = player && rsm.guardOn && !rsm.offWanted ? (LONG) RD32(player, U_ID) : -1;
    if (shield != dmgShieldId) {
        InterlockedExchange(&dmgShieldId, shield);
        if (damagePatched || dmgRequest) logLine(shield != -1 ? "damage: the protected hero keeps his life on every hit" : "damage: shield off");
    }
    LONG kept = InterlockedCompareExchange(&dmgShielded, 0, 0);
    if (kept != dmgShieldedSeen) {
        logLine("damage: %ld hit(s) on the protected hero kept off", (long) (kept - dmgShieldedSeen));
        dmgShieldedSeen = kept;
    }
}


static void damageDpsHit(DWORD now, DWORD value) {
    if (!value) return;
    DWORD h = dmgHitNext++ % DMG_HITS;
    dmgHits[h].at = now;
    dmgHits[h].value = value;
    damageFightNote(&dmgFight, now);
}


static void updateDamage(void* player, DWORD now) {
    damageShieldStep(player);

    if (dmgDpsGame != srvResets) {
        dmgDpsGame = srvResets;
        memset(dmgHits, 0, sizeof(dmgHits));
        memset(&dmgFight, 0, sizeof(dmgFight));
        dmgDpsMax = DMG_DPS_NONE;
    }
    if (!damageSites) return;

    if (player && singlePlayerGame() && (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))))
        srvCheck();
    damageNumbersOn = (int) (now - damageNumbersUntil) < 0;
    damageDpsOn = (int) (now - damageDpsUntil) < 0;
    damageStyle = (int) (now - damageElementUntil) < 0 ? DMG_STYLE_ELEMENT : DMG_STYLE_SIMPLE;
    int wanted = (damageNumbersOn || damageDpsOn) && player && singlePlayerGame();

    if (wanted && !damagePatched && srvState == 1 && !dmgRequest) {
        dmgRequest = srvSubmit(SRV_CODE_DAMAGE, 0, 0);
        dmgRequestResets = srvResets;
    }
    InterlockedExchange(&dmgRecord, wanted && damagePatched);
    int draw = wanted && damagePatched;
    if (draw != damageDrawApplied) applyDamageDraw(draw);

    LONG written = InterlockedCompareExchange(&dmgWritten, 0, 0);
    if (written - dmgRead > DMG_RING) dmgRead = written - DMG_RING;
    DWORD me = player ? RD32(player, U_ID) : 0xFFFFFFFF;

    static DWORD summaryAt, sumHits, sumDealt, sumCrit, sumAlly, sumHidden, sumElement[5], sumStream;
    for (; dmgRead < written; dmgRead++) {
        LONG k = dmgRead % DMG_RING;


        int kind = damageKindAligned(dmgRing[k].attType, dmgRing[k].attId, dmgRing[k].attTxt, me, dmgRing[k].result,
            dmgRing[k].attAlign, (int) dmgRing[k].attNpc);

        int element = damageElement(dmgRing[k].phys, dmgRing[k].fire, dmgRing[k].cold, dmgRing[k].light, dmgRing[k].magic);


        int stream = kind != DMG_HIDE && damageStream(dmgStreams, DMG_DOT_SLOTS, dmgRing[k].defId, kind, element, now);


        if (!dmgRing[k].amount) stream = 1;
        if (dmgRing[k].result & DMG_RESULT_KILL) damageDotKill(dmgDot, DMG_DOT_SLOTS, dmgRing[k].defId);
#ifdef D2MOD_TESTCMDS
        static const char* const elementName[] = {"physical", "fire", "cold", "lightning", "magic"};
        trace("damage: monster %lu %lu by type %lu row %lu (alignment %ld, npc %ld): %s, %s (1/256: phys %ld fire %ld cold %ld light %ld magic %ld poison %ld burn %ld, result 0x%lx, took %lu of the life)%s",
            (unsigned long) dmgRing[k].defId, (unsigned long) dmgRing[k].amount, (unsigned long) dmgRing[k].attType,
            (unsigned long) dmgRing[k].attTxt, (long) dmgRing[k].attAlign, (long) dmgRing[k].attNpc, kind == DMG_CRIT ? "own critical" : kind == DMG_OWN ? "own" : kind == DMG_ALLY ? "ally" : "left out",
            elementName[element], (long) dmgRing[k].phys, (long) dmgRing[k].fire, (long) dmgRing[k].cold, (long) dmgRing[k].light,
            (long) dmgRing[k].magic, (long) dmgRing[k].poison, (long) dmgRing[k].burn, (unsigned long) dmgRing[k].result,
            (unsigned long) ((dmgRing[k].taken + 128) >> 8), stream ? ", stream" : "");
#endif


        if (dmgRing[k].attType == UNIT_MONSTER)
            damageDotOwnerNote(dmgDotOwners, 32, &dmgDotOwnerNext, dmgRing[k].attId, kind == DMG_ALLY ? DMG_ALLY : DMG_HIDE);
        if (kind == DMG_HIDE) {
            sumHidden++;
            continue;
        }
        if (stream) {
            sumStream++;

            damageDotAdd(dmgDot, DMG_DOT_SLOTS, dmgRing[k].defId, (long) dmgRing[k].taken, kind == DMG_CRIT ? DMG_OWN : kind,
                element, (dmgRing[k].result & DMG_RESULT_KILL) != 0, now);
            continue;
        }
        sumHits++;
        sumDealt += dmgRing[k].amount;
        sumCrit += kind == DMG_CRIT;
        sumAlly += kind == DMG_ALLY;
        sumElement[element]++;
        DWORD f = dmgFloatNext++ % DMG_FLOATERS;
        dmgFloat[f].id = dmgRing[k].defId;
        dmgFloat[f].value = dmgRing[k].amount;
        dmgFloat[f].at = now;
        dmgFloat[f].color = damageColor(kind, element, damageStyle);
        dmgFloat[f].large = damageLarge(kind, damageStyle);
        dmgFloat[f].dx = 0;
        if (kind != DMG_ALLY) damageDpsHit(now, (dmgRing[k].taken + 128) >> 8);
    }

    static DWORD sumDot, sumDotPoison, sumDotFire, sumDotCold, sumDotOther, sumDotDealt, sumDotAlly, sumDotDead, sumDotHidden, sumDotUnknown;
    LONG dotWritten = InterlockedCompareExchange(&dmgDotWritten, 0, 0);


    if (!(wanted && poisonPatched)) dmgDotRead = dotWritten;
    if (!wanted) for (int s = 0; s < DMG_DOT_SLOTS; s++) dmgDot[s].used = 0;
    if (dotWritten - dmgDotRead > DMG_DOT_RING) dmgDotRead = dotWritten - DMG_DOT_RING;
    for (; dmgDotRead < dotWritten; dmgDotRead++) {
        LONG k = dmgDotRead % DMG_DOT_RING;
        if (dmgDotRing[k].element == 0xFFFFFFFF) {
            sumDotUnknown++;
            continue;
        }


        DWORD ownerType = dmgDotRing[k].ownerType, ownerId = dmgDotRing[k].ownerId, ownerTxt = 0xFFFFFFFF;
        if (ownerType == UNIT_MONSTER) {
            void* owner = findUnit(ownerId, UNIT_MONSTER);
            if (readable(owner, 0x10)) ownerTxt = RD32(owner, U_TXTNO);
        }
        int kind = damageDotKind(ownerType, ownerId, ownerTxt, me, dmgDotOwners, 32);
        if (kind == DMG_HIDE) {
            sumDotHidden++;
            continue;
        }
        damageDotAdd(dmgDot, DMG_DOT_SLOTS, dmgDotRing[k].defId, (long) dmgDotRing[k].loss, kind,
            (int) dmgDotRing[k].element, (int) dmgDotRing[k].dead, now);
    }
    for (int s = 0; s < DMG_DOT_SLOTS; s++) {
        int dead = dmgDot[s].dead, kind = dmgDot[s].kind, element = dmgDot[s].element;
        DWORD id = dmgDot[s].id;
        DWORD value = damageDotDue(&dmgDot[s], now);
        if (!value) continue;
        sumDot++;
        sumDotPoison += element == DMG_EL_POISON;
        sumDotFire += element == DMG_EL_FIRE;
        sumDotCold += element == DMG_EL_COLD;
        sumDotOther += element != DMG_EL_POISON && element != DMG_EL_FIRE && element != DMG_EL_COLD;
        sumDotDealt += value;
        sumDotAlly += kind == DMG_ALLY;
        sumDotDead += dead != 0;
#ifdef D2MOD_TESTCMDS
        static const char* const elementName2[] = {"physical", "fire", "cold", "lightning", "magic"};
        trace("damage: over time monster %lu %lu %s (%s%s)", (unsigned long) id, (unsigned long) value,
            element == DMG_EL_POISON ? "poison" : elementName2[element < 5 ? element : 0], kind == DMG_ALLY ? "ally" : "own",
            dead ? ", died" : "");
#endif
        DWORD f = dmgFloatNext++ % DMG_FLOATERS;
        dmgFloat[f].id = id;
        dmgFloat[f].value = value;
        dmgFloat[f].at = now;
        dmgFloat[f].color = damageDotColor(kind, element, damageStyle);
        dmgFloat[f].large = 0;
        dmgFloat[f].dx = DMG_DOT_DX;
        if (kind != DMG_ALLY) damageDpsHit(now, value);
    }

    if (damageDpsOn && damagePatched && singlePlayerGame()) {
        dmgDpsNow = damageDpsOf(dmgHits, DMG_HITS, &dmgFight, now);
        if (dmgDpsMax == DMG_DPS_NONE || dmgDpsNow > dmgDpsMax) dmgDpsMax = dmgDpsNow;
    }
    else dmgDpsNow = DMG_DPS_NONE;
    if (now - summaryAt >= 1000) {
        if (sumHits || sumHidden || sumStream)
            trace("damage: %lu hits, %lu dealt, %lu critical, %lu by mercenary or minions, %lu left out, %lu summed over time; %s, largest part phys %lu fire %lu cold %lu light %lu magic %lu",
                (unsigned long) sumHits, (unsigned long) sumDealt, (unsigned long) sumCrit, (unsigned long) sumAlly, (unsigned long) sumHidden,
                (unsigned long) sumStream,
                damageStyle == DMG_STYLE_ELEMENT ? "by element" : "simple", (unsigned long) sumElement[DMG_EL_PHYS],
                (unsigned long) sumElement[DMG_EL_FIRE], (unsigned long) sumElement[DMG_EL_COLD], (unsigned long) sumElement[DMG_EL_LIGHT],
                (unsigned long) sumElement[DMG_EL_MAGIC]);
        if (sumDot || sumDotHidden || sumDotUnknown)
            trace("damage: over time %lu numbers (poison %lu, fire %lu, cold %lu, other %lu), %lu dealt, %lu by mercenary or minions, %lu monsters died of it, %lu left out, %lu losses without a source",
                (unsigned long) sumDot, (unsigned long) sumDotPoison, (unsigned long) sumDotFire, (unsigned long) sumDotCold,
                (unsigned long) sumDotOther, (unsigned long) sumDotDealt,
                (unsigned long) sumDotAlly, (unsigned long) sumDotDead, (unsigned long) sumDotHidden, (unsigned long) sumDotUnknown);
        if (dmgDpsNow != DMG_DPS_NONE && (sumHits || sumStream || sumDot))
            trace("damage: DPS %lu, highest of this game %lu", (unsigned long) dmgDpsNow, (unsigned long) dmgDpsMax);
        summaryAt = now;
        sumHits = sumDealt = sumCrit = sumAlly = sumHidden = sumStream = 0;
        sumDot = sumDotPoison = sumDotFire = sumDotCold = sumDotOther = sumDotDealt = sumDotAlly = sumDotDead = sumDotHidden = sumDotUnknown = 0;
        for (int e = 0; e < 5; e++) sumElement[e] = 0;
    }
}
