






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







static int isPersonOutsideTownRow(unsigned long monFlags, unsigned nameStr) {
    return (monFlags & MONSTATS_INTERACT) != 0 && !isHelperNameStr(nameStr);
}







static int isAncientStatue(unsigned long cls) {
    return cls >= 537 && cls <= 539;
}



#define MON_OUT    0
#define MON_TARGET 1
#define MON_PERSON 2







static int monsterReport(int town, int ally, unsigned long cls, unsigned long monFlags, unsigned nameStr) {
    if (town) return monsterRowVerdict(monFlags, nameStr, 1) ? MON_PERSON : MON_OUT;
    if (isPersonOutsideTownRow(monFlags, nameStr) && !isAncientStatue(cls)) return MON_PERSON;
    if (ally) return MON_OUT;
    return monsterRowVerdict(monFlags, nameStr, 0) ? MON_TARGET : MON_OUT;
}

#endif
