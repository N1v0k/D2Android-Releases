
#include "d2cats.h"

#define LOOT_AMMO          0x0001
#define LOOT_SCROLLS       0x0002
#define LOOT_KEYS          0x0004
#define LOOT_POTIONS_SMALL 0x0008
#define LOOT_POTIONS       0x0010
#define LOOT_POTIONS_OTHER 0x0020
#define LOOT_REJUV         0x0040
#define LOOT_GEMS_CHIPPED  0x0080
#define LOOT_GEMS_FLAWED   0x0100
#define LOOT_INFERIOR      0x0200
#define LOOT_WHITE         0x0400
#define LOOT_MAGIC         0x0800
#define LOOT_GOLD          0x1000
#define LOOT_SUPERIOR      0x2000
#define LOOT_ALL           0x3FFF

#define LQ_INFERIOR 1
#define LQ_NORMAL   2
#define LQ_SUPERIOR 3
#define LQ_MAGIC    4

typedef struct {
    int category;
    int tier;
    int quality;
    int ethereal, sockets;
    int baseClass;
    long gold;
} LootItem;

static int lootHiddenBy(const LootItem* it, unsigned mask, unsigned goldBelow) {
    if (!mask) return 0;
    switch (it->category) {
    case CAT_AMMO: return (mask & LOOT_AMMO) != 0;
    case CAT_TP_SCROLL: case CAT_ID_SCROLL: return (mask & LOOT_SCROLLS) != 0;
    case CAT_KEY: return (mask & LOOT_KEYS) != 0;
    case CAT_HEAL: case CAT_MANA: return (mask & (it->tier <= 2 ? LOOT_POTIONS_SMALL : LOOT_POTIONS)) != 0;
    case CAT_POTION_OTHER: return (mask & LOOT_POTIONS_OTHER) != 0;
    case CAT_REJUV: return (mask & LOOT_REJUV) != 0;
    case CAT_GEM:
        if (it->tier == 1) return (mask & LOOT_GEMS_CHIPPED) != 0;
        if (it->tier == 2) return (mask & LOOT_GEMS_FLAWED) != 0;
        return 0;
    case CAT_GOLD: return (mask & LOOT_GOLD) && it->gold >= 0 && (unsigned long) it->gold < goldBelow;
    case CAT_WEAPON: case CAT_ARMOR:
        if (it->quality == LQ_MAGIC) return (mask & LOOT_MAGIC) != 0;
        if (it->quality != LQ_INFERIOR && it->quality != LQ_NORMAL && it->quality != LQ_SUPERIOR) return 0;
        if (it->sockets > 0 || it->ethereal || it->baseClass != 0) return 0;
        return (mask & (it->quality == LQ_INFERIOR ? LOOT_INFERIOR : it->quality == LQ_SUPERIOR ? LOOT_SUPERIOR : LOOT_WHITE)) != 0;
    default: return 0;
    }
}

static void lootExpire(int filterWanted, unsigned* mask, unsigned* goldBelow) {
    if (filterWanted) return;
    *mask = 0;
    *goldBelow = 0;
}

static void lootParse(const BYTE* buf, int n, unsigned* mask, unsigned* goldBelow) {
    if (n >= 15) {
        *mask = (unsigned) (buf[11] | buf[12] << 8) & LOOT_ALL;
        *goldBelow = (unsigned) (buf[13] | buf[14] << 8);
    }
    else {
        *mask = 0;
        *goldBelow = 0;
    }
}
