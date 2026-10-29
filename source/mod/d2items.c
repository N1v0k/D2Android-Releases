










#define RVA_GetItemName        0x958C0
#define RVA_GetItemNameEnd     0x96775
#define RVA_ItemToBeltSite     0x9B317
#define RVA_SendPacket5        0xB6A30

#define ITXT_MAXAC    0xD0
#define ITXT_MINDAM   0xFE
#define ITXT_MINMISDAM 0x100
#define ITXT_2HMINDAM 0x102
#define ITXT_WIDTH    0x10F
#define ITXT_HEIGHT   0x110
#define ITXT_TYPE     0x11E
#define ITXT_QUEST    0x12A
#define IDATA_FLAGS   0x18
#define IFLAG_IDENTIFIED 0x10
#define IFLAG_SOCKETED   0x800
#define IFLAG_ETHEREAL   0x400000
#define IFLAG_RUNEWORD   0x4000000
#define STAT_SOCKETS  194

#define INV_W 10
#define INV_H 4
#define INV_MAX_W 16
#define INV_MAX_H 8
#define INV_MAX_CELLS (INV_MAX_W * INV_MAX_H)
#define PKT_GRID 17

static int invGridW = INV_W, invGridH = INV_H;


#include "d2cats.h"

#define CMD_BELT 11
#define PKT_ITEMS 4

static int itemNameAvailable, beltMoveAvailable;
static BYTE beltMoves;
static BYTE stackMax(int row);
static int singlePlayerGame(void);

static void checkItemSites(void) {
    static const BYTE nameStart[] = {0xB8, 0xCC, 0x1B, 0x00, 0x00, 0xE8};
    static const BYTE nameEnd[] = {0x81, 0xC4, 0xCC, 0x1B, 0x00, 0x00, 0xC2, 0x0C, 0x00};
    static const BYTE belt[] = {0x8B, 0x4D, 0x0C, 0xB0, 0x63, 0xE8};
    BYTE* name = (BYTE*) (client + RVA_GetItemName);
    BYTE* end = (BYTE*) (client + RVA_GetItemNameEnd - 6);
    BYTE* site = (BYTE*) (client + RVA_ItemToBeltSite);
    itemNameAvailable = readable(name, sizeof(nameStart)) && memcmp(name, nameStart, sizeof(nameStart)) == 0
        && readable(end, sizeof(nameEnd)) && memcmp(end, nameEnd, sizeof(nameEnd)) == 0;

    beltMoveAvailable = readable(site, 10) && memcmp(site, belt, sizeof(belt)) == 0
        && (DWORD) (site + 10) + *(DWORD*) (site + 6) == client + RVA_SendPacket5;
    logLine("item sites: names %s, belt refill %s", itemNameAvailable ? "verified" : "NOT as expected",
        beltMoveAvailable ? "verified" : "NOT as expected");
}

static int codeIs(const char* code, const char* what) {
    return code[0] == what[0] && code[1] == what[1] && code[2] == what[2];
}


