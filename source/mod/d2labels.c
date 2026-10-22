
#define RVA_LabelItemCheck   0x4E60E
#define RVA_LabelItemJump    0x4E611
#define RVA_LabelItemNext    0x4E617
#define RVA_LabelUnitSkip    0x4E9A9
#define CMD_LABELS 12
#define LABEL_NEW 1
#define LABEL_NEAR 2
#define LABEL_BUTTON 4

#include "d2lootrules.c"

static int labelFilterAvailable, labelFilterApplied;
static unsigned lootMask, lootGoldBelow;
static int lootHidden(void* unit);
static int realAltDown;
static DWORD labelFilterUntil, labelBits, labelSeconds = 5, labelRadius = 12;
static BYTE labelJumpOriginal[6];
DWORD labelContinue, labelSkip;

static DWORD dropId[64], dropAt[64], dropNext;

static void checkLabelFilterSite(void) {
    static const BYTE expected[] = {0x83, 0x3E, 0x04, 0x0F, 0x85, 0x92, 0x03, 0x00, 0x00};
    BYTE* at = (BYTE*) (client + RVA_LabelItemCheck);
    labelFilterAvailable = readable(at, sizeof(expected)) && memcmp(at, expected, sizeof(expected)) == 0
        && RVA_LabelItemNext + 0x392 == RVA_LabelUnitSkip;
    if (labelFilterAvailable) memcpy(labelJumpOriginal, at + 3, 6);
    labelContinue = client + RVA_LabelItemNext;
    labelSkip = client + RVA_LabelUnitSkip;
    logLine(labelFilterAvailable ? "label filter site verified" : "label filter site not as expected: all names as before");
}

static void noteItemMode(void* unit, DWORD mode, DWORD now) {
    if (mode != 5) return;
    DWORD id = RD32(unit, U_ID);
    for (int k = 0; k < 64; k++) {
        if (dropId[k] == id + 1) {
            dropAt[k] = now;
            return;
        }
    }
    dropId[dropNext] = id + 1;
    dropAt[dropNext] = now;
    dropNext = (dropNext + 1) % 64;
}

static int isNewLoot(DWORD id, DWORD now) {
    for (int k = 0; k < 64; k++) {
        if (dropId[k] == id + 1) return now - dropAt[k] < labelSeconds * 1000;
    }
    return 0;
}

static int newLootVisible(DWORD now) {
    if (!(labelBits & LABEL_NEW)) return 0;
    for (int k = 0; k < 64; k++) {
        if (!dropId[k] || now - dropAt[k] >= labelSeconds * 1000) continue;
        if (!lootMask) return 1;
        void* unit = findUnit(dropId[k] - 1, UNIT_ITEM);
        if (!unit || !lootHidden(unit)) return 1;
    }
    return 0;
}

__attribute__((force_align_arg_pointer)) int __cdecl wantLabel(void* unit) {
    DWORD now = GetTickCount();
    if (!unit) return 1;
    if (!labelsApplied || realAltDown) return 1;
    DWORD id = RD32(unit, U_ID);
    if (unit == originalSelectedUnit()) return 1;
    if (id == highlightId && highlightType == UNIT_ITEM && (int) (now - highlightUntil) < 0) return 1;
    if (lootMask && lootHidden(unit)) return 0;
    if ((labelBits & LABEL_NEW) && isNewLoot(id, now)) return 1;
    if (!(labelBits & LABEL_BUTTON)) return 0;
    if (!(labelBits & LABEL_NEAR)) return 1;
    void* player = fGetPlayerUnit();
    if (!player) return 1;
    long dx = (long) fGetUnitX(unit) - (long) fGetUnitX(player), dy = (long) fGetUnitY(unit) - (long) fGetUnitY(player);
    return dx * dx + dy * dy <= (long) (labelRadius * labelRadius);
}

void labelFilterTrampoline(void);
__asm__(
    ".text\n"
    ".globl _labelFilterTrampoline\n"
    "_labelFilterTrampoline:\n"
    "    jne 1f\n"
    "    pushal\n"
    "    pushl %esi\n"
    "    call _wantLabel\n"
    "    addl $4, %esp\n"
    "    testl %eax, %eax\n"
    "    popal\n"
    "    je 1f\n"
    "    jmp *_labelContinue\n"
    "1:  jmp *_labelSkip\n"
);

static void applyLabelFilter(int on) {
    BYTE* at = (BYTE*) (client + RVA_LabelItemJump);
    BYTE patch[6];
    if (on) {
        patch[0] = 0xE9;
        *(DWORD*) (patch + 1) = (DWORD) labelFilterTrampoline - ((DWORD) at + 5);
        patch[5] = 0x90;
    }
    else memcpy(patch, labelJumpOriginal, 6);
    DWORD old;
    if (!VirtualProtect(at, 6, PAGE_EXECUTE_READWRITE, &old)) return;
    memcpy(at, patch, 6);
    VirtualProtect(at, 6, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 6);
    labelFilterApplied = on;
    logLine(on ? "label filter on (bits %lu, %lu s, radius %lu)" : "label filter off", (unsigned long) labelBits,
        (unsigned long) labelSeconds, (unsigned long) labelRadius);
}

static void updateLabelFilter(DWORD now) {
    if (!labelFilterAvailable) return;
    int alt = (GetAsyncKeyState(VK_MENU) & 0x8000) != 0;
    if (alt != realAltDown) trace("label filter: real Alt %s", alt ? "down" : "up");
    realAltDown = alt;
    int want = (int) (now - labelFilterUntil) < 0;
    if (want != labelFilterApplied) applyLabelFilter(want);
    if (!want && lootMask) logLine("loot filter: no command 12 any more, mask cleared");
    lootExpire(want, &lootMask, &lootGoldBelow);
}

static void labelFilterCommand(const BYTE* buf, int n) {
    static DWORD lastBits = 0xFF;
    labelBits = buf[8] & 7;
    if (n >= 10 && buf[9]) labelSeconds = buf[9];
    if (n >= 11 && buf[10]) labelRadius = buf[10];
    unsigned mask, gold;
    lootParse(buf, n, &mask, &gold);
    if (mask != lootMask || gold != lootGoldBelow)
        logLine("loot filter: mask 0x%04X, gold below %u", mask, gold);
    lootMask = mask;
    lootGoldBelow = gold;
    if (labelBits != lastBits) trace("label filter bits %lu -> %lu", (unsigned long) lastBits, (unsigned long) labelBits);
    lastBits = labelBits;
    labelFilterUntil = GetTickCount() + 2000;
}
