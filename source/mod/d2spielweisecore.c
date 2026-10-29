















#define SW_VERSION 1
#define SW_REGULATORS 13
#define SW_QUALITY 1
#define SW_NODROP 2
#define SW_RUNES 3
#define SW_GAMBLE 4
#define SW_EXP 5
#define SW_DENSITY 6
#define SW_LADDER 7
#define SW_BOSS 9
#define SW_UNIQUES 10


static const unsigned char swMaxStage[SW_REGULATORS] = {0, 2, 3, 3, 2, 2, 4, 1, 0, 2, 1, 0, 0};


static int swParse(const char* text, int len, unsigned char stage[SW_REGULATORS]) {
    for (int i = 0; i < SW_REGULATORS; i++) stage[i] = 0;
    int version = -1, at = 0;
    unsigned char got[SW_REGULATORS] = {0};
    while (at < len) {
        int end = at;
        while (end < len && text[end] != '\n') end++;
        int e = end;
        if (e > at && text[e - 1] == '\r') e--;
        if (e - at > 8 && !memcmp(text + at, "version=", 8)) {
            int v = 0, i = at + 8;
            for (; i < e && text[i] >= '0' && text[i] <= '9' && v < 1000; i++) v = v * 10 + text[i] - '0';
            if (i == e) version = v;
        }
        else if (e > at && text[at] >= '0' && text[at] <= '9') {
            int n = 0, i = at;
            for (; i < e && text[i] >= '0' && text[i] <= '9' && n < 100; i++) n = n * 10 + text[i] - '0';
            if (i < e && text[i] == '=' && i + 2 == e && text[i + 1] >= '0' && text[i + 1] <= '9'
                && n > 0 && n < SW_REGULATORS) {
                got[n] = (unsigned char) (text[i + 1] - '0' + 1);
            }
        }
        at = end + 1;
    }
    if (version != SW_VERSION) return -1;
    for (int n = 1; n < SW_REGULATORS; n++) {
        if (got[n] && got[n] - 1 <= swMaxStage[n]) stage[n] = (unsigned char) (got[n] - 1);
    }
    return version;
}






#define SW_RATIO_RECORDS 6
#define SW_RATIO_SIZE 0x44
#define SW_RATIO_FIELDS 9
static const unsigned int swRatioOrig[SW_RATIO_RECORDS][SW_RATIO_FIELDS] = {
    {400, 2, 6400, 160, 3, 3200, 125, 6, 5600},
    {240, 2, 6400, 96, 3, 3200, 96, 6, 5600},
    {400, 1, 6400, 100, 2, 3200, 160, 2, 5600},
    {400, 1, 6400, 100, 2, 3200, 160, 2, 5600},
    {240, 3, 6400, 80, 3, 3200, 120, 3, 5600},
    {240, 3, 6400, 80, 3, 3200, 120, 3, 5600},
};

static const unsigned int swRatioStrong[SW_RATIO_RECORDS][SW_RATIO_FIELDS] = {
    {180, 3, 2400, 160, 3, 1200, 100, 3, 600},
    {180, 3, 2400, 96, 3, 1200, 100, 3, 600},
    {180, 3, 2400, 90, 3, 1200, 100, 3, 600},
    {180, 3, 2400, 90, 3, 1200, 100, 3, 600},
    {180, 3, 2400, 80, 3, 1200, 100, 3, 600},
    {180, 3, 2400, 80, 3, 1200, 100, 3, 600},
};

static const unsigned char swRatioKind[SW_RATIO_RECORDS][3] = {{0, 0, 0}, {0, 1, 0}, {1, 0, 0}, {1, 1, 0}, {1, 0, 1}, {1, 1, 1}};


static unsigned int swRatioValue(int record, int field, int stage) {
    unsigned int o = swRatioOrig[record][field], s = swRatioStrong[record][field];
    return stage <= 0 ? o : stage == 1 ? (o + s) / 2 : s;
}


