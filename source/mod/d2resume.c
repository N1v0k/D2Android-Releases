































#define CMD_RESUME           25





#define RESUME_ARRIVE_MS     3000
#define RESUME_ARRIVE_TICKS  75
#define RESUME_ARRIVE_MAX_MS 8000
#define RESUME_SPOT_MS       600
#define RESUME_SPOT_TICKS    15
#define RESUME_SPOT_MAX_MS   3000
#define RESUME_GUARD_MS      3000
#define RESUME_SEEN_FRAMES   3
#define RESUME_GUARD_MAX_MS  15000
#define RESUME_OFF_REPEAT_MS 200
#define RESUME_MAX_FRAME     1500
#define RESUME_NEAR          2


#define RS_NONE     0
#define RS_RUNNING  1
#define RS_SPOT     2
#define RS_WAYPOINT 3
#define RS_REFUSED  4

#define RR_NONE        0
#define RR_MULTIPLAYER 1
#define RR_NOT_READY   2
#define RR_DIFFICULTY  3
#define RR_SEED        4
#define RR_LEVEL       5
#define RR_FUNCTIONS   6
#define RR_WARP        7
#define RR_ROOM        8
#define RR_WALL        9
#define RR_JUMP        10
#define RR_TIME        11
#define RR_CANCELLED   12
#define RR_LATE        13
#define RR_REGEN       16

#define RO_TOWN        1
#define RO_WAYPOINT    2
#define RX_NONE        0
#define RX_RUNNING     1
#define RX_TOWN        2
#define RX_WAYPOINT    3
#define RX_REFUSED     4
#define RESUME_EXIT_PLAY_TICKS 10
#define RESUME_EXIT_MS 3000

static struct {
    int phase;
    BYTE state, reason;
    int usedGame;
    WORD level, x, y;
    DWORD seed;
    WORD rx, ry, rw, rh;
    BYTE difficulty;
    LONG seq;
    LONG dropSeq[4];
    DWORD started, phaseAt;
    DWORD ticks, phaseTicks;
    int offAt;
    DWORD offSince, offTicks;
    int backTries;
    BYTE backLevel;
    int seenAt;
    DWORD seenFrames, seenSince, seenTicks;
    int arrived;

    int guardOn;
    LONG onSeq, offSeq;
    DWORD guardUntil, guardHard, offAskedAt;
    DWORD lastTick;
    int offWanted;

    int offerDue;
    BYTE offer, offerLevel, exitState;
    int exitOpen;
    int exitPaused;
    int exitResumed;
    DWORD exitPlayTicks;
    int exitWant;
    LONG exitSeq;
    DWORD exitAt;
    WORD exitLevel;
} rsm;
static void* resumePlayer;
static DWORD resumePlayerId;
static int resumeDifficulty = -1;
static LONG resumeDiffSeq;
static DWORD resumeGames;
static int resumeRegen = -1;





static int resumeRegenMap(void) {
    if (resumeRegen >= 0) return resumeRegen;
    HMODULE plugy = GetModuleHandleA("PlugY.dll");
    char path[MAX_PATH];
    DWORD n = plugy ? GetModuleFileNameA(plugy, path, sizeof(path)) : 0;
    if (!plugy) resumeRegen = 0;
    else if (n == 0 || n >= sizeof(path) - 16) resumeRegen = 1;
    else {
        char* slash = strrchr(path, '\\');
        strcpy(slash ? slash + 1 : path, "PlugY.ini");
        resumeRegen = GetPrivateProfileIntA("EXTRA", "AlwaysRegenMapInSP", 0, path) != 0;
    }
    logLine("resume: PlugY %s, map built anew at every load: %s", plugy ? "loaded" : "not loaded", resumeRegen ? "yes" : "no");
    return resumeRegen;
}

static void resumeLog(const char* what) {
    logLine("resume: %s", what);
}


static int resumeWaitOver(DWORD since, DWORD ticks, int ms, DWORD minTicks, int maxMs, DWORD now) {
    int elapsed = (int) (now - since);
    return (elapsed > ms && ticks > minTicks) || elapsed > maxMs;
}


