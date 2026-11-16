







#define RVA_ListX         0x101048
#define RVA_ListY         0x101138
#define RVA_ListSkill     0x101228
#define RVA_ListLoop      0xA78CD
#define RVA_ListLoopEnd   0xA79DB
#define LIST_ENTRIES      60
#define LIST_ICON         48
#define LIST_NONE         0xFFFF
#define LIST_UNKNOWN      0xFFFE
#define LIST_CLOSED       0xFFFD
#define CMD_LIST_AT       39



static unsigned skillListHit(const long* xs, const long* ys, const unsigned long* skills, int count, long mx, long my) {
    for (int i = 0; i < count; i++) {
        if (ys[i] <= 0) continue;
        if (mx >= xs[i] && mx <= xs[i] + LIST_ICON && my >= ys[i] - LIST_ICON && my <= ys[i]) return skills[i] & 0xFFFF;
    }
    return LIST_NONE;
}


static int listChecked = -1;
static BYTE listSeq, listSentSeq;
static long listX, listY;
static WORD listNow = LIST_UNKNOWN, listSent = LIST_UNKNOWN;


static int skillListKnown(void) {
    if (listChecked >= 0) return listChecked;
    const BYTE* loop = (const BYTE*) (client + RVA_ListLoop);
    const BYTE* end = (const BYTE*) (client + RVA_ListLoopEnd);
    listChecked = readable(loop, 5) && readable(end, 6) && loop[0] == 0xBE && *(DWORD*) (loop + 1) == client + RVA_ListSkill
        && end[0] == 0x81 && end[1] == 0xFE && *(DWORD*) (end + 2) == client + RVA_ListSkill + LIST_ENTRIES * 4;
    logLine(listChecked ? "skill list: entries readable" : "skill list: code NOT as expected, no entry reported");
    return listChecked;
}


static void skillListCommand(const BYTE* buf, int n) {
    if (n < 13 || buf[8] == 0) return;
    listSeq = buf[8];
    listX = *(const short*) (buf + 9);
    listY = *(const short*) (buf + 11);
}


static int skillListTick(void) {
    WORD w = LIST_UNKNOWN;
    const void* xs = (const void*) (client + RVA_ListX);
    const void* ys = (const void*) (client + RVA_ListY);
    const void* skills = (const void*) (client + RVA_ListSkill);
    if (client && fGetUiVar && skillListKnown() && readable(xs, LIST_ENTRIES * 4) && readable(ys, LIST_ENTRIES * 4)
        && readable(skills, LIST_ENTRIES * 4)) {
        w = getUiVar(3) == 0 ? LIST_CLOSED
            : (WORD) skillListHit((const long*) xs, (const long*) ys, (const unsigned long*) skills, LIST_ENTRIES, listX, listY);
    }
    listNow = w;
    return listSeq != listSentSeq || w != listSent;
}


static void putSkillListField(Packet* p) {
    putU16(p, listNow);
    putU8(p, listSeq);
    listSent = listNow;
    listSentSeq = listSeq;
}
