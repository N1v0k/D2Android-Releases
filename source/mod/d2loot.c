
#define ITXT_NORMCODE  0x84
#define ITXT_UBERCODE  0x88
#define ITXT_ULTRACODE 0x8C

static int lootBaseClass(const BYTE* txt) {
    if (!readable((void*) txt, 0x90)) return -1;
    DWORD code = *(const DWORD*) (txt + ITXT_CODE);
    if (code == *(const DWORD*) (txt + ITXT_ULTRACODE)) return 2;
    if (code == *(const DWORD*) (txt + ITXT_UBERCODE)) return 1;
    if (code == *(const DWORD*) (txt + ITXT_NORMCODE)) return 0;
    return -1;
}

static int lootHidden(void* unit) {
    if (!lootMask || !unit || RD32(unit, U_TYPE) != UNIT_ITEM) return 0;
    void* data = RDP(unit, U_DATA);
    BYTE* txt = (BYTE*) fGetItemText(RD32(unit, U_TXTNO));
    if (!readable(data, 0x20) || !readable(txt, 0x130)) return 0;
    LootItem it;
    const char* code = itemCode(unit);
    it.category = itemCategory(unit, code, &it.tier);
    it.quality = (int) RD32(data, IDATA_QUALITY);
    DWORD flags = RD32(data, IDATA_FLAGS);
    it.ethereal = (flags & IFLAG_ETHEREAL) != 0;
    it.sockets = (flags & IFLAG_SOCKETED) ? (int) fGetUnitStat(unit, STAT_SOCKETS, 0) : 0;
    it.baseClass = lootBaseClass(txt);
    it.gold = it.category == CAT_GOLD ? (long) fGetUnitStat(unit, 14, 0) : -1;
    return lootHiddenBy(&it, lootMask, lootGoldBelow);
}
