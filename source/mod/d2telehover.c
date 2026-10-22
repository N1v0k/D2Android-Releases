
#define TELE_SKILL 54
#define TELE_HOVER_RADIUS 8
#define TELE_UNIT_WARP 5

static DWORD teleHoverIgnored;
static DWORD teleHoverTracedId = 0xFFFFFFFF;

static int teleHoverDecide(int hooked, long sinceAsk, int found, int playable, int leftPressed, int leftSkill,
                           int rightPressed, int rightSkill, long mx, long my, long sx, long sy, int unitType) {
    if (!hooked || sinceAsk < 0 || sinceAsk > TELE_STALE_MS || !found || !playable) return 0;
    if (!(rightPressed && rightSkill == TELE_SKILL) && !(leftPressed && leftSkill == TELE_SKILL)) return 0;
    if (unitType < 0 || unitType == TELE_UNIT_WARP) return 0;
    long dx = mx - sx, dy = my - sy;
    return dx * dx + dy * dy <= (long) TELE_HOVER_RADIUS * TELE_HOVER_RADIUS;
}

static int teleHoverCapable(void) {
    return unitActions;
}

static void teleHoverButtons(int clickType, int keyLeft, int keyRight, int* left, int* right) {
    if (clickType < 0) {
        *left = keyLeft;
        *right = keyRight;
        return;
    }
    *left = clickType == 0 || clickType == 1;
    *right = clickType == 3 || clickType == 4;
}

static int teleportIgnoresHover(void* hovered) {
    if (!hovered || !teleActive) return 0;
    void* player = fGetPlayerUnit();
    if (!player) return 0;
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) info = NULL;
    int left = info ? skillId(RDP(info, INFO_LEFT)) : -1, right = info ? skillId(RDP(info, INFO_RIGHT)) : -1;
    int type = (int) RD32(hovered, U_TYPE);
    long mx = (long) RD32(client, RVA_MouseX), my = (long) RD32(client, RVA_MouseY);
    int leftPressed, rightPressed;
    teleHoverButtons(clickMapType, GetKeyState(VK_LBUTTON) < 0, GetKeyState(VK_RBUTTON) < 0, &leftPressed, &rightPressed);
    int ignore = teleHoverDecide(unitActions, (long) (GetTickCount() - teleAskedAt), teleAnsweredFound,
        singlePlayerGame() && !pauseApplied, leftPressed, left, rightPressed, right, mx, my, teleAnsweredSx, teleAnsweredSy, type);
    if (ignore) {
        teleHoverIgnored++;
        DWORD id = RD32(hovered, U_ID);
        if (id != teleHoverTracedId) {
            trace("teleport: click on the target %ld,%ld, unit type %d #%lu under it ignored (%lu so far)", teleAnsweredSx,
                teleAnsweredSy, type, (unsigned long) id, (unsigned long) teleHoverIgnored);
            teleHoverTracedId = id;
        }
    }
    return ignore;
}