static int itemCategory(void* item, const char* code, int* tier) {
    *tier = 0;
    int potion = potionKind(code);
    if (codeIs(code, "gld")) return CAT_GOLD;
    if (potion >= 1 && potion <= 3) {
        *tier = potionTier(code);
        return potion == 1 ? CAT_HEAL : (potion == 2 ? CAT_MANA : CAT_REJUV);
    }
    if (codeIs(code, "vps") || codeIs(code, "yps") || codeIs(code, "wms")) return CAT_POTION_OTHER;
    if (codeIs(code, "tsc")) return CAT_TP_SCROLL;
    if (codeIs(code, "isc")) return CAT_ID_SCROLL;
    if (codeIs(code, "tbk")) return CAT_TP_TOME;
    if (codeIs(code, "ibk")) return CAT_ID_TOME;
    if (code[0] == 'r' && code[1] >= '0' && code[1] <= '3' && code[2] >= '0' && code[2] <= '9') {
        *tier = (code[1] - '0') * 10 + (code[2] - '0');
        return CAT_RUNE;
    }
    if (code[0] == 'g' && code[1] && strchr("cfslzp", code[1]) && code[2] && strchr("vybgrw", code[2])) {
        *tier = (int) (strchr("cfs", code[1]) ? strchr("cfs", code[1]) - "cfs" + 1 : (code[1] == 'p' ? 5 : 4));
        return CAT_GEM;
    }
    if (code[0] == 's' && code[1] == 'k' && code[2] && strchr("cfulz", code[2])) {
        *tier = (int) (strchr("cfulz", code[2]) - "cfulz") + 1;
        return CAT_GEM;
    }
    if (codeIs(code, "jew")) return CAT_JEWEL;
    if (codeIs(code, "rin")) return CAT_RING;
    if (codeIs(code, "amu")) return CAT_AMULET;
    if (code[0] == 'c' && code[1] == 'm' && code[2] >= '1' && code[2] <= '3') return CAT_CHARM;
    if (codeIs(code, "aqv") || codeIs(code, "cqv")) return CAT_AMMO;
    if (codeIs(code, "key")) return CAT_KEY;
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    if (!readable(txt, 0x130)) return CAT_OTHER;
    if (txt[ITXT_QUEST]) return CAT_QUEST;
    if (*(int*) (txt + ITXT_MAXAC) > 0) return CAT_ARMOR;
    if (txt[ITXT_MINDAM] || txt[ITXT_MINMISDAM] || txt[ITXT_2HMINDAM]) return CAT_WEAPON;
    return CAT_OTHER;
}


static int beltRows(const char* code) {
    if (!code) return 1;
    if (codeIs(code, "lbl") || codeIs(code, "vbl")) return 2;
    if (codeIs(code, "mbl") || codeIs(code, "tbl")) return 3;
    return 4;
}


typedef struct {
    DWORD id, flags;
    BYTE len;
    char text[63];
} NameEntry;

static NameEntry nameCache[64];

static int itemNameUtf8(void* item, char* out, int size) {
    DWORD id = RD32(item, U_ID);
    void* data = RDP(item, U_DATA);
    DWORD flags = readable(data, 0x20) ? RD32(data, IDATA_FLAGS) : 0;
    NameEntry* e = &nameCache[id & 63];
    if (e->id == id && e->flags == flags && e->len) {
        memcpy(out, e->text, e->len);
        return e->len;
    }
    if (!itemNameAvailable) return 0;
    wchar_t name[256];
    name[0] = 0;
    BOOL (__stdcall *getItemName)(void*, wchar_t*, DWORD) = (BOOL (__stdcall *)(void*, wchar_t*, DWORD)) (client + RVA_GetItemName);
    getItemName(item, name, 255);
    name[255] = 0;

    wchar_t clean[128];
    int n = 0;
    for (int i = 0; name[i] && n < 127; i++) {
        if (name[i] == 0xFF && name[i + 1] == 'c' && name[i + 2]) {
            i += 2;
            continue;
        }
        clean[n++] = name[i];
    }
    int len = WideCharToMultiByte(CP_UTF8, 0, clean, n, out, size, NULL, NULL);
    if (len <= 0) return 0;
    if (len <= (int) sizeof(e->text)) {
        e->id = id;
        e->flags = flags;
        e->len = (BYTE) len;
        memcpy(e->text, out, len);
    }
    return len;
}


typedef struct {
    BYTE rows;
    BYTE belt[3], inventory[3];
    BYTE tpTome, tpScrolls, idTome, idScrolls;
    BYTE used[INV_MAX_CELLS / 8];
} Carried;