#define SW_DIFF_RECORDS 3
#define SW_DIFF_SIZE 0x58
#define SW_DIFF_GAMBLE 0x44
static const unsigned int swGamble[3][5] = {
    {10000, 100, 50, 90, 33},
    {13333, 3000, 2000, 900, 400},
    {35000, 15000, 10000, 900, 400},
};
static const int swDiffResist[SW_DIFF_RECORDS] = {0, -40, -100};


#define SW_EXP_SIZE 0x20
#define SW_EXP_RATIO 0x1C
#define SW_EXP_FIRST 70
#define SW_EXP_LEVELS 29
static const unsigned int swExpOrig[SW_EXP_LEVELS] = {976, 928, 880, 832, 784, 736, 688, 640, 592, 544, 496, 448, 400,
    352, 304, 256, 192, 144, 108, 81, 61, 46, 35, 26, 20, 15, 11, 8, 6};


static unsigned int swExpValue(int i, int stage) {
    return stage <= 0 ? swExpOrig[i] : stage == 1 ? (1024 + swExpOrig[i]) / 2 : 1024;
}


#define SW_UNIQUE_COUNT 402
#define SW_UNIQUE_SIZE 0x14C
#define SW_UNIQUE_NAME 0x02
#define SW_UNIQUE_FLAGS 0x2C
#define SW_FLAG_NOLIMIT 0x2u
#define SW_FLAG_LADDER 0x8u
#define SW_LADDER_ROWS 29

static const unsigned short swLadderRows[SW_LADDER_ROWS] = {290, 292, 298, 300, 301, 302, 309, 310, 311, 313, 314, 315,
    316, 317, 322, 325, 326, 330, 331, 334, 336, 338, 345, 354, 357, 359, 371, 375, 386};

#define SW_UNIQUE_CHECK1 359
#define SW_UNIQUE_NAME1 "Gheed's Fortune"
#define SW_UNIQUE_CHECK2 0
#define SW_UNIQUE_NAME2 "The Gnasher"

static int swIsLadderRow(int row) {
    for (int i = 0; i < SW_LADDER_ROWS; i++) if (swLadderRows[i] == row) return 1;
    return 0;
}




static int swApplyRatio(unsigned char* base, int stage) {
    int changed = 0;
    for (int r = 0; r < SW_RATIO_RECORDS; r++) {
        unsigned char* rec = base + r * SW_RATIO_SIZE;
        unsigned short version;
        memcpy(&version, rec + 0x40, 2);
        if (version != swRatioKind[r][0] || rec[0x42] != swRatioKind[r][1] || rec[0x43] != swRatioKind[r][2]) return -1;
        for (int f = 0; f < SW_RATIO_FIELDS; f++) {
            unsigned int v = ((unsigned int*) rec)[f];
            int known = 0;
            for (int s = 0; s <= 2; s++) known |= v == swRatioValue(r, f, s);
            if (!known) return -1;
            changed |= v != swRatioValue(r, f, stage);
        }
    }
    if (!changed) return 0;
    for (int r = 0; r < SW_RATIO_RECORDS; r++)
        for (int f = 0; f < SW_RATIO_FIELDS; f++) ((unsigned int*) (base + r * SW_RATIO_SIZE))[f] = swRatioValue(r, f, stage);
    return 1;
}

static int swApplyGamble(unsigned char* base, int count, int stage) {
    if (count != SW_DIFF_RECORDS) return -1;
    int changed = 0;
    for (int r = 0; r < SW_DIFF_RECORDS; r++) {
        unsigned char* rec = base + r * SW_DIFF_SIZE;
        int resist;
        memcpy(&resist, rec, 4);
        if (resist != swDiffResist[r]) return -1;
        for (int f = 0; f < 5; f++) {
            unsigned int v = ((unsigned int*) (rec + SW_DIFF_GAMBLE))[f];
            if (v != swGamble[0][f] && v != swGamble[1][f] && v != swGamble[2][f]) return -1;
            changed |= v != swGamble[stage][f];
        }
    }
    if (!changed) return 0;
    for (int r = 0; r < SW_DIFF_RECORDS; r++)
        for (int f = 0; f < 5; f++) ((unsigned int*) (base + r * SW_DIFF_SIZE + SW_DIFF_GAMBLE))[f] = swGamble[stage][f];
    return 1;
}

