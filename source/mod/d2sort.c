















#include "d2sortlayout.c"

#define CMD_SORT 24
#define CAP_SORT 0x10
#define SORT_STEP_MS 1000
#define SORT_LIMIT_MS 20000
#define SORT_MONSTER_NEAR 18

#define SORT_RUNNING     0
#define SORT_DONE        1
#define SORT_PARTIAL     2
#define SORT_CURSOR      3
#define SORT_NOT_STARTED 4

#define SORT_NO_ROOM     5
#define SORT_ON_CURSOR   0x80

enum { SO_NONE, SO_PICK, SO_PLACE, SO_UNDO };

static BYTE sortCounter, sortState = SORT_NOT_STARTED, sortMoves, sortReportedCounter, sortReportedState;
static struct {
    int active, step, n, moves;
    void* player;
    DWORD since, lastTick, activeMs;
    int k;
    BYTE fromX, fromY, toX, toY;
    SortItem items[SORT_MAX];
} sort;
static DWORD takeSentAt;
#ifdef D2MOD_TESTCMDS
static int sortFaultNext;
#endif








#define RVA_DrawCursorItem      0x5F480
static struct {
    DWORD rva;
    BYTE original[5];
} sortItemSites[2] = {{0x14FF7, {0}}, {0x14E6C, {0}}};
static const BYTE sortItemSkip[5] = {0x83, 0xC4, 0x08, 0x90, 0x90};
static int sortItemChecked, sortItemAvailable, sortItemHidden;

static void sortCheckItemSites(void) {
    sortItemChecked = 1;
    sortItemAvailable = 1;
    for (int i = 0; i < 2; i++) {
        BYTE* at = (BYTE*) (client + sortItemSites[i].rva);
        DWORD target = (DWORD) at + 5 + *(DWORD*) (at + 1);
        if (at[0] != 0xE8 || target != client + RVA_DrawCursorItem) sortItemAvailable = 0;
        else memcpy(sortItemSites[i].original, at, 5);
    }
    logLine(sortItemAvailable ? "sort: cursor item sites verified"
                              : "sort: cursor item sites not as expected: the item shows while sorting");
}

static void sortHideCursorItem(int hide) {
    for (int i = 0; i < 2; i++) {
        BYTE* at = (BYTE*) (client + sortItemSites[i].rva);
        DWORD old;
        if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) continue;
        memcpy(at, hide ? sortItemSkip : sortItemSites[i].original, 5);
        VirtualProtect(at, 5, old, &old);
        FlushInstructionCache(GetCurrentProcess(), at, 5);
    }
    sortItemHidden = hide;
}


static int sortRead(void* player, SortItem* out, int charmsFixed) {
    int cubeOpen = getUiVar(0x1A) != 0;
    void* inventory = RDP(player, U_INVENTORY);
    int n = 0, guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) return -1;
        if (RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1) {
            void* path = RDP(item, U_PATH);
            int w, h, tier;
            if (n >= SORT_MAX || !readable(path, 0x14) || !equipSize(item, &w, &h)) return -1;
            int cat = itemCategory(item, itemCode(item), &tier), group;
            switch (cat) {
            case CAT_TP_TOME: case CAT_ID_TOME: group = SG_TOMES; break;
            case CAT_TP_SCROLL: case CAT_ID_SCROLL: group = SG_SCROLLS; break;
            case CAT_KEY: group = SG_KEYS; break;
            case CAT_HEAL: case CAT_MANA: case CAT_REJUV: case CAT_POTION_OTHER: group = SG_POTIONS; break;
            case CAT_RUNE: group = SG_RUNES; break;
            case CAT_GEM: group = SG_GEMS; break;
            case CAT_JEWEL: group = SG_JEWELS; break;
            case CAT_RING: group = SG_RINGS; break;
            case CAT_AMULET: group = SG_AMULETS; break;
            case CAT_CHARM: group = SG_CHARMS; break;
            case CAT_QUEST: group = SG_BIG; break;
            default: group = w * h == 1 ? SG_SMALL : SG_BIG;
            }
            SortItem* s = &out[n++];
            s->id = RD32(item, U_ID);
            s->order = RD32(item, U_TXTNO);
            s->x = (BYTE) RD32(path, IPATH_X);
            s->y = (BYTE) RD32(path, IPATH_Y);
            s->w = (BYTE) w;
            s->h = (BYTE) h;
            s->group = (BYTE) group;
            s->fixed = (BYTE) ((cat == CAT_CHARM && charmsFixed) || (cubeOpen && strncmp(itemCode(item), "box", 3) == 0));
            s->tx = s->x;
            s->ty = s->y;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return n;
}