static void resumeBack(BYTE reason, const char* why, DWORD now) {
    rsm.reason = reason;
    rsm.phase = 4;
    logLine("resume: %s after %lu ms / %lu ticks (phase %lu ms / %lu ticks): back to town", why,
            (unsigned long) (now - rsm.started), (unsigned long) rsm.ticks, (unsigned long) (now - rsm.phaseAt),
            (unsigned long) rsm.phaseTicks);
}


static void resumeDrop(LONG* seq) {
    if (*seq && !srvCancel(*seq)) {
        for (int i = 0; i < 4; i++) if (!rsm.dropSeq[i]) { rsm.dropSeq[i] = *seq; break; }
    }
    *seq = 0;
}

static void resumeEnd(BYTE state, BYTE reason, const char* what) {
    rsm.phase = 0;
    rsm.state = state;
    rsm.reason = reason;
    resumeDrop(&rsm.seq);
    if (!rsm.arrived && rsm.guardOn) {
        rsm.offWanted = 1;
        rsm.offAskedAt = 0;
    }
    logLine("resume: %s – state %d, reason %d", what, state, reason);
}


static int resumeHeroAt(void* player, long* x, long* y) {
    void* path = player ? RDP(player, U_PATH) : NULL;
    if (!readable(path, PATH_Y + 4)) return 0;
    *x = (long) (RD32(path, PATH_X) >> 16);
    *y = (long) (RD32(path, PATH_Y) >> 16);
    return *x > 0 && *y > 0;
}


static DWORD resumeSeed(void* player) {
    void* act = player ? RDP(player, U_ACT) : NULL;
    return readable(act, ACT_SEED + 4) ? RD32(act, ACT_SEED) : 0;
}


static int resumeHeroRoom(void* player, WORD* rx, WORD* ry, WORD* rw, WORD* rh) {
    void* path = player ? RDP(player, U_PATH) : NULL;
    void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
    void* room2 = readable(room1, ROOM1_ROOM2 + 4) ? RDP(room1, ROOM1_ROOM2) : NULL;
    if (!readable(room2, ROOM2_H + 4)) return 0;
    *rx = (WORD) (RD32(room2, ROOM2_X) * 5);
    *ry = (WORD) (RD32(room2, ROOM2_Y) * 5);
    *rw = (WORD) (RD32(room2, ROOM2_W) * 5);
    *rh = (WORD) (RD32(room2, ROOM2_H) * 5);
    return 1;
}

static const int UI_PANELS[] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x08, 0x09, 0x0B, 0x0C, 0x0E, 0x0F, 0x14, 0x16, 0x17, 0x18, 0x19,
    0x1A, 0x21, 0x24, 0x25};


static int resumeCalm(void* player) {
    DWORD mode = RD32(player, U_MODE);
    if (mode == 0 || mode == 17 || !isTown(unitLevel(player)) || worldPaused() || equipCursorItem(player)) return 0;
    if (fGetUiVar) for (unsigned i = 0; i < sizeof(UI_PANELS) / sizeof(UI_PANELS[0]); i++) if (getUiVar(UI_PANELS[i])) return 0;
    return 1;
}



static BYTE resumeOfferFor(int inTown) {
    return inTown ? 0 : (BYTE) RO_TOWN;
}





static int resumeExitTake(int kind, int want, BYTE exitState, int open, BYTE offer) {
    if (want || exitState == RX_RUNNING || exitState == RX_TOWN || exitState == RX_WAYPOINT) return -1;
    return open && (offer & (kind == 2 ? RO_TOWN : RO_WAYPOINT)) ? 1 : 0;
}


static int resumeExitPlaying(int paused, int* pausedSeen, DWORD* playTicks) {
    if (paused) {
        *pausedSeen = 1;
        *playTicks = 0;
        return 0;
    }
    return *pausedSeen && ++*playTicks > RESUME_EXIT_PLAY_TICKS;
}


static void resumeGuardOn(DWORD now);
static void resumeArrived(DWORD now, BYTE state, BYTE reason, const char* what);
static void damageShieldPrepare(void* player);


