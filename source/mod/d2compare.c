
#define RVA_DrawHoverCall   0x1D791
#define RVA_DrawHoverThunk  0xD35E
#define RVA_ItemHover       0x97820
#define RVA_HoverGrid       0x11CB18
#define RVA_HoverBody       0x11CB1C
#define RVA_HoverItem       0x11CB28
#define RVA_HoverItem2      0x11CB2C
#define RVA_HoverSlot       0x11CB58
#define RVA_TradeMode       0x11CB24
#define RVA_HoverAnchor     0xEE49C
#define RVA_BodySlots       0x11CB90
#define ORD_DrawHover       10124
#define ORD_SetHover        10137
#define ORD_TextSize        10179
#define ORD_BodyLocItem     10292
#define ORD_ItemIsType      10601

#define ITYPE_SHIELD 2
#define ITYPE_ARMOR 3
#define ITYPE_BOWQUIVER 5
#define ITYPE_XBOWQUIVER 6
#define ITYPE_RING 10
#define ITYPE_AMULET 12
#define ITYPE_BOOTS 15
#define ITYPE_GLOVES 16
#define ITYPE_BELT 19
#define ITYPE_HELM 37
#define ITYPE_WEAPON 45

#define HOVER_CHARS 0x400
#define COMPARE_GAP 4

typedef void (*DrawHover_t)(void);
typedef void (__fastcall *TextSize_t)(const wchar_t*, int*, int*);
typedef void* (__stdcall *BodyLocItem_t)(void*, DWORD);
typedef BOOL (__stdcall *ItemIsType_t)(void*, DWORD);
typedef void (__stdcall *ItemHover_t)(void*, DWORD);

static DrawHover_t fDrawHover;
static TextSize_t fTextSize;
static BodyLocItem_t fBodyLocItem;
static ItemIsType_t fItemIsType;
static wchar_t* hoverText;
static int compareAvailable, compareApplied, compareOn;
static DWORD compareUntil;

static int valueLineAdd(void);
static void valueLineRemove(int added);
static int valueWanted(DWORD now);
static int diffLinesAdd(void);
static void diffLinesRemove(int added);
static int diffWanted(DWORD now);

static void checkCompareSites(void) {
    HMODULE win = GetModuleHandleA("D2Win.dll");
    fDrawHover = win ? (DrawHover_t) GetProcAddress(win, (LPCSTR) ORD_DrawHover) : NULL;
    BYTE* set = win ? (BYTE*) GetProcAddress(win, (LPCSTR) ORD_SetHover) : NULL;
    fTextSize = win ? (TextSize_t) GetProcAddress(win, (LPCSTR) ORD_TextSize) : NULL;
    fBodyLocItem = (BodyLocItem_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_BodyLocItem);
    fItemIsType = (ItemIsType_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_ItemIsType);
    static const BYTE hoverStart[] = {0xB8, 0x50, 0x66, 0x00, 0x00, 0xE8};
    BYTE* thunk = (BYTE*) (client + RVA_DrawHoverThunk);
    DWORD text = 0;
    int win1 = readable(set, 0x64) && set[0x19] == 0xB9 && set[0x43] == 0x89 && set[0x44] == 0x1D && set[0x49] == 0xA3
        && set[0x4E] == 0x89 && set[0x4F] == 0x0D && set[0x54] == 0x89 && set[0x55] == 0x15 && set[0x5A] == 0xC7;
    if (win1) {
        text = *(DWORD*) (set + 0x1A);
        win1 = *(DWORD*) (set + 0x45) == text + 0x800 && *(DWORD*) (set + 0x4A) == text + 0x804
            && *(DWORD*) (set + 0x50) == text + 0x808 && *(DWORD*) (set + 0x56) == text + 0x80C
            && *(DWORD*) (set + 0x5C) == text + 0x810 && readable((void*) text, 0x814);
    }
    int client1 = callAt(client, RVA_DrawHoverCall, RVA_DrawHoverThunk) && readable(thunk, 6) && thunk[0] == 0xFF
        && thunk[1] == 0x25 && readable(*(void**) (thunk + 2), 4) && **(DWORD**) (thunk + 2) == (DWORD) fDrawHover;
    int client2 = bytesAre(client + RVA_ItemHover, hoverStart, sizeof(hoverStart))
        && addressAt(client, 0x97DF8, RVA_HoverBody) && addressAt(client, 0x97E01, RVA_HoverGrid)
        && addressAt(client, 0x97E0E, RVA_HoverItem) && addressAt(client, 0x97FA6, RVA_HoverAnchor)
        && addressAt(client, 0x97F94, RVA_HoverAnchor + 4)
        && addressAt(client, 0x8FFCA, RVA_HoverBody) && addressAt(client, 0x8FFF6, RVA_HoverItem)
        && addressAt(client, 0x8FFFC, RVA_HoverItem2) && addressAt(client, 0x8FFDD, RVA_BodySlots)
        && addressAt(client, 0x90003, RVA_HoverAnchor) && addressAt(client, 0x98AF5, RVA_HoverGrid)
        && addressAt(client, 0x987D1, RVA_TradeMode) && addressAt(client, 0x9882C, RVA_TradeMode);
    compareAvailable = win1 && client1 && client2 && fDrawHover && fTextSize && fBodyLocItem && fItemIsType;
    hoverText = compareAvailable ? (wchar_t*) text : NULL;
    logLine(compareAvailable ? "compare sites verified" : "compare sites not as expected (D2Win %d, call %d, D2Client %d): no comparison",
        win1, client1, client2);
}