static int swApplyExp(unsigned char* base, int stage) {

    unsigned int* first = (unsigned int*) base;
    for (int c = 0; c < 7; c++) if (first[c] != 99) return -1;
    if (first[7] != 10) return -1;
    int changed = 0;
    for (int i = 0; i < SW_EXP_LEVELS; i++) {
        unsigned int v = *(unsigned int*) (base + (SW_EXP_FIRST + i + 1) * SW_EXP_SIZE + SW_EXP_RATIO);
        if (v != swExpValue(i, 0) && v != swExpValue(i, 1) && v != swExpValue(i, 2)) return -1;
        changed |= v != swExpValue(i, stage);
    }
    if (!changed) return 0;
    for (int i = 0; i < SW_EXP_LEVELS; i++)
        *(unsigned int*) (base + (SW_EXP_FIRST + i + 1) * SW_EXP_SIZE + SW_EXP_RATIO) = swExpValue(i, stage);
    return 1;
}


static int swUniqueTable(unsigned char* base, int count) {
    if (count != SW_UNIQUE_COUNT) return 0;
    return !strncmp((const char*) base + SW_UNIQUE_CHECK1 * SW_UNIQUE_SIZE + SW_UNIQUE_NAME, SW_UNIQUE_NAME1, 32)
        && !strncmp((const char*) base + SW_UNIQUE_CHECK2 * SW_UNIQUE_SIZE + SW_UNIQUE_NAME, SW_UNIQUE_NAME2, 32);
}


static int swApplyLadder(unsigned char* base, int count, int stage) {
    if (!swUniqueTable(base, count)) return -1;
    int changed = 0;
    for (int r = 0; r < count; r++) {
        unsigned int flags = *(unsigned int*) (base + r * SW_UNIQUE_SIZE + SW_UNIQUE_FLAGS);
        int ladder = (flags & SW_FLAG_LADDER) != 0;
        if (!swIsLadderRow(r)) {
            if (ladder) return -1;
            continue;
        }
        changed |= ladder != !stage;
    }
    if (!changed) return 0;
    for (int i = 0; i < SW_LADDER_ROWS; i++) {
        unsigned int* flags = (unsigned int*) (base + swLadderRows[i] * SW_UNIQUE_SIZE + SW_UNIQUE_FLAGS);
        *flags = stage ? *flags & ~SW_FLAG_LADDER : *flags | SW_FLAG_LADDER;
    }
    return 1;
}


static int swApplyNolimit(unsigned char* base, int count, int stage) {
    if (!swUniqueTable(base, count)) return -1;
    int changed = 0;
    for (int r = 0; r < count; r++) {
        unsigned int flags = *(unsigned int*) (base + r * SW_UNIQUE_SIZE + SW_UNIQUE_FLAGS);
        changed |= ((flags & SW_FLAG_NOLIMIT) != 0) != (stage != 0);
    }
    if (!changed) return 0;
    for (int r = 0; r < count; r++) {
        unsigned int* flags = (unsigned int*) (base + r * SW_UNIQUE_SIZE + SW_UNIQUE_FLAGS);
        *flags = stage ? *flags | SW_FLAG_NOLIMIT : *flags & ~SW_FLAG_NOLIMIT;
    }
    return 1;
}




static int swNoDropFloor(int stage) {
    static const int floor[4] = {1, 2, 3, 4};
    return stage >= 0 && stage <= 3 ? floor[stage] : 1;
}




#define SW_TC_COUNT 1013
#define SW_TC_SIZE 0x2C
#define SW_TC_ITEMS 0x04
#define SW_TC_TOTAL 0x0C
#define SW_TC_PICKS 0x10
#define SW_TC_NODROP 0x14
#define SW_TC_ENTRIES 0x28
#define SW_TC_ENTRY_SIZE 0x1C
#define SW_TC_ENTRY_START 0x04