static void resumeCommand(void* player, const BYTE* buf, int n) {
    if (n >= 9 && buf[8] == 0) {

        if (rsm.phase && rsm.arrived) resumeArrived(GetTickCount(), RS_REFUSED, RR_CANCELLED, "cancelled by the app");
        else if (rsm.phase) resumeEnd(RS_REFUSED, RR_CANCELLED, "cancelled by the app");
        return;
    }
    if (n >= 9 && (buf[8] == 2 || buf[8] == 3)) {

        int town = buf[8] == 2, take = resumeExitTake(buf[8], rsm.exitWant, rsm.exitState, rsm.exitOpen, rsm.offer);
        if (take < 0) {
            logLine("resume: way out again, ignored (state %d)", rsm.exitState);
            return;
        }
        if (!take) {
            logLine("resume: way out %s refused (offer %d, open %d)", town ? "to town" : "to the waypoint", rsm.offer, rsm.exitOpen);
            rsm.exitState = RX_REFUSED;
            return;
        }
        rsm.exitOpen = 0;
        rsm.exitWant = buf[8];
        rsm.exitState = RX_RUNNING;
        logLine("resume: way out %s asked for, after the pause", town ? "to town" : "to the waypoint");
        return;
    }
    if (n < 28 || buf[8] != 1 || !player) return;
    if (rsm.phase) return;
    rsm.level = *(WORD*) (buf + 9);
    rsm.x = *(WORD*) (buf + 11);
    rsm.y = *(WORD*) (buf + 13);
    rsm.seed = *(DWORD*) (buf + 15);
    rsm.rx = *(WORD*) (buf + 19);
    rsm.ry = *(WORD*) (buf + 21);
    rsm.rw = *(WORD*) (buf + 23);
    rsm.rh = *(WORD*) (buf + 25);
    rsm.difficulty = buf[27];
    DWORD first, end, firstHere, endHere;
    if (rsm.usedGame) {

        logLine("resume: command 25 again in this game, ignored (state %d)", rsm.state);
        return;
    }
    rsm.usedGame = 1;
    rsm.arrived = 0;
    if (!singlePlayerGame()) { resumeEnd(RS_REFUSED, RR_MULTIPLAYER, "not single player"); return; }
    if (resumeRegenMap()) { resumeEnd(RS_REFUSED, RR_REGEN, "PlugY builds the map anew at every load"); return; }
    if (!resumeCalm(player)) { resumeEnd(RS_REFUSED, RR_NOT_READY, "hero not ready (dead, not in town, window, cursor, pause)"); return; }
    if (resumeDifficulty < 0 || resumeDifficulty != rsm.difficulty) { resumeEnd(RS_REFUSED, RR_DIFFICULTY, "difficulty unknown or not the saved one"); return; }
    if (!rsm.seed || resumeSeed(player) != rsm.seed) { resumeEnd(RS_REFUSED, RR_SEED, "another map (act seed)"); return; }
    if (isTown(rsm.level) || !actOf(rsm.level, &first, &end) || !actOf(unitLevel(player), &firstHere, &endHere) || first != firstHere) {
        resumeEnd(RS_REFUSED, RR_LEVEL, "a town, another act or not a level");
        return;
    }
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1 || !resumeFunctionsKnown()) { resumeEnd(RS_REFUSED, RR_FUNCTIONS, "server functions not as known"); return; }
    damageShieldPrepare(player);
    resumeGuardOn(GetTickCount());

    rsm.seq = srvSubmitEx(SRV_CODE_DIRECT, rsm.level | RESUME_MAX_FRAME << 8, rsm.x | (LONG) rsm.y << 16,
                          rsm.rx | (LONG) rsm.ry << 16, rsm.rw | (LONG) rsm.rh << 16);
    if (!rsm.seq) { resumeEnd(RS_REFUSED, RR_FUNCTIONS, "server queue full"); return; }
    rsm.phase = 1;
    rsm.state = RS_RUNNING;
    rsm.reason = RR_NONE;
    rsm.started = rsm.phaseAt = GetTickCount();
    rsm.ticks = rsm.phaseTicks = 0;
    rsm.offAt = 0;
    rsm.seenAt = 0;
    rsm.backTries = 0;
    logLine("resume: straight to level %u at %u,%u (room %u,%u %ux%u)", rsm.level, rsm.x, rsm.y, rsm.rx, rsm.ry, rsm.rw,
            rsm.rh);
}







static int resumeSaveGate(void) {
    return rsm.phase != 0 ? -1 : 0;
}


