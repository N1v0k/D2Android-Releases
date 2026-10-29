



























#define ABGABE_MAX      48
#define ABGABE_WAIT_MS  1000
#define ABGABE_PAGE_MS  600
#define ABGABE_PAGES    30
#define ABGABE_STASH_W  6
#define ABGABE_STASH_H  8
#define UI_STASH_OPEN   0x19

static struct {
    DWORD ids[ABGABE_MAX];
    int n, next, phase, pages;
    DWORD item, sentAt, waitUntil;
    DWORD fromX, fromY, toX, toY;
    DWORD gold, goldBefore;
} abgabe;

enum { AB_START, AB_NEXT, AB_SOLD_WAIT, AB_PICK_WAIT, AB_PLACE_WAIT, AB_BACK_WAIT, AB_PAGE_WAIT, AB_GOLD, AB_GOLD_WAIT };


static void* abgabeInInventory(void* player, DWORD id) {
    return equipFind(player, id, 0, 0);
}


static int abgabeStashPlace(void* player, int w, int h, DWORD* outX, DWORD* outY) {
    BYTE used[ABGABE_STASH_W * ABGABE_STASH_H] = {0};
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        void* path = RDP(item, U_PATH);
        int iw, ih;
        if (RD8(data, IDATA_LOCATION) == 0x04 && RD8(data, IDATA_NODEPAGE) == 1 && readable(path, 0x14) && equipSize(item, &iw, &ih)) {
            int x0 = (int) RD32(path, IPATH_X), y0 = (int) RD32(path, IPATH_Y);
            for (int y = y0; y < y0 + ih && y < ABGABE_STASH_H; y++)
                for (int x = x0; x < x0 + iw && x < ABGABE_STASH_W; x++)
                    if (x >= 0 && y >= 0) used[y * ABGABE_STASH_W + x] = 1;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    for (int x = 0; x + w <= ABGABE_STASH_W; x++) {
        for (int y = 0; y + h <= ABGABE_STASH_H; y++) {
            int free = 1;
            for (int dy = 0; dy < h && free; dy++)
                for (int dx = 0; dx < w && free; dx++)
                    if (used[(y + dy) * ABGABE_STASH_W + x + dx]) free = 0;
            if (free) {
                *outX = (DWORD) x;
                *outY = (DWORD) y;
                return 1;
            }
        }
    }
    return 0;
}


static int abgabeInStash(void* player, DWORD id) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD32(item, U_ID) == id) return RD8(data, IDATA_LOCATION) == 0x04;
        item = RDP(data, IDATA_NEXTINV);
    }
    return 0;
}


static void abgabeSendStash(DWORD id, DWORD x, DWORD y) {
    BYTE packet[17] = {0x18};
    *(DWORD*) (packet + 1) = id;
    *(DWORD*) (packet + 5) = x;
    *(DWORD*) (packet + 9) = y;
    *(DWORD*) (packet + 13) = 4;
    fSendPacket(17, 1, packet);
}

static int abgabePlugY(void) {
    return plugyLoaded && GetModuleHandleA("PlugY.dll") != NULL;
}


static void abgabeStart(void* player, const BYTE* buf, int n) {
    int count = buf[8];
    if (count > ABGABE_MAX) count = ABGABE_MAX;
    if (20 + 4 * count > n) count = (n - 20) / 4;
    memset(&abgabe, 0, sizeof(abgabe));
    for (int i = 0; i < count; i++) abgabe.ids[i] = *(DWORD*) (buf + 20 + 4 * i);
    abgabe.n = count;
    abgabe.phase = AB_START;
    abgabe.gold = shopJob.cmd == CMD_STASH_LOOT ? *(DWORD*) (buf + 12) : 0;
    logLine("abgabe job %lu: command %d, %d items", (unsigned long) shopJob.id, shopJob.cmd, count);
    if (shopJob.cmd == CMD_SELL_LOOT) {
        void* npc = shopNpc();
        if (!sellAvailable) shopFinish(SHOP_END_UNAVAILABLE);
        else if (!npc) shopFinish(SHOP_END_CLOSED);
        else shopJob.npc = RD32(npc, U_ID);
    }
    else if (!equipAvailable || !fSendPacket) shopFinish(SHOP_END_UNAVAILABLE);
    else if (!getUiVar(UI_STASH_OPEN)) shopFinish(SHOP_END_CLOSED);
    (void) player;
}

