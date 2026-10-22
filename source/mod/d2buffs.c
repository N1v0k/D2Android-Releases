
#define ORD_GetSkillLevel  10007
#define SKILL_LEVEL        0x28

typedef int (__stdcall *GetSkillLevel_t)(void* unit, void* skill, BOOL total);
static GetSkillLevel_t fGetSkillLevel;

static const BYTE BUFF_STATES[] = {10, 88, 20, 30, 16, 38, 26, 32, 51, 14};

static void* findSkill(void* player, WORD skill) {
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) return NULL;
    void* found = NULL;
    int guard = 0;
    for (void* s = RDP(info, INFO_FIRST); readable(s, SKILL_ITEM + 4) && guard < 400; s = RDP(s, SKILL_NEXT), guard++) {
        if (skillId(s) != skill) continue;
        if (RD32(s, SKILL_ITEM) == 0xFFFFFFFF) return s;
        if (!found) found = s;
    }
    return found;
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
        DWORD skill = RD32(client, RVA_HotkeySkill + i * 4);
        void* s = skill < 0xFFFF && fGetSkillLevel ? findSkill(player, (WORD) skill) : NULL;
        int level = s && readable(s, SKILL_LEVEL + 4) ? fGetSkillLevel(player, s, TRUE) : 0;
        putU8(p, level < 0 ? 0 : level > 255 ? 255 : level);
    }
}
