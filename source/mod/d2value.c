














#define CMD_VALUE      26
#define VALUE_HOLD_MS  3000
#define ITXT_NORMCODE  0x84
#define ITXT_UBERCODE  0x88
#define ITXT_ULTRACODE 0x8C
#define ITXT_COST      0xE0

#include "d2valuerules.c"

static ValueRules valueRules;
static int valueAvailable, valueHave, valueOn;
static DWORD valueUntil;
static wchar_t valueLine[24];


static void checkValueSites(void) {
    BYTE* txt = compareAvailable && fGetItemText ? (BYTE*) fGetItemText(0) : NULL;
    int items = readable(txt, ITXT_COST + 4) && !memcmp(txt + ITXT_CODE, "hax ", 4) && !memcmp(txt + ITXT_NORMCODE, "hax ", 4)
        && !memcmp(txt + ITXT_UBERCODE, "9ha ", 4) && !memcmp(txt + ITXT_ULTRACODE, "7ha ", 4) && RD32(txt, ITXT_COST) == 170;
    valueAvailable = compareAvailable && items && fItemIsType && fGetUnitStat;
    logLine(valueAvailable ? "value sites verified" : "value sites not as expected (tooltip %d, items.txt %d): no value line",
        compareAvailable, items);
}

static void valueCommand(const BYTE* buf, int n) {
    static ValueRules parsed;
    if (!valueParse(buf, n, &parsed)) {
        static DWORD lastBad;
        if (GetTickCount() - lastBad > 10000) logLine("value: command 26 of %d bytes not understood", n);
        lastBad = GetTickCount();
        return;
    }
    if (!valueHave || memcmp(&parsed, &valueRules, sizeof(parsed))) {
        logLine("value: rules (%d runeword bases)", parsed.pairs);
        valueRules = parsed;
    }
    valueHave = 1;
    valueUntil = GetTickCount() + VALUE_HOLD_MS;
}


static int valueCapable(void) {
    return valueAvailable;
}


static int valueWanted(DWORD now) {
    valueOn = valueAvailable && valueHave && (int) (now - valueUntil) < 0;
    return valueOn;
}

static int valueIsType(void* item, int type) {
    return fItemIsType(item, (DWORD) type) != 0;
}


static int itemValue(void* item) {
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    void* data = RDP(item, U_DATA);
    if (!readable(txt, ITXT_COST + 4) || !readable(data, 0x20)) return -1;
    const char* code = itemCode(item);
    ValueItem it;
    int category = itemCategory(item, code, &it.tier);
    it.kind = category == CAT_RUNE ? VK_RUNE : category == CAT_GEM ? VK_GEM : category == CAT_REJUV ? VK_REJUV
        : category == CAT_RING || category == CAT_AMULET || category == CAT_JEWEL || category == CAT_CHARM ? VK_JEWELRY
        : category == CAT_QUEST ? VK_QUEST : VK_OTHER;
    it.quality = (int) RD32(data, IDATA_QUALITY);
    DWORD flags = RD32(data, IDATA_FLAGS);
    it.ethereal = (flags & IFLAG_ETHEREAL) != 0;
    it.runeword = (flags & IFLAG_RUNEWORD) != 0;
    it.sockets = (flags & IFLAG_SOCKETED) ? (int) fGetUnitStat(item, STAT_SOCKETS, 0) : 0;
    it.baseClass = 0;
    if (category == CAT_WEAPON || category == CAT_ARMOR) {
        if (!memcmp(txt + ITXT_CODE, txt + ITXT_NORMCODE, 4)) it.baseClass = 1;
        else if (!memcmp(txt + ITXT_CODE, txt + ITXT_UBERCODE, 4)) it.baseClass = 2;
        else if (!memcmp(txt + ITXT_CODE, txt + ITXT_ULTRACODE, 4)) it.baseClass = 3;
    }
    it.cost = RD32(txt, ITXT_COST);
    int difficulty = questSite == 1 ? RD8(client, RVA_QuestDifficulty) : 0;
    int note = valueNote(&valueRules, &it, difficulty, valueIsType, item);
    static void* lastItem;
    if (item != lastItem) {
        trace("value: item %.3s kind %d tier %d q %d eth %d rw %d sockets %d class %d cost %lu difficulty %d -> %d", code,
            it.kind, it.tier, it.quality, it.ethereal, it.runeword, it.sockets, it.baseClass, it.cost, difficulty, note);
        lastItem = item;
    }
    return note;
}


static int valueLineAdd(void) {
    if (!valueOn || !hoverText || !hoverText[0]) return 0;
    if (RD32(client, RVA_HoverGrid) != 1 && RD32(client, RVA_HoverBody) != 1) return 0;
    void* item = RDP(client, RVA_HoverItem);
    if (!readable(item, 0x18) || RD32(item, U_TYPE) != UNIT_ITEM) return 0;
    int note = itemValue(item);
    if (note < 0) return 0;


    int n = wsprintfW(valueLine, gameGerman() ? L"\x00FF" L"c4Bewertung %d/10\n" : L"\x00FF" L"c4Rating %d/10\n", note);
    int length = lstrlenW(hoverText);
    if (n <= 0 || length + n >= HOVER_CHARS) return 0;
    memmove(hoverText + n, hoverText, (length + 1) * sizeof(wchar_t));
    memcpy(hoverText, valueLine, n * sizeof(wchar_t));
    return n;
}


static void valueLineRemove(int added) {
    if (added <= 0 || memcmp(hoverText, valueLine, added * sizeof(wchar_t))) return;
    memmove(hoverText, hoverText + added, (lstrlenW(hoverText + added) + 1) * sizeof(wchar_t));
}