static void abgabeSkip(const char* why) {
    if (shopJob.b < 255) shopJob.b++;
    logLine("abgabe: item %lu skipped: %s", (unsigned long) abgabe.item, why);
    abgabe.phase = AB_NEXT;
}

static void abgabeDone(void) {
    if (shopJob.a < 255) shopJob.a++;
    abgabe.phase = AB_NEXT;
}


static void* abgabeNextItem(void* player) {
    while (abgabe.next < abgabe.n) {
        abgabe.item = abgabe.ids[abgabe.next++];
        void* item = abgabeInInventory(player, abgabe.item);
        if (!item) {
            abgabeSkip("not in the inventory");
            continue;
        }
        int tier;
        const char* code = itemCode(item);
        if (itemCategory(item, code, &tier) == CAT_QUEST) {
            abgabeSkip("quest item");
            continue;
        }
        return item;
    }
    return NULL;
}


static void abgabeSellTick(void* player, void* npc, DWORD now) {
    if (abgabe.phase == AB_SOLD_WAIT) {
        if (!abgabeInInventory(player, abgabe.item)) {
            logLine("abgabe: sold item %lu", (unsigned long) abgabe.item);
            abgabeDone();
        }
        else if ((int) (now - abgabe.sentAt) > ABGABE_WAIT_MS) abgabeSkip("not sold");
        return;
    }
    void* item = abgabeNextItem(player);
    if (!item) {
        shopFinish(SHOP_END_DONE);
        return;
    }
    int price = fGetItemPrice(player, item, RD8((void*) client, RVA_ShopDifficulty), *(void**) (client + RVA_ShopQuestFlags),
        RD32(npc, U_TXTNO), 1);
    BYTE packet[17] = {0x33};
    *(DWORD*) (packet + 1) = RD32(npc, U_ID);
    *(DWORD*) (packet + 5) = abgabe.item;
    *(DWORD*) (packet + 9) = RD32(item, U_MODE);
    *(DWORD*) (packet + 13) = (DWORD) (price > 0 ? price : 0);
    fSendPacket(17, 1, packet);
    trace("abgabe: sell %.3s (item %lu) for %d", itemCode(item), (unsigned long) abgabe.item, price);
    abgabe.phase = AB_SOLD_WAIT;
    abgabe.sentAt = now;
}


