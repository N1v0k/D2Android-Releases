
#define DIFF_STATS 364
#define DS_W_IS 360
#define DS_W_TWO 361
#define DS_W_STRB 362
#define DS_W_DEXB 363
enum {
    DS_ENERGY = 1, DS_VIT = 3, DS_LIFE = 7, DS_MANA = 9, DS_LIFE_PCT = 76, DS_MANA_PCT = 77, DS_FHR = 99, DS_FBR = 102,
    DS_FCR = 105, DS_LIFE_PER_LEVEL = 216, DS_MANA_PER_LEVEL = 217, DS_STR = 0, DS_DEX = 2, DS_MIN = 21, DS_MAX = 22,
    DS_MIN2 = 23, DS_MAX2 = 24, DS_DEMON = 121, DS_UNDEAD = 122
};
static const int diffElemMin[4] = {48, 50, 52, 54}, diffElemMax[4] = {49, 51, 53, 55};
static const int diffRes[4] = {39, 43, 41, 45}, diffResMax[4] = {40, 44, 42, 46};
static const int diffPenalty[3] = {0, 40, 100};
#define DIFF_LINES 10
#define DIFF_LINE_CHARS 112

typedef struct { int fpd, speed; } DiffAnim;

typedef struct {
    int level, difficulty;
    long long baseLife, baseMana;
    int lifePerVit, manaPerEnergy;
    int stats[DIFF_STATS];
    DiffAnim anim[3];
} DiffFigure;

typedef struct {
    int damage;
    int dmgBefore[2], dmgAfter[2], elemBefore[2], elemAfter[2];
    int undeadBefore, undeadAfter, demonBefore, demonAfter;
    int lifeBefore, lifeAfter, manaBefore, manaAfter;
    int resBefore[4], resAfter[4];
    int framesBefore[3], framesAfter[3];
} DiffResult;

static int diffStat(const DiffFigure* f, const int* item, int id) {
    return f->stats[id] + (item ? item[id] : 0);
}

static int diffMax(const DiffFigure* f, const int* item, int life) {
    long long base = life ? f->baseLife : f->baseMana;
    long long flat = diffStat(f, item, life ? DS_LIFE : DS_MANA);
    long long attr = (long long) diffStat(f, item, life ? DS_VIT : DS_ENERGY) * (life ? f->lifePerVit : f->manaPerEnergy) * 64;
    long long perLevel = (long long) diffStat(f, item, life ? DS_LIFE_PER_LEVEL : DS_MANA_PER_LEVEL) * f->level / 8;
    long long pct = base * diffStat(f, item, life ? DS_LIFE_PCT : DS_MANA_PCT) / 100;
    return (int) ((base + flat + attr + perLevel + pct) >> 8);
}

static int diffResistance(int value, int max, int difficulty) {
    int cap = 75 + max < 95 ? 75 + max : 95, v = value - diffPenalty[difficulty];
    return v < -100 ? -100 : v > cap ? cap : v;
}

static int diffFrames(int art, DiffAnim a, int x) {
    int e = x > 0 ? 120 * x / (120 + x) : 0;
    if (art == 0 && e > 75) e = 75;
    int step = a.speed * ((art == 0 ? 100 : 50) + e) / 100;
    return step > 0 ? (256 * a.fpd + step - 1) / step - 1 : -1;
}

static void diffDamage(const DiffFigure* f, const int* w, int* out) {
    int two = w[DS_W_TWO] != 0;
    int pct = 100 + diffStat(f, w, DS_STR) * w[DS_W_STRB] / 100 + diffStat(f, w, DS_DEX) * w[DS_W_DEXB] / 100;
    out[0] = (int) ((long long) diffStat(f, w, two ? DS_MIN2 : DS_MIN) * pct / 100);
    out[1] = (int) ((long long) diffStat(f, w, two ? DS_MAX2 : DS_MAX) * pct / 100);
}

static void diffElements(const DiffFigure* f, const int* w, int* out) {
    out[0] = out[1] = 0;
    for (int i = 0; i < 4; i++) {
        out[0] += diffStat(f, w, diffElemMin[i]);
        out[1] += diffStat(f, w, diffElemMax[i]);
    }
}

static void diffCompute(const DiffFigure* f, const int* worn, const int* item, DiffResult* r) {
    r->damage = worn && item && worn[DS_W_IS] && item[DS_W_IS];
    if (r->damage) {
        diffDamage(f, worn, r->dmgBefore);
        diffDamage(f, item, r->dmgAfter);
        diffElements(f, worn, r->elemBefore);
        diffElements(f, item, r->elemAfter);
        r->undeadBefore = diffStat(f, worn, DS_UNDEAD);
        r->undeadAfter = diffStat(f, item, DS_UNDEAD);
        r->demonBefore = diffStat(f, worn, DS_DEMON);
        r->demonAfter = diffStat(f, item, DS_DEMON);
    }
    static const int ids[3] = {DS_FCR, DS_FHR, DS_FBR};
    r->lifeBefore = diffMax(f, worn, 1);
    r->lifeAfter = diffMax(f, item, 1);
    r->manaBefore = diffMax(f, worn, 0);
    r->manaAfter = diffMax(f, item, 0);
    for (int i = 0; i < 4; i++) {
        r->resBefore[i] = diffResistance(diffStat(f, worn, diffRes[i]), diffStat(f, worn, diffResMax[i]), f->difficulty);
        r->resAfter[i] = diffResistance(diffStat(f, item, diffRes[i]), diffStat(f, item, diffResMax[i]), f->difficulty);
    }
    for (int k = 0; k < 3; k++) {
        r->framesBefore[k] = f->anim[k].fpd > 0 ? diffFrames(k, f->anim[k], diffStat(f, worn, ids[k])) : -1;
        r->framesAfter[k] = f->anim[k].fpd > 0 ? diffFrames(k, f->anim[k], diffStat(f, item, ids[k])) : -1;
    }
}

