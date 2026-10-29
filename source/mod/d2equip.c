



















#define CMD_EQUIP 22
#ifndef EQUIP_STEP_MS
#define EQUIP_STEP_MS 1000
#endif


#define EQUIP_LATE_MS 5000
#define EQ_INV_CURSOR 0x20

#define CAP_EQUIP 0x04

#define EQUIP_OK          1
#define EQUIP_REFUSED     2
#define EQUIP_CURSOR      3
#define EQUIP_NOT_STARTED 4


enum { EQ_NONE, EQ_TO_CURSOR, EQ_TO_BODY, EQ_OLD_BACK, EQ_UNDO };

static int equipAvailable;
static BYTE equipCounter, equipResult, equipReported;
static struct { int step; DWORD id, oldId, since, fromX, fromY; BYTE loc; } equip;
static struct { DWORD id, fromX, fromY, since; } equipLate;

static int singlePlayerGame(void);
static int worldPaused(void);


static void checkEquipSites(void) {
    static const BYTE toCursor[] = {0x8B, 0x4D, 0x0C, 0xB0, 0x19, 0xE8};
    static const BYTE swap[] = {0x8B, 0x4E, 0x0C, 0x0F, 0xB7, 0xD5, 0xB0, 0x1D, 0xE8};
    static const BYTE toBody[] = {0x8B, 0x4E, 0x0C, 0x0F, 0xB7, 0xD5, 0xB0, 0x1A, 0xE8};
    static const BYTE toInventory[] = {0xB0, 0x18, 0x8B, 0x4D, 0x0C, 0xE8};
    BYTE *a = (BYTE*) (client + 0x9B350), *b = (BYTE*) (client + 0x9A778), *c = (BYTE*) (client + 0x9A6A9),
         *d = (BYTE*) (client + 0x9B7DB);
    equipAvailable = fSendPacket && fGetItemText && fGetUiVar
        && readable(a, sizeof(toCursor)) && memcmp(a, toCursor, sizeof(toCursor)) == 0
        && readable(b, sizeof(swap)) && memcmp(b, swap, sizeof(swap)) == 0
        && readable(c, sizeof(toBody)) && memcmp(c, toBody, sizeof(toBody)) == 0
        && readable(d, sizeof(toInventory)) && memcmp(d, toInventory, sizeof(toInventory)) == 0;
    logLine(equipAvailable ? "equip sites verified" : "equip sites not as expected: no Anlegen");
}

static void* equipCursorItem(void* player) {
    void* inventory = RDP(player, U_INVENTORY);
    void* item = readable(inventory, 0x24) ? RDP(inventory, EQ_INV_CURSOR) : NULL;
    return readable(item, 0x10) ? item : NULL;
}

static DWORD equipCursorId(void* player) {
    void* item = equipCursorItem(player);
    return item ? RD32(item, U_ID) : 0;
}


static void* equipFind(void* player, DWORD id, int body, BYTE loc) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        BYTE location = RD8(data, IDATA_LOCATION), page = RD8(data, IDATA_NODEPAGE);
        if (body ? page == 3 && RD8(data, IDATA_BODYLOC) == loc : location == 0x00 && page == 1 && RD32(item, U_ID) == id)
            return item;
        item = RDP(data, IDATA_NEXTINV);
    }
    return NULL;
}

static int equipSize(void* item, int* w, int* h) {
    BYTE* txt = item ? (BYTE*) fGetItemText(RD32(item, U_TXTNO)) : NULL;
    if (!readable(txt, 0x120)) return 0;
    *w = txt[ITXT_WIDTH];
    *h = txt[ITXT_HEIGHT];
    return *w >= 1 && *h >= 1;
}


