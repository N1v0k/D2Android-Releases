















#define CMD_TAKE 15
#define TAKE_RANGE     20.0
#define TAKE_NPC_NEAR  4.0

static int takeAvailable;



static void checkTakeSites(void) {
    static const BYTE item[] = {0xB9, 0x04, 0x00, 0x00, 0x00, 0xB0, 0x16, 0xE8};
    static const BYTE object[] = {0xB9, 0x02, 0x00, 0x00, 0x00, 0xB0, 0x13, 0xE8};
    BYTE* a = (BYTE*) (client + 0x4A13E);
    BYTE* b = (BYTE*) (client + 0x4A057);
    takeAvailable = unitActions && fSendPacket && readable(a, sizeof(item)) && readable(b, sizeof(object))
        && memcmp(a, item, sizeof(item)) == 0 && memcmp(b, object, sizeof(object)) == 0;
    logLine(takeAvailable ? "take sites verified" : "take sites not as expected: Nehmen stays a click");
}

static void sendUnitPacket(BYTE op, DWORD type, DWORD id, int withFlag) {
    BYTE packet[13] = {op};
    *(DWORD*) (packet + 1) = type;
    *(DWORD*) (packet + 5) = id;

    fSendPacket(withFlag ? 13 : 9, 1, packet);
}

static double unitDistance(void* player, void* unit) {
    double px, py, ux, uy;
    unitPosition(player, &px, &py);
    unitPosition(unit, &ux, &uy);
    return __builtin_sqrt((ux - px) * (ux - px) + (uy - py) * (uy - py));
}

static void takeUnit(void* player, DWORD type, DWORD id, DWORD now) {
    if (!takeAvailable) {
        unitAction(0, PHASE_CLICK, 0, type, id, now);
        return;
    }
    void* unit = findUnit(id, type);
    if (!unit) {
        trace("take type %lu id %lu: NOT FOUND", (unsigned long) type, (unsigned long) id);
        return;
    }
    double d = unitDistance(player, unit);

    if (type == UNIT_TILE) {
        double tx, ty;
        unitPosition(unit, &tx, &ty);
        if (tileUnderGate(lastGates, nLastGates, (int) tx, (int) ty)) {
            trace("take type 5 id %lu: a closed object stands on it, not taken", (unsigned long) id);
            return;
        }
    }
    const char* how = "click";

    stopWalk(player);
    pendingPress.active = 0;
    releaseHeld("app: take");
    if (type == UNIT_ITEM && d <= TAKE_RANGE) {
        sendUnitPacket(0x16, UNIT_ITEM, id, 1);
        how = "packet 0x16";
    }
    else if ((type == UNIT_OBJECT || type == UNIT_PLAYER || type == UNIT_TILE) && d <= TAKE_RANGE) {
        sendUnitPacket(0x13, type, id, 0);
        how = "packet 0x13";
    }
    else if (type == UNIT_MONSTER && d <= TAKE_NPC_NEAR) {
        sendUnitPacket(0x13, UNIT_MONSTER, id, 0);
        how = "packet 0x13";
    }
    else unitAction(0, PHASE_CLICK, 0, type, id, now);
    trace("take type %lu id %lu (%.1f subtiles, player mode %lu): %s", (unsigned long) type, (unsigned long) id, d,
        (unsigned long) playerMode(), how);
}
