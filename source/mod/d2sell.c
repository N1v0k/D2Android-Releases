
























#define CMD_SELL         30
#define RVA_SellPrice    0x9B29F
#define RVA_SellMode     0x9B291
#define RVA_SellSite     0x9B2AF
#define SELL_WAIT_MS     1000
#define SELL_INV_CURSOR  0x20

static int sellAvailable;

static struct {
    int pending;
    DWORD request, item, npc, sentAt, goldBefore;
    int price;
    char code[4];
} sell;

static void sellReset(void) {
    if (sell.pending) logLine("sell: item %lu: new game, no answer counted", (unsigned long) sell.item);
    sell.pending = 0;
}

static int sellPending(void) {
    return sell.pending;
}

static void checkSellSite(int priceOk) {
    BYTE* mode = (BYTE*) (client + RVA_SellMode);
    BYTE* price = (BYTE*) (client + RVA_SellPrice);
    BYTE* site = (BYTE*) (client + RVA_SellSite);
    int ok = priceOk && readable(mode, 2) && mode[0] == 0x6A && mode[1] == 0x01
        && readable(price, 5) && price[0] == 0xE8 && (DWORD) (price + 5) + *(DWORD*) (price + 1) == client + RVA_PriceThunk
        && readable(site, 7) && site[0] == 0xB0 && site[1] == 0x33 && site[2] == 0xE8
        && (DWORD) (site + 7) + *(DWORD*) (site + 3) == client + RVA_Send17;
    sellAvailable = ok && shopAvailable;
    logLine("sell site %s: sell by packet %s", ok ? "verified" : "NOT as expected", sellAvailable ? "available" : "not offered");
}


static DWORD sellGold(void* player) {
    return fGetUnitStat(player, 14, 0) + fGetUnitStat(player, 15, 0);
}


static void* sellItemAt(void* player, int x, int y) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        void* path = RDP(item, U_PATH);
        if (RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1 && readable(path, IPATH_Y + 4)
            && (int) RD32(path, IPATH_X) == x && (int) RD32(path, IPATH_Y) == y) return item;
        item = RDP(data, IDATA_NEXTINV);
    }
    return NULL;
}


static int sellStillThere(void* player, DWORD id) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD32(item, U_ID) == id) return RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1;
        item = RDP(data, IDATA_NEXTINV);
    }
    return 0;
}

static void sellCommand(void* player, const BYTE* buf, int n) {
    if (n < 16) return;
    DWORD request = *(DWORD*) (buf + 12);
    int place = buf[8], x = buf[9], y = buf[10];
    if (!sellAvailable) {
        logLine("sell %lu: refused, not available", (unsigned long) request);
        return;
    }
    if (sell.pending) {
        logLine("sell %lu: refused, item %lu still waits for the server", (unsigned long) request, (unsigned long) sell.item);
        return;
    }
    if (shopJob.state == JOB_RUNNING) {
        logLine("sell %lu: refused, shop job %lu runs", (unsigned long) request, (unsigned long) shopJob.id);
        return;
    }
    void* npc = shopNpc();
    if (!npc) {
        logLine("sell %lu: refused, no shop open", (unsigned long) request);
        return;
    }
    void* inventory = RDP(player, U_INVENTORY);
    if (readable(inventory, 0x24) && RDP(inventory, SELL_INV_CURSOR)) {
        logLine("sell %lu: refused, an item is on the pointer", (unsigned long) request);
        return;
    }
    void* item = place == 0 ? sellItemAt(player, x, y) : NULL;
    if (!item) {
        logLine("sell %lu: refused, no item of the inventory at place %d cell %d,%d", (unsigned long) request, place, x, y);
        return;
    }

    if (!compareAvailable || RD32(client, RVA_HoverGrid) != 1 || RDP(client, RVA_HoverItem) != item) {
        logLine("sell %lu: refused, the pointer is not on the item at cell %d,%d", (unsigned long) request, x, y);
        return;
    }
    const char* code = itemCode(item);
    int tier;
    if (itemCategory(item, code, &tier) == CAT_QUEST) {
        logLine("sell %lu: refused, %.3s is a quest item", (unsigned long) request, code);
        return;
    }
    DWORD npcTxt = RD32(npc, U_TXTNO);

    int price = fGetItemPrice(player, item, RD8((void*) client, RVA_ShopDifficulty), *(void**) (client + RVA_ShopQuestFlags), npcTxt, 1);
#ifdef D2MOD_TESTCMDS

    if (n >= 20 && *(DWORD*) (buf + 16)) price = (int) *(DWORD*) (buf + 16);
#endif
    BYTE packet[17] = {0x33};
    *(DWORD*) (packet + 1) = RD32(npc, U_ID);
    *(DWORD*) (packet + 5) = RD32(item, U_ID);
    *(DWORD*) (packet + 9) = RD32(item, U_MODE);
    *(DWORD*) (packet + 13) = (DWORD) price;
    fSendPacket(17, 1, packet);
    sell.pending = 1;
    sell.request = request;
    sell.item = RD32(item, U_ID);
    sell.npc = RD32(npc, U_ID);
    sell.price = price;
    sell.goldBefore = sellGold(player);
    sell.sentAt = GetTickCount();
    memcpy(sell.code, code, 3);
    sell.code[3] = 0;
    logLine("sell %lu: %.3s (item %lu, cell %d,%d) to npc %lu (txt %lu) for %d, gold %lu", (unsigned long) request, code,
        (unsigned long) sell.item, x, y, (unsigned long) sell.npc, (unsigned long) npcTxt, price, (unsigned long) sell.goldBefore);
}


static void sellTick(void* player, DWORD now) {
    if (!sell.pending) return;
    DWORD gold = sellGold(player);
    int gone = !sellStillThere(player, sell.item);
    if (gone && gold > sell.goldBefore) {
        logLine("sell: sold %s (item %lu) for %lu, gold %lu -> %lu", sell.code, (unsigned long) sell.item,
            (unsigned long) (gold - sell.goldBefore), (unsigned long) sell.goldBefore, (unsigned long) gold);
        sell.pending = 0;
    }
    else if ((int) (now - sell.sentAt) > SELL_WAIT_MS) {


        if (gone) logLine("sell: %s (item %lu) gone but no gold within %d ms, gold %lu (body gold at its limit?)", sell.code,
            (unsigned long) sell.item, SELL_WAIT_MS, (unsigned long) gold);
        else logLine("sell: %s (item %lu) not sold within %d ms, gold %lu", sell.code, (unsigned long) sell.item, SELL_WAIT_MS,
            (unsigned long) gold);
        sell.pending = 0;
    }
}
