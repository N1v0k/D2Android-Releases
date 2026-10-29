











#define SP_PY_CURRENT   0x10
#define SP_PY_SELF      0x14
#define SP_PY_SHARED    0x18
#define SP_STASH_ID     0x00
#define SP_STASH_FLAGS  0x04
#define SP_STASH_ITEMS  0x0C
#define SP_STASH_NEXT   0x14
#define SP_MAX_ITEMS    120
#define SP_MAX_PAGES    10000
#define CAP3_STASHPAGES 0x02

static int stashPagesState;


static int stashPagesKnown(void) {
    if (stashPagesState) return stashPagesState > 0;
    if (!plugyLoaded || !common) return 0;
    HMODULE plugy = GetModuleHandleA("PlugY.dll");
    char path[MAX_PATH];
    DWORD n = plugy ? GetModuleFileNameA(plugy, path, sizeof(path)) : 0;
    int active = 0;
    if (n > 0 && n < sizeof(path) - 16) {
        char* slash = strrchr(path, '\\');
        strcpy(slash ? slash + 1 : path, "PlugY.ini");
        active = GetPrivateProfileIntA("STASH", "ActiveMultiPageStash", 0, path) != 0;
    }
    int ok = active && sgPlayerDataHooked(plugy);
    stashPagesState = ok ? 1 : -1;
    logLine(ok ? "stash pages: PlugY's player data verified" : active ? "stash pages: PlugY's player data not as expected"
        : "stash pages: off (no PlugY or no multi-page stash)");
    return ok;
}


static const BYTE* stashPagesData(void* hero) {
    if (!hero || !stashPagesKnown() || !readable((const BYTE*) hero + SG_UNIT_DATA, 4)) return NULL;
    const BYTE* data = *(const BYTE* const*) ((const BYTE*) hero + SG_UNIT_DATA);
    const BYTE* py = data ? data + SG_PLAYERDATA_SIZE : NULL;
    return py && readable(py, SP_PY_SHARED + 4) ? py : NULL;
}



static void putPageItem(Packet* p, void* item, DWORD page, int shared) {
    void* data = RDP(item, U_DATA);
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    const char* code = itemCode(item);
    DWORD flags = RD32(data, IDATA_FLAGS);
    putU32(p, RD32(item, U_ID));
    for (int k = 0; k < 4; k++) putU8(p, code[k] && code[k] != ' ' ? (BYTE) code[k] : ' ');
    putU8(p, (BYTE) RD32(data, IDATA_QUALITY));
    putU8(p, ((flags & IFLAG_ETHEREAL) ? 1 : 0) | ((flags & IFLAG_SOCKETED) ? 2 : 0) | ((flags & IFLAG_IDENTIFIED) ? 4 : 0)
        | ((flags & IFLAG_RUNEWORD) ? 8 : 0));
    putU8(p, readable(txt, 0x120) ? ((txt[ITXT_WIDTH] & 15) << 4) | (txt[ITXT_HEIGHT] & 15) : 0x11);

    DWORD sockets = (flags & IFLAG_SOCKETED) ? fGetUnitStat(item, STAT_SOCKETS, 0) : 0;
    int filled = 0, guard = 0;
    void* inside = RDP(item, U_INVENTORY);
    for (void* s = readable(inside, 0x10) ? RDP(inside, INV_FIRST) : NULL; readable(s, 0x18) && guard < 8; guard++) {
        filled++;
        void* sd = RDP(s, U_DATA);
        s = readable(sd, 0x70) ? RDP(sd, IDATA_NEXTINV) : NULL;
    }
    putU8(p, (BYTE) (sockets > 255 ? 255 : sockets));
    putU8(p, (BYTE) filled);
    putU16(p, (WORD) (page + 1 > 0xFFFF ? 0xFFFF : page + 1));
    putU8(p, (BYTE) (shared ? 1 : 0));
}





static void putStashPages(Packet* p, void* hero) {
    const BYTE* py = stashPagesData(hero);
    if (!py) return;
    int room = ((int) sizeof(p->data) - p->len - 2) / 16;
    if (room <= 0) return;
    int max = room < SP_MAX_ITEMS ? room : SP_MAX_ITEMS;
    int countAt = p->len, count = 0, cut = 0;
    putU8(p, 0);
    putU8(p, 0);
    const BYTE* current = RDP(py, SP_PY_CURRENT);
    for (int list = 0; list < 2; list++) {
        int pages = 0;
        for (const BYTE* st = RDP(py, list ? SP_PY_SHARED : SP_PY_SELF); st && pages < SP_MAX_PAGES; pages++) {
            if (!readable(st, SP_STASH_NEXT + 4)) break;
            if (st != current) {
                DWORD id = RD32(st, SP_STASH_ID);
                int shared = RD32(st, SP_STASH_FLAGS) & 1, guard = 0;
                for (void* item = RDP(st, SP_STASH_ITEMS); item && guard < 512; guard++) {
                    void* data = readable(item, 0x30) ? RDP(item, U_DATA) : NULL;
                    if (!readable(data, 0x70)) break;
                    if (count >= max) {
                        cut = 1;
                        break;
                    }
                    putPageItem(p, item, id, shared);
                    count++;
                    item = RDP(data, IDATA_NEXTINV);
                }
            }
            st = RDP(st, SP_STASH_NEXT);
        }
    }
    p->data[countAt] = (BYTE) count;
    p->data[countAt + 1] = (BYTE) cut;
}
