




















#define CMD_CUBE           36
#define CUBE_MAX           8
#define CUBE_W             3
#define CUBE_H             4
#define CUBE_STEP_MS       1000
#define CUBE_STASH_REACH   12.0
#define CUBE_MAX_STEPS     (2 * CUBE_MAX + CUBE_W * CUBE_H)

enum { CB_NONE, CB_TO_CURSOR, CB_TO_TARGET, CB_SETTLE, CB_BACK_TO_CURSOR, CB_BACK_TO_PLACE };

typedef struct { DWORD id; int fromPlace, fx, fy, toPlace, tx, ty, w, h; } CubeStep;

static struct {
    int state, steps, at, undoing, failed;
    DWORD request, since;
    CubeStep step[CUBE_MAX_STEPS];
} cube;

static int cubeBusy(void) {
    return cube.state != CB_NONE;
}


static int cubeGrid(void* player, int place, int* w, int* h) {
    if (place == 3) {
        *w = CUBE_W;
        *h = CUBE_H;
        return 1;
    }
    return moveGrid(player, place, w, h);
}


static void* cubeFind(void* player, DWORD id, int* place, int* x, int* y) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD32(item, U_ID) == id) {
            void* path = RDP(item, U_PATH);
            int location = RD8(data, IDATA_LOCATION);
            if (RD8(data, IDATA_NODEPAGE) != 1 || !readable(path, IPATH_Y + 4)) return NULL;
            if (location != 0 && location != 3 && location != 4) return NULL;
            *place = location;
            *x = (int) RD32(path, IPATH_X);
            *y = (int) RD32(path, IPATH_Y);
            return item;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return NULL;
}


static int cubeUsed(void* player, int place, BYTE* used, int w, int h) {
    memset(used, 0, (size_t) (w * h));
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) return 0;
        if (RD8(data, IDATA_LOCATION) == place && RD8(data, IDATA_NODEPAGE) == 1) {
            void* path = RDP(item, U_PATH);
            int iw, ih;
            if (!readable(path, 0x14) || !equipSize(item, &iw, &ih)) return 0;
            int x0 = (int) RD32(path, IPATH_X), y0 = (int) RD32(path, IPATH_Y);
            for (int y = y0; y < y0 + ih && y < h; y++)
                for (int x = x0; x < x0 + iw && x < w; x++)
                    if (x >= 0 && y >= 0) used[y * w + x] = 1;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return 1;
}

static void cubeMark(BYTE* used, int w, int x0, int y0, int iw, int ih, BYTE v) {
    for (int y = y0; y < y0 + ih; y++) for (int x = x0; x < x0 + iw; x++) used[y * w + x] = v;
}


static int cubeTake(BYTE* used, int w, int h, int iw, int ih, int* tx, int* ty) {
    for (int y = 0; y + ih <= h; y++) {
        for (int x = 0; x + iw <= w; x++) {
            int free = 1;
            for (int j = 0; j < ih && free; j++) for (int i = 0; i < iw && free; i++) if (used[(y + j) * w + x + i]) free = 0;
            if (free) {
                cubeMark(used, w, x, y, iw, ih, 1);
                *tx = x;
                *ty = y;
                return 1;
            }
        }
    }
    return 0;
}


static void cubeSortLarge(CubeStep* steps, int n) {
    for (int i = 1; i < n; i++) {
        CubeStep t = steps[i];
        int j = i;
        while (j > 0 && steps[j - 1].w * steps[j - 1].h < t.w * t.h) {
            steps[j] = steps[j - 1];
            j--;
        }
        steps[j] = t;
    }
}


static int cubeStashNear(void* player) {
    void* act = RDP(player, U_ACT);
    double hx, hy;
    unitPosition(player, &hx, &hy);
    int units = 0;
    for (void* room = act ? RDP(act, ACT_ROOM1) : NULL; room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            if (RD32(unit, U_TYPE) != UNIT_OBJECT || RD32(unit, U_TXTNO) != 267) continue;
            double dx = fGetUnitX(unit) - hx, dy = fGetUnitY(unit) - hy;
            if (dx * dx + dy * dy <= CUBE_STASH_REACH * CUBE_STASH_REACH) return 1;
        }
    }
    return 0;
}

static void cubeSendPlace(DWORD id, int x, int y, int place) {
    BYTE packet[17] = {0x18};
    *(DWORD*) (packet + 1) = id;
    *(DWORD*) (packet + 5) = (DWORD) x;
    *(DWORD*) (packet + 9) = (DWORD) y;
    *(DWORD*) (packet + 13) = (DWORD) place;
    fSendPacket(17, 1, packet);
}

