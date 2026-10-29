











#define CMD_SOUND           35
#define SOUND_TABLE_AT      0x1E
#define SOUND_COUNT_AT      0x28

static DWORD* soundTable;
static DWORD* soundCount;

static int insideClient(DWORD at) {
    return at >= client && at < client + 0x130000;
}

static void checkGameSoundSite(void) {
    BYTE* play = (BYTE*) (client + RVA_PlaySound);
    soundTable = NULL;
    soundCount = NULL;
    if (soundAvailable && readable(play + SOUND_TABLE_AT, 16) && play[SOUND_TABLE_AT] == 0xA1
        && play[SOUND_COUNT_AT] == 0x3B && play[SOUND_COUNT_AT + 1] == 0x1D) {
        DWORD table = *(DWORD*) (play + SOUND_TABLE_AT + 1), count = *(DWORD*) (play + SOUND_COUNT_AT + 2);
        if (insideClient(table) && insideClient(count)) {
            soundTable = (DWORD*) table;
            soundCount = (DWORD*) count;
        }
    }
    logLine("sound sites: command 35 %s", soundTable ? "verified" : "NOT as expected, sounds of the app stay silent");
}


static void soundCommand(const BYTE* buf, int n) {
    if (n < 10) return;
    int id = *(const WORD*) (buf + 8);
    if (!soundTable || !*soundTable || id <= 0 || id >= (int) *soundCount) {
        trace("sound %d: not played (rows %d)", id, soundCount ? (int) *soundCount : -1);
        return;
    }
    DWORD play = client + RVA_PlaySound;
    int sound = id;
    __asm__ volatile ("pushl $0\n\tpushl $0\n\tpushl $0\n\tpushl $0\n\tcall *%%eax"
        : "+a"(play), "+b"(sound) : : "ecx", "edx", "esi", "edi", "memory", "cc");
    trace("sound %d played", id);
}
