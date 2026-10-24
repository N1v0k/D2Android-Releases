


















#define CMD_SHOP_POTIONS 16
#define CMD_REPAIR       29




#define RVA_RepairOne        0x7A372
#define RVA_RepairAllPush    0x7A359
#define RVA_RepairCall       0x7A396
#define REPAIR_MAX           16
#define RVA_BuySite          0x7A333
#define RVA_Send17           0xB6990
#define RVA_PriceCall        0x7D2BC
#define RVA_PriceThunk       0xC6AA
#define RVA_ShopDifficulty   0x11D1D8
#define RVA_ShopQuestFlags   0x1018B3
#define ORD_GetItemPrice     10186
#define UI_SHOP              0x0C
#define SHOP_JOB_MAX_MS      90000
#define SHOP_WAIT_MS         1000
#define SHOP_GAP_MS          300

enum { JOB_NONE = 0, JOB_RUNNING = 1, JOB_DONE = 2 };

enum {
    SHOP_END_DONE = 0, SHOP_END_GOLD = 1, SHOP_END_NO_WARE = 2, SHOP_END_REFUSED = 3, SHOP_END_CLOSED = 4,
    SHOP_END_BAD_LIST = 5, SHOP_END_STASH_FULL = 6, SHOP_END_NO_OWN_PAGE = 7, SHOP_END_TIME = 8,
    SHOP_END_UNAVAILABLE = 9, SHOP_END_INVENTORY_FULL = 10, SHOP_END_PARTIAL = 11
};

typedef int (__stdcall *GetItemPrice_t)(void* player, void* item, DWORD difficulty, void* questFlags, DWORD npcTxt, DWORD mode);
static GetItemPrice_t fGetItemPrice;
static int shopAvailable;
static int repairAvailable;

static struct {
    DWORD id;
    BYTE cmd, state, a, b, reason;

    BYTE want[2], toBelt[2], noWare[2];
    DWORD goldLimit, npc, started, nextAt, sentAt, goldBefore;
    int waiting, kind, countBefore, misses;
    void* player;

    DWORD tried[REPAIR_MAX], itemId;
    int nTried;

    DWORD effectAt, goldStart;
} shopJob;

static void checkSellSite(int priceOk);

static void checkShopSites(void) {
    static const BYTE send17[] = {0x83, 0xEC, 0x14, 0x53, 0x89, 0x54, 0x24, 0x09, 0x88, 0x44, 0x24, 0x04};
    static const BYTE len17[] = {0xBB, 0x11, 0x00, 0x00, 0x00};
    BYTE* buy = (BYTE*) (client + RVA_BuySite);
    BYTE* sender = (BYTE*) (client + RVA_Send17);
    BYTE* call = (BYTE*) (client + RVA_PriceCall);
    BYTE* thunk = (BYTE*) (client + RVA_PriceThunk);
    fGetItemPrice = (GetItemPrice_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetItemPrice);
    int senderOk = readable(sender, 0x22) && memcmp(sender, send17, sizeof(send17)) == 0 && memcmp(sender + 0x1D, len17, sizeof(len17)) == 0;
    int buyOk = readable(buy, 7) && buy[0] == 0xB0 && buy[1] == 0x32 && buy[2] == 0xE8
        && (DWORD) (buy + 7) + *(DWORD*) (buy + 3) == client + RVA_Send17 && senderOk;

    BYTE* one = (BYTE*) (client + RVA_RepairOne);
    BYTE* all = (BYTE*) (client + RVA_RepairAllPush);
    BYTE* rcall = (BYTE*) (client + RVA_RepairCall);
    int repairOk = senderOk && readable(one, 4) && one[0] == 0xB0 && one[1] == 0x35 && one[2] == 0xEB
        && readable(one + 8, 4) && one[8] == 0xB0 && one[9] == 0x35 && one[10] == 0xEB
        && readable(all, 5) && all[0] == 0x68 && *(DWORD*) (all + 1) == 0x80000000u
        && readable(rcall, 5) && rcall[0] == 0xE8 && (DWORD) (rcall + 5) + *(DWORD*) (rcall + 1) == client + RVA_Send17;

    int priceOk = fGetItemPrice && readable(call, 5) && call[0] == 0xE8
        && (DWORD) (call + 5) + *(DWORD*) (call + 1) == client + RVA_PriceThunk
        && readable(thunk, 6) && thunk[0] == 0xFF && thunk[1] == 0x25 && readable(*(void**) (thunk + 2), 4)
        && **(DWORD**) (thunk + 2) == (DWORD) fGetItemPrice;

    shopAvailable = buyOk && priceOk && npcMenuAvailable && beltMoveAvailable && fSendPacket;
    repairAvailable = repairOk && npcMenuAvailable && fSendPacket;
    logLine("shop sites: buy %s, price %s: refill potions %s", buyOk ? "verified" : "NOT as expected",
        priceOk ? "verified" : "NOT as expected", shopAvailable ? "available" : "not offered");
    logLine("shop sites: repair %s", repairAvailable ? "verified, available" : "NOT as expected, not offered");
    checkSellSite(priceOk);
}