static void resumeGuardOn(DWORD now) {
    if (rsm.guardOn) return;
    rsm.guardOn = 1;
    rsm.offWanted = 0;
    rsm.guardUntil = now + RESUME_GUARD_MAX_MS;
    rsm.guardHard = now + RESUME_GUARD_MAX_MS;
    rsm.onSeq = srvSubmit(SRV_CODE_INVUL_ON, 0, 0);
}

static void resumeArrived(DWORD now, BYTE state, BYTE reason, const char* what) {
    resumeEnd(state, reason, what);
    if (state == RS_REFUSED && !rsm.arrived) return;
    rsm.offerDue = 1;
    if (!rsm.guardOn) resumeGuardOn(now);
    if (!rsm.offWanted) {
        rsm.guardUntil = now + RESUME_GUARD_MS;

        if ((int) (rsm.guardUntil - rsm.guardHard) > 0) rsm.guardHard = rsm.guardUntil;
    }
}


static void updateResumeOffer(void* player) {
    rsm.offerDue = 0;
    rsm.offer = resumeOfferFor(!player || isTown(unitLevel(player)));
    rsm.offerLevel = 0;
    if (!rsm.offer) {
        rsm.exitOpen = 0;
        logLine("resume: in town, no way out offered");
        return;
    }
    rsm.exitOpen = 1;
    rsm.exitPaused = 0;
    rsm.exitResumed = 0;
    rsm.exitPlayTicks = 0;
    rsm.exitState = RX_NONE;
    logLine("resume: way out offered: town");
}


static void updateResumeExit(void* player, DWORD now) {
    LONG answer, count;
    if (rsm.exitOpen && resumeExitPlaying(worldPaused(), &rsm.exitPaused, &rsm.exitPlayTicks)) {
        rsm.exitOpen = 0;
        logLine("resume: way out no longer offered: playing on");
    }


    if (rsm.exitPaused && rsm.exitPlayTicks == 1 && !rsm.exitResumed) {
        rsm.exitResumed = 1;
        if (rsm.guardOn && !rsm.offWanted) {
            rsm.guardUntil = now + RESUME_GUARD_MS;
            if ((int) (rsm.guardUntil - rsm.guardHard) > 0) rsm.guardHard = rsm.guardUntil;
            logLine("resume: played on after the arrival pause, protected for %d ms from now", RESUME_GUARD_MS);
        }
    }
    if (rsm.exitWant && !rsm.exitSeq && !worldPaused()) {
        DWORD first, end;

        rsm.exitLevel = actOf(unitLevel(player), &first, &end) ? (WORD) first : 0;
        rsm.exitSeq = rsm.exitLevel ? srvSubmit(SRV_CODE_WARP, rsm.exitLevel, 0) : 0;
        logLine("resume: way out %s to level %u sent", rsm.exitWant == 2 ? "to town" : "to the waypoint", rsm.exitLevel);
        if (!rsm.exitSeq) {
            rsm.exitState = RX_REFUSED;
            logLine("resume: way out not sent (queue full or level unknown)");
        }
        rsm.exitAt = now;
        rsm.exitWant = rsm.exitSeq ? rsm.exitWant : 0;
        if (!rsm.exitSeq) return;
    }
    if (!rsm.exitSeq) return;
    if (srvTake(rsm.exitSeq, &answer, &count)) {
        int town = rsm.exitWant == 2;
        rsm.exitSeq = 0;
        rsm.exitWant = 0;
        if (answer != 1) {
            rsm.exitState = RX_REFUSED;
            logLine("resume: way out %s refused by the server (%ld)", town ? "to town" : "to the waypoint", (long) answer);
            return;
        }
        rsm.exitState = town ? RX_TOWN : RX_WAYPOINT;
        logLine("resume: way out %s after %lu ms", town ? "to town" : "to the waypoint", (unsigned long) (now - rsm.exitAt));
        if (town) {

            if (rsm.guardOn) rsm.offWanted = 1, rsm.offAskedAt = 0;
        }
    }
    else if ((int) (now - rsm.exitAt) > RESUME_EXIT_MS && srvCancel(rsm.exitSeq)) {
        rsm.exitSeq = 0;
        rsm.exitWant = 0;
        rsm.exitState = RX_REFUSED;
        logLine("resume: way out not answered in time");
    }
}


