#ifndef D2MONRULES_H
#define D2MONRULES_H

#define MONSTATS_ID       0x00
#define MONSTATS_NAMESTR  0x06
#define MONSTATS_FLAGS    0x0C
#define MONSTATS_NPC      0x100
#define MONSTATS_INTERACT 0x200

static int isHelperNameStr(unsigned nameStr) {
    return nameStr >= 5382 && nameStr <= 5386;
}

static int isTalkableTownUnit(unsigned long monFlags) {
    return (monFlags & MONSTATS_INTERACT) != 0;
}

static int monsterRowVerdict(unsigned long monFlags, unsigned nameStr, int town) {
    if (town) return isTalkableTownUnit(monFlags);
    return !isHelperNameStr(nameStr);
}

#endif