static int compareSlots(void* item, void* inventory, void* found[2]) {
    found[0] = found[1] = NULL;
    static const struct { DWORD type, loc; } single[] = {
        {ITYPE_HELM, 1}, {ITYPE_AMULET, 2}, {ITYPE_ARMOR, 3}, {ITYPE_BELT, 8}, {ITYPE_BOOTS, 9}, {ITYPE_GLOVES, 10},
    };
    for (unsigned i = 0; i < sizeof(single) / sizeof(single[0]); i++) {
        if (fItemIsType(item, single[i].type)) {
            found[0] = fBodyLocItem(inventory, single[i].loc);
            return found[0] != NULL;
        }
    }
    if (fItemIsType(item, ITYPE_RING)) {
        found[0] = fBodyLocItem(inventory, 7);
        found[1] = fBodyLocItem(inventory, 6);
        if (!found[0]) {
            found[0] = found[1];
            found[1] = NULL;
        }
        return (found[0] != NULL) + (found[1] != NULL);
    }
    int weapon = fItemIsType(item, ITYPE_WEAPON);
    int offhand = fItemIsType(item, ITYPE_SHIELD) || fItemIsType(item, ITYPE_BOWQUIVER) || fItemIsType(item, ITYPE_XBOWQUIVER);
    if (!weapon && !offhand) return 0;
    for (DWORD loc = 4; loc <= 5; loc++) {
        void* held = fBodyLocItem(inventory, weapon ? loc : 9 - loc);
        if (!held) continue;
        int heldWeapon = fItemIsType(held, ITYPE_WEAPON);
        if (weapon ? heldWeapon : !heldWeapon) {
            found[0] = held;
            return 1;
        }
    }
    return 0;
}

static void hoverBox(const wchar_t* text, long x, long y, DWORD centered, long* box) {
    long sw = (long) RD32(client, RVA_ScreenSizeX), sh = (long) RD32(client, RVA_ScreenSizeY);
    int w = 0, h = 0;
    fTextSize(text, &w, &h);
    long bw = w + 8, left = centered == 1 ? x - bw / 2 : x;
    if (left < 0) left = 0;
    if (left > sw - bw) left = sw - bw;
    long bottom = y + 2;
    if (bottom < h - 5) bottom = h - 5;
    if (bottom > sh - 5) bottom = sh - 5;
    box[0] = left;
    box[1] = bottom - h;
    box[2] = bw;
    box[3] = h;
}

static int gameGerman(void) {
    const wchar_t* t = npcMenuAvailable ? localeText(5382) : NULL;
    return t && t[0] == L'e' && t[1] == L'i' && t[2] == L'n' && t[3] == L'e' && t[4] == L' ';
}

static const wchar_t* equippedHeader(void) {
    return gameGerman() ? L"\x00FF" L"c5Angelegt" : L"\x00FF" L"c5Equipped";
}

static int buildEquipped(void* player, void* equipped, int* size) {
    *(DWORD*) (client + RVA_HoverGrid) = 0;
    *(DWORD*) (client + RVA_HoverBody) = 1;
    *(void**) (client + RVA_HoverItem) = equipped;
    *(void**) (client + RVA_HoverItem2) = equipped;
    hoverText[0] = 0;
    DWORD trade = RD32(client, RVA_TradeMode);
    *(DWORD*) (client + RVA_TradeMode) = 0;
    ((ItemHover_t) (client + RVA_ItemHover))(player, 0);
    *(DWORD*) (client + RVA_TradeMode) = trade;
    if (!hoverText[0]) return 0;
    const wchar_t* header = equippedHeader();
    int length = lstrlenW(hoverText);
    if (length + lstrlenW(header) + 1 < HOVER_CHARS) {
        hoverText[length] = L'\n';
        lstrcpyW(hoverText + length + 1, header);
    }
    size[0] = size[1] = 0;
    fTextSize(hoverText, &size[0], &size[1]);
    size[0] += 8;
    return 1;
}

static void drawAt(long left, long top, const int* size) {
    DWORD* hover = (DWORD*) (hoverText + HOVER_CHARS);
    hover[0] = (DWORD) left;
    hover[1] = (DWORD) (top + size[1] - 2);
    hover[3] = 0;
    fDrawHover();
}