static void updateResume(void* player, DWORD now) {
    DWORD playerId = player ? RD32(player, U_ID) : 0;
    if (player && (player != resumePlayer || playerId != resumePlayerId)) {

        logLine("resume: hero unit %p id %lu (before %p id %lu): a new game", player, (unsigned long) playerId,
                resumePlayer, (unsigned long) resumePlayerId);
        resumePlayer = player;
        resumePlayerId = playerId;
        memset(&rsm, 0, sizeof(rsm));
        resumeDifficulty = -1;
        resumeDiffSeq = 0;
        resumeGames = srvResets;
    }
    if (srvResets != resumeGames) {








        int out = player && !isTown(unitLevel(player));

        int away = out || rsm.phase == 1 || rsm.phase == 2;
        logLine("resume: server queue emptied (hero %p id %lu, %s, phase %d, protection %d)", player,
                (unsigned long) playerId, out ? "out of town" : "in town or none", rsm.phase, rsm.guardOn);
        resumeGames = srvResets;
        rsm.seq = rsm.onSeq = rsm.offSeq = 0;
        memset(rsm.dropSeq, 0, sizeof(rsm.dropSeq));
        if (rsm.exitSeq || rsm.exitWant) {
            rsm.exitSeq = 0;
            rsm.exitWant = 0;
            rsm.exitState = RX_REFUSED;
            logLine("resume: way out dropped with the queue");
        }
        rsm.exitOpen = 0;
        rsm.offerDue = 0;
        rsm.offer = 0;
        resumeDiffSeq = 0;
        resumeDifficulty = -1;
        if (rsm.guardOn && away && !rsm.offWanted) rsm.onSeq = srvSubmit(SRV_CODE_INVUL_ON, 0, 0);
        else if (rsm.guardOn) {
            rsm.offWanted = 1;
            rsm.offAskedAt = 0;
        }
        if (!rsm.phase) {
            rsm.usedGame = 0;
            rsm.state = RS_NONE;
            rsm.reason = RR_NONE;
        }
        else if (rsm.phase == 4 && (!player || unitLevel(player) != rsm.level)) {

            resumeEnd(RS_REFUSED, RR_TIME, "stalled: not on the level any more");
        }
        else if (rsm.phase == 4) rsm.phaseAt = now;
        else {


            rsm.arrived = 1;
            rsm.phase = 2;
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
        }
    }
    if (!player || !singlePlayerGame()) return;
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1) return;
    LONG answer, count;

    if (resumeDifficulty < 0 && !resumeDiffSeq) resumeDiffSeq = srvSubmit(SRV_CODE_DIFFICULTY, 0, 0);
    if (resumeDiffSeq && srvTake(resumeDiffSeq, &answer, NULL)) {
        resumeDiffSeq = 0;
        resumeDifficulty = answer >= 0 && answer <= 2 ? answer : -1;
        if (resumeDifficulty < 0) resumeDiffSeq = 0;
    }
    for (int i = 0; i < 4; i++) if (rsm.dropSeq[i] && srvTake(rsm.dropSeq[i], &answer, NULL)) rsm.dropSeq[i] = 0;
    if (rsm.lastTick && (now - rsm.lastTick > 500 || worldPaused())) {


        DWORD gap = now - rsm.lastTick;
        if (rsm.guardOn) rsm.guardUntil += gap, rsm.guardHard += gap;
        if (rsm.phase) rsm.phaseAt += gap, rsm.started += gap;
        if (rsm.offAt) rsm.offSince += gap;
        if (rsm.seenAt) rsm.seenSince += gap;
    }
    else if (rsm.phase) rsm.ticks++, rsm.phaseTicks++;
    rsm.lastTick = now;
    long hx, hy;
    DWORD mode = RD32(player, U_MODE);
    switch (rsm.phase) {
    case 1:
        if (srvTake(rsm.seq, &answer, &count)) {
            rsm.seq = 0;
            logLine("resume: jump answered %ld at game frame %ld (limit %d) after %lu ms", (long) answer, (long) count,
                    RESUME_MAX_FRAME, (unsigned long) (now - rsm.started));
            if (answer != 1) {

                resumeEnd(RS_REFUSED, answer == 3 ? RR_FUNCTIONS : answer == 4 ? RR_LATE : answer == 5 || answer == 6 ? RR_ROOM
                          : answer == 0 ? RR_WALL : answer == 2 ? RR_LEVEL : RR_WARP,
                          answer == 4 ? "jump refused: the game runs longer than 60 s"
                          : answer == 5 ? "jump refused: the place is not in the saved room of the level"
                          : answer == 6 ? "jump refused: the room there is of another level"
                          : answer == 0 ? "jump refused: no free spot at the place" : "jump refused");
                break;
            }
            rsm.arrived = 1;
            rsm.phase = 2;
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
        }
        else if (resumeWaitOver(rsm.phaseAt, rsm.phaseTicks, RESUME_ARRIVE_MS, RESUME_ARRIVE_TICKS, RESUME_ARRIVE_MAX_MS, now)) {
            if (srvCancel(rsm.seq)) {
                rsm.seq = 0;
                resumeEnd(RS_REFUSED, RR_TIME, "jump: no answer");
                break;
            }

            resumeDrop(&rsm.seq);
            rsm.arrived = 1;
            rsm.phase = 2;
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
        }
        break;
    case 2:
        if (unitLevel(player) == rsm.level && resumeHeroAt(player, &hx, &hy)) {
            if (labs(hx - rsm.x) <= RESUME_NEAR && labs(hy - rsm.y) <= RESUME_NEAR) {


                DWORD frames = (DWORD) InterlockedCompareExchange(&worldFrames, 0, 0);
                if (!rsm.seenAt) rsm.seenAt = 1, rsm.seenFrames = frames, rsm.seenSince = now, rsm.seenTicks = 0;
                else rsm.seenTicks++;
                if (worldHooked && frames - rsm.seenFrames < RESUME_SEEN_FRAMES
                    && !resumeWaitOver(rsm.seenSince, rsm.seenTicks, RESUME_SPOT_MS, RESUME_SPOT_TICKS, RESUME_SPOT_MAX_MS, now)) break;
                logLine("resume: at the place after %lu ms / %lu ticks, %lu pictures drawn there", (unsigned long) (now - rsm.started),
                        (unsigned long) rsm.ticks, (unsigned long) (frames - rsm.seenFrames));
                resumeArrived(now, RS_SPOT, RR_NONE, "at the place");
                break;
            }
            rsm.seenAt = 0;

            if (!rsm.offAt) rsm.offAt = 1, rsm.offSince = now, rsm.offTicks = 0;
            else rsm.offTicks++;
            if (resumeWaitOver(rsm.offSince, rsm.offTicks, RESUME_SPOT_MS, RESUME_SPOT_TICKS, RESUME_SPOT_MAX_MS, now)) {
                logLine("resume: hero at %ld,%ld, the place is %u,%u", hx, hy, rsm.x, rsm.y);
                resumeBack(RR_WALL, "not at the place", now);
            }
        }
        else if (resumeWaitOver(rsm.phaseAt, rsm.phaseTicks, RESUME_ARRIVE_MS, RESUME_ARRIVE_TICKS, RESUME_ARRIVE_MAX_MS, now)) {
            if (isTown(unitLevel(player))) resumeEnd(RS_REFUSED, RR_TIME, "jump: the level not shown in time, still in town");
            else resumeBack(RR_JUMP, "jump: not shown in the level in time", now);
        }
        break;
    case 4:
        if (rsm.seq) {
            if (srvTake(rsm.seq, &answer, NULL)) {
                rsm.seq = 0;
                if (answer == 1) {
                    BYTE why = rsm.reason ? rsm.reason : RR_JUMP;
                    resumeArrived(now, RS_REFUSED, why, "back in town");
                    if (rsm.guardOn) rsm.offWanted = 1, rsm.offAskedAt = 0;
                }
            }

            else if ((int) (now - rsm.phaseAt) > RESUME_ARRIVE_MS && srvCancel(rsm.seq)) rsm.seq = 0;
            break;
        }
        if (rsm.backTries >= 3) { resumeArrived(now, RS_REFUSED, rsm.reason ? rsm.reason : RR_JUMP, "back failed"); break; }
        if (rsm.backTries && (int) (now - rsm.phaseAt) < 1000) break;
        if (isTown(unitLevel(player))) { resumeEnd(RS_REFUSED, rsm.reason ? rsm.reason : RR_TIME, "back: in town already"); break; }
        rsm.backTries++;
        rsm.phaseAt = now;

        DWORD first, end;
        rsm.backLevel = actOf(rsm.level, &first, &end) ? (BYTE) first : 0;
        rsm.seq = rsm.backLevel ? srvSubmit(SRV_CODE_WARP, rsm.backLevel, 0) : 0;
        logLine("resume: back to level %u", rsm.backLevel);
        break;
    }
    if (rsm.offerDue) updateResumeOffer(player);
    updateResumeExit(player, now);

    if (rsm.onSeq && srvTake(rsm.onSeq, &answer, &count)) {
        rsm.onSeq = 0;
        resumeLog(answer ? "invulnerable" : "invulnerable: refused");
    }
    if (rsm.offSeq && srvTake(rsm.offSeq, &answer, &count)) {
        rsm.offSeq = 0;
        if (answer == 1 || answer == 0) {
            rsm.guardOn = 0;
            rsm.offWanted = 0;
            resumeDrop(&rsm.onSeq);
            resumeLog(answer ? "invulnerability off" : "invulnerability: hero dead, the game keeps the flags");
        }
    }
    if (rsm.guardOn && !rsm.offWanted) {
        int attack = mode == 7 || mode == 8 || (mode >= 10 && mode <= 16) || mode == 18;


        if ((int) (now - rsm.guardUntil) > 0 || (int) (now - rsm.guardHard) > 0 || attack || mode == 0 || mode == 17) {
            rsm.offWanted = 1;
            rsm.offAskedAt = 0;
        }
    }
    if (rsm.exitOpen && (!rsm.guardOn || rsm.offWanted)) {

        rsm.exitOpen = 0;
        logLine("resume: way out no longer offered: the protection ended");
    }
    if (rsm.guardOn && rsm.offWanted) {

        if (!rsm.offSeq && now - rsm.offAskedAt >= RESUME_OFF_REPEAT_MS) {
            rsm.offAskedAt = now;
            rsm.offSeq = srvSubmit(SRV_CODE_INVUL_OFF, 0, 0);
        }
        else if (rsm.offSeq && now - rsm.offAskedAt >= 1000 && srvCancel(rsm.offSeq)) rsm.offSeq = 0;
    }
}