static void shopFinish(int reason) {
    shopJob.state = JOB_DONE;
    shopJob.reason = (BYTE) reason;
    logLine("shop job %lu (command %d) done: reason %d, %d / %d", (unsigned long) shopJob.id, shopJob.cmd, reason,
        shopJob.a, shopJob.b);
}

static void* shopNpc(void) {
    if (!npcMenuAvailable || !getUiVar(UI_SHOP)) return NULL;
    void* (*currentNpc)(void) = (void* (*)(void)) (client + RVA_GetCurrentNpc);
    void* npc = currentNpc();
    return readable(npc, 0x64) ? npc : NULL;
}


static int sellPending(void);

static void shopCommand(void* player, const BYTE* buf, int n) {
    if (shopJob.state == JOB_RUNNING) {
        logLine("shop: command %d dropped, job %lu still runs", buf[5], (unsigned long) shopJob.id);
        return;
    }

    if (sellPending()) {
        logLine("shop: command %d dropped, a sale waits for the server", buf[5]);
        return;
    }
    if (n < 20) return;
    memset(&shopJob, 0, sizeof(shopJob));
    shopJob.id = *(DWORD*) (buf + 16);
    shopJob.cmd = buf[5];
    shopJob.state = JOB_RUNNING;
    shopJob.player = player;
    shopJob.started = GetTickCount();
    shopJob.want[0] = buf[8] > 40 ? 40 : buf[8];
    shopJob.want[1] = buf[9] > 40 ? 40 : buf[9];
    shopJob.toBelt[0] = buf[10] > 16 ? 16 : buf[10];
    shopJob.toBelt[1] = buf[11] > 16 ? 16 : buf[11];
    shopJob.goldLimit = *(DWORD*) (buf + 12);
    void* npc = shopNpc();
    if (shopJob.cmd == CMD_REPAIR) {
        shopJob.want[0] = shopJob.want[1] = shopJob.toBelt[0] = shopJob.toBelt[1] = 0;
        logLine("shop job %lu: repair", (unsigned long) shopJob.id);
        if (!repairAvailable) shopFinish(SHOP_END_UNAVAILABLE);
        else if (!npc) shopFinish(SHOP_END_CLOSED);
        else shopJob.npc = RD32(npc, U_ID);
        return;
    }
    logLine("shop job %lu: refill %d healing (%d belt), %d mana (%d belt), gold limit %lu", (unsigned long) shopJob.id,
        shopJob.want[0], shopJob.toBelt[0], shopJob.want[1], shopJob.toBelt[1], (unsigned long) shopJob.goldLimit);
    if (!shopAvailable) shopFinish(SHOP_END_UNAVAILABLE);
    else if (!npc) shopFinish(SHOP_END_CLOSED);
    else shopJob.npc = RD32(npc, U_ID);
}