#define SW_RUNE_TCS 7
#define SW_RUNE_FIRST 196
static const unsigned int swRuneItems[SW_RUNE_TCS] = {3, 3, 3, 3, 3, 3, 2};
static const unsigned int swRuneStart[SW_RUNE_TCS] = {5, 5, 5, 5, 5, 5, 1};
static const unsigned int swRuneDown[SW_RUNE_TCS] = {720, 1066, 1519, 2170, 2941, 3957, 5170};


static unsigned int swRuneWeight(int i, int stage) {
    unsigned int f = 1u << (stage < 0 ? 0 : stage > 3 ? 3 : stage);
    unsigned int w = (swRuneDown[i] + f / 2) / f;
    return w ? w : 1;
}


static int swApplyRunes(unsigned char* base, int count, const unsigned int start[SW_RUNE_TCS], int stage) {
    if (count != SW_TC_COUNT) return -1;
    int changed = 0;
    for (int i = 0; i < SW_RUNE_TCS; i++) {
        unsigned char* rec = base + (SW_RUNE_FIRST + i) * SW_TC_SIZE;
        unsigned int items, total, picks, nodrop;
        memcpy(&items, rec + SW_TC_ITEMS, 4);
        memcpy(&total, rec + SW_TC_TOTAL, 4);
        memcpy(&picks, rec + SW_TC_PICKS, 4);
        memcpy(&nodrop, rec + SW_TC_NODROP, 4);
        if (items != swRuneItems[i] || picks != 1 || nodrop != 0 || start[i] != swRuneStart[i]) return -1;
        int known = 0;
        for (int s = 0; s <= 3; s++) known |= total == swRuneStart[i] + swRuneWeight(i, s);
        if (!known) return -1;
        changed |= total != swRuneStart[i] + swRuneWeight(i, stage);
    }
    if (!changed) return 0;
    for (int i = 0; i < SW_RUNE_TCS; i++) {
        unsigned int total = swRuneStart[i] + swRuneWeight(i, stage);
        memcpy(base + (SW_RUNE_FIRST + i) * SW_TC_SIZE + SW_TC_TOTAL, &total, 4);
    }
    return 1;
}



#define SW_BOSS_TCS 51
#define SW_BOSS_ACT 30
static const unsigned short swBoss[SW_BOSS_TCS][5] = {
    {830, 7, 48, 4, 19}, {831, 7, 48, 4, 19}, {832, 7, 49, 5, 19},
    {833, 7, 22, 2, 19}, {834, 7, 22, 2, 19}, {835, 7, 23, 3, 19},
    {836, 7, 48, 4, 19}, {837, 7, 48, 4, 19}, {838, 7, 50, 5, 19},
    {839, 7, 22, 2, 19}, {840, 7, 22, 2, 19}, {841, 7, 24, 3, 19},
    {848, 7, 65, 4, 15}, {849, 7, 65, 4, 15}, {850, 7, 66, 5, 15},
    {851, 7, 55, 2, 15}, {852, 7, 55, 2, 15}, {853, 7, 56, 3, 15},
    {854, 7, 65, 4, 15}, {855, 7, 65, 4, 15}, {856, 7, 66, 5, 15},
    {857, 7, 55, 2, 15}, {858, 7, 55, 2, 15}, {859, 7, 56, 3, 15},
    {881, 7, 65, 4, 15}, {882, 7, 65, 4, 15}, {883, 7, 66, 5, 15},
    {884, 7, 55, 2, 15}, {885, 7, 55, 2, 15}, {886, 7, 56, 3, 15},
    {887, 5, 41, 4, 19}, {888, 5, 41, 4, 19}, {889, 5, 41, 4, 19},
    {842, 5, 48, 4, 19}, {843, 5, 48, 4, 19}, {844, 5, 48, 4, 19},
    {860, 5, 36, 5, 19}, {861, 5, 36, 5, 19}, {862, 5, 37, 6, 19},
    {863, 3, 36, 5, 19}, {864, 3, 36, 5, 19}, {865, 3, 36, 5, 19},
    {893, 5, 41, 4, 19}, {894, 5, 41, 4, 19}, {895, 5, 41, 4, 19},
    {875, 5, 41, 4, 19}, {876, 5, 41, 4, 19}, {877, 5, 41, 4, 19},
    {878, 5, 41, 4, 19}, {879, 5, 41, 4, 19}, {880, 5, 42, 5, 19},
};