static void putResumeFields(Packet* p) {
    void* player = fGetPlayerUnit();
    long x = 0, y = 0;
    WORD rx = 0, ry = 0, rw = 0, rh = 0;
    int ok = player && singlePlayerGame() && resumeHeroAt(player, &x, &y) && resumeHeroRoom(player, &rx, &ry, &rw, &rh);
    putU16(p, ok ? (WORD) unitLevel(player) : 0);
    putU16(p, (WORD) x);
    putU16(p, (WORD) y);
    putU32(p, ok ? resumeSeed(player) : 0);
    putU16(p, rx);
    putU16(p, ry);
    putU16(p, rw);
    putU16(p, rh);
    putU8(p, resumeDifficulty >= 0 ? (BYTE) resumeDifficulty : 0xFF);
    putU8(p, (srvState == 1 ? 1 : 0) | (resumeFunctionsKnown() ? 6 : 0));
    putU8(p, rsm.state);
    putU8(p, rsm.reason);
    DWORD now = GetTickCount();
    long left = rsm.guardOn && !rsm.offWanted ? (long) (rsm.guardUntil - now) : 0;
    putU8(p, left > 0 ? (BYTE) (left / 100 > 255 ? 255 : left / 100) : 0);
}


static void putResumeExitFields(Packet* p) {
    if (resumeRegen < 0 && fGetPlayerUnit()) resumeRegenMap();


    putU8(p, (srvState == 1 && resumeFunctionsKnown() ? 1 : 0) | (resumeRegen > 0 ? 2 : 0));
    putU8(p, rsm.exitOpen ? rsm.offer : 0);
    putU8(p, rsm.exitOpen ? rsm.offerLevel : 0);
    putU8(p, rsm.exitState);
}
