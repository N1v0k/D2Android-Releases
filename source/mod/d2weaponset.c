















#define WS_STAT_OSKILL       97
#define WS_STAT_CHARGED      204
#define WS_BODY_SWAP_RIGHT   11
#define WS_BODY_SWAP_LEFT    12
#define WS_KEYS              16
#define CAP3_WEAPONSET       0x04

static BYTE wsCounter;
static WORD wsMask;
static WORD wsHaveKeys;
static DWORD wsHands[2];
static int wsHave;


static int wsHasSkill(void* player, WORD skill) {
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) return 1;
    int guard = 0;
    for (void* k = RDP(info, INFO_FIRST); readable(k, SKILL_ITEM + 4) && guard < 400; k = RDP(k, SKILL_NEXT), guard++)
        if (skillId(k) == skill) return 1;
    return 0;
}


static int wsItemGives(void* item, WORD skill) {
    if (fGetUnitStat(item, WS_STAT_OSKILL, skill) > 0) return 1;
    for (DWORD level = 1; level < 64; level++)
        if (fGetUnitStat(item, WS_STAT_CHARGED, ((DWORD) skill << 6) | level) & 0xFF) return 1;
    return 0;
}





static int weaponSetTick(void* player) {
    static DWORD maskAt, handsAt;
    void* inventory = player ? RDP(player, U_INVENTORY) : NULL;
    if (!fGetUnitStat || !readable(inventory, 0x10)) return 0;
    DWORD hands[2] = {0, 0};
    void* swap[2] = {NULL, NULL};
    int steps = 0;
    for (void* item = RDP(inventory, INV_FIRST); item && steps < 512; steps++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) return 0;


        BYTE page = RD8(data, IDATA_NODEPAGE), location = RD8(data, IDATA_BODYLOC);
        if (page == 3 && (location == 4 || location == 5)) hands[location - 4] = RD32(item, U_ID);
        else if ((page == 3 || page == 4) && (location == WS_BODY_SWAP_RIGHT || location == WS_BODY_SWAP_LEFT))
            swap[location - WS_BODY_SWAP_RIGHT] = item;
        item = RDP(data, IDATA_NEXTINV);
    }
    int changed = 0;
    if (wsHave && (hands[0] != wsHands[0] || hands[1] != wsHands[1])) {
        wsCounter++;
        changed = 1;
        trace("weapon set: hands %lu/%lu (counter %u)", (unsigned long) hands[0], (unsigned long) hands[1], wsCounter);
    }
    wsHands[0] = hands[0];
    wsHands[1] = hands[1];
    wsHave = 1;
    DWORD now = GetTickCount();
    if (changed) handsAt = now;


    if (!changed && now - maskAt < 500 && now - handsAt > 2000) return 0;
    maskAt = now;
    WORD mask = 0, have = 0;
    for (int i = 0; i < WS_KEYS; i++) {
        DWORD skill = RD32(client, RVA_HotkeySkill + i * 4);
        if (skill >= 0xFFFF) continue;
        if (wsHasSkill(player, (WORD) skill)) {
            have |= (WORD) (1u << i);
            continue;
        }
        for (int k = 0; k < 2; k++)
            if (swap[k] && wsItemGives(swap[k], (WORD) skill)) { mask |= (WORD) (1u << i); break; }
    }


    if (have != wsHaveKeys) {
        wsHaveKeys = have;
        changed = 1;
    }
    if (mask != wsMask) {
        logLine("weapon set: keys only with the other weapons 0x%04X", mask);
        wsMask = mask;
        changed = 1;
    }
    return changed;
}

static BYTE weaponSetCapabilities3(void) {
    return fGetUnitStat ? CAP3_WEAPONSET : 0;
}


static void putWeaponSetFields(Packet* p) {
    putU8(p, wsCounter);
    putU16(p, wsMask);
}