static const char* cubePlaceName(int place) {
    return place == 3 ? "cube" : place == 4 ? "stash" : "inventory";
}

static void cubeCommand(void* player, const BYTE* buf, int n) {
    if (n < 16) return;
    DWORD request = *(DWORD*) (buf + 12);
    int count = buf[8];
    DWORD ids[CUBE_MAX];
    const char* why = NULL;
    if (!equipAvailable) why = "the senders are not as expected";
    else if (cubeBusy() || mv.step != MV_NONE || mvLate.id || equip.step != EQ_NONE || sort.active) why = "another job runs";
    else if (!singlePlayerGame()) why = "not a single player game";
    else if (worldPaused()) why = "paused";
    else if (!getUiVar(0x1A)) why = "the cube is not open";
    else if (count < 1 || count > CUBE_MAX || n < 16 + 4 * count) why = "a wrong count";
    else if (equipCursorId(player)) why = "an item is on the cursor";
    if (why) {
        logLine("cube %lu: refused, %s", (unsigned long) request, why);
        return;
    }
    memset(&cube, 0, sizeof(cube));
    cube.request = request;
    int stashNear = -1, inCube = 0;
    for (int i = 0; i < count && !why; i++) {
        ids[i] = *(DWORD*) (buf + 16 + 4 * i);
        for (int j = 0; j < i; j++) if (ids[j] == ids[i]) why = "an item twice";
        int place, x, y;
        void* item = why ? NULL : cubeFind(player, ids[i], &place, &x, &y);
        if (why) break;
        if (!item) why = "an item not in the inventory, the cube or the stash";
        else if (strncmp(itemCode(item), "box", 3) == 0) why = "the cube itself";
        else if (place == 4 && (stashNear < 0 ? (stashNear = cubeStashNear(player)) : stashNear) == 0) why = "the stash is out of reach";
        else if (place == 3) inCube++;
    }
    if (why) {
        logLine("cube %lu: refused, %s", (unsigned long) request, why);
        return;
    }

    int iw, ih, cw, chh;
    BYTE invUsed[MOVE_MAX_CELLS], cubeUsedCells[CUBE_W * CUBE_H];
    if (!cubeGrid(player, 0, &iw, &ih) || !cubeUsed(player, 0, invUsed, iw, ih) || !cubeGrid(player, 3, &cw, &chh)
        || !cubeUsed(player, 3, cubeUsedCells, cw, chh)) {
        logLine("cube %lu: refused, the grids are not readable", (unsigned long) request);
        return;
    }
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512 && !why; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        void* next = RDP(data, IDATA_NEXTINV);
        if (RD8(data, IDATA_LOCATION) == 3 && RD8(data, IDATA_NODEPAGE) == 1) {
            DWORD id = RD32(item, U_ID);
            int listed = 0;
            for (int i = 0; i < count; i++) listed |= ids[i] == id;
            void* path = RDP(item, U_PATH);
            int w, h;
            if (!listed && readable(path, 0x14) && equipSize(item, &w, &h)) {
                if (cube.steps >= CUBE_MAX_STEPS) why = "too many items in the cube";
                else {
                    int fx = (int) RD32(path, IPATH_X), fy = (int) RD32(path, IPATH_Y);
                    cubeMark(cubeUsedCells, cw, fx, fy, w, h, 0);
                    cube.step[cube.steps++] = (CubeStep) {id, 3, fx, fy, 0, 0, 0, w, h};
                }
            }
        }
        item = next;
    }
    int out = cube.steps;
    for (int i = 0; i < count && !why; i++) {
        int place, x, y, w, h;
        void* item = cubeFind(player, ids[i], &place, &x, &y);
        if (!item || !equipSize(item, &w, &h)) why = "an item not readable";
        else if (place == 3) continue;
        else cube.step[cube.steps++] = (CubeStep) {ids[i], place, x, y, 3, 0, 0, w, h};
    }

    cubeSortLarge(cube.step, out);
    cubeSortLarge(cube.step + out, cube.steps - out);
    for (int k = 0; k < cube.steps && !why; k++) {
        CubeStep* st = &cube.step[k];
        if (k < out ? !cubeTake(invUsed, iw, ih, st->w, st->h, &st->tx, &st->ty)
                    : !cubeTake(cubeUsedCells, cw, chh, st->w, st->h, &st->tx, &st->ty))
            why = k < out ? "no room in the inventory" : "no room in the cube";
    }
    if (why) {
        logLine("cube %lu: refused, %s", (unsigned long) request, why);
        cube.steps = 0;
        return;
    }
    if (cube.steps == 0) {
        logLine("cube %lu: all %d already in the cube", (unsigned long) request, count);
        return;
    }
    logLine("cube %lu: %d item(s), %d in the cube already, %d step(s)", (unsigned long) request, count, inCube, cube.steps);
    cube.at = 0;
    cube.since = GetTickCount();
    cube.state = CB_TO_CURSOR;
    equipSendId(0x19, cube.step[0].id);
}

