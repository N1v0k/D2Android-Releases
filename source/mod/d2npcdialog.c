












#define RVA_DialogTop        0x11D584
#define RVA_DialogTopStore   0x15DDA
#define RVA_DialogAddLine    0x154B0
#define RVA_DialogClickCall  0x15922
#define RVA_NpcLists         0x1018CF
#define RVA_NpcListMain      0x1018DB
#define RVA_NpcListTopics    0x1018DF
#define RVA_NpcListMainStore 0x7B4EA
#define RVA_NpcListTopicsStore 0x7C711
#define NPC_LISTS            8
#define DLG_SIZE             0xB10
#define DLG_SELECTED         0x44
#define DLG_LINES            0x50
#define DLG_ESC              0x60
#define DLG_LINE0            0x68
#define DLG_LINE_SIZE        0x110
#define DLG_LINE_KIND        0x104
#define DLG_LINE_FN          0x108
#define NPC_SUB_MAX          9

static int npcDialogAvailable;

static void checkNpcDialogSites(void) {
    static const BYTE addLine[] = {0x56, 0x57, 0x8B, 0xF9, 0x8B, 0x77, 0x50, 0x83, 0xFE, 0x0A};
    static const BYTE click[] = {0x8B, 0x56, 0x44, 0x69, 0xD2, 0x10, 0x01, 0x00, 0x00, 0x8B, 0x84, 0x32, 0x70, 0x01, 0x00, 0x00};
    BYTE* add = (BYTE*) (client + RVA_DialogAddLine);
    BYTE* clk = (BYTE*) (client + RVA_DialogClickCall);
    BYTE* top = (BYTE*) (client + RVA_DialogTopStore);
    BYTE* mainStore = (BYTE*) (client + RVA_NpcListMainStore);
    BYTE* topicsStore = (BYTE*) (client + RVA_NpcListTopicsStore);
    npcDialogAvailable = readable(add, sizeof(addLine)) && memcmp(add, addLine, sizeof(addLine)) == 0
        && readable(clk, sizeof(click)) && memcmp(clk, click, sizeof(click)) == 0
        && readable(top, 6) && top[0] == 0x89 && top[1] == 0x35 && *(DWORD*) (top + 2) == client + RVA_DialogTop
        && readable(mainStore, 5) && mainStore[0] == 0xA3 && *(DWORD*) (mainStore + 1) == client + RVA_NpcListMain
        && readable(topicsStore, 5) && topicsStore[0] == 0xA3 && *(DWORD*) (topicsStore + 1) == client + RVA_NpcListTopics;
    logLine(npcDialogAvailable ? "NPC list sites verified" : "NPC list sites not as expected: only the NPC menu's first level");
}


static BYTE* npcSubDialog(void) {
    if (!npcMenuAvailable || !npcDialogAvailable || !getUiVar(8)) return NULL;
    BYTE* top = (BYTE*) RDP(client, RVA_DialogTop);
    if (!top || !readable(top, DLG_SIZE)) return NULL;
    for (int i = 0; i < NPC_LISTS; i++) {
        DWORD at = RVA_NpcLists + i * 4;
        if (at != RVA_NpcListMain && (BYTE*) RDP(client, at) == top) return top;
    }
    return NULL;
}


static int npcSubLines(BYTE* dialog, int* lines, int max) {
    int all = (int) RD32(dialog, DLG_LINES), n = 0;
    if (all > 10) all = 10;
    DWORD esc = RD32(dialog, DLG_ESC);
    for (int i = 0; i < all && n < max; i++) {
        BYTE* line = dialog + DLG_LINE0 + i * DLG_LINE_SIZE;
        DWORD fn = RD32(line, DLG_LINE_FN);
        if (RD32(line, DLG_LINE_KIND) != 0 || !fn || fn == esc) continue;
        lines[n++] = i;
    }
    return n;
}





static int npcSubTexts(const wchar_t** texts) {
    BYTE* dialog = npcSubDialog();
    if (!dialog) return -1;
    int lines[NPC_SUB_MAX];
    int n = npcSubLines(dialog, lines, NPC_SUB_MAX);
    for (int i = 0; i < n; i++) texts[i] = (const wchar_t*) (dialog + DLG_LINE0 + lines[i] * DLG_LINE_SIZE);
    return n;
}


static int chooseNpcSubEntry(int index) {
    BYTE* dialog = npcSubDialog();
    if (!dialog) return 0;
    int lines[NPC_SUB_MAX];
    int n = npcSubLines(dialog, lines, NPC_SUB_MAX);
    if (index < 0 || index >= n) {
        logLine("NPC list: no line %d (%d lines)", index, n);
        return 1;
    }
    DWORD handler = RD32(dialog + DLG_LINE0 + lines[index] * DLG_LINE_SIZE, DLG_LINE_FN);
    *(DWORD*) (dialog + DLG_SELECTED) = (DWORD) lines[index];
    logLine("NPC list: entry %d (line %d)", index, lines[index]);
    DWORD a;
    __asm__ volatile ("call *%1" : "=a"(a) : "r"(handler) : "ebx", "ecx", "edx", "esi", "edi", "memory", "cc");
    return 1;
}
