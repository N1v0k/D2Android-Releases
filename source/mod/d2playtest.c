
















#define PKT_PLAYTEST 7
#define PT_UI_SHOP     0x0C
#define PT_UI_STASH    0x19
#define PT_UI_CUBE     0x1A
#define PT_DETAILS     1
#define IDATA_ITEMLEVEL 0x2C
#define ITXT_NORMCODE  0x84
#define ITXT_UBERCODE  0x88
#define ITXT_ULTRACODE 0x8C
#define INV_CURSOR     0x20
#define PDATA_NAME     0x00
#define MAX_PT_ITEMS   80
#define MAX_PT_PLACES  24

static WORD playtestGeneration;



static int placeKind(DWORD type, DWORD txt, BYTE* otxt) {
    if (type == UNIT_MONSTER) return isMercenary(txt) ? 0 : 1;
    if (type != UNIT_OBJECT) return 0;
    if (txt == 267) return 2;
    if (!otxt && fGetObjectTxt && readable(objectTxtCount, 4) && txt < *objectTxtCount) otxt = fGetObjectTxt(txt);
    return readable(otxt, OTXT_SUBCLASS + 1) && (otxt[OTXT_SUBCLASS] & 64) ? 3 : 0;
}

static void putPlace(Packet* p, int kind, DWORD txt, DWORD id, long x, long y) {
    putU8(p, kind);
    putU16(p, (WORD) txt);
    putU32(p, id);
    putU16(p, (WORD) x);
    putU16(p, (WORD) y);
}


static int itemPlace(void* data) {
    BYTE location = RD8(data, IDATA_LOCATION), page = RD8(data, IDATA_NODEPAGE);
    if (page == 2) return 1;
    if (page == 3) return 2;
    if (location == 0x00) return 0;
    if (location == 0x03) return 3;
    if (location == 0x04) return 4;
    return 0xFF;
}







