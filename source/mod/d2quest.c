
#define PKT_QUESTS            9
#define RVA_QuestFlagsLoad    0x7D2A4
#define RVA_QuestFlags        0x1018B3
#define RVA_QuestDifficulty   0x11D1D8
#define RVA_QuestPriceCall    0x7D2BC
#define RVA_QuestPriceThunk   0xC6AA
#define ORD_QuestItemPrice    10186
#define QUEST_BITS            0x300
#define QUEST_WORDS           (QUEST_BITS / 16)
#define QUEST_REPEAT_MS       5000

static int questSite = -1;

static void checkQuestSite(void) {
    const BYTE* load = (const BYTE*) (client + RVA_QuestFlagsLoad);
    const BYTE* call = (const BYTE*) (client + RVA_QuestPriceCall);
    const BYTE* thunk = (const BYTE*) (client + RVA_QuestPriceThunk);
    FARPROC price = GetProcAddress((HMODULE) common, (LPCSTR) ORD_QuestItemPrice);
    questSite = readable(load, 13) && load[0] == 0x8B && load[1] == 0x0D && *(const DWORD*) (load + 2) == client + RVA_QuestFlags
        && load[6] == 0x0F && load[7] == 0xB6 && load[8] == 0x15 && *(const DWORD*) (load + 9) == client + RVA_QuestDifficulty
        && readable(call, 5) && call[0] == 0xE8 && (DWORD) (call + 5) + *(const DWORD*) (call + 1) == client + RVA_QuestPriceThunk
        && price && readable(thunk, 6) && thunk[0] == 0xFF && thunk[1] == 0x25 && readable(*(void* const*) (thunk + 2), 4)
        && **(DWORD* const*) (thunk + 2) == (DWORD) price;
    logLine("quest site: %s", questSite ? "verified, packet type 9" : "NOT as expected, no packet type 9");
}

static void sendQuests(void* player) {
    static DWORD lastHash, lastAt;
    static Packet p;
    if (questSite < 0) checkQuestSite();
    if (!questSite || !player) return;
    const DWORD* flags = *(const DWORD* const*) (client + RVA_QuestFlags);
    if (!readable(flags, 8) || flags[1] != QUEST_BITS) return;
    const BYTE* buffer = (const BYTE*) flags[0];
    if (!readable(buffer, QUEST_BITS / 8)) return;
    BYTE difficulty = RD8(client, RVA_QuestDifficulty);
    DWORD hash = 2166136261u ^ difficulty;
    for (int i = 0; i < QUEST_BITS / 8; i++) hash = (hash ^ buffer[i]) * 16777619u;
    DWORD now = GetTickCount();
    if (hash == lastHash && now - lastAt < QUEST_REPEAT_MS) return;
    lastHash = hash;
    lastAt = now;
    putHeader(&p, PKT_QUESTS);
    putU8(&p, difficulty);
    putU8(&p, QUEST_WORDS);
    for (int i = 0; i < QUEST_WORDS; i++) putU16(&p, buffer[2 * i] | buffer[2 * i + 1] << 8);
    sendPacket(&p);
}