static unsigned int swBossNoDrop(int i, int stage) {
    return stage >= (i < SW_BOSS_ACT ? 1 : 2) ? 0 : swBoss[i][4];
}

static int swApplyBoss(unsigned char* base, int count, int stage) {
    if (count != SW_TC_COUNT) return -1;
    int changed = 0;
    for (int i = 0; i < SW_BOSS_TCS; i++) {
        unsigned char* rec = base + swBoss[i][0] * SW_TC_SIZE;
        unsigned int items, total, picks, nodrop;
        memcpy(&items, rec + SW_TC_ITEMS, 4);
        memcpy(&total, rec + SW_TC_TOTAL, 4);
        memcpy(&picks, rec + SW_TC_PICKS, 4);
        memcpy(&nodrop, rec + SW_TC_NODROP, 4);
        if (picks != swBoss[i][1] || total != swBoss[i][2] || items != swBoss[i][3]) return -1;
        if (nodrop != swBoss[i][4] && nodrop != 0) return -1;
        changed |= nodrop != swBossNoDrop(i, stage);
    }
    if (!changed) return 0;
    for (int i = 0; i < SW_BOSS_TCS; i++) {
        unsigned int nodrop = swBossNoDrop(i, stage);
        memcpy(base + swBoss[i][0] * SW_TC_SIZE + SW_TC_NODROP, &nodrop, 4);
    }
    return 1;
}







#define SW_LEVELS 137
#define SW_REGION_MONDEN 0x2B8
#define SW_REGION_UMIN 0x2BC
#define SW_REGION_UMAX 0x2BD
#define SW_REGION_SIZE 0x2E4