static void abgabeStashTick(void* player, DWORD now) {
    if (!getUiVar(UI_STASH_OPEN)) {

        shopFinish(equipCursorId(player) ? SHOP_END_REFUSED : SHOP_END_CLOSED);
        return;
    }
    if ((int) (now - abgabe.waitUntil) < 0) return;
    switch (abgabe.phase) {
        case AB_START:
            abgabe.phase = abgabe.gold ? AB_GOLD : AB_NEXT;
            if (abgabePlugY()) {
                plugyCommand(0x1B);
                plugyCommand(0x1F);
                abgabe.waitUntil = now + ABGABE_PAGE_MS;
            }
            return;
        case AB_PICK_WAIT:
            if (equipCursorId(player) == abgabe.item) {
                abgabeSendStash(abgabe.item, abgabe.toX, abgabe.toY);
                abgabe.phase = AB_PLACE_WAIT;
                abgabe.sentAt = now;
            }
            else if ((int) (now - abgabe.sentAt) > ABGABE_WAIT_MS) {
                if (equipCursorId(player)) {
                    shopFinish(SHOP_END_REFUSED);
                    return;
                }
                abgabeSkip("not taken to the pointer");
            }
            return;
        case AB_PLACE_WAIT:
            if (!equipCursorId(player) && abgabeInStash(player, abgabe.item)) {
                logLine("abgabe: item %lu into the stash at %lu,%lu", (unsigned long) abgabe.item, (unsigned long) abgabe.toX,
                    (unsigned long) abgabe.toY);
                abgabeDone();
            }
            else if ((int) (now - abgabe.sentAt) > ABGABE_WAIT_MS) {

                equipSendToInventory(abgabe.item, abgabe.fromX, abgabe.fromY);
                abgabe.phase = AB_BACK_WAIT;
                abgabe.sentAt = now;
            }
            return;
        case AB_BACK_WAIT:
            if (!equipCursorId(player)) abgabeSkip("refused by the stash, back in the inventory");
            else if ((int) (now - abgabe.sentAt) > ABGABE_WAIT_MS) shopFinish(SHOP_END_REFUSED);
            return;
        case AB_GOLD: {
            DWORD body = fGetUnitStat(player, 14, 0);
            DWORD amount = abgabe.gold < body ? abgabe.gold : body;
            abgabe.phase = AB_NEXT;
            if (!amount) return;
            BYTE packet[7] = {0x4F, 0x14, 0x00};
            *(WORD*) (packet + 3) = (WORD) (amount >> 16);
            *(WORD*) (packet + 5) = (WORD) (amount & 0xFFFF);
            fSendPacket(7, 1, packet);
            abgabe.goldBefore = fGetUnitStat(player, 15, 0);
            abgabe.sentAt = now;
            abgabe.phase = AB_GOLD_WAIT;
            logLine("abgabe: deposit %lu gold (body %lu, stash %lu)", (unsigned long) amount, (unsigned long) body,
                (unsigned long) abgabe.goldBefore);
            return;
        }
        case AB_GOLD_WAIT: {
            DWORD stash = fGetUnitStat(player, 15, 0);
            if (stash > abgabe.goldBefore || (int) (now - abgabe.sentAt) > ABGABE_WAIT_MS) {
                logLine("abgabe: stash gold %lu -> %lu", (unsigned long) abgabe.goldBefore, (unsigned long) stash);
                abgabe.phase = AB_NEXT;
            }
            return;
        }
        case AB_PAGE_WAIT:
            abgabe.phase = AB_NEXT;
            abgabe.next--;
            return;
        default:
            break;
    }
    if (equipCursorId(player)) {
        shopFinish(SHOP_END_REFUSED);
        return;
    }


    if ((int) (now - shopJob.started) > SHOP_JOB_MAX_MS) {
        shopFinish(SHOP_END_TIME);
        return;
    }
    void* item = abgabeNextItem(player);
    if (!item) {
        shopFinish(SHOP_END_DONE);
        return;
    }
    int w, h;
    void* path = RDP(item, U_PATH);
    if (!equipSize(item, &w, &h) || !readable(path, 0x14)) {
        abgabeSkip("size unknown");
        return;
    }
    if (!abgabeStashPlace(player, w, h, &abgabe.toX, &abgabe.toY)) {
        if (abgabePlugY() && abgabe.pages < ABGABE_PAGES) {
            abgabe.pages++;
            plugyCommand(0x1A);
            abgabe.phase = AB_PAGE_WAIT;
            abgabe.waitUntil = now + ABGABE_PAGE_MS;
            return;
        }
        logLine("abgabe: no place for item %lu (%dx%d) after %d pages", (unsigned long) abgabe.item, w, h, abgabe.pages);
        shopFinish(SHOP_END_STASH_FULL);
        return;
    }
    abgabe.fromX = RD32(path, IPATH_X);
    abgabe.fromY = RD32(path, IPATH_Y);
    equipSendId(0x19, abgabe.item);
    trace("abgabe: item %lu (%.3s %dx%d) to the pointer, then stash %lu,%lu", (unsigned long) abgabe.item, itemCode(item), w, h,
        (unsigned long) abgabe.toX, (unsigned long) abgabe.toY);
    abgabe.phase = AB_PICK_WAIT;
    abgabe.sentAt = now;
}
