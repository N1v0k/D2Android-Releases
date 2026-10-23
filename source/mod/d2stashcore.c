















#define STASH_KINDS 68
#define STASH_MAX 65535u
#define STASH_HEADER "D2Android-Materialien 1\n"

static const char stashCodes[STASH_KINDS][4] = {
    "r01", "r02", "r03", "r04", "r05", "r06", "r07", "r08", "r09", "r10", "r11",
    "r12", "r13", "r14", "r15", "r16", "r17", "r18", "r19", "r20", "r21", "r22",
    "r23", "r24", "r25", "r26", "r27", "r28", "r29", "r30", "r31", "r32", "r33",
    "gcv", "gfv", "gsv", "gzv", "gpv",
    "gcy", "gfy", "gsy", "gly", "gpy",
    "gcb", "gfb", "gsb", "glb", "gpb",
    "gcg", "gfg", "gsg", "glg", "gpg",
    "gcr", "gfr", "gsr", "glr", "gpr",
    "gcw", "gfw", "gsw", "glw", "gpw",
    "skc", "skf", "sku", "skl", "skz",
};

typedef struct {
    unsigned count[STASH_KINDS];
    unsigned disk[STASH_KINDS];
    unsigned deposits[STASH_KINDS];
} StashState;


static int stashKind(const char* code) {
    if (code[3] != ' ' && code[3] != 0) return -1;
    for (int i = 0; i < STASH_KINDS; i++)
        if (code[0] == stashCodes[i][0] && code[1] == stashCodes[i][1] && code[2] == stashCodes[i][2]) return i;
    return -1;
}






#define STASH_D2S_VERSION 0x60
static int stashD2sHeader(const unsigned char* head, unsigned got, int* hardcore) {
    if (got < 0x28) return 0;
    unsigned magic = head[0] | head[1] << 8 | head[2] << 16 | (unsigned) head[3] << 24;
    unsigned version = head[4] | head[5] << 8 | head[6] << 16 | (unsigned) head[7] << 24;
    if (magic != 0xAA55AA55u) return 0;
    if (version != STASH_D2S_VERSION) return -1;
    *hardcore = (head[0x24] & 4) != 0;
    return 1;
}

static unsigned stashFnv(const char* s, int len) {
    unsigned h = 2166136261u;
    for (int i = 0; i < len; i++) {
        h ^= (unsigned char) s[i];
        h *= 16777619u;
    }
    return h;
}


static int stashFormat(const unsigned* v, char* buf, int size) {
    int n = snprintf(buf, (size_t) size, "%s", STASH_HEADER);
    for (int i = 0; i < STASH_KINDS && n >= 0 && n < size; i++)
        if (v[i]) n += snprintf(buf + n, (size_t) (size - n), "%.3s=%u\n", stashCodes[i], v[i]);
    if (n < 0 || n >= size) return -1;
    unsigned sum = stashFnv(buf, n);
    int m = snprintf(buf + n, (size_t) (size - n), "sum=%08x\n", sum);
    return m < 0 || n + m >= size ? -1 : n + m;
}


static int stashParse(const char* buf, int len, unsigned* v) {
    int hl = (int) strlen(STASH_HEADER);
    if (len < hl || memcmp(buf, STASH_HEADER, (size_t) hl) != 0) return 0;
    unsigned tmp[STASH_KINDS] = {0};
    int at = hl;
    while (at < len) {
        int end = at;
        while (end < len && buf[end] != '\n') end++;
        if (end >= len) return 0;
        int line = end - at;
        if (line == 12 && memcmp(buf + at, "sum=", 4) == 0) {
            if (end + 1 != len) return 0;
            char hex[9];
            memcpy(hex, buf + at + 4, 8);
            hex[8] = 0;
            char* stop;
            unsigned sum = (unsigned) strtoul(hex, &stop, 16);
            if (*stop || sum != stashFnv(buf, at)) return 0;
            memcpy(v, tmp, sizeof(tmp));
            return 1;
        }
        if (line < 5 || buf[at + 3] != '=') return 0;
        char code[4] = {buf[at], buf[at + 1], buf[at + 2], 0};
        int k = stashKind(code);
        if (k < 0 || tmp[k]) return 0;
        unsigned value = 0;
        for (int i = at + 4; i < end; i++) {
            if (buf[i] < '0' || buf[i] > '9' || value > STASH_MAX / 10) return 0;
            value = value * 10 + (unsigned) (buf[i] - '0');
        }
        if (value == 0 || value > STASH_MAX) return 0;
        tmp[k] = value;
        at = end + 1;
    }
    return 0;
}


static void stashLoaded(StashState* s, const unsigned* v) {
    for (int i = 0; i < STASH_KINDS; i++) {
        s->count[i] = s->disk[i] = v[i];
        s->deposits[i] = 0;
    }
}


__attribute__((unused)) static int stashDeposit(StashState* s, int kind) {
    if (kind < 0 || kind >= STASH_KINDS || s->count[kind] >= STASH_MAX) return 0;
    s->count[kind]++;
    s->deposits[kind]++;
    return 1;
}


__attribute__((unused)) static int stashWithdraw(StashState* s, int kind) {
    if (kind < 0 || kind >= STASH_KINDS || !s->count[kind]) return 0;
    s->count[kind]--;
    return 1;
}


static void stashPreValues(const StashState* s, unsigned* out) {
    for (int i = 0; i < STASH_KINDS; i++) out[i] = s->disk[i] + s->deposits[i];
}


static void stashDiskIsPre(StashState* s, const unsigned* pre) {
    for (int i = 0; i < STASH_KINDS; i++) {
        s->disk[i] = pre[i];
        s->deposits[i] = 0;
    }
}






static int stashHasChanges(const StashState* s, int unsaved) {
    if (unsaved) return 1;
    for (int i = 0; i < STASH_KINDS; i++)
        if (s->deposits[i] || s->count[i] != s->disk[i]) return 1;
    return 0;
}


static void stashDiskIsExact(StashState* s) {
    for (int i = 0; i < STASH_KINDS; i++) {
        s->disk[i] = s->count[i];
        s->deposits[i] = 0;
    }
}