static void cubeDone(const char* why) {
    logLine("cube %lu: %s", (unsigned long) cube.request, why);
    cube.state = CB_NONE;
}


static void cubeUndoFrom(int k, DWORD now) {
    cube.undoing = 1;
    cube.at = k;
    cube.since = now;
    if (k < 0) {
        cubeDone(cube.failed ? "not done, everything back where it was" : "done");
        return;
    }
    cube.state = CB_BACK_TO_CURSOR;
    equipSendId(0x19, cube.step[k].id);
}


static void cubeTick(void* player, DWORD now) {
    if (cube.state == CB_NONE) return;
    if (!player) {
        cubeDone("ended: left the game");
        return;
    }
    if (worldPaused()) {
        cube.since = now;
        return;
    }
    long waited = (long) (now - cube.since);
    DWORD cursor = equipCursorId(player);
    CubeStep* s = &cube.step[cube.at];
    int place, x, y;
    switch (cube.state) {
    case CB_TO_CURSOR:
        if (cursor == s->id) {
            cubeSendPlace(s->id, s->tx, s->ty, s->toPlace);
            cube.state = CB_TO_TARGET;
            cube.since = now;
        }
        else if (waited > CUBE_STEP_MS) {

            if (!cursor) {
                mvLate.id = s->id;
                mvLate.since = now;
                mvLate.place = s->fromPlace;
                mvLate.x = s->fx;
                mvLate.y = s->fy;
            }
            logLine("cube %lu: step %d (item %lu) not taken to the cursor", (unsigned long) cube.request, cube.at, (unsigned long) s->id);
            cube.failed = 1;
            if (cursor) cubeDone("another item on the cursor");
            else {

                cube.state = CB_SETTLE;
                cube.since = now;
            }
        }
        break;
    case CB_TO_TARGET:
        if (cubeFind(player, s->id, &place, &x, &y) && place == s->toPlace && !cursor) {
            if (++cube.at >= cube.steps) {
                logLine("cube %lu: done after %d step(s)", (unsigned long) cube.request, cube.steps);
                cube.state = CB_NONE;
                return;
            }
            cube.state = CB_TO_CURSOR;
            cube.since = now;
            equipSendId(0x19, cube.step[cube.at].id);
        }
        else if (waited > CUBE_STEP_MS) {

            logLine("cube %lu: step %d (item %lu) not placed in the %s, back", (unsigned long) cube.request, cube.at,
                (unsigned long) s->id, cubePlaceName(s->toPlace));
            cube.failed = 1;
            cube.undoing = 1;
            cube.state = CB_BACK_TO_PLACE;
            cube.since = now;
            if (cursor == s->id) cubeSendPlace(s->id, s->fx, s->fy, s->fromPlace);
        }
        break;
    case CB_SETTLE:
        if (!mvLate.id && !cursor) cubeUndoFrom(cube.at - 1, now);
        else if (waited > MOVE_LATE_MS + CUBE_STEP_MS) cubeDone("ended: a late item not settled");
        break;
    case CB_BACK_TO_CURSOR:
        if (cursor == s->id) {
            cubeSendPlace(s->id, s->fx, s->fy, s->fromPlace);
            cube.state = CB_BACK_TO_PLACE;
            cube.since = now;
        }
        else if (waited > CUBE_STEP_MS) {


            if (!cursor) {
                mvLate.id = s->id;
                mvLate.since = now;
                mvLate.place = s->fromPlace;
                mvLate.x = s->fx;
                mvLate.y = s->fy;
            }
            logLine("cube %lu: undo of step %d (item %lu) not taken, it and the ones before stay where they are",
                (unsigned long) cube.request, cube.at, (unsigned long) s->id);
            cubeDone("not done, undo stopped");
        }
        break;
    case CB_BACK_TO_PLACE:
        if (cubeFind(player, s->id, &place, &x, &y) && place == s->fromPlace && !cursor) cubeUndoFrom(cube.at - 1, now);
        else if (waited > CUBE_STEP_MS) {
            logLine("cube %lu: item %lu left on the cursor", (unsigned long) cube.request, (unsigned long) s->id);
            cubeDone("ended with an item on the cursor");
        }
        break;
    }
}