static void readCarried(void* player, Carried* c) {
    memset(c, 0, sizeof(*c));
    c->tpTome = c->idTome = 0xFF;
    const char* beltCode = NULL;
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        BYTE location = RD8(data, IDATA_LOCATION), page = RD8(data, IDATA_NODEPAGE);
        const char* code = itemCode(item);
        int inBelt = location == 0xFF && page == 2, inInventory = location == 0x00 && page == 1;
        if (page == 3 && RD8(data, IDATA_BODYLOC) == 8) beltCode = code;
        int kind = potionKind(code);
        if ((inBelt || inInventory) && kind >= 1 && kind <= 3) {
            BYTE* count = inBelt ? &c->belt[kind - 1] : &c->inventory[kind - 1];
            if (*count < 255) (*count)++;
        }
        if (inInventory && codeIs(code, "tbk")) c->tpTome = (BYTE) fGetUnitStat(item, STAT_QUANTITY, 0);
        if (inInventory && codeIs(code, "ibk")) c->idTome = (BYTE) fGetUnitStat(item, STAT_QUANTITY, 0);
        if ((inBelt || inInventory) && codeIs(code, "tsc") && c->tpScrolls < 255) c->tpScrolls++;
        if ((inBelt || inInventory) && codeIs(code, "isc") && c->idScrolls < 255) c->idScrolls++;
        if (inInventory) {
            void* path = RDP(item, U_PATH);
            BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
            if (readable(path, 0x14) && readable(txt, 0x120)) {
                int x0 = (int) RD32(path, IPATH_X), y0 = (int) RD32(path, IPATH_Y);
                for (int y = y0; y < y0 + txt[ITXT_HEIGHT] && y < invGridH; y++) {
                    for (int x = x0; x < x0 + txt[ITXT_WIDTH] && x < invGridW; x++) {
                        if (x >= 0 && y >= 0) c->used[(y * invGridW + x) / 8] |= (BYTE) (1 << ((y * invGridW + x) % 8));
                    }
                }
            }
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    c->rows = (BYTE) beltRows(beltCode);
}





static void sendItems(void* player, void** units, int count) {
    static DWORD lastHash, lastAt;
    Carried c;
    readCarried(player, &c);
    DWORD hash = 2166136261u;
#define MIX(v) (hash = (hash ^ (DWORD) (v)) * 16777619u)
    for (unsigned i = 0; i < sizeof(c); i++) MIX(((BYTE*) &c)[i]);
    for (int i = 0; i < count; i++) {
        void* data = RDP(units[i], U_DATA);
        MIX(RD32(units[i], U_ID));
        MIX(readable(data, 0x20) ? RD32(data, IDATA_FLAGS) : 0);
    }
    MIX(beltMoves);
    MIX(count);
    MIX(newLootVisible(GetTickCount()));
    MIX(shopJobKey(0));
    MIX(shopJobKey(1));
    MIX(shopJobKey(2));
#undef MIX
    DWORD now = GetTickCount();
    if (hash == lastHash && now - lastAt < 1000) return;
    lastHash = hash;
    lastAt = now;

    Packet p;


    if (invGridH > INV_H || invGridW != INV_W) {
        int bytes = (invGridW * invGridH + 7) / 8;
        putHeader(&p, PKT_GRID);
        putU8(&p, (unsigned) invGridW);
        putU8(&p, (unsigned) invGridH);
        putU8(&p, (unsigned) bytes);
        for (int i = 0; i < bytes; i++) putU8(&p, c.used[i]);
        sendPacket(&p);
    }
    putHeader(&p, PKT_ITEMS);
    putU8(&p, (itemNameAvailable ? 1 : 0) | (beltMoveAvailable ? 2 : 0) | (labelFilterAvailable ? 4 : 0)
        | (newLootVisible(now) ? 8 : 0) | (takeAvailable ? 16 : 0) | (shopAvailable ? 32 : 0) | (sellAvailable ? 64 : 0) | (stackMax(0) ? 128 : 0));
    putU8(&p, beltMoves);
    putU8(&p, c.rows);
    for (int k = 0; k < 3; k++) {
        putU8(&p, c.belt[k]);
        putU8(&p, c.inventory[k]);
    }
    putU8(&p, c.tpTome);
    putU8(&p, c.tpScrolls);
    putU8(&p, c.idTome);
    putU8(&p, c.idScrolls);
    for (int i = 0; i < 5; i++) putU8(&p, c.used[i]);
    putU8(&p, count);
    for (int i = 0; i < count; i++) {
        void* item = units[i];
        void* data = RDP(item, U_DATA);
        const char* code = itemCode(item);
        BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
        int tier, category = itemCategory(item, code, &tier);
        DWORD flags = readable(data, 0x20) ? RD32(data, IDATA_FLAGS) : 0;
        putU32(&p, RD32(item, U_ID));
        for (int k = 0; k < 4; k++) putU8(&p, code[k] && code[k] != ' ' ? (BYTE) code[k] : ' ');
        putU8(&p, category);
        putU8(&p, tier);
        putU8(&p, data ? RD32(data, IDATA_QUALITY) : 0);
        putU8(&p, ((flags & IFLAG_ETHEREAL) ? 1 : 0) | ((flags & IFLAG_SOCKETED) ? 2 : 0) | ((flags & IFLAG_IDENTIFIED) ? 4 : 0)
            | ((flags & IFLAG_RUNEWORD) ? 8 : 0) | (lootHidden(item) ? 16 : 0));
        DWORD sockets = (flags & IFLAG_SOCKETED) ? fGetUnitStat(item, STAT_SOCKETS, 0) : 0;
        putU8(&p, sockets > 6 ? 6 : sockets);
        putU16(&p, readable(txt, 0x120) ? *(WORD*) (txt + ITXT_TYPE) : 0xFFFF);
        putU8(&p, readable(txt, 0x120) ? ((txt[ITXT_WIDTH] & 15) << 4) | (txt[ITXT_HEIGHT] & 15) : 0x11);
        DWORD quantity = category == CAT_GOLD || category == CAT_AMMO ? fGetUnitStat(item, category == CAT_GOLD ? 14 : STAT_QUANTITY, 0) : 0;
        putU16(&p, quantity > 0xFFFF ? 0xFFFF : quantity);
        char name[120];
        int len = itemNameUtf8(item, name, sizeof(name));
        if (len > 60) len = 60;
        putU8(&p, len);
        for (int k = 0; k < len; k++) putU8(&p, (BYTE) name[k]);
    }
    putShopJob(&p);


    if (stackMax(0)) for (int i = 0; i < 3; i++) putU8(&p, stackMax(i));
    sendPacket(&p);
}














#define CMD_STACKS     20
#define ITXT_MAXSTACK  0xE8
#define ITXT_STACKABLE 0x132
static const struct { char code[4]; DWORD original, large; } stackRows[] = {
    {{'t', 'b', 'k', ' '}, 20, 80}, {{'i', 'b', 'k', ' '}, 20, 80}, {{'k', 'e', 'y', ' '}, 12, 50},
};
#define STACK_ROWS 3
static BYTE* stackTxt[STACK_ROWS];
static int stackRowsState;
static int stacksLarge;

static void findStackRows(void) {
    stackRowsState = -1;
    int found = 0;
    for (DWORD no = 0; no < 4096 && found < STACK_ROWS; no++) {
        BYTE* txt = (BYTE*) fGetItemText(no);
        if (!txt) break;
        if (!readable(txt, ITXT_STACKABLE + 1)) return;
        for (int i = 0; i < STACK_ROWS; i++) {
            if (!stackTxt[i] && memcmp(txt + ITXT_CODE, stackRows[i].code, 4) == 0) {
                stackTxt[i] = txt;
                found++;
            }
        }
    }
    for (int i = 0; i < STACK_ROWS; i++) {
        DWORD max = stackTxt[i] ? *(DWORD*) (stackTxt[i] + ITXT_MAXSTACK) : 0;
        if (!stackTxt[i] || stackTxt[i][ITXT_STACKABLE] != 1 || (max != stackRows[i].original && max != stackRows[i].large)) {
            logLine("stack rows NOT as expected: %.3s %s, stackable %d, max %lu", stackRows[i].code,
                stackTxt[i] ? "found" : "missing", stackTxt[i] ? stackTxt[i][ITXT_STACKABLE] : -1, (unsigned long) max);
            return;
        }
    }
    stackRowsState = 1;
    logLine("stack rows verified: tbk %lu, ibk %lu, key %lu", (unsigned long) *(DWORD*) (stackTxt[0] + ITXT_MAXSTACK),
        (unsigned long) *(DWORD*) (stackTxt[1] + ITXT_MAXSTACK), (unsigned long) *(DWORD*) (stackTxt[2] + ITXT_MAXSTACK));
}

static void setLargeStacks(int large) {
    if (large == stacksLarge) return;
    if (stackRowsState == 0) findStackRows();
    if (stackRowsState != 1) return;
    for (int i = 0; i < STACK_ROWS; i++) {
        DWORD* max = (DWORD*) (stackTxt[i] + ITXT_MAXSTACK);
        DWORD from = large ? stackRows[i].original : stackRows[i].large;
        if (*max == from) *max = large ? stackRows[i].large : stackRows[i].original;
    }
    stacksLarge = large;
    logLine("stacks %s: tomes %lu, keys %lu", large ? "large" : "as in the game", (unsigned long) *(DWORD*) (stackTxt[0] + ITXT_MAXSTACK),
        (unsigned long) *(DWORD*) (stackTxt[2] + ITXT_MAXSTACK));
}


static BYTE stackMax(int row) {
    if (stackRowsState == 0) findStackRows();
    if (stackRowsState != 1) return 0;
    DWORD max = *(DWORD*) (stackTxt[row] + ITXT_MAXSTACK);
    return (BYTE) (max > 254 ? 254 : max);
}


static DWORD loggedItems[64];

static void logNewItems(void** units, int count) {
    for (int i = 0; i < count; i++) {
        DWORD id = RD32(units[i], U_ID);
        if (loggedItems[id & 63] == id + 1) continue;
        loggedItems[id & 63] = id + 1;
        const char* code = itemCode(units[i]);
        BYTE* txt = (BYTE*) fGetItemText(RD32(units[i], U_TXTNO));
        int tier, category = itemCategory(units[i], code, &tier);
        char name[120];
        int len = itemNameUtf8(units[i], name, sizeof(name) - 1);
        name[len > 0 ? len : 0] = 0;
        for (int k = 0; name[k]; k++) if (name[k] == '\n') name[k] = '|';
        void* data = RDP(units[i], U_DATA);
        trace("item %lu: %.3s cat %d tier %d q %lu size %dx%d type %u ac %d dmg %d/%d/%d flags %08lX \"%s\"",
            (unsigned long) id, code, category, tier, (unsigned long) (data ? RD32(data, IDATA_QUALITY) : 0),
            txt ? txt[ITXT_WIDTH] : 0, txt ? txt[ITXT_HEIGHT] : 0, txt ? *(WORD*) (txt + ITXT_TYPE) : 0,
            txt ? *(int*) (txt + ITXT_MAXAC) : 0, txt ? txt[ITXT_MINDAM] : 0, txt ? txt[ITXT_MINMISDAM] : 0,
            txt ? txt[ITXT_2HMINDAM] : 0, (unsigned long) (data ? RD32(data, IDATA_FLAGS) : 0), name);
    }
}


static void forgetItems(void) {
    memset(nameCache, 0, sizeof(nameCache));
    memset(loggedItems, 0, sizeof(loggedItems));
}






static void moveToBelt(void* player, int kind) {
    if (!beltMoveAvailable) return;
    void* inventory = RDP(player, U_INVENTORY);
    void* best = NULL;
    int bestTier = 127, guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1) {
            const char* code = itemCode(item);
            if (potionKind(code) == kind && potionTier(code) < bestTier) {
                best = item;
                bestTier = potionTier(code);
            }
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    if (!best) {
        logLine("belt refill kind %d: none in the inventory", kind);
        return;
    }
    BYTE packet[5];
    packet[0] = 0x63;
    *(DWORD*) (packet + 1) = RD32(best, U_ID);
    fSendPacket(5, 1, packet);
    beltMoves++;
    logLine("belt refill kind %d: item %lu (%.3s)", kind, (unsigned long) RD32(best, U_ID), itemCode(best));
}
