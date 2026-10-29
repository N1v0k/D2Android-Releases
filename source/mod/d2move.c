















#define CMD_MOVE        34
#define MOVE_STEP_MS    1000
#define MOVE_BUF_INV    0
#define MOVE_BUF_STASH  4
#define MOVE_MAX_CELLS  (16 * 16)
#define MOVE_LATE_MS    5000

enum { MV_NONE, MV_TO_CURSOR, MV_TO_TARGET, MV_BACK };

static struct {
    int step;
    DWORD id, request, since;
    int fromPlace, fromX, fromY, toPlace, toX, toY;
} mv;

static struct { DWORD id, since; int place, x, y; } mvLate;
static int cubeBusy(void);


static void* moveItemAt(void* player, int place, int x, int y) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        void* path = RDP(item, U_PATH);
        if (RD8(data, IDATA_LOCATION) == place && RD8(data, IDATA_NODEPAGE) == 1 && readable(path, IPATH_Y + 4)
            && (int) RD32(path, IPATH_X) == x && (int) RD32(path, IPATH_Y) == y) return item;
        item = RDP(data, IDATA_NEXTINV);
    }
    return NULL;
}


static int moveWhere(void* player, DWORD id, int* x, int* y) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD32(item, U_ID) == id) {
            void* path = RDP(item, U_PATH);
            if (RD8(data, IDATA_NODEPAGE) != 1 || !readable(path, IPATH_Y + 4)) return -1;
            *x = (int) RD32(path, IPATH_X);
            *y = (int) RD32(path, IPATH_Y);
            return RD8(data, IDATA_LOCATION);
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return -1;
}



static int moveGrid(void* player, int place, int* w, int* h) {
    const BYTE* table = common && readable((void*) (common + RVA_InvTable), 8) ? RDP(common, RVA_InvTable) : NULL;
    int count = table ? (int) RD32(common, RVA_InvCount) : 0;
    if (!readable(table, BI_RECORDS * BI_RECORD_SIZE)) table = NULL;
    BYTE bw, bh;
    biGridOf(table, count, place == 4 ? BI_ROW_STASH : biHeroRow((int) RD32(player, U_TXTNO)), &bw, &bh);
    *w = bw;
    *h = bh;
    return bw > 0 && bh > 0 && bw * bh <= MOVE_MAX_CELLS;
}


static int moveFreePlace(void* player, int place, int iw, int ih, int* tx, int* ty) {
    int w, h;
    if (!moveGrid(player, place, &w, &h) || iw <= 0 || ih <= 0) return 0;
    BYTE used[MOVE_MAX_CELLS];
    memset(used, 0, sizeof(used));
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) return 0;
        if (RD8(data, IDATA_LOCATION) == place && RD8(data, IDATA_NODEPAGE) == 1) {
            void* path = RDP(item, U_PATH);
            BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
            if (!readable(path, 0x14) || !readable(txt, 0x120)) return 0;
            int x0 = (int) RD32(path, IPATH_X), y0 = (int) RD32(path, IPATH_Y);
            for (int y = y0; y < y0 + txt[ITXT_HEIGHT] && y < h; y++)
                for (int x = x0; x < x0 + txt[ITXT_WIDTH] && x < w; x++)
                    if (x >= 0 && y >= 0) used[y * w + x] = 1;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    for (int y = 0; y + ih <= h; y++) {
        for (int x = 0; x + iw <= w; x++) {
            int free = 1;
            for (int j = 0; j < ih && free; j++) for (int i = 0; i < iw && free; i++) if (used[(y + j) * w + x + i]) free = 0;
            if (free) {
                *tx = x;
                *ty = y;
                return 1;
            }
        }
    }
    return 0;
}

static void moveSendPlace(DWORD id, int x, int y, int place) {
    BYTE packet[17] = {0x18};
    *(DWORD*) (packet + 1) = id;
    *(DWORD*) (packet + 5) = (DWORD) x;
    *(DWORD*) (packet + 9) = (DWORD) y;

    *(DWORD*) (packet + 13) = place == 4 ? MOVE_BUF_STASH : place == 3 ? 3 : MOVE_BUF_INV;
    fSendPacket(17, 1, packet);
}

static void moveDone(const char* why) {
    logLine("move %lu: item %lu %s", (unsigned long) mv.request, (unsigned long) mv.id, why);
    mv.step = MV_NONE;
}