static void* shopPotion(void* npc, int kind) {
    void* inventory = RDP(npc, U_INVENTORY);
    void* best = NULL;
    int bestTier = 0, guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        const char* code = itemCode(item);
        if (potionKind(code) == kind + 1 && potionTier(code) > bestTier) {
            best = item;
            bestTier = potionTier(code);
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return best;
}

static int shopFreeCells(const Carried* c) {
    int used = 0;
    for (int i = 0; i < INV_W * INV_H; i++) used += (c->used[i / 8] >> (i % 8)) & 1;
    return INV_W * INV_H - used;
}






typedef struct { DWORD id; int rank; DWORD missing; } Worn;

static int repairWorn(void* player, Worn* out, int max, int* broken) {
    int n = 0, steps = 0;
    *broken = 0;
    void* equipment = RDP(player, U_INVENTORY);
    for (void* item = readable(equipment, 0x10) ? RDP(equipment, INV_FIRST) : NULL; item && steps < 512; steps++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD8(data, IDATA_NODEPAGE) == 3) {
            DWORD full = fGetUnitStat(item, 73, 0), dur = fGetUnitStat(item, 72, 0);
            full = full * (100 + fGetUnitStat(item, 75, 0)) / 100;
            if (full > 0 && !fGetUnitStat(item, 152, 0) && dur < full) {
                int rank = dur == 0 ? 2 : (int) (dur * 100 / full) <= DURA_WARN_PERCENT ? 1 : 0;
                if (rank == 2) (*broken)++;
                if (n < max) {
                    out[n].id = RD32(item, U_ID);
                    out[n].rank = rank;
                    out[n].missing = full - dur;
                    n++;
                }
            }
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return n;
}

static void repairSend(DWORD item, DWORD flags) {
    BYTE packet[17] = {0x35};
    *(DWORD*) (packet + 1) = shopJob.npc;
    *(DWORD*) (packet + 5) = item;
    *(DWORD*) (packet + 9) = 0;
    *(DWORD*) (packet + 13) = flags;
    fSendPacket(17, 1, packet);
}

static void repairFinish(void* player, int reason) {
    Worn w[REPAIR_MAX];
    int broken, n = repairWorn(player, w, REPAIR_MAX, &broken);
    shopJob.b = broken > 0 ? 1 : 0;
    DWORD gold = fGetUnitStat(player, 14, 0) + fGetUnitStat(player, 15, 0);
    if (shopJob.goldStart) {
        logLine("repair: job %lu, %d repaired, %d worn left (%d broken), gold %lu -> %lu (paid %lu)", (unsigned long) shopJob.id,
            shopJob.a, n, broken, (unsigned long) shopJob.goldStart, (unsigned long) gold,
            (unsigned long) (shopJob.goldStart > gold ? shopJob.goldStart - gold : 0));
    }
    shopFinish(reason);
}








static void updateRepair(void* player, DWORD now, DWORD gold) {
    Worn w[REPAIR_MAX];
    int broken, n = repairWorn(player, w, REPAIR_MAX, &broken);






    if (shopJob.waiting == 1) {
        if (gold < shopJob.goldBefore || n < shopJob.countBefore) {
            shopJob.waiting = 3;
            shopJob.effectAt = now;
            logLine("repair: all, effect after %d ms: worn %d -> %d, gold %lu -> %lu", (int) (now - shopJob.sentAt),
                shopJob.countBefore, n, (unsigned long) shopJob.goldBefore, (unsigned long) gold);
        }
        else if ((int) (now - shopJob.sentAt) > SHOP_WAIT_MS) {
            shopJob.waiting = 0;
            shopJob.kind = 1;
            logLine("repair: all did nothing (gold %lu), one by one", (unsigned long) gold);
        }
        return;
    }
    if (shopJob.waiting == 3) {
        int paid = gold < shopJob.goldBefore;
        if (!paid && (int) (now - shopJob.effectAt) <= SHOP_WAIT_MS) return;
        if (paid) {
            logLine("repair: all, gold %lu -> %lu (paid %lu), worn %d -> %d", (unsigned long) shopJob.goldBefore, (unsigned long) gold,
                (unsigned long) (shopJob.goldBefore - gold), shopJob.countBefore, n);
        }
        else {
            logLine("repair: all, worn %d -> %d without gold charged within %d ms (gold %lu)", shopJob.countBefore, n,
                SHOP_WAIT_MS, (unsigned long) gold);
        }
        shopJob.a = (BYTE) (shopJob.countBefore > n ? shopJob.countBefore - n : 0);
        shopJob.waiting = 0;
        shopJob.nextAt = now + SHOP_GAP_MS;
        if (n == 0) repairFinish(player, SHOP_END_DONE);
        else shopJob.kind = 1;
        return;
    }
    if (shopJob.waiting == 2) {
        int still = 0;
        for (int i = 0; i < n; i++) if (w[i].id == shopJob.itemId) still = 1;
        if (!still || gold < shopJob.goldBefore) {
            shopJob.waiting = 4;
            shopJob.effectAt = now;
            logLine("repair: item %lu, effect after %d ms: %s, gold %lu -> %lu", (unsigned long) shopJob.itemId,
                (int) (now - shopJob.sentAt), still ? "still worn" : "durability full", (unsigned long) shopJob.goldBefore,
                (unsigned long) gold);
        }
        else if ((int) (now - shopJob.sentAt) > SHOP_WAIT_MS) {
            logLine("repair: item %lu not repaired (gold %lu)", (unsigned long) shopJob.itemId, (unsigned long) gold);
            shopJob.waiting = 0;
            shopJob.nextAt = now + SHOP_GAP_MS;
        }
        return;
    }
    if (shopJob.waiting == 4) {
        int paid = gold < shopJob.goldBefore;
        if (!paid && (int) (now - shopJob.effectAt) <= SHOP_WAIT_MS) return;
        if (paid) {
            logLine("repair: item %lu, gold %lu -> %lu (paid %lu)", (unsigned long) shopJob.itemId, (unsigned long) shopJob.goldBefore,
                (unsigned long) gold, (unsigned long) (shopJob.goldBefore - gold));
        }
        else {
            logLine("repair: item %lu back without gold charged within %d ms (gold %lu)", (unsigned long) shopJob.itemId,
                SHOP_WAIT_MS, (unsigned long) gold);
        }
        shopJob.a++;
        shopJob.waiting = 0;
        shopJob.nextAt = now + SHOP_GAP_MS;
        return;
    }
    if ((int) (now - shopJob.nextAt) < 0) return;
    if (n == 0) {
        repairFinish(player, SHOP_END_DONE);
        return;
    }
    if (shopJob.kind == 0) {
        shopJob.goldStart = gold;
        repairSend(0, 0x80000000u);
        trace("repair: all at npc %lu, %d worn (%d broken), gold %lu", (unsigned long) shopJob.npc, n, broken, (unsigned long) gold);
        shopJob.waiting = 1;
        shopJob.countBefore = n;
        shopJob.goldBefore = gold;
        shopJob.sentAt = now;
        return;
    }
    int best = -1;
    for (int i = 0; i < n; i++) {
        int done = 0;
        for (int t = 0; t < shopJob.nTried; t++) if (shopJob.tried[t] == w[i].id) done = 1;
        if (done) continue;
        if (best < 0 || w[i].rank > w[best].rank || (w[i].rank == w[best].rank && w[i].missing < w[best].missing)) best = i;
    }
    if (best < 0 || shopJob.nTried >= REPAIR_MAX) {
        repairFinish(player, shopJob.a > 0 ? SHOP_END_PARTIAL : SHOP_END_GOLD);
        return;
    }
    shopJob.tried[shopJob.nTried++] = w[best].id;
    shopJob.itemId = w[best].id;
    repairSend(w[best].id, 0);
    trace("repair: item %lu (rank %d, missing %lu), gold %lu", (unsigned long) w[best].id, w[best].rank,
        (unsigned long) w[best].missing, (unsigned long) gold);
    shopJob.waiting = 2;
    shopJob.goldBefore = gold;
    shopJob.sentAt = now;
}

static void sellTick(void* player, DWORD now);
static void sellReset(void);


static void updateShop(void* player, DWORD now) {
    sellTick(player, now);
    if (shopJob.state != JOB_RUNNING) return;
    if (player != shopJob.player) {
        shopFinish(SHOP_END_CLOSED);
        return;
    }
    void* npc = shopNpc();
    if (!npc || RD32(npc, U_ID) != shopJob.npc) {
        shopFinish(SHOP_END_CLOSED);
        return;
    }

    if ((int) (now - shopJob.started) > SHOP_JOB_MAX_MS) {
        shopFinish(SHOP_END_TIME);
        return;
    }

    DWORD gold = fGetUnitStat(player, 14, 0) + fGetUnitStat(player, 15, 0);
    if (shopJob.cmd == CMD_REPAIR) {
        updateRepair(player, now, gold);
        return;
    }
    Carried c;
    readCarried(player, &c);
    int count[2] = {c.belt[0] + c.inventory[0], c.belt[1] + c.inventory[1]};
    if (shopJob.waiting == 1) {
        int k = shopJob.kind;
        if (count[k] > shopJob.countBefore) {
            if (k == 0) shopJob.a++;
            else shopJob.b++;
            shopJob.misses = 0;
            trace("shop: bought %s %d, gold %lu -> %lu, belt %d inventory %d", k ? "mana" : "healing", k ? shopJob.b : shopJob.a,
                (unsigned long) shopJob.goldBefore, (unsigned long) gold, c.belt[k], c.inventory[k]);
            shopJob.waiting = 0;
            shopJob.nextAt = now + SHOP_GAP_MS;
        }
        else if ((int) (now - shopJob.sentAt) > SHOP_WAIT_MS) {
            shopJob.waiting = 0;
            shopJob.nextAt = now + SHOP_GAP_MS;
            logLine("shop: no %s potion after the buy packet (gold %lu)", k ? "mana" : "healing", (unsigned long) gold);
            if (++shopJob.misses >= 2) shopFinish(SHOP_END_REFUSED);
        }
        return;
    }
    if (shopJob.waiting == 2) {
        int k = shopJob.kind;
        if (c.belt[k] > shopJob.countBefore || (int) (now - shopJob.sentAt) > SHOP_WAIT_MS) {

            if (c.belt[k] <= shopJob.countBefore) {
                logLine("shop: the belt did not take a %s potion", k ? "mana" : "healing");
                shopJob.toBelt[k] = 0;
            }
            shopJob.waiting = 0;
            shopJob.nextAt = now + SHOP_GAP_MS;
        }
        return;
    }
    if ((int) (now - shopJob.nextAt) < 0) return;

    for (int k = 0; k < 2; k++) {
        if (shopJob.toBelt[k] == 0 || c.inventory[k] == 0) continue;
        shopJob.toBelt[k]--;
        moveToBelt(player, k + 1);
        shopJob.waiting = 2;
        shopJob.kind = k;
        shopJob.countBefore = c.belt[k];
        shopJob.sentAt = now;
        return;
    }
    int bought[2] = {shopJob.a, shopJob.b};
    int k = bought[0] < shopJob.want[0] && !shopJob.noWare[0] ? 0 : (bought[1] < shopJob.want[1] && !shopJob.noWare[1] ? 1 : -1);
    if (k < 0) {
        shopFinish(shopJob.noWare[0] || shopJob.noWare[1] ? SHOP_END_NO_WARE : SHOP_END_DONE);
        return;
    }
    void* item = shopPotion(npc, k);
    if (!item) {
        shopJob.noWare[k] = 1;
        logLine("shop: no %s potion for sale", k ? "mana" : "healing");
        return;
    }
    DWORD npcTxt = RD32(npc, U_TXTNO);
    int price = fGetItemPrice(player, item, RD8((void*) client, RVA_ShopDifficulty), *(void**) (client + RVA_ShopQuestFlags), npcTxt, 0);
    if (price <= 0) {
        logLine("shop: price of %.3s is %d", itemCode(item), price);
        shopFinish(SHOP_END_REFUSED);
        return;
    }
    if (gold < (DWORD) price || gold - (DWORD) price < shopJob.goldLimit) {
        shopFinish(SHOP_END_GOLD);
        return;
    }

    if (shopFreeCells(&c) == 0) {
        shopFinish(SHOP_END_INVENTORY_FULL);
        return;
    }
    BYTE packet[17] = {0x32};
    *(DWORD*) (packet + 1) = shopJob.npc;
    *(DWORD*) (packet + 5) = RD32(item, U_ID);
    *(DWORD*) (packet + 9) = 0;
    *(DWORD*) (packet + 13) = (DWORD) price;
    fSendPacket(17, 1, packet);
    trace("shop: buy %.3s (item %lu) from npc %lu (txt %lu) for %d, gold %lu", itemCode(item), (unsigned long) RD32(item, U_ID),
        (unsigned long) shopJob.npc, (unsigned long) npcTxt, price, (unsigned long) gold);
    shopJob.waiting = 1;
    shopJob.kind = k;
    shopJob.countBefore = count[k];
    shopJob.goldBefore = gold;
    shopJob.sentAt = now;
}







static BYTE shopWares(void) {
    void* npc = shopAvailable ? shopNpc() : NULL;
    if (!npc) return 0;
    return (shopPotion(npc, 0) ? 1 : 0) | (shopPotion(npc, 1) ? 2 : 0);
}

static void putShopJob(Packet* p) {
    putU32(p, shopJob.id);
    putU8(p, shopJob.cmd);
    putU8(p, shopJob.state);
    putU8(p, shopJob.a);
    putU8(p, shopJob.b);
    putU8(p, shopJob.reason);
    putU8(p, shopWares());
}





static DWORD shopJobKey(int part) {
    if (part == 0) return shopJob.id;
    if (part == 2) return shopWares();
    return shopJob.cmd | (DWORD) shopJob.state << 8 | (DWORD) shopJob.a << 16 | (DWORD) shopJob.b << 24 | (DWORD) shopJob.reason * 2654435761u;
}


static void shopNewPlayer(void* player) {
    static void* last;
    if (player == last) return;
    last = player;
    if (shopJob.state == JOB_RUNNING) logLine("shop job %lu ended: new game", (unsigned long) shopJob.id);
    memset(&shopJob, 0, sizeof(shopJob));
    sellReset();
}

