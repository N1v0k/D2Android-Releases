







#ifndef D2SELFRULES_H
#define D2SELFRULES_H


#define SC_WHY_MONSTER 1
#define SC_WHY_OBJECT  2
#define SC_WHY_GATE    3
#define SC_WHY_LIMIT   4
#define SC_WHY_ITEM    5
#define SC_WHY_TILE    6


#define SC_FINE        0
#define SC_MISSING     1
#define SC_ON_PURPOSE  2
#define SC_OVER_LIMIT  3
#define SC_EXTRA       4

static const char* scWhyName(int why) {
    switch (why) {
        case SC_WHY_MONSTER: return "monster-rule";
        case SC_WHY_OBJECT: return "object-mode";
        case SC_WHY_GATE: return "gate";
        case SC_WHY_LIMIT: return "limit";
        case SC_WHY_ITEM: return "item-mode";
        case SC_WHY_TILE: return "tile";
        default: return "?";
    }
}

#define SC_OBJECT_ORIFICE 152










static const char* scOnPurpose(unsigned long type, unsigned long txt, unsigned long mode, int why, int fixedList,
                               int statue, int companion) {
    if (why == SC_WHY_GATE) return "closed gate on it";
    if (why == SC_WHY_TILE) return "room tile under it";
    if (type == 1 && statue) return "statue of the Ancients";
    if (type == 1 && companion) return "companion";
    if (type == 1 && fixedList) return "fixed list";
    if (type == 2 && txt == SC_OBJECT_ORIFICE && mode != 0) return "Orifice after the staff";
    return 0;
}


static int scVerdict(int selectable, int reported, int why, const char* onPurpose) {
    if (selectable && !reported) {
        if (onPurpose) return SC_ON_PURPOSE;
        return why == SC_WHY_LIMIT ? SC_OVER_LIMIT : SC_MISSING;
    }
    if (!selectable && reported) return SC_EXTRA;
    return SC_FINE;
}


static unsigned long long scKey(unsigned long type, unsigned long id, unsigned long mode, unsigned long level, int verdict) {
    return (unsigned long long) (type & 0xF) << 60 | (unsigned long long) (verdict & 0xF) << 56
        | (unsigned long long) (mode & 0xFF) << 48 | (unsigned long long) (level & 0xFFFF) << 32 | (id & 0xFFFFFFFFul);
}









#define SC_PENDING 64

typedef struct {
    unsigned long long key[SC_PENDING];
    unsigned long seq[SC_PENDING];
    unsigned char count[SC_PENDING];
    int next;
} ScPending;

static int scSeen(ScPending* t, unsigned long long key, unsigned long seq) {
    for (int i = 0; i < SC_PENDING; i++) {
        if (t->count[i] && t->key[i] == key) {
            if (t->seq[i] == seq) return t->count[i];
            t->count[i] = t->seq[i] + 1 == seq ? (unsigned char) (t->count[i] < 250 ? t->count[i] + 1 : 250) : 1;
            t->seq[i] = seq;
            return t->count[i];
        }
    }
    for (int k = 0; k < SC_PENDING; k++) {
        int i = (t->next + k) % SC_PENDING;
        if (t->count[i] && (t->seq[i] == seq || t->seq[i] + 1 == seq)) continue;
        t->next = (i + 1) % SC_PENDING;
        t->key[i] = key;
        t->seq[i] = seq;
        t->count[i] = 1;
        return 1;
    }
    return 1;
}


#define SC_ONCE 256

typedef struct {
    unsigned long long key[SC_ONCE];
    int n, next;
} ScOnce;


static int scOnce(ScOnce* s, unsigned long long key) {
    for (int i = 0; i < s->n; i++) if (s->key[i] == key) return 0;
    if (s->n < SC_ONCE) s->key[s->n++] = key;
    else {
        s->key[s->next] = key;
        s->next = (s->next + 1) % SC_ONCE;
    }
    return 1;
}


static int scCap(int verdict) {
    switch (verdict) {
        case SC_MISSING: return 50;
        case SC_ON_PURPOSE: return 20;
        case SC_OVER_LIMIT: return 20;
        case SC_EXTRA: return 30;
        default: return 0;
    }
}

#endif
