


















#include "d2companions.h"

#define DMG_RESULT_CRIT 0x2000
#define DMG_RESULT_KILL 0x0002
enum { DMG_HIDE = 0, DMG_OWN, DMG_CRIT, DMG_ALLY };
enum { DMG_EL_PHYS = 0, DMG_EL_FIRE, DMG_EL_COLD, DMG_EL_LIGHT, DMG_EL_MAGIC, DMG_EL_POISON  };
enum { DMG_STYLE_SIMPLE = 1, DMG_STYLE_ELEMENT = 2 };


#define DMG_COLOR_WHITE 0
#define DMG_COLOR_RED 1
#define DMG_COLOR_GREEN 2
#define DMG_COLOR_BLUE 3
#define DMG_COLOR_GOLD 4
#define DMG_COLOR_GREY 5
#define DMG_COLOR_ORANGE 8
#define DMG_COLOR_YELLOW 9



static int damageKind(unsigned long attType, unsigned long attId, unsigned long attTxt, unsigned long me, unsigned long result) {
    if (attType == 0 && attId == me) return (result & DMG_RESULT_CRIT) ? DMG_CRIT : DMG_OWN;
    if (attType == 1 && (isMercenary(attTxt) || isSummon(attTxt))) return DMG_ALLY;
    return DMG_HIDE;
}









static int damageAllyRowLeftOut(unsigned long txt) {
    static const unsigned short rows[] = {516, 517, 518, 519, 522, 523, 543, 545};
    for (unsigned i = 0; i < sizeof(rows) / sizeof(rows[0]); i++) if (txt == rows[i]) return 1;
    return 0;
}
static int damageKindAligned(unsigned long attType, unsigned long attId, unsigned long attTxt, unsigned long me,
                             unsigned long result, unsigned long align, int npcRow) {
    if (attType == 1 && align == 2 && npcRow == 0 && !damageAllyRowLeftOut(attTxt)) return DMG_ALLY;
    return damageKind(attType, attId, attTxt, me, result);
}



static int damageElement(long phys, long fire, long cold, long light, long magic) {
    const long part[] = {phys, fire, cold, light, magic};
    int best = DMG_EL_PHYS;
    long most = 0;
    for (int k = 0; k < 5; k++) if (part[k] > most) {
        most = part[k];
        best = k;
    }
    return best;
}


static int damageColor(int kind, int element, int style) {
    static const int byElement[] = {DMG_COLOR_WHITE, DMG_COLOR_RED, DMG_COLOR_BLUE, DMG_COLOR_YELLOW, DMG_COLOR_ORANGE};
    if (kind == DMG_ALLY) return DMG_COLOR_GREY;
    if (style == DMG_STYLE_ELEMENT) return element >= 0 && element < 5 ? byElement[element] : DMG_COLOR_WHITE;
    return kind == DMG_CRIT ? DMG_COLOR_GOLD : DMG_COLOR_WHITE;
}


static int damageLarge(int kind, int style) {
    return kind == DMG_CRIT && style == DMG_STYLE_ELEMENT;
}



#define DMG_DOT_MS 750
#define DMG_DOT_QUIET_MS 250




static int damageDotColor(int kind, int element, int style) {
    static const int byElement[] = {DMG_COLOR_WHITE, DMG_COLOR_RED, DMG_COLOR_BLUE, DMG_COLOR_YELLOW, DMG_COLOR_ORANGE, DMG_COLOR_GREEN};
    (void) style;
    if (kind == DMG_ALLY) return DMG_COLOR_GREY;
    return element >= 0 && element <= DMG_EL_POISON ? byElement[element] : DMG_COLOR_WHITE;
}





typedef struct {
    unsigned long id, first, last;
    long sum;
    int kind, element, dead, used;
} DmgDot;




static int damageDotAdd(DmgDot* acc, int n, unsigned long id, long loss, int kind, int element, int dead, unsigned long now) {
    int slot = -1, freeSlot = -1, oldest = 0;
    for (int k = 0; k < n; k++) {
        if (acc[k].used && acc[k].id == id && acc[k].kind == kind && acc[k].element == element) {
            slot = k;
            break;
        }
        if (!acc[k].used && freeSlot < 0) freeSlot = k;
        if (acc[k].used && (long) (acc[k].last - acc[oldest].last) < 0) oldest = k;
    }
    if (slot < 0) {
        slot = freeSlot >= 0 ? freeSlot : oldest;
        acc[slot].used = 1;
        acc[slot].id = id;
        acc[slot].sum = 0;
        acc[slot].first = now;
        acc[slot].dead = 0;
    }
    if (loss > 0) acc[slot].sum += loss;
    acc[slot].last = now;
    acc[slot].kind = kind;
    acc[slot].element = element;
    acc[slot].dead |= dead;
    return slot;
}





static unsigned long damageDotDue(DmgDot* p, unsigned long now) {
    if (!p->used) return 0;
    if (p->dead || now - p->last >= DMG_DOT_QUIET_MS) {
        unsigned long rest = p->sum >= 128 ? (unsigned long) (p->sum + 128) >> 8 : 0;
        p->used = 0;
        p->sum = 0;
        p->dead = 0;
        return rest;
    }
    if (now - p->first < DMG_DOT_MS || p->sum < 128) return 0;
    long whole = (p->sum + 128) >> 8;
    p->sum -= whole << 8;
    p->first = now;
    return (unsigned long) whole;
}




typedef struct { unsigned long id; int kind; } DmgDotOwner;

static void damageDotOwnerNote(DmgDotOwner* t, int n, unsigned long* next, unsigned long id, int kind) {
    for (int k = 0; k < n; k++) if (t[k].kind != DMG_HIDE && t[k].id == id) {
        t[k].kind = kind;
        return;
    }
    if (kind == DMG_HIDE) return;
    t[*next % n].id = id;
    t[*next % n].kind = kind;
    ++*next;
}


static int damageDotKind(unsigned long ownerType, unsigned long ownerId, unsigned long ownerTxt, unsigned long me,
                            const DmgDotOwner* t, int n) {
    int kind = damageKind(ownerType, ownerId, ownerTxt, me, 0);
    if (kind != DMG_HIDE || ownerType != 1) return kind;
    for (int k = 0; k < n; k++) if (t[k].kind != DMG_HIDE && t[k].id == ownerId) return t[k].kind == DMG_ALLY ? DMG_ALLY : DMG_HIDE;
    return DMG_HIDE;
}






#define DMG_STREAM_MS 120

typedef struct { unsigned long id, at; int kind, element, used; } DmgStream;



static int damageStream(DmgStream* t, int n, unsigned long id, int kind, int element, unsigned long now) {
    int slot = -1, oldest = 0;
    if (kind == DMG_CRIT) return 0;
    for (int k = 0; k < n; k++) {
        if (t[k].used && t[k].id == id && t[k].kind == kind && t[k].element == element) {
            slot = k;
            break;
        }
        if (!t[k].used || (t[oldest].used && (long) (t[k].at - t[oldest].at) < 0)) oldest = k;
    }
    if (slot < 0) {
        t[oldest].used = 1;
        t[oldest].id = id;
        t[oldest].kind = kind;
        t[oldest].element = element;
        t[oldest].at = now;
        return 0;
    }
    int series = now - t[slot].at < DMG_STREAM_MS;
    t[slot].at = now;
    return series;
}



static void damageDotKill(DmgDot* acc, int n, unsigned long id) {
    for (int k = 0; k < n; k++) if (acc[k].used && acc[k].id == id) acc[k].dead = 1;
}
