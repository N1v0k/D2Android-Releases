






#ifdef D2MOD_TESTCMDS

static const DWORD probeStats[] = {0, 1, 2, 3, 7, 9, 31, 39, 40, 41, 42, 43, 44, 45, 46, 72, 73, 76, 77, 96, 99, 102,
    105, 127, 194, 216, 217};

static void probeUnit(const char* who, void* unit) {
    char line[480];
    int at = 0;
    for (unsigned i = 0; i < sizeof(probeStats) / sizeof(probeStats[0]) && at < (int) sizeof(line) - 32; i++) {
        long full = (long) fGetUnitStat(unit, probeStats[i], 0);
        long base = fGetBaseStat ? (long) fGetBaseStat(unit, probeStats[i], 0) : -1;
        if (full == 0 && base == 0) continue;
        at += full == base ? wsprintfA(line + at, " %lu=%ld", (unsigned long) probeStats[i], full)
            : wsprintfA(line + at, " %lu=%ld/b%ld", (unsigned long) probeStats[i], full, base);
    }
    line[at] = 0;
    void* data = RD32(unit, U_TYPE) == UNIT_ITEM ? RDP(unit, U_DATA) : NULL;
    if (readable(data, 0x70)) {
        trace("probe: %s %.3s quality %lu flags 0x%lx location %u page %u body %u:%s", who, itemCode(unit),
            (unsigned long) RD32(data, IDATA_QUALITY), (unsigned long) RD32(data, IDATA_FLAGS), RD8(data, IDATA_LOCATION),
            RD8(data, IDATA_NODEPAGE), RD8(data, IDATA_BODYLOC), line);
    }
    else trace("probe: %s:%s", who, line);
}


static void statProbe(void* player) {
    typedef DWORD (__stdcall *MaxLife_t)(void*);
    MaxLife_t maxLife = (MaxLife_t) GetProcAddress((HMODULE) common, (LPCSTR) 10574);
    trace("probe: hero level %lu, max life (ordinal 10574) %lu", (unsigned long) fGetUnitStat(player, 12, 0),
        maxLife ? (unsigned long) maxLife(player) : 0ul);
    probeUnit("hero", player);
    void* inventory = RDP(player, U_INVENTORY);
    for (DWORD loc = 1; loc <= 12 && fBodyLocItem && inventory; loc++) {
        void* item = fBodyLocItem(inventory, loc);
        if (!readable(item, 0x70)) continue;
        char who[16];
        wsprintfA(who, "body %lu", (unsigned long) loc);
        probeUnit(who, item);
        void* sockets = RDP(item, U_INVENTORY);
        int guard = 0;
        for (void* s = readable(sockets, 0x10) ? RDP(sockets, INV_FIRST) : NULL; readable(s, 0x18) && guard < 8; guard++) {
            probeUnit("  in socket", s);
            void* sd = RDP(s, U_DATA);
            s = readable(sd, 0x70) ? RDP(sd, IDATA_NEXTINV) : NULL;
        }
    }
    void* hovered = RDP(client, RVA_HoverItem);
    if (readable(hovered, 0x70) && RD32(hovered, U_TYPE) == UNIT_ITEM) {
        trace("probe: hovered item, mode %lu (0 inventory, 1 equipped, 3 on the ground)", (unsigned long) RD32(hovered, U_MODE));
        probeUnit("hovered", hovered);
    }
    else trace("probe: no hovered item (D2Client+0x%x)", RVA_HoverItem);
}

#endif