static const unsigned short swLevelDensity[SW_LEVELS][9] = {
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {520, 520, 520, 0, 4, 7, 0, 5, 9},
    {520, 520, 520, 1, 4, 7, 1, 5, 9},
    {520, 520, 520, 1, 4, 7, 2, 6, 9},
    {520, 520, 520, 1, 4, 7, 2, 6, 9},
    {520, 520, 520, 1, 4, 7, 2, 6, 9},
    {520, 520, 520, 1, 4, 7, 2, 6, 9},
    {600, 600, 600, 0, 0, 0, 0, 0, 0},
    {600, 600, 600, 1, 4, 6, 2, 6, 8},
    {600, 600, 600, 1, 4, 6, 2, 6, 8},
    {600, 600, 600, 1, 4, 6, 2, 6, 8},
    {600, 600, 600, 1, 4, 6, 2, 6, 8},
    {600, 600, 600, 1, 2, 2, 2, 3, 3},
    {600, 600, 600, 1, 2, 2, 2, 3, 3},
    {600, 600, 600, 1, 2, 2, 2, 3, 3},
    {600, 600, 600, 1, 2, 2, 2, 3, 3},
    {520, 520, 520, 0, 0, 0, 0, 0, 0},
    {1056, 1056, 1056, 1, 3, 4, 1, 4, 5},
    {1056, 1056, 1056, 1, 3, 4, 1, 4, 5},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {1024, 1024, 1024, 1, 2, 3, 1, 3, 4},
    {1024, 1024, 1024, 1, 2, 3, 1, 3, 4},
    {1024, 1024, 1024, 1, 2, 3, 1, 3, 4},
    {1024, 1024, 1024, 1, 2, 3, 1, 3, 4},
    {1024, 1024, 1024, 1, 2, 3, 1, 3, 4},
    {600, 600, 600, 0, 0, 1, 1, 1, 2},
    {600, 600, 600, 0, 1, 2, 1, 2, 3},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {720, 720, 720, 0, 1, 2, 1, 2, 2},
    {680, 680, 680, 1, 2, 3, 2, 3, 3},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {680, 680, 680, 1, 4, 6, 2, 5, 8},
    {520, 520, 520, 1, 3, 5, 2, 4, 6},
    {800, 800, 800, 0, 3, 6, 1, 4, 8},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {880, 880, 880, 1, 4, 7, 2, 6, 9},
    {880, 880, 880, 1, 4, 7, 2, 6, 9},
    {880, 880, 880, 1, 4, 7, 2, 6, 9},
    {880, 880, 880, 1, 4, 7, 2, 6, 9},
    {880, 880, 880, 1, 4, 7, 2, 6, 9},
    {880, 880, 880, 0, 3, 7, 0, 4, 9},
    {680, 680, 680, 0, 3, 6, 1, 4, 8},
    {680, 680, 680, 0, 3, 6, 1, 4, 8},
    {680, 680, 680, 0, 3, 6, 1, 4, 8},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {2200, 2200, 2200, 0, 0, 0, 0, 0, 0},
    {2200, 2200, 2200, 0, 3, 5, 1, 4, 7},
    {2200, 2200, 2200, 0, 3, 5, 1, 4, 7},
    {2200, 2200, 2200, 0, 3, 5, 1, 4, 7},
    {2200, 2200, 2200, 0, 3, 6, 1, 4, 8},
    {2200, 2200, 2200, 0, 3, 6, 1, 4, 8},
    {2200, 2200, 2200, 0, 3, 6, 1, 4, 8},
    {2200, 2200, 2200, 0, 3, 6, 1, 4, 8},
    {2200, 2200, 2200, 0, 0, 1, 1, 1, 2},
    {2200, 2200, 2200, 0, 0, 1, 1, 1, 2},
    {2200, 2200, 2200, 0, 0, 1, 1, 1, 2},
    {990, 990, 990, 0, 3, 5, 2, 4, 6},
    {990, 990, 990, 0, 3, 5, 2, 4, 6},
    {990, 990, 990, 0, 2, 4, 2, 3, 5},
    {680, 680, 680, 0, 3, 6, 2, 4, 8},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 4, 7, 2, 6, 9},
    {2200, 2200, 2200, 0, 0, 0, 2, 2, 2},
    {990, 990, 990, 0, 4, 7, 1, 5, 9},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {650, 650, 650, 1, 4, 10, 2, 6, 15},
    {650, 650, 650, 1, 4, 10, 2, 6, 15},
    {650, 650, 650, 1, 4, 10, 2, 6, 15},
    {325, 325, 325, 1, 4, 7, 2, 6, 9},
    {325, 325, 325, 1, 4, 7, 2, 6, 9},
    {325, 325, 325, 1, 4, 7, 2, 6, 9},
    {325, 325, 325, 1, 4, 7, 2, 6, 9},
    {325, 325, 325, 0, 3, 6, 1, 5, 8},
    {700, 700, 700, 0, 3, 4, 1, 4, 5},
    {700, 700, 700, 0, 3, 4, 1, 4, 5},
    {700, 700, 700, 0, 3, 5, 1, 4, 6},
    {700, 700, 700, 0, 3, 5, 1, 4, 5},
    {700, 700, 700, 0, 3, 5, 1, 4, 6},
    {700, 700, 700, 0, 3, 5, 1, 4, 5},
    {700, 700, 700, 0, 1, 2, 1, 2, 3},
    {700, 700, 700, 0, 1, 2, 1, 2, 3},
    {650, 650, 650, 1, 3, 6, 2, 4, 7},
    {650, 650, 650, 1, 3, 6, 2, 4, 7},
    {990, 990, 990, 1, 1, 2, 2, 2, 2},
    {990, 990, 990, 1, 1, 2, 2, 2, 2},
    {990, 990, 990, 1, 1, 2, 2, 2, 2},
    {990, 990, 990, 1, 1, 2, 2, 2, 2},
    {990, 990, 990, 1, 1, 2, 2, 2, 2},
    {990, 990, 990, 1, 1, 2, 2, 2, 2},
    {800, 800, 800, 1, 3, 5, 2, 4, 7},
    {800, 800, 800, 1, 3, 5, 2, 4, 7},
    {800, 800, 800, 1, 3, 5, 2, 4, 7},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {800, 800, 800, 0, 5, 8, 1, 6, 10},
    {800, 800, 800, 0, 5, 8, 1, 6, 10},
    {800, 800, 800, 0, 5, 8, 1, 6, 10},
    {800, 800, 800, 0, 4, 6, 1, 5, 7},
    {800, 800, 800, 0, 4, 6, 1, 5, 7},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 650, 650, 1, 3, 4, 2, 5, 6},
    {450, 450, 450, 1, 6, 9, 2, 7, 11},
    {350, 350, 350, 1, 6, 9, 2, 7, 11},
    {700, 700, 700, 0, 4, 7, 1, 5, 9},
    {800, 800, 800, 0, 3, 6, 1, 4, 8},
    {700, 700, 700, 0, 4, 7, 1, 5, 9},
    {600, 600, 600, 0, 3, 6, 1, 4, 8},
    {550, 550, 550, 2, 4, 9, 3, 6, 11},
    {650, 650, 650, 0, 4, 7, 1, 5, 9},
    {650, 650, 650, 0, 3, 6, 1, 4, 8},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {700, 700, 700, 0, 2, 6, 0, 3, 7},
    {700, 700, 700, 0, 2, 6, 0, 3, 7},
    {700, 700, 700, 0, 0, 0, 0, 0, 0},
    {800, 800, 800, 0, 1, 3, 1, 2, 4},
    {800, 800, 800, 0, 1, 3, 1, 2, 4},
    {700, 700, 700, 0, 1, 3, 1, 2, 4},
    {700, 700, 700, 0, 2, 6, 0, 3, 8},
    {700, 700, 700, 0, 2, 6, 0, 3, 8},
    {700, 700, 700, 0, 2, 6, 0, 3, 8},
    {700, 700, 700, 0, 1, 4, 0, 2, 5},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {1056, 1056, 1056, 1, 3, 4, 1, 4, 5},
    {880, 880, 880, 0, 3, 7, 0, 4, 9},
    {800, 800, 800, 0, 1, 3, 1, 2, 4},
    {0, 0, 0, 3, 3, 3, 3, 3, 3},
};