static int drawBeside(const long* first, const int* size, int side) {
    long sw = (long) RD32(client, RVA_ScreenSizeX);
    long left = side < 0 ? first[0] - COMPARE_GAP - size[0] : first[0] + first[2] + COMPARE_GAP;
    if (left < 0 || left + size[0] > sw) return 0;
    drawAt(left, first[1], size);
    return 1;
}

static int drawAboveOrBelow(const long* first, const int* size) {
    long sw = (long) RD32(client, RVA_ScreenSizeX), sh = (long) RD32(client, RVA_ScreenSizeY);
    long left = first[0];
    if (left + size[0] > sw) left = sw - size[0];
    if (left < 0) left = 0;
    long top = first[1] - COMPARE_GAP - size[1];
    if (top < -5) {
        top = first[1] + first[3] + COMPARE_GAP;
        if (top + size[1] > sh - 5) return 0;
    }
    drawAt(left, top, size);
    return 1;
}

static void drawComparison(void) {
    if (RD32(client, RVA_HoverGrid) != 1 || RD32(client, RVA_HoverBody) != 0 || !hoverText[0]) return;
    void* item = RDP(client, RVA_HoverItem);
    void* player = fGetPlayerUnit();
    void* inventory = player ? RDP(player, U_INVENTORY) : NULL;
    if (!readable(item, 0x18) || RD32(item, U_TYPE) != UNIT_ITEM || RD32(item, U_MODE) != 0 || !inventory) return;
    void* equipped[2];
    int count = compareSlots(item, inventory, equipped);
    static void* lastItem;
    if (item != lastItem) {
        BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
        trace("compare: item %.3s type %u, %d equipped", itemCode(item),
            readable(txt, 0x120) ? (unsigned) *(WORD*) (txt + ITXT_TYPE) : 0u, count);
        lastItem = item;
    }
    if (!count) return;
    static DWORD saved[0x814 / 4];
    DWORD vars[5] = {RD32(client, RVA_HoverGrid), RD32(client, RVA_HoverBody), RD32(client, RVA_HoverItem),
        RD32(client, RVA_HoverItem2), RD32(client, RVA_HoverSlot)}, anchor[6];
    memcpy(anchor, (void*) (client + RVA_HoverAnchor), sizeof(anchor));
    memcpy(saved, hoverText, sizeof(saved));
    DWORD* hover = (DWORD*) (hoverText + HOVER_CHARS);
    long first[4];
    hoverBox(hoverText, (long) hover[0], (long) hover[1], hover[3], first);
    int size[2];
    if (buildEquipped(player, equipped[0], size)) {
        if (drawBeside(first, size, -1)) {
            if (count == 2 && buildEquipped(player, equipped[1], size) && !drawBeside(first, size, 1))
                drawAboveOrBelow(first, size);
        }
        else if (!drawBeside(first, size, 1) && !drawAboveOrBelow(first, size)) {
            static void* noRoomFor;
            if (noRoomFor != item) trace("compare: no room for the equipped tooltip (%d x %d beside %ld,%ld %ld x %ld)",
                size[0], size[1], first[0], first[1], first[2], first[3]);
            noRoomFor = item;
        }
    }
    *(DWORD*) (client + RVA_HoverGrid) = vars[0];
    *(DWORD*) (client + RVA_HoverBody) = vars[1];
    *(DWORD*) (client + RVA_HoverItem) = vars[2];
    *(DWORD*) (client + RVA_HoverItem2) = vars[3];
    *(DWORD*) (client + RVA_HoverSlot) = vars[4];
    memcpy((void*) (client + RVA_HoverAnchor), anchor, sizeof(anchor));
    memcpy(hoverText, saved, sizeof(saved));
}

__attribute__((force_align_arg_pointer)) static void __cdecl hoverDrawn(void) {
    int added = valueLineAdd();
    int diffAdded = diffLinesAdd();
    fDrawHover();
    if (compareOn) drawComparison();
    diffLinesRemove(diffAdded);
    valueLineRemove(added);
}

static void applyCompare(int on) {
    BYTE* at = (BYTE*) (client + RVA_DrawHoverCall);
    DWORD target = on ? (DWORD) hoverDrawn : client + RVA_DrawHoverThunk, old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    *(DWORD*) (at + 1) = target - ((DWORD) at + 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    compareApplied = on;
    logLine(on ? "tooltip hook on (comparison %d)" : "tooltip hook off (comparison %d)", compareOn);
}

static void updateCompare(DWORD now) {
    if (!compareAvailable) return;
    compareOn = (int) (now - compareUntil) < 0;
    int want = valueWanted(now) | diffWanted(now) | compareOn;
    if (want != compareApplied) applyCompare(want);
}
