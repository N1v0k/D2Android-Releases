










#define VALUE_VERSION 1
#define VALUE_FIXED   88
#define VALUE_PAIRS   56
#define VALUE_MAX     10


enum { VK_OTHER = 0, VK_RUNE, VK_GEM, VK_REJUV, VK_JEWELRY, VK_QUEST };


#define VQ_NORMAL   2
#define VQ_SUPERIOR 3

typedef struct {
    unsigned char runeword, runes[33], gems[5], rejuv, jewelry[8], other[8], base[3];
    unsigned char drop, dropGems, dropRunes;
    unsigned short goldAt[4];
    unsigned char goldFactor[8];
    int pairs;
    unsigned short pairType[VALUE_PAIRS];
    unsigned char pairSockets[VALUE_PAIRS];
} ValueRules;

typedef struct {
    int kind;
    int tier;
    int quality;
    int ethereal, runeword, sockets;
    int baseClass;
    unsigned long cost;
} ValueItem;


static int valueParse(const unsigned char* buf, int n, ValueRules* r) {
    if (n < VALUE_FIXED || buf[8] != VALUE_VERSION) return 0;
    int pairs = buf[87];
    if (pairs > VALUE_PAIRS || n < VALUE_FIXED + 3 * pairs) return 0;
    const unsigned char* p = buf + 9;
    r->runeword = *p++;
    for (int i = 0; i < 33; i++) r->runes[i] = *p++;
    for (int i = 0; i < 5; i++) r->gems[i] = *p++;
    r->rejuv = *p++;
    for (int i = 0; i < 8; i++) r->jewelry[i] = *p++;
    for (int i = 0; i < 8; i++) r->other[i] = *p++;
    for (int i = 0; i < 3; i++) r->base[i] = *p++;
    r->drop = *p++;
    r->dropGems = *p++;
    r->dropRunes = *p++;
    for (int i = 0; i < 4; i++, p += 2) r->goldAt[i] = (unsigned short) (p[0] | p[1] << 8);
    for (int i = 0; i < 8; i++) r->goldFactor[i] = *p++;
    r->pairs = *p++;
    for (int i = 0; i < pairs; i++, p += 3) {
        r->pairType[i] = (unsigned short) (p[0] | p[1] << 8);
        r->pairSockets[i] = p[2];
    }
    return 1;
}

static int valueCap(int note) {
    return note < 0 ? 0 : (note > VALUE_MAX ? VALUE_MAX : note);
}



static int valueSocketBase(const ValueRules* r, const ValueItem* it, int (*isType)(void*, int), void* ctx) {
    if (it->quality != VQ_NORMAL && it->quality != VQ_SUPERIOR) return 0;
    if (it->ethereal || it->runeword || it->sockets <= 0 || it->baseClass < 1 || it->baseClass > 3) return 0;
    for (int i = 0; i < r->pairs; i++)
        if (r->pairSockets[i] == it->sockets && isType(ctx, r->pairType[i])) return 1;
    return 0;
}


static int valueNote(const ValueRules* r, const ValueItem* it, int difficulty, int (*isType)(void*, int), void* ctx) {
    if (it->kind == VK_QUEST) return -1;
    int q = it->quality >= 1 && it->quality <= 8 ? it->quality - 1 : -1;
    int note = 0;
    switch (it->kind) {
    case VK_RUNE:
        if (it->tier >= 1 && it->tier <= 33) {
            note = r->runes[it->tier - 1];
            if (difficulty > 0 && it->tier <= r->dropRunes) note -= r->drop;
        }
        break;
    case VK_GEM:
        if (it->tier >= 1 && it->tier <= 5) {
            note = r->gems[it->tier - 1];
            if (difficulty > 0 && it->tier <= r->dropGems) note -= r->drop;
        }
        break;
    case VK_REJUV:
        note = r->rejuv;
        break;
    case VK_JEWELRY:
        note = q >= 0 ? r->jewelry[q] : 0;
        break;
    default:
        note = q >= 0 ? r->other[q] : 0;
        if (it->runeword && r->runeword > note) note = r->runeword;
        if (valueSocketBase(r, it, isType, ctx) && r->base[it->baseClass - 1] > note) note = r->base[it->baseClass - 1];
        break;
    }
    note = valueCap(note);

    unsigned long long gold = q >= 0 ? (unsigned long long) it->cost * r->goldFactor[q] / 16 : 0;
    for (int i = 0; i < 4; i++)
        if (r->goldAt[i] && gold >= r->goldAt[i] && i + 1 > note) note = i + 1;
    return note;
}
