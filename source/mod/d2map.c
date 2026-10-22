
#define PKT_MAP 8
#define CMD_MAP 14
#define MAP_PART_BYTES 960
#define MAP_LIST_PER_PACKET 60
#define MAP_PER_TICK 2
#define MAP_QUEUE 16

typedef struct {
    BYTE kind;
    WORD index;
    BYTE part;
} MapRequest;

static MapRequest mapQueue[MAP_QUEUE];
static int mapQueueLen;
static DWORD mapLargestW, mapLargestH, mapLargestLevel;

static void mapCommand(const BYTE* buf, int n) {
    if (n < 12 || buf[8] > 1) return;
    if (mapQueueLen >= MAP_QUEUE) return;
    MapRequest* r = &mapQueue[mapQueueLen++];
    r->kind = buf[8];
    r->index = (WORD) (buf[9] | (buf[10] << 8));
    r->part = buf[11];
}

static void* playerLevel(void* player, DWORD* levelNo) {
    void* path = RDP(player, U_PATH);
    void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
    void* room2 = room1 ? RDP(room1, ROOM1_ROOM2) : NULL;
    void* level = room2 ? RDP(room2, ROOM2_LEVEL) : NULL;
    if (!readable(level, LEVEL_NO + 4)) return NULL;
    *levelNo = RD32(level, LEVEL_NO);
    return level;
}

static int mapStand(void) {
    return mapAvailable ? (reveal.next >= reveal.end && reveal.act ? 2 : 1) : 0;
}

static void sendRoomList(void* player, WORD first) {
    DWORD levelNo = 0;
    void* level = playerLevel(player, &levelNo);
    if (!level) return;
    int total = 0;
    for (void* r = RDP(level, LEVEL_ROOM2); readable(r, 0x60) && total < 2048; r = RDP(r, ROOM2_NEXT)) total++;
    Packet p;
    putHeader(&p, PKT_MAP);
    putU8(&p, 0);
    putU16(&p, levelNo);
    putU8(&p, mapStand());
    putU16(&p, total);
    putU16(&p, first);
    int countAt = p.len, count = 0, i = 0;
    putU8(&p, 0);
    for (void* r = RDP(level, LEVEL_ROOM2); readable(r, 0x60) && i < total && count < MAP_LIST_PER_PACKET; r = RDP(r, ROOM2_NEXT), i++) {
        if (i < first) continue;
        DWORD w = RD32(r, ROOM2_W) * 5, h = RD32(r, ROOM2_H) * 5;
        putU16(&p, i);
        putU16(&p, RD32(r, ROOM2_X) * 5);
        putU16(&p, RD32(r, ROOM2_Y) * 5);
        putU16(&p, w);
        putU16(&p, h);
        putU8(&p, RDP(r, ROOM2_ROOM1) ? 1 : 0);
        count++;
        if (w * h > mapLargestW * mapLargestH) {
            mapLargestW = w;
            mapLargestH = h;
            mapLargestLevel = levelNo;
            logLine("map: largest room so far %lux%lu subtiles in level %lu", (unsigned long) w, (unsigned long) h, (unsigned long) levelNo);
        }
    }
    p.data[countAt] = (BYTE) count;
    sendPacket(&p);
}

static void sendRoomBitmap(void* player, WORD index, BYTE part) {
    DWORD levelNo = 0;
    void* level = playerLevel(player, &levelNo);
    if (!level) return;
    void* room2 = NULL;
    int i = 0;
    for (void* r = RDP(level, LEVEL_ROOM2); readable(r, 0x60) && i <= index; r = RDP(r, ROOM2_NEXT), i++) {
        if (i == index) room2 = r;
    }
    if (!room2) return;
    void* act = RDP(player, U_ACT);
    DWORD tx = RD32(room2, ROOM2_X), ty = RD32(room2, ROOM2_Y);
    int added = 0;
    if (!RDP(room2, ROOM2_ROOM1) && fAddRoomData && act) {
        fAddRoomData(act, levelNo, tx, ty, NULL);
        added = 1;
    }
    void* room1 = RDP(room2, ROOM2_ROOM1);
    if (gridReadable(room1)) {
        long rx = (long) RD32(room1, ROOM1_SUBX), ry = (long) RD32(room1, ROOM1_SUBX + 4);
        long w = (long) RD32(room1, ROOM1_SUBX + 8), h = (long) RD32(room1, ROOM1_SUBX + 12);
        BYTE* grid = RDP(room1, ROOM1_COLL);
        WORD* flags = (WORD*) RDP(grid, 0x20);
        long stride = (long) RD32(grid, 8);
        if (flags && w > 0 && h > 0 && w <= 1024 && h <= 1024) {
            long rowBytes = (w + 7) / 8;
            long rowsPerPart = MAP_PART_BYTES / rowBytes;
            if (rowsPerPart < 1) rowsPerPart = 1;
            long parts = (h + rowsPerPart - 1) / rowsPerPart;
            long firstRow = (long) part * rowsPerPart;
            if (part < parts) {
                long rows = h - firstRow < rowsPerPart ? h - firstRow : rowsPerPart;
                Packet p;
                putHeader(&p, PKT_MAP);
                putU8(&p, 1);
                putU16(&p, levelNo);
                putU16(&p, index);
                putU16(&p, rx);
                putU16(&p, ry);
                putU16(&p, w);
                putU16(&p, h);
                putU8(&p, part);
                putU8(&p, (unsigned) parts);
                putU16(&p, firstRow);
                putU16(&p, rows);
                for (long y = firstRow; y < firstRow + rows; y++) {
                    for (long bx = 0; bx < rowBytes; bx++) {
                        unsigned v = 0;
                        for (int b = 0; b < 8; b++) {
                            long x = bx * 8 + b;
                            if (x < w && !(flags[y * stride + x] & COLL_BLOCKED)) v |= 1u << b;
                        }
                        putU8(&p, v);
                    }
                }
                sendPacket(&p);
            }
        }
    }
    if (added) fRemoveRoomData(act, levelNo, tx, ty, RDP(room2, ROOM2_ROOM1));
}

static void roomMapTick(void* player) {
    if (!mapQueueLen) return;
    if (!player || (int) (GetTickCount() - playtestUntil) >= 0) {
        mapQueueLen = 0;
        return;
    }
    int done = 0;
    while (mapQueueLen > 0 && done < MAP_PER_TICK) {
        MapRequest r = mapQueue[0];
        memmove(mapQueue, mapQueue + 1, (size_t) (mapQueueLen - 1) * sizeof(MapRequest));
        mapQueueLen--;
        if (r.kind == 0) sendRoomList(player, r.index);
        else sendRoomBitmap(player, r.index, r.part);
        done++;
    }
}
