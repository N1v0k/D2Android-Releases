


























#define REVEAL_LEVELS        64
#define REVEAL_SHARE_PCT     20
#define REVEAL_BUDGET_MIN_US 1000
#define REVEAL_BUDGET_MAX_US 8000
#define REVEAL_FRAME_START_US 40000
#define REVEAL_GAP_MIN_US    2000
#define REVEAL_GAP_MAX_US    250000

typedef struct {
    unsigned frameUs;
    unsigned limitFps;
} RevealPace;


static unsigned revealFpsFromText(const char* text) {
    unsigned fps = 0;
    if (!text || !text[0]) return 0;
    for (const char* c = text; *c; c++) {
        if (*c < '0' || *c > '9' || c - text >= 4) return 0;
        fps = fps * 10 + (unsigned) (*c - '0');
    }
    return fps <= 1000 ? fps : 0;
}

static void revealPaceStart(RevealPace* p, unsigned limitFps) {
    p->limitFps = limitFps;
    p->frameUs = limitFps ? 1000000u / limitFps : REVEAL_FRAME_START_US;
}


static void revealPaceNote(RevealPace* p, unsigned gapUs, unsigned ownUs) {
    if (gapUs < REVEAL_GAP_MIN_US || gapUs > REVEAL_GAP_MAX_US) return;
    unsigned game = gapUs > ownUs ? gapUs - ownUs : 0;
    if (game < REVEAL_GAP_MIN_US) game = REVEAL_GAP_MIN_US;
    p->frameUs = p->frameUs - p->frameUs / 8 + game / 8;
}


static unsigned revealFrameUs(const RevealPace* p) {
    unsigned frame = p->frameUs;
    if (p->limitFps && 1000000u / p->limitFps > frame) frame = 1000000u / p->limitFps;
    return frame;
}


static unsigned revealBudgetUs(const RevealPace* p) {
    unsigned budget = revealFrameUs(p) * REVEAL_SHARE_PCT / 100;
    if (budget < REVEAL_BUDGET_MIN_US) budget = REVEAL_BUDGET_MIN_US;
    if (budget > REVEAL_BUDGET_MAX_US) budget = REVEAL_BUDGET_MAX_US;
    return budget;
}

#define REVEAL_NO_LAYER 0xFFFFFFFFu

typedef struct {
    unsigned first, end;
    unsigned long long done;
    int left;
    unsigned layer[REVEAL_LEVELS];
    int room[REVEAL_LEVELS];
} RevealPlan;


static void revealPlanStart(RevealPlan* r, unsigned first, unsigned end) {
    if (end < first) end = first;
    if (end - first > REVEAL_LEVELS) end = first + REVEAL_LEVELS;
    r->first = first;
    r->end = end;
    r->done = 0;
    r->left = (int) (end - first);
    for (int i = 0; i < REVEAL_LEVELS; i++) {
        r->layer[i] = REVEAL_NO_LAYER;
        r->room[i] = 0;
    }
}

static int revealPlanIsDone(const RevealPlan* r, unsigned level) {
    return level < r->first || level >= r->end || (r->done >> (level - r->first) & 1);
}

static int revealPlanFinished(const RevealPlan* r) {
    return r->left <= 0;
}






static int revealPlanPickable(const RevealPlan* r, unsigned level, unsigned shownLayer) {
    if (revealPlanIsDone(r, level)) return 0;
    unsigned layer = r->layer[level - r->first];
    return layer == REVEAL_NO_LAYER || layer == shownLayer;
}





static unsigned revealPlanLevel(const RevealPlan* r, unsigned playerLevel, unsigned shownLayer) {
    if (r->left <= 0) return 0;
    if (playerLevel >= r->first && playerLevel < r->end && revealPlanPickable(r, playerLevel, shownLayer)) return playerLevel;
    for (unsigned l = r->first; l < r->end; l++)
        if (r->room[l - r->first] > 0 && revealPlanPickable(r, l, shownLayer)) return l;
    for (unsigned l = r->first; l < r->end; l++)
        if (revealPlanPickable(r, l, shownLayer)) return l;
    return 0;
}


static void revealPlanLevelDone(RevealPlan* r, unsigned level) {
    if (revealPlanIsDone(r, level)) return;
    r->done |= 1ULL << (level - r->first);
    r->room[level - r->first] = 0;
    r->left--;
}





static int revealPlanSettled(const RevealPlan* r, unsigned playerLevel, unsigned shownLayer) {
    if (playerLevel >= r->first && playerLevel < r->end && !revealPlanIsDone(r, playerLevel)) return 0;
    return revealPlanLevel(r, playerLevel, shownLayer) == 0;
}