static int sortMonsterNear(void* player) {
    if (isTown(unitLevel(player))) return 0;
    void* act = RDP(player, U_ACT);
    int units = 0;
    for (void* room = act ? RDP(act, ACT_ROOM1) : NULL; room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            if (RD32(unit, U_TYPE) != UNIT_MONSTER || !isValidMonster(unit, 0)) continue;
            if (unitDistance(player, unit) < SORT_MONSTER_NEAR) return 1;
        }
    }
    return 0;
}


static int sortBusy(void) {
    return sort.active;
}

static void sortDone(int result, const char* why) {
    sort.active = 0;
    sort.step = SO_NONE;
    sortState = (BYTE) result;
    sortMoves = (BYTE) (sort.moves > 255 ? 255 : sort.moves);
    logLine("sort: %s after %d moves, %lu ms (result %d)", why, sort.moves, (unsigned long) sort.activeMs, result);
}


static int sortReportDue(void* player) {
    BYTE state = (BYTE) (sortState | (player && equipCursorId(player) ? SORT_ON_CURSOR : 0));
    if (sortReportedCounter == sortCounter && sortReportedState == state) return 0;
    sortReportedCounter = sortCounter;
    sortReportedState = state;
    return 1;
}


static BYTE sortStateByte(void* player) {
    return (BYTE) (sortState | (player && equipCursorId(player) ? SORT_ON_CURSOR : 0));
}


static void sortStart(void* player, BYTE place, BYTE options, DWORD now) {
    if (sort.active) {
        logLine("sort: still running, command dropped");
        return;
    }
    sortCounter++;
    sort.moves = 0;
    sort.activeMs = 0;
    const char* why = !equipAvailable ? "senders not checked" : !singlePlayerGame() ? "not single player"
        : worldPaused() ? "paused" : place != 0 ? "not the inventory"
        : getUiVar(0x0C) || getUiVar(0x17) || getUiVar(0x19) ? "shop, trade or stash open"
        : equipCursorId(player) ? "something on the cursor" : equip.step != EQ_NONE ? "Anlegen running"
        : takeSentAt && (long) (now - takeSentAt) < 3000 ? "Nehmen running" : sortMonsterNear(player) ? "monster near"
        : NULL;
    if (why) {
        sortDone(SORT_NOT_STARTED, why);
        return;
    }

    sortW = invGridW < SORT_MAX_W ? invGridW : SORT_MAX_W;
    sortH = invGridH < SORT_MAX_H ? invGridH : SORT_MAX_H;
    int n = sortRead(player, sort.items, options & 1);
    if (n < 0) {
        sortDone(SORT_NOT_STARTED, "inventory not readable");
        return;
    }
    sort.n = n;
    int layout = sortLayout(sort.items, n);
    if (!layout) {
        sortDone(SORT_NO_ROOM, "no layout");
        return;
    }
    sort.active = 1;
    sort.step = SO_NONE;
    sort.player = player;
    sort.since = sort.lastTick = now;
    sortState = SORT_RUNNING;
    logLine("sort: %d items, layout %s, charms %s", n, layout == 1 ? "two-sided" : "biggest first",
        options & 1 ? "fixed" : "moving");
}