#define SW_DENSITY_STAGES 5
static const unsigned char swDensityPercent[SW_DENSITY_STAGES] = {100, 125, 150, 200, 110};
static unsigned int swDensityValue(int level, int field, int stage) {
    unsigned int o = swLevelDensity[level][field];
    if (stage <= 0) return o;
    if (stage >= SW_DENSITY_STAGES) stage = SW_DENSITY_STAGES - 1;
    return (o * swDensityPercent[stage] + 50) / 100;
}


static int swDensityKnown(int level, int field, unsigned int v) {
    for (int s = 0; s < SW_DENSITY_STAGES; s++) if (v == swDensityValue(level, field, s)) return 1;
    return 0;
}



static int swDensityRegion(unsigned char* region, int level, int diff, int stage, int write) {
    unsigned int den, want = swDensityValue(level, diff, stage);
    memcpy(&den, region + SW_REGION_MONDEN, 4);
    unsigned int umin = region[SW_REGION_UMIN], umax = region[SW_REGION_UMAX];
    if (!swDensityKnown(level, diff, den) || !swDensityKnown(level, 3 + diff, umin) || !swDensityKnown(level, 6 + diff, umax))
        return -1;
    int changed = den != want || umin != swDensityValue(level, 3 + diff, stage) || umax != swDensityValue(level, 6 + diff, stage);
    if (!changed || !write) return changed;
    memcpy(region + SW_REGION_MONDEN, &want, 4);
    region[SW_REGION_UMIN] = (unsigned char) swDensityValue(level, 3 + diff, stage);
    region[SW_REGION_UMAX] = (unsigned char) swDensityValue(level, 6 + diff, stage);
    return 1;
}
