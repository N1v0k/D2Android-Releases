












#define CMD_CUBE_OPEN 37


static void* cubeOpenFind(void* player, int stashOpen, int* where) {
    void* inventory = RDP(player, U_INVENTORY);
    void* inStash = NULL;
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD8(data, IDATA_NODEPAGE) == 1 && strncmp(itemCode(item), "box", 3) == 0) {
            int location = RD8(data, IDATA_LOCATION);
            if (location == 0) {
                *where = 0;
                return item;
            }
            if (location == 4 && stashOpen && !inStash) inStash = item;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    *where = 4;
    return inStash;
}

static void cubeOpenCommand(void* player) {
    const char* why = NULL;
    if (!equipAvailable) why = "the senders are not as expected";
    else if (!singlePlayerGame()) why = "not a single player game";
    else if (worldPaused()) why = "paused";
    else if (getUiVar(0x1A)) why = "already open";
    else if (cubeBusy() || mv.step != MV_NONE || mvLate.id || equip.step != EQ_NONE || sort.active) why = "another job runs";
    else if (equipCursorId(player)) why = "an item is on the cursor";
    int where = 0;
    void* item = why ? NULL : cubeOpenFind(player, getUiVar(0x19) != 0, &where);
    if (!why && !item) why = "no cube in the inventory or on the open stash page";
    if (why) {
        logLine("cube open: not sent, %s", why);
        return;
    }
    BYTE packet[13] = {0x20};
    void* path = RDP(player, U_PATH);
    *(DWORD*) (packet + 1) = RD32(item, U_ID);
    *(WORD*) (packet + 5) = path ? (WORD) (RD32(path, PATH_X) >> 16) : 0;
    *(WORD*) (packet + 9) = path ? (WORD) (RD32(path, PATH_Y) >> 16) : 0;
    fSendPacket(13, 1, packet);
    logLine("cube open: sent for the cube in the %s", where == 0 ? "inventory" : "stash");
}