static void moveCommand(void* player, const BYTE* buf, int n) {
    if (n < 16) return;
    DWORD request = *(DWORD*) (buf + 12);
    int place = buf[8], x = buf[9], y = buf[10];
    const char* why = NULL;
    void* item = NULL;
    if (!equipAvailable) why = "the senders are not as expected";
    else if (mv.step != MV_NONE || mvLate.id || equip.step != EQ_NONE || sort.active || cubeBusy()) why = "another move runs";
    else if (!singlePlayerGame()) why = "not a single player game";
    else if (worldPaused()) why = "paused";
    else if (!getUiVar(0x19)) why = "the stash is not open";
    else if (place != 0 && place != 4) why = "not the inventory or the stash";
    else if (equipCursorId(player)) why = "an item is on the cursor";
    else if (!(item = moveItemAt(player, place, x, y))) why = "no item at that cell";
    else if (!compareAvailable || RD32(client, RVA_HoverGrid) != 1 || RDP(client, RVA_HoverItem) != item)
        why = "the pointer is not on the item";
    else {
        int tier;
        if (itemCategory(item, itemCode(item), &tier) == CAT_QUEST) why = "a quest item";
    }
    if (why) {
        logLine("move %lu: refused, %s (place %d cell %d,%d)", (unsigned long) request, why, place, x, y);
        return;
    }
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    int to = place == 0 ? 4 : 0, tx, ty;
    if (!readable(txt, 0x120) || !moveFreePlace(player, to, txt[ITXT_WIDTH], txt[ITXT_HEIGHT], &tx, &ty)) {
        logLine("move %lu: refused, no room in the %s for %.3s", (unsigned long) request, to == 4 ? "stash" : "inventory",
            itemCode(item));
        return;
    }
    memset(&mv, 0, sizeof(mv));
    mv.request = request;
    mv.id = RD32(item, U_ID);
    mv.fromPlace = place;
    mv.fromX = x;
    mv.fromY = y;
    mv.toPlace = to;
    mv.toX = tx;
    mv.toY = ty;
    mv.since = GetTickCount();
    mv.step = MV_TO_CURSOR;
    equipSendId(0x19, mv.id);
    logLine("move %lu: %.3s (item %lu) from %s %d,%d to %s %d,%d: 0x19", (unsigned long) request, itemCode(item),
        (unsigned long) mv.id, place == 4 ? "stash" : "inventory", x, y, to == 4 ? "stash" : "inventory", tx, ty);
}


static void moveLateTick(void* player, DWORD now) {
    if (!mvLate.id) return;
    if (!player) {
        mvLate.id = 0;
        return;
    }
    if (worldPaused()) {
        mvLate.since = now;
        return;
    }
    if (equipCursorId(player) == mvLate.id) {
        moveSendPlace(mvLate.id, mvLate.x, mvLate.y, mvLate.place);
        logLine("move: item %lu late on the cursor: back to %s %d,%d", (unsigned long) mvLate.id,
            mvLate.place == 4 ? "stash" : "inventory", mvLate.x, mvLate.y);
        mvLate.id = 0;
    }
    else if ((long) (now - mvLate.since) > MOVE_LATE_MS) mvLate.id = 0;
}


static void moveTick(void* player, DWORD now) {
    if (mv.step == MV_NONE) {
        moveLateTick(player, now);
        return;
    }
    if (!player) {
        moveDone("ended: left the game");
        return;
    }
    if (worldPaused()) {
        mv.since = now;
        return;
    }
    long waited = (long) (now - mv.since);
    DWORD cursor = equipCursorId(player);
    int x, y, at;
    switch (mv.step) {
    case MV_TO_CURSOR:
        if (cursor == mv.id) {
            moveSendPlace(mv.id, mv.toX, mv.toY, mv.toPlace);
            mv.step = MV_TO_TARGET;
            mv.since = now;
        }
        else if (waited > MOVE_STEP_MS) {
            if (!cursor) {
                mvLate.id = mv.id;
                mvLate.since = now;
                mvLate.place = mv.fromPlace;
                mvLate.x = mv.fromX;
                mvLate.y = mv.fromY;
            }
            moveDone(cursor ? "not taken, another item on the cursor" : "not taken to the cursor");
        }
        break;
    case MV_TO_TARGET:
        at = moveWhere(player, mv.id, &x, &y);
        if (at == mv.toPlace && !cursor) {
            logLine("move %lu: moved after %ld ms to %d,%d", (unsigned long) mv.request, waited, x, y);
            mv.step = MV_NONE;
        }
        else if (waited > MOVE_STEP_MS) {

            moveSendPlace(mv.id, mv.fromX, mv.fromY, mv.fromPlace);
            mv.step = MV_BACK;
            mv.since = now;
            logLine("move %lu: not placed within %d ms, back", (unsigned long) mv.request, MOVE_STEP_MS);
        }
        break;
    case MV_BACK:
        at = moveWhere(player, mv.id, &x, &y);
        if (at == mv.fromPlace && !cursor) moveDone("back where it was");
        else if (waited > MOVE_STEP_MS) moveDone("left on the cursor");
        break;
    }
}
