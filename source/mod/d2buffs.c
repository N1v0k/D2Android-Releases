










#define ORD_GetSkillLevel  10007
#define SKILL_LEVEL        0x28

typedef int (__stdcall *GetSkillLevel_t)(void* unit, void* skill, BOOL total);
static GetSkillLevel_t fGetSkillLevel;



static BYTE srvSlotLevel[16];
static WORD srvSlotSkill[16];
static int srvSlotValid;






static const BYTE BUFF_STATES[] = {10, 88, 20, 30, 16, 38, 26, 32, 51, 14};


#define SKILL_SOURCES 8







static int pickSkillSource(const DWORD* owner, const int* level, const int* charges, int n) {
    int charged = -1;
    for (int i = 0; i < n; i++) {
        if (level[i] <= 0) continue;
        if (owner[i] == 0xFFFFFFFFu) return i;
        if (charges[i] > 0 && charged < 0) charged = i;
    }
    return charged;
}


static void buildSelectPacket(BYTE* packet, WORD skill, int left, DWORD owner) {
    DWORD word = (DWORD) skill | (left ? 0x80000000u : 0);
    packet[0] = 0x3C;
    memcpy(packet + 1, &word, 4);
    memcpy(packet + 5, &owner, 4);
}






static void packSkills8(const WORD* skill, LONG* arg) {
    for (int i = 0; i < 4; i++) arg[i] = (LONG) ((DWORD) skill[2 * i] | (DWORD) skill[2 * i + 1] << 16);
}

static void unpackSkills8(const LONG* arg, WORD* skill) {
    for (int i = 0; i < 8; i++) skill[i] = (WORD) ((DWORD) arg[i / 2] >> (16 * (i % 2)));
}

static LONG packLevels4(const int* level) {
    DWORD v = 0;
    for (int i = 0; i < 4; i++) v |= (DWORD) (level[i] < 0 ? 0 : level[i] > 127 ? 127 : level[i]) << (8 * i);
    return (LONG) v;
}


static int unpackLevels8(LONG answer, LONG count, BYTE* level) {
    DWORD a = (DWORD) answer, c = (DWORD) count;
    if ((a | c) & 0x80808080u) return 0;
    for (int i = 0; i < 4; i++) {
        level[i] = (BYTE) (a >> (8 * i));
        level[4 + i] = (BYTE) (c >> (8 * i));
    }
    return 1;
}








static void* usableSkillEntry(void* player, WORD skill) {
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) return NULL;
    void* entry[SKILL_SOURCES];
    DWORD owner[SKILL_SOURCES];
    int level[SKILL_SOURCES], charges[SKILL_SOURCES];
    int n = 0, guard = 0;
    for (void* s = RDP(info, INFO_FIRST); readable(s, SKILL_CHARGED + 4) && guard < 400 && n < SKILL_SOURCES;
         s = RDP(s, SKILL_NEXT), guard++) {
        if (skillId(s) != skill) continue;
        entry[n] = s;
        owner[n] = RD32(s, SKILL_ITEM);
        level[n] = fGetSkillLevel ? fGetSkillLevel(player, s, TRUE) : 1;
        charges[n] = (int) RD32(s, SKILL_CHARGES);
        n++;
    }
    int i = pickSkillSource(owner, level, charges, n);
    return i < 0 ? NULL : entry[i];
}





static void putBuffsAndLevels(Packet* p, void* player) {
    BYTE* count = p->data + p->len;
    putU8(p, 0);
    for (unsigned i = 0; i < sizeof(BUFF_STATES) && fGetUnitState; i++) {
        if (!fGetUnitState(player, BUFF_STATES[i])) continue;
        putU8(p, BUFF_STATES[i]);
        (*count)++;
    }
    for (int i = 0; i < 16; i++) {
        DWORD skill = slotSkill(i);
        int level = 0;
        if (appSlotsSet && srvSlotValid && skill < 0xFFFF && srvSlotSkill[i] == (WORD) skill)
            level = srvSlotLevel[i];
        else {
            void* s = skill < 0xFFFF && fGetSkillLevel ? usableSkillEntry(player, (WORD) skill) : NULL;
            level = s && readable(s, SKILL_LEVEL + 4) ? fGetSkillLevel(player, s, TRUE) : 0;
        }
        putU8(p, level < 0 ? 0 : level > 255 ? 255 : level);
    }
}