static void diffPut(wchar_t* line, const wchar_t* text) {
    int at = 0;
    while (at < DIFF_LINE_CHARS - 1 && line[at]) at++;
    for (int i = 0; text[i] && at < DIFF_LINE_CHARS - 1; i++) line[at++] = text[i];
    line[at] = 0;
}

static void diffNumber(wchar_t* line, int v, int plus) {
    wchar_t buf[16];
    int at = 15;
    unsigned u = v < 0 ? 0u - (unsigned) v : (unsigned) v;
    buf[at] = 0;
    do buf[--at] = (wchar_t) (L'0' + u % 10); while ((u /= 10) && at > 1);
    if (v < 0) buf[--at] = L'-';
    else if (plus && v > 0) buf[--at] = L'+';
    diffPut(line, buf + at);
}

static void diffPart(wchar_t* line, int delta, const wchar_t* name, int showAfter, int after) {
    if (line[0]) diffPut(line, L" | ");
    diffPut(line, delta > 0 ? L"\x00FF" L"c2" : L"\x00FF" L"c1");
    diffPut(line, name);
    diffPut(line, L" ");
    diffNumber(line, delta, 1);
    if (showAfter) {
        diffPut(line, L" (");
        diffNumber(line, after, 0);
        diffPut(line, L")");
    }
}

static int diffLines(const DiffResult* r, int german, wchar_t lines[DIFF_LINES][DIFF_LINE_CHARS]) {
    static const wchar_t* const resDe[4] = {L"Feuer", L"K\x00E4lte", L"Blitz", L"Gift"};
    static const wchar_t* const resEn[4] = {L"Fire", L"Cold", L"Lightning", L"Poison"};
    static const wchar_t* const bpDe[3] = {L"Zaubertempo", L"Trefferholung", L"Blocktempo"};
    static const wchar_t* const bpEn[3] = {L"Cast rate", L"Hit recovery", L"Block rate"};
    int n = 0;
    lines[n][0] = 0;
    if (r->damage) {
        const int* pairs[2][2] = {{r->dmgBefore, r->dmgAfter}, {r->elemBefore, r->elemAfter}};
        static const wchar_t* const nameDe[2] = {L"Schaden", L"Element"}, *const nameEn[2] = {L"Damage", L"Element"};
        for (int k = 0; k < 2; k++) {
            const int* b = pairs[k][0], *a = pairs[k][1];
            if (b[0] == a[0] && b[1] == a[1]) continue;
            wchar_t* line = lines[n];
            diffPut(line, a[0] + a[1] > b[0] + b[1] ? L"\x00FF" L"c2" : L"\x00FF" L"c1");
            diffPut(line, german ? nameDe[k] : nameEn[k]);
            diffPut(line, L" ");
            diffNumber(line, b[0], 0);
            diffPut(line, L"-");
            diffNumber(line, b[1], 0);
            diffPut(line, L" -> ");
            diffNumber(line, a[0], 0);
            diffPut(line, L"-");
            diffNumber(line, a[1], 0);
            lines[++n][0] = 0;
        }
        int bonus[2][2] = {{r->undeadBefore, r->undeadAfter}, {r->demonBefore, r->demonAfter}};
        static const wchar_t* const vsDe[2] = {L"an Untoten", L"an D\x00E4monen"}, *const vsEn[2] = {L"vs undead", L"vs demons"};
        for (int k = 0; k < 2; k++) {
            int d = bonus[k][1] - bonus[k][0];
            if (!d) continue;
            wchar_t* line = lines[n];
            if (line[0]) diffPut(line, L" | ");
            diffPut(line, d > 0 ? L"\x00FF" L"c2" : L"\x00FF" L"c1");
            diffPut(line, german ? vsDe[k] : vsEn[k]);
            diffPut(line, L" ");
            diffNumber(line, d, 1);
            diffPut(line, L"% (");
            diffNumber(line, bonus[k][1], 0);
            diffPut(line, L"%)");
        }
        if (lines[n][0]) lines[++n][0] = 0;
    }
    int life = r->lifeAfter - r->lifeBefore, mana = r->manaAfter - r->manaBefore;
    if (life) diffPart(lines[n], life, german ? L"Leben" : L"Life", 0, 0);
    if (mana) diffPart(lines[n], mana, L"Mana", 0, 0);
    if (lines[n][0]) lines[++n][0] = 0;
    for (int i = 0; i < 4; i++) {
        int d = r->resAfter[i] - r->resBefore[i];
        if (d) diffPart(lines[n], d, german ? resDe[i] : resEn[i], 1, r->resAfter[i]);
    }
    if (lines[n][0]) lines[++n][0] = 0;
    for (int k = 0; k < 3 && n < DIFF_LINES; k++) {
        int before = r->framesBefore[k], after = r->framesAfter[k];
        if (before < 0 || after < 0 || before == after) continue;
        int gain = after < before;
        wchar_t* line = lines[n];
        diffPut(line, gain ? L"\x00FF" L"c2" : L"\x00FF" L"c1");
        diffPut(line, german ? bpDe[k] : bpEn[k]);
        diffPut(line, L": ");
        diffPut(line, german ? (gain ? L"Stufe erreicht" : L"Stufe verloren") : (gain ? L"breakpoint reached" : L"breakpoint lost"));
        diffPut(line, L" (");
        diffNumber(line, before, 0);
        diffPut(line, L" -> ");
        diffNumber(line, after, 0);
        diffPut(line, L")");
        if (++n < DIFF_LINES) lines[n][0] = 0;
    }
    return n;
}