static void putHoverItem(Packet* p, void* player) {
    int category = 0xFF, place = 0xFF, x = 0xFF, y = 0xFF;
    void* hovered = compareAvailable && RD32(client, RVA_HoverGrid) == 1 ? RDP(client, RVA_HoverItem) : NULL;
    void* inventory = player && readable(hovered, 0x18) && RD32(hovered, U_TYPE) == UNIT_ITEM ? RDP(player, U_INVENTORY) : NULL;
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (item == hovered) {
            int tier;
            category = itemCategory(item, itemCode(item), &tier);
            place = itemPlace(data);
            void* path = RDP(item, U_PATH);
            if (readable(path, IPATH_Y + 4) && RD32(path, IPATH_X) < 0xFF && RD32(path, IPATH_Y) < 0xFF) {
                x = (int) RD32(path, IPATH_X);
                y = (int) RD32(path, IPATH_Y);
            }
            break;
        }
        item = RDP(data, IDATA_NEXTINV);
    }

    void* npc = category == 0xFF && inventory ? shopNpc() : NULL;
    void* wares = npc ? RDP(npc, U_INVENTORY) : NULL;
    guard = 0;
    for (void* item = readable(wares, 0x24) ? RDP(wares, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (item == hovered) {
            int tier;
            category = itemCategory(item, itemCode(item), &tier);
            place = 5;
            void* path = RDP(item, U_PATH);
            if (readable(path, IPATH_Y + 4) && RD32(path, IPATH_X) < 0xFF && RD32(path, IPATH_Y) < 0xFF) {
                x = (int) RD32(path, IPATH_X);
                y = (int) RD32(path, IPATH_Y);
            }
            break;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    putU8(p, (BYTE) category);
    putU8(p, (BYTE) place);
    putU8(p, (BYTE) x);
    putU8(p, (BYTE) y);
}

static void putItemEntry(Packet* p, void* item, int place) {
    void* data = RDP(item, U_DATA);
    void* path = RDP(item, U_PATH);
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    const char* code = itemCode(item);
    int tier, category = itemCategory(item, code, &tier);
    DWORD flags = readable(data, 0x20) ? RD32(data, IDATA_FLAGS) : 0;
    putU32(p, RD32(item, U_ID));
    for (int k = 0; k < 4; k++) putU8(p, code[k] && code[k] != ' ' ? (BYTE) code[k] : ' ');
    putU8(p, category);
    putU8(p, readable(data, 0x10) ? RD32(data, IDATA_QUALITY) : 0);
    putU8(p, ((flags & IFLAG_ETHEREAL) ? 1 : 0) | ((flags & IFLAG_SOCKETED) ? 2 : 0) | ((flags & IFLAG_IDENTIFIED) ? 4 : 0)
        | ((flags & IFLAG_RUNEWORD) ? 8 : 0));
    putU8(p, place);
    putU8(p, readable(data, 0x70) ? RD8(data, IDATA_BODYLOC) : 0);
    putU8(p, readable(path, 0x14) ? (BYTE) RD32(path, IPATH_X) : 0);
    putU8(p, readable(path, 0x14) ? (BYTE) RD32(path, IPATH_Y) : 0);
    putU8(p, readable(txt, 0x120) ? ((txt[ITXT_WIDTH] & 15) << 4) | (txt[ITXT_HEIGHT] & 15) : 0x11);
}






static void putItemDetails(Packet* p, void* item) {
    void* data = RDP(item, U_DATA);
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    DWORD flags = readable(data, 0x20) ? RD32(data, IDATA_FLAGS) : 0;
    DWORD sockets = (flags & IFLAG_SOCKETED) ? fGetUnitStat(item, STAT_SOCKETS, 0) : 0;
    int filled = 0, guard = 0;
    void* inside = RDP(item, U_INVENTORY);
    for (void* s = readable(inside, 0x10) ? RDP(inside, INV_FIRST) : NULL; readable(s, 0x18) && guard < 8; guard++) {
        filled++;
        void* sd = RDP(s, U_DATA);
        s = readable(sd, 0x70) ? RDP(sd, IDATA_NEXTINV) : NULL;
    }
    int tier = 255;
    if (readable(txt, ITXT_ULTRACODE + 4)) {
        const BYTE* code = txt + ITXT_CODE;
        int norm = memcmp(code, txt + ITXT_NORMCODE, 4) == 0, uber = memcmp(code, txt + ITXT_UBERCODE, 4) == 0;
        int ultra = memcmp(code, txt + ITXT_ULTRACODE, 4) == 0;

        tier = norm && !(uber && ultra) ? 0 : uber && !norm ? 1 : ultra && !norm && !uber ? 2 : 255;
    }
    DWORD level = readable(data, IDATA_ITEMLEVEL + 4) ? RD32(data, IDATA_ITEMLEVEL) : 0;
    putU8(p, sockets > 255 ? 255 : (BYTE) sockets);
    putU8(p, (BYTE) filled);
    putU8(p, (BYTE) tier);
    putU8(p, level > 255 ? 255 : (BYTE) level);
}



static int putInventoryItems(Packet* p, void* inventory, int shop, int limit, int* full, void** items) {
    int count = 0;


    for (int pass = shop ? 1 : 0; pass < 2; pass++) {
        int guard = 0;
        for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
            void* data = RDP(item, U_DATA);
            if (!readable(data, 0x70)) break;
            int place = shop ? 5 : itemPlace(data);
            if (place != 0xFF && (shop || (pass == 0) == (place == 0))) {
                if (count >= limit) {
                    *full = 1;
                    return count;
                }
                putItemEntry(p, item, place);
                if (items) items[count] = item;
                count++;
            }
            item = RDP(data, IDATA_NEXTINV);
        }
    }
    return count;
}





static int townPresetPeople(void* level, DWORD* out, int max) {
    int n = 0, rooms = 0;
    for (void* r = readable(level, 0x14) ? RDP(level, LEVEL_ROOM2) : NULL; readable(r, 0x60) && rooms < 2048 && n < max;
         r = RDP(r, ROOM2_NEXT), rooms++) {
        int guard = 0;
        for (void* pu = RDP(r, ROOM2_PRESETS); readable(pu, 0x1C) && guard < 512 && n < max; pu = RDP(pu, 0x0C), guard++) {
            if (RD32(pu, 0x14) == UNIT_MONSTER && !isMercenary(RD32(pu, 0x04))) out[n++] = RD32(pu, 0x04);
        }
    }
    return n;
}

static void sendPlaytest(void* player, void* act, DWORD levelNo) {
    static DWORD lastHash, lastPlacesHash, lastAt;
    DWORD now = GetTickCount();


    if ((int) (now - playtestUntil) >= 0 && !getUiVar(PT_UI_SHOP) && !getUiVar(PT_UI_STASH) && !getUiVar(PT_UI_CUBE))
        return;

    Packet p;
    putHeader(&p, PKT_PLAYTEST);
    int genAt = p.len;
    putU16(&p, 0);
    putU32(&p, now);

    void* pdata = RDP(player, U_DATA);
    for (int k = 0; k < 16; k++) putU8(&p, readable(pdata, 16) ? RD8(pdata, PDATA_NAME + k) : 0);
    for (DWORD stat = 0; stat < 6; stat++) putU16(&p, (WORD) fGetUnitStat(player, stat, 0));
    putU8(&p, (BYTE) fGetUnitStat(player, 12, 0));
    putU32(&p, fGetUnitStat(player, 14, 0));
    putU32(&p, fGetUnitStat(player, 15, 0));

    void* inventory = RDP(player, U_INVENTORY);
    void* cursor = readable(inventory, 0x24) ? RDP(inventory, INV_CURSOR) : NULL;
    putU32(&p, readable(cursor, 0x10) ? RD32(cursor, U_ID) : 0);

    int placesAt = p.len, places = 0, corpse = 0, placesFull = 0, itemsFull = 0, shopFull = 0;
    long corpseX = 0, corpseY = 0;
    DWORD seen[MAX_PT_PLACES];
    putU8(&p, 0);
    int units = 0;
    void* path = RDP(player, U_PATH);
    void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
    void* room2 = room1 ? RDP(room1, ROOM1_ROOM2) : NULL;
    void* level = room2 ? RDP(room2, ROOM2_LEVEL) : NULL;

    DWORD people[64];
    int nPeople = isTown(levelNo) ? townPresetPeople(level, people, 64) : 0;
    for (void* room = act ? RDP(act, ACT_ROOM1) : NULL; room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            DWORD type = RD32(unit, U_TYPE);
            if (type == UNIT_PLAYER && unit != player && !corpse) {
                void* other = RDP(unit, U_DATA);
                DWORD mode = RD32(unit, U_MODE);
                if ((mode == 0 || mode == 17) && readable(other, 16) && readable(pdata, 16) && memcmp(other, pdata, 16) == 0) {
                    corpse = 1;
                    corpseX = fGetUnitX(unit);
                    corpseY = fGetUnitY(unit);
                }
            }
            if (!isTown(levelNo)) continue;
            if (type == UNIT_MONSTER && !isValidMonster(unit, 1)) continue;
            DWORD txt = RD32(unit, U_TXTNO);
            if (type == UNIT_MONSTER) {
                int person = !isSummon(txt);
                if (person && nPeople > 0) {
                    person = 0;
                    for (int i = 0; i < nPeople && !person; i++) person = people[i] == txt;
                }
                if (!person) continue;
            }
            void* odata = type == UNIT_OBJECT ? RDP(unit, U_DATA) : NULL;
            BYTE* otxt = readable(odata, 4) ? (BYTE*) RDP(odata, ODATA_TXT) : NULL;
            int kind = placeKind(type, txt, otxt);
            if (!kind) continue;
            if (places >= MAX_PT_PLACES) {
                placesFull = 1;
                continue;
            }
            putPlace(&p, kind, txt, RD32(unit, U_ID), fGetUnitX(unit), fGetUnitY(unit));
            seen[places++] = (DWORD) kind << 16 | txt;
        }
    }

    int rooms = 0;
    for (void* r = isTown(levelNo) && readable(level, 0x14) ? RDP(level, LEVEL_ROOM2) : NULL;
         readable(r, 0x60) && rooms < 2048 && !placesFull; r = RDP(r, ROOM2_NEXT), rooms++) {
        long baseX = (long) RD32(r, ROOM2_X) * 5, baseY = (long) RD32(r, ROOM2_Y) * 5;
        int guard = 0;
        for (void* pu = RDP(r, ROOM2_PRESETS); readable(pu, 0x1C) && guard < 512 && !placesFull;
             pu = RDP(pu, 0x0C), guard++) {
            DWORD txt = RD32(pu, 0x04);
            int kind = placeKind(RD32(pu, 0x14), txt, NULL), known = 0;
            if (!kind) continue;
            for (int i = 0; i < places && !known; i++) known = seen[i] == ((DWORD) kind << 16 | txt);
            if (known) continue;
            if (places >= MAX_PT_PLACES) {
                placesFull = 1;
                break;
            }
            putPlace(&p, kind, txt, 0, baseX + (long) RD32(pu, 0x08), baseY + (long) RD32(pu, 0x18));
            seen[places++] = (DWORD) kind << 16 | txt;
        }
    }
    p.data[placesAt] = (BYTE) places;
    int corpseAt = p.len;
    putU8(&p, corpse);
    putU16(&p, (WORD) corpseX);
    putU16(&p, (WORD) corpseY);

    int itemsAt = p.len;
    putU8(&p, 0);
    void* carried[MAX_PT_ITEMS];
    int carriedCount = putInventoryItems(&p, inventory, 0, MAX_PT_ITEMS, &itemsFull, carried);
    p.data[itemsAt] = (BYTE) carriedCount;
    int shopAt = p.len;
    putU8(&p, 0);
    if (getUiVar(0x0C)) {
        void* (*currentNpc)(void) = (void* (*)(void)) (client + RVA_GetCurrentNpc);
        void* npc = npcMenuAvailable ? currentNpc() : NULL;
        if (readable(npc, 0x64)) p.data[shopAt] = (BYTE) putInventoryItems(&p, RDP(npc, U_INVENTORY), 1, MAX_PT_ITEMS, &shopFull, NULL);
    }

    putU8(&p, (placesFull ? 1 : 0) | (itemsFull ? 2 : 0) | (shopFull ? 4 : 0));

    if (p.len + 1 + 4 * carriedCount <= (int) sizeof(p.data)) {
        putU8(&p, PT_DETAILS);
        for (int i = 0; i < carriedCount; i++) putItemDetails(&p, carried[i]);
    }
    else putU8(&p, 0);
    putStashPages(&p, player);




    DWORD hash = 2166136261u, placesHash = 2166136261u;
    for (int i = genAt + 6; i < p.len; i++) {
        if (i >= placesAt && i < corpseAt) placesHash = (placesHash ^ p.data[i]) * 16777619u;
        else hash = (hash ^ p.data[i]) * 16777619u;
    }
    if (hash != lastHash) playtestGeneration++;
    else if (placesHash == lastPlacesHash && now - lastAt < 1000) return;
    lastHash = hash;
    lastPlacesHash = placesHash;
    lastAt = now;
    p.data[genAt] = (BYTE) (playtestGeneration & 0xFF);
    p.data[genAt + 1] = (BYTE) (playtestGeneration >> 8);
    sendPacket(&p);
}