static void sortStep(void* player, DWORD now) {
    if (!sort.active) return;
    if (!player || player != sort.player) {

        sortDone(sort.step == SO_NONE ? SORT_PARTIAL : SORT_CURSOR, player ? "new player" : "left the game");
        return;
    }
    if (worldPaused()) {
        sort.since = sort.lastTick = now;
        return;
    }

    long dt = (long) (now - sort.lastTick);
    if (dt > 0) sort.activeMs += (DWORD) dt;
    sort.lastTick = now;
    DWORD cursor = equipCursorId(player);
    long waited = (long) (now - sort.since);
    SortItem* s = &sort.items[sort.k];
    switch (sort.step) {
    case SO_NONE: {
        if (cursor) {
            sortDone(SORT_CURSOR, "something on the cursor");
            break;
        }

        SortItem now_[SORT_MAX];
        int n = sortRead(player, now_, 0);
        if (n != sort.n) {
            sortDone(SORT_PARTIAL, "inventory changed");
            break;
        }
        for (int i = 0; i < sort.n; i++) {
            int found = 0;
            for (int j = 0; j < n && !found; j++) {
                if (now_[j].id != sort.items[i].id) continue;
                sort.items[i].x = now_[j].x;
                sort.items[i].y = now_[j].y;
                found = 1;
            }
            if (!found) {
                sortDone(SORT_PARTIAL, "inventory changed");
                return;
            }
        }
        int k;
        BYTE x, y;
        int r = sortNextMove(sort.items, sort.n, &k, &x, &y);
        if (r == 0) {
            sortDone(SORT_DONE, "sorted");
            break;
        }

        if (sort.activeMs > SORT_LIMIT_MS || sort.moves >= 3 * sort.n) {
            sortDone(SORT_PARTIAL, "limit");
            break;
        }
        if (r < 0) {
            sortDone(sort.moves ? SORT_PARTIAL : SORT_NO_ROOM, "no free place for a move");
            break;
        }
        sort.k = k;
        sort.fromX = sort.items[k].x;
        sort.fromY = sort.items[k].y;
        sort.toX = x;
        sort.toY = y;
#ifdef D2MOD_TESTCMDS
        if (sortFaultNext) {
            sortFaultNext = 0;
            sort.toX = SORT_W + 10;
            logLine("sort: test fault, %lu to %u,%u", (unsigned long) sort.items[k].id, sort.toX, sort.toY);
        }
#endif
        equipSendId(0x19, sort.items[k].id);
        sort.step = SO_PICK;
        sort.since = now;
        break;
    }
    case SO_PICK:
        if (cursor == s->id) {
            equipSendToInventory(s->id, sort.toX, sort.toY);
            sort.step = SO_PLACE;
            sort.since = now;
        }
        else if (waited > SORT_STEP_MS) {
            if (!cursor) {

                equipLate.id = s->id;
                equipLate.fromX = sort.fromX;
                equipLate.fromY = sort.fromY;
                equipLate.since = now;
            }
            sortDone(cursor ? SORT_CURSOR : SORT_PARTIAL, "not taken to the cursor");
        }
        break;
    case SO_PLACE:
        if (!cursor) {
            sort.moves++;
            sortMoves = (BYTE) (sort.moves > 255 ? 255 : sort.moves);
            sort.step = SO_NONE;
            logLine("sort: %lu from %u,%u to %u,%u after %ld ms", (unsigned long) s->id, sort.fromX, sort.fromY,
                sort.toX, sort.toY, waited);
        }
        else if (waited > SORT_STEP_MS) {
            if (cursor == s->id) {

                equipSendToInventory(s->id, sort.fromX, sort.fromY);
                sort.step = SO_UNDO;
                sort.since = now;
                logLine("sort: %lu refused at %u,%u, back to %u,%u", (unsigned long) s->id, sort.toX, sort.toY,
                    sort.fromX, sort.fromY);
            }
            else sortDone(SORT_CURSOR, "another item on the cursor");
        }
        break;
    case SO_UNDO:
        if (!cursor) sortDone(SORT_PARTIAL, "refused, put back");
        else if (waited > SORT_STEP_MS) sortDone(SORT_CURSOR, "refused, stayed on the cursor");
        break;
    }
}


static void sortTick(void* player, DWORD now) {
    sortStep(player, now);
    if (sort.active && !sortItemChecked) sortCheckItemSites();
    if (sortItemAvailable && sortItemHidden != sort.active) sortHideCursorItem(sort.active);
}