static int equipFreePlace(void* player, DWORD skip, int w, int h, DWORD* outX, DWORD* outY) {
    BYTE used[INV_MAX_CELLS] = {0};
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        void* path = RDP(item, U_PATH);
        int iw, ih;
        if (RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1 && RD32(item, U_ID) != skip
            && readable(path, 0x14) && equipSize(item, &iw, &ih)) {
            int x0 = (int) RD32(path, IPATH_X), y0 = (int) RD32(path, IPATH_Y);
            for (int y = y0; y < y0 + ih && y < invGridH; y++)
                for (int x = x0; x < x0 + iw && x < invGridW; x++)
                    if (x >= 0 && y >= 0) used[y * invGridW + x] = 1;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    for (int x = 0; x + w <= invGridW; x++) {
        for (int y = 0; y + h <= invGridH; y++) {
            int free = 1;
            for (int dy = 0; dy < h && free; dy++)
                for (int dx = 0; dx < w && free; dx++)
                    if (used[(y + dy) * invGridW + x + dx]) free = 0;
            if (free) {
                *outX = (DWORD) x;
                *outY = (DWORD) y;
                return 1;
            }
        }
    }
    return 0;
}

static void equipSendId(BYTE op, DWORD id) {
    BYTE packet[5] = {op};
    *(DWORD*) (packet + 1) = id;
    fSendPacket(5, 1, packet);
}

static void equipSendBody(BYTE op, DWORD id, DWORD loc) {
    BYTE packet[9] = {op};
    *(DWORD*) (packet + 1) = id;
    *(DWORD*) (packet + 5) = loc;
    fSendPacket(9, 1, packet);
}

static void equipSendToInventory(DWORD id, DWORD x, DWORD y) {
    BYTE packet[17] = {0x18};
    *(DWORD*) (packet + 1) = id;
    *(DWORD*) (packet + 5) = x;
    *(DWORD*) (packet + 9) = y;
    *(DWORD*) (packet + 13) = 0;
    fSendPacket(17, 1, packet);
}

static void equipDone(int result, const char* why) {
    equip.step = EQ_NONE;
    equipResult = (BYTE) result;
    equipCounter++;
    logLine("equip %lu: %s (result %d)", (unsigned long) equip.id, why, result);
}


static int equipReportDue(void) {
    if (equipReported == equipCounter) return 0;
    equipReported = equipCounter;
    return 1;
}


static void equipStart(void* player, BYTE loc, DWORD id, DWORD now) {
    if (equip.step != EQ_NONE) {
        logLine("equip %lu: still busy with %lu", (unsigned long) id, (unsigned long) equip.id);
        return;
    }
    equipLate.id = 0;
    equip.id = id;
    equip.loc = loc;
    equip.oldId = 0;
    const char* why = !equipAvailable ? "senders not checked" : !singlePlayerGame() ? "not single player"
        : worldPaused() ? "paused" : loc != 4 && loc != 5 ? "not a hand"
        : getUiVar(0x0C) || getUiVar(0x19) || getUiVar(0x1A) ? "shop, stash or cube open"
        : equipCursorId(player) ? "something on the cursor" : NULL;
    void* item = why ? NULL : equipFind(player, id, 0, 0);
    void* path = item ? RDP(item, U_PATH) : NULL;
    if (!why && (!item || !readable(path, 0x14))) why = "not in the inventory";
    if (why) {
        equipDone(EQUIP_NOT_STARTED, why);
        return;
    }
    equip.fromX = RD32(path, IPATH_X);
    equip.fromY = RD32(path, IPATH_Y);
    void* old = equipFind(player, 0, 1, loc);
    if (old) {

        int w, h;
        DWORD x, y;
        if (!equipSize(old, &w, &h) || !equipFreePlace(player, id, w, h, &x, &y)) {
            equipDone(EQUIP_NOT_STARTED, "no room for the old item");
            return;
        }
        equip.oldId = RD32(old, U_ID);
    }
    equipSendId(0x19, id);
    equip.step = EQ_TO_CURSOR;
    equip.since = now;
    logLine("equip %lu to place %u (old %lu): 0x19 from %lu,%lu", (unsigned long) id, loc, (unsigned long) equip.oldId,
        (unsigned long) equip.fromX, (unsigned long) equip.fromY);
}


static void equipLateTick(void* player, DWORD now) {
    if (!equipLate.id) return;
    if (!player) {
        equipLate.id = 0;
        return;
    }
    if (worldPaused()) {
        equipLate.since = now;
        return;
    }
    if (equipCursorId(player) == equipLate.id) {
        equipSendToInventory(equipLate.id, equipLate.fromX, equipLate.fromY);
        logLine("equip %lu late on the cursor: back to %lu,%lu", (unsigned long) equipLate.id,
            (unsigned long) equipLate.fromX, (unsigned long) equipLate.fromY);
        equipLate.id = 0;
    }
    else if ((long) (now - equipLate.since) > EQUIP_LATE_MS) equipLate.id = 0;
}


static void equipTick(void* player, DWORD now) {
    if (equip.step == EQ_NONE) {
        equipLateTick(player, now);
        return;
    }
    if (!player) {

        equipDone(EQUIP_CURSOR, "left the game");
        return;
    }
    if (worldPaused()) {
        equip.since = now;
        return;
    }
    DWORD cursor = equipCursorId(player);


    long waited = (long) (now - equip.since);
    switch (equip.step) {
    case EQ_TO_CURSOR:
        if (cursor == equip.id) {
            equipSendBody(equip.oldId ? 0x1D : 0x1A, equip.id, equip.loc);
            equip.step = EQ_TO_BODY;
            equip.since = now;
            logLine("equip %lu on the cursor after %ld ms: 0x%02X", (unsigned long) equip.id, waited,
                equip.oldId ? 0x1D : 0x1A);
        }
        else if (waited > EQUIP_STEP_MS) {
            if (!cursor) {

                equipLate.id = equip.id;
                equipLate.fromX = equip.fromX;
                equipLate.fromY = equip.fromY;
                equipLate.since = now;
            }
            equipDone(cursor ? EQUIP_CURSOR : EQUIP_REFUSED, "not taken to the cursor");
        }
        break;
    case EQ_TO_BODY: {
        void* body = equipFind(player, 0, 1, equip.loc);
        DWORD onBody = body ? RD32(body, U_ID) : 0;
        if (onBody == equip.id && cursor == equip.oldId) {
            if (!equip.oldId) {
                equipDone(EQUIP_OK, "on the body");
                break;
            }
            int w, h;
            DWORD x, y;
            void* old = equipCursorItem(player);
            if (!equipSize(old, &w, &h) || !equipFreePlace(player, 0, w, h, &x, &y)) {
                equipDone(EQUIP_CURSOR, "on the body, no room for the old item");
                break;
            }
            equipSendToInventory(equip.oldId, x, y);
            equip.step = EQ_OLD_BACK;
            equip.since = now;
            logLine("equip %lu on the body after %ld ms: old %lu to %lu,%lu", (unsigned long) equip.id,
                waited, (unsigned long) equip.oldId, (unsigned long) x, (unsigned long) y);
        }
        else if (waited > EQUIP_STEP_MS) {
            if (cursor == equip.id) {

                equipSendToInventory(equip.id, equip.fromX, equip.fromY);
                equip.step = EQ_UNDO;
                equip.since = now;
                logLine("equip %lu refused: back to %lu,%lu", (unsigned long) equip.id, (unsigned long) equip.fromX,
                    (unsigned long) equip.fromY);
            }
            else equipDone(cursor ? EQUIP_CURSOR : EQUIP_REFUSED, "body place not as expected");
        }
        break;
    }
    case EQ_OLD_BACK:
        if (cursor == 0) equipDone(EQUIP_OK, "on the body, the old item in the inventory");
        else if (waited > EQUIP_STEP_MS) equipDone(EQUIP_CURSOR, "the old item stayed on the cursor");
        break;
    case EQ_UNDO:
        if (cursor == 0) equipDone(EQUIP_REFUSED, "refused, put back");
        else if (waited > EQUIP_STEP_MS) equipDone(EQUIP_CURSOR, "refused, stayed on the cursor");
        break;
    }
}
