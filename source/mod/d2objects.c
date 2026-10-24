













#define PKT_OBJECTS 12
#define ODATA_INTERACT 0x04

static void sendObjectDetails(void* const* units, int n) {
    static DWORD lastHash, lastAt;
    static Packet p;
    DWORD now = GetTickCount();
    if ((int) (now - playtestUntil) >= 0) return;
    DWORD ids[MAX_OBJECTS];
    WORD rows[MAX_OBJECTS];
    BYTE interact[MAX_OBJECTS];
    int k = 0;
    DWORD hash = 2166136261u;
    for (int i = 0; i < n && k < MAX_OBJECTS; i++) {
        void* unit = units[i];
        if (!readable(unit, U_DATA + 4) || RD32(unit, U_TYPE) != UNIT_OBJECT) continue;
        void* data = RDP(unit, U_DATA);
        if (!readable(data, ODATA_INTERACT + 1)) continue;
        ids[k] = RD32(unit, U_ID);
        rows[k] = (WORD) RD32(unit, U_TXTNO);
        interact[k] = RD8(data, ODATA_INTERACT);
        hash = (hash ^ ids[k]) * 16777619u;
        hash = (hash ^ rows[k] ^ (DWORD) interact[k] << 16) * 16777619u;
        k++;
    }
    hash = (hash ^ (DWORD) k) * 16777619u;
    if (hash == lastHash && now - lastAt < 1000) return;
    lastHash = hash;
    lastAt = now;
    putHeader(&p, PKT_OBJECTS);
    putU8(&p, (BYTE) k);
    for (int i = 0; i < k; i++) {
        putU32(&p, ids[i]);
        putU16(&p, rows[i]);
        putU8(&p, interact[i]);
    }
    sendPacket(&p);
}
