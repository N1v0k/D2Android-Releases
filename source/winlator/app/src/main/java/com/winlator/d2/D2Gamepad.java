package com.winlator.d2;








public final class D2Gamepad {
    private D2Gamepad() {}


    public static final int A = 0, B = 1, X = 2, Y = 3, LB = 4, RB = 5, VIEW = 6, START = 7, L3 = 8, R3 = 9, LT = 10, RT = 11,
        DPAD_LEFT = 12, DPAD_RIGHT = 13, DPAD_DOWN = 14, DPAD_UP = 15, COUNT = 16, NONE = -1;


    public static final String[] NAMES = {"A", "B", "X", "Y", "LB", "RB", "View", "Start", "L3", "R3", "LT", "RT",
        "←", "→", "↓", "↑"};



    static final int KEYCODE_BACK = 4, KEYCODE_DPAD_UP = 19, KEYCODE_DPAD_DOWN = 20, KEYCODE_DPAD_LEFT = 21,
        KEYCODE_DPAD_RIGHT = 22, KEYCODE_BUTTON_A = 96, KEYCODE_BUTTON_B = 97, KEYCODE_BUTTON_X = 99,
        KEYCODE_BUTTON_Y = 100, KEYCODE_BUTTON_L1 = 102, KEYCODE_BUTTON_R1 = 103, KEYCODE_BUTTON_L2 = 104,
        KEYCODE_BUTTON_R2 = 105, KEYCODE_BUTTON_THUMBL = 106, KEYCODE_BUTTON_THUMBR = 107, KEYCODE_BUTTON_START = 108,
        KEYCODE_BUTTON_SELECT = 109;

    static final int FLAG_FALLBACK = 0x400;






    public static int buttonOf(int keyCode, int flags) {
        if ((flags & FLAG_FALLBACK) != 0) return NONE;
        switch (keyCode) {
            case KEYCODE_BUTTON_A: return A;
            case KEYCODE_BUTTON_B: return B;
            case KEYCODE_BUTTON_X: return X;
            case KEYCODE_BUTTON_Y: return Y;
            case KEYCODE_BUTTON_L1: return LB;
            case KEYCODE_BUTTON_R1: return RB;
            case KEYCODE_BUTTON_L2: return LT;
            case KEYCODE_BUTTON_R2: return RT;
            case KEYCODE_BUTTON_THUMBL: return L3;
            case KEYCODE_BUTTON_THUMBR: return R3;
            case KEYCODE_BUTTON_START: return START;
            case KEYCODE_BUTTON_SELECT: case KEYCODE_BACK: return VIEW;
            case KEYCODE_DPAD_LEFT: return DPAD_LEFT;
            case KEYCODE_DPAD_RIGHT: return DPAD_RIGHT;
            case KEYCODE_DPAD_DOWN: return DPAD_DOWN;
            case KEYCODE_DPAD_UP: return DPAD_UP;
            default: return NONE;
        }
    }


    public static int beltRow(int button) {
        return button >= DPAD_LEFT && button <= DPAD_UP ? button - DPAD_LEFT : -1;
    }




    public static final int[] ASSIGNABLE = {A, X, B, Y, RB, RT, LB, L3, R3, VIEW};


    public static int defaultAction(int button, boolean layer2) {
        switch (button) {
            case A: return D2TriggerAction.ATTACK_OR_TAKE;
            case X: return layer2 ? D2TriggerAction.SKILL_F1 + 5 : D2TriggerAction.SKILL_F1;
            case B: return layer2 ? D2TriggerAction.SKILL_F1 + 6 : D2TriggerAction.SKILL_F1 + 1;
            case Y: return layer2 ? D2TriggerAction.SKILL_F8 : D2TriggerAction.SKILL_F1 + 2;
            case RB: return layer2 ? D2TriggerAction.PORTAL : D2TriggerAction.SKILL_F1 + 3;
            case RT: return layer2 ? D2TriggerAction.TAKE : D2TriggerAction.SKILL_F1 + 4;
            case LB: return D2TriggerAction.NAMES;
            case L3: return D2TriggerAction.RUN_TOGGLE;
            case R3: return D2TriggerAction.WEAPON_SWAP;
            case VIEW: return D2TriggerAction.INVENTORY;
            default: return D2TriggerAction.NOTHING;
        }
    }


    public static String slot(int button, boolean layer2) {
        return (layer2 ? "LT+" : "") + NAMES[button];
    }





    public static int[][] parseMap(String text) {
        int[][] map = new int[2][COUNT];
        for (int layer = 0; layer < 2; layer++) {
            for (int b = 0; b < COUNT; b++) map[layer][b] = defaultAction(b, layer == 1);
        }
        if (text == null) return map;
        for (String part : text.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String name = part.substring(0, eq).trim();
            boolean layer2 = name.startsWith("LT+");
            if (layer2) name = name.substring(3);
            int button = -1;
            for (int b : ASSIGNABLE) if (NAMES[b].equals(name)) button = b;
            if (button < 0) continue;
            int value;
            try {
                value = Integer.parseInt(part.substring(eq + 1).trim());
            }
            catch (NumberFormatException e) {
                continue;
            }
            int layer = layer2 ? 1 : 0;
            map[layer][button] = D2TriggerAction.ofPad(value, map[layer][button]);
        }
        return map;
    }


    public static String formatMap(int[][] map) {
        StringBuilder sb = new StringBuilder();
        for (int layer = 0; layer < 2; layer++) {
            for (int b : ASSIGNABLE) {
                if (map[layer][b] == defaultAction(b, layer == 1)) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(slot(b, layer == 1)).append('=').append(map[layer][b]);
            }
        }
        return sb.toString();
    }




    public static final int DEADZONE_MIN = 5, DEADZONE_MAX = 40, DEADZONE_DEFAULT = 15;

    static final float TURN_FROM = 0.25f, TURN_DEGREES = 8f;





    public static final class Stick {
        public boolean active, hasDirection;
        public float dirX = 1, dirY = 0;


        public boolean update(float x, float y, int deadzonePct) {
            float len = (float) Math.hypot(x, y);
            boolean wasActive = active;
            active = len > deadzonePct / 100f;
            if (!active) {
                hasDirection = false;
                return wasActive;
            }
            if (len < Math.max(TURN_FROM, deadzonePct / 100f)) return !wasActive;
            float nx = x / len, ny = y / len;
            if (hasDirection) {
                double cos = nx * dirX + ny * dirY;
                if (cos >= Math.cos(Math.toRadians(TURN_DEGREES))) return !wasActive;
            }
            dirX = nx;
            dirY = ny;
            hasDirection = true;
            return true;
        }

        public void reset() {
            active = hasDirection = false;
        }
    }


    public static boolean triggerDown(boolean wasDown, float value) {
        return wasDown ? value > 0.3f : value >= 0.5f;
    }




    public static final long LONG_MS = 700;





    public static int shortAction(int button) {
        return button == START ? D2TriggerAction.ESC_MENU : button == VIEW ? D2TriggerAction.INVENTORY : D2TriggerAction.NOTHING;
    }

    public static int longAction(int button) {
        return button == START ? D2TriggerAction.APP_MENU : D2TriggerAction.NOTHING;
    }







    public static final int WHEEL_INVENTORY = 0, WHEEL_CHARACTER = 1, WHEEL_SKILLS = 2, WHEEL_MAP = 3, WHEEL_QUESTS = 4,
        WHEEL_MERC = 5, WHEEL_PAUSE = 6, WHEEL_COUNT = 7;


    public static int wheelEntry(float x, float y, int deadzonePct) {
        if (Math.hypot(x, y) <= Math.max(0.2f, deadzonePct / 100f)) return -1;
        double angle = Math.toDegrees(Math.atan2(x, -y));
        if (angle < 0) angle += 360;
        double sector = 360.0 / WHEEL_COUNT;
        return (int) ((angle + sector / 2) / sector) % WHEEL_COUNT;
    }





    public static boolean sortsWhenHeld(int button, boolean inventoryOpen) {
        return button == R3 && inventoryOpen;
    }


    public static boolean hasLongPress(int button) {
        return button == START || button == VIEW;
    }




    public static final int A_SKIP_TALK = 0, A_TAKE_LINE = 1, A_ATTACK = 2, A_TAKE = 3;












    public static int attackOrTake(boolean npcTalking, boolean npcMenu, boolean inTown, boolean aimOn, boolean attackRight,
                                   boolean monsterPicked, boolean monsterInAttackReach, boolean takeClose) {
        if (npcTalking) return A_SKIP_TALK;
        if (npcMenu) return A_TAKE_LINE;
        if (inTown) return takeClose ? A_TAKE : A_ATTACK;
        if (monsterPicked) return A_ATTACK;
        if (aimOn && !attackRight && !monsterInAttackReach && takeClose) return A_TAKE;
        return A_ATTACK;
    }


    public static final float TAKE_REACH_OF_HEIGHT = 0.55f;







    public static boolean aTakes(double isoDistance, int gameHeight) {
        return isoDistance <= gameHeight * TAKE_REACH_OF_HEIGHT;
    }








    public static final int CURSOR_SPEED_MIN = 50, CURSOR_SPEED_MAX = 200, CURSOR_SPEED_DEFAULT = 80;
    static final long POINTER_CROSS_MS = 1200;

    static final long BOOST_AFTER_MS = 400, BOOST_RAMP_MS = 600;
    static final float BOOST = 1.5f, FULL = 0.95f, RIGHT_STICK = 0.25f;

    public static final long WALK_AFTER_POINTER_MS = 150;








    public static boolean pointerMode(boolean stateFresh, boolean controlsVisible, long panels, boolean npcMenu,
                                      boolean cursorTaken, boolean assigning, boolean editMode, boolean paused) {
        if (editMode || paused) return false;
        return !stateFresh || !controlsVisible || panels != 0 || npcMenu || cursorTaken || assigning;
    }






    public static float pointerSpeed(float m, int deadzonePct, float vmax, long fullForMs, boolean rightStick) {
        float dz = deadzonePct / 100f;
        if (m <= dz) return 0;
        float t = Math.min(1f, (m - dz) / (1f - dz));
        float v = vmax * t * t;
        if (fullForMs > BOOST_AFTER_MS) v *= 1f + (BOOST - 1f) * Math.min(1f, (fullForMs - BOOST_AFTER_MS) / (float) BOOST_RAMP_MS);
        return rightStick ? v * RIGHT_STICK : v;
    }


    public static float pointerVmax(int screenWidth, int speedPct) {
        return screenWidth * 1000f / POINTER_CROSS_MS * speedPct / 100f;
    }


    public static boolean full(float m) {
        return m >= FULL;
    }







    public static final int ESC_MENU_X = 400, ESC_MENU_HALF_W = 245, ESC_MENU_HALF_H = 22;
    public static final int[] ESC_MENU_Y = {210, 260, 310};
    public static final int ESC_RETURN = 2;






    public static boolean escMenuOpened(boolean wasOpen, boolean open, boolean ownPause) {
        return open && !wasOpen && !ownPause;
    }





    public static float[] escMenuPoint(int entry, int gameW, int gameH) {
        return new float[]{(gameW - 800) / 2f + ESC_MENU_X, (gameH - 600) / 2f + ESC_MENU_Y[entry]};
    }


    public static int escMenuEntryAt(float gx, float gy, int gameW, int gameH) {
        for (int i = 0; i < ESC_MENU_Y.length; i++) {
            float[] c = escMenuPoint(i, gameW, gameH);
            if (Math.abs(gx - c[0]) <= ESC_MENU_HALF_W && Math.abs(gy - c[1]) <= ESC_MENU_HALF_H) return i;
        }
        return -1;
    }





    public static int escMenuStep(int current, int dir) {
        if (current < 0) return ESC_RETURN;
        return Math.max(0, Math.min(ESC_MENU_Y.length - 1, current + dir));
    }


    public static final int CLICK_NONE = 0, CLICK_LEFT = 1, CLICK_RIGHT = 2, CLICK_ESC = 3;

    public static final int CLICK_SELL = 4;

    static final int PLACE_INVENTORY = 0;







    public static int padShopClick(String label, int click, boolean shopOpen, int hoverPlace, int hoverX, int hoverY,
            boolean sellPossible) {
        if (shopClick(click, shopOpen, hoverPlace) == CLICK_SELL) {
            if (sellPossible && hoverX >= 0 && hoverY >= 0) {
                D2Trace.log("pad " + label + ": sell cell " + hoverX + "," + hoverY + " (mod)");
                return CLICK_SELL;
            }
            D2Trace.log("pad " + label + ": sell not possible (no sell by packet)");
            return CLICK_NONE;
        }

        if (click != CLICK_LEFT && shopOpen && hoverPlace < 0) D2Trace.log("pad " + label + ": right click in a shop (buy)");
        return click;
    }


    public static final int CMD_SELL = 30;





    public static byte[] sellPayload(int x, int y, int request) {
        return new byte[]{0, (byte) x, (byte) y, 0, (byte) request, (byte) (request >> 8), (byte) (request >> 16), (byte) (request >> 24)};
    }








    public static int shopClick(int click, boolean shopOpen, int hoverPlace) {
        return click == CLICK_RIGHT && shopOpen && hoverPlace == PLACE_INVENTORY ? CLICK_SELL : click;
    }





    public static float[] pointerView(int screenX, int screenY, float aspect, float offsetX, float offsetY) {
        return new float[]{screenX * aspect + offsetX, screenY * aspect + offsetY};
    }


    public static boolean ownButtonTakes(int click) {
        return click == CLICK_LEFT || click == CLICK_RIGHT;
    }

    public static int pointerClick(int button) {
        switch (button) {
            case A: return CLICK_LEFT;
            case B: case X: return CLICK_RIGHT;
            case Y: return CLICK_ESC;
            default: return CLICK_NONE;
        }
    }








    public static boolean holdOpensRing(int resolvedAction, boolean inTown, boolean takeClose, boolean npcTalking, boolean npcMenu) {
        if (npcTalking || npcMenu) return false;
        if (resolvedAction == D2TriggerAction.TAKE) return true;
        return resolvedAction == D2TriggerAction.ATTACK_OR_TAKE && inTown && takeClose;
    }






    public static float[] ringPoint(float x, float y, int deadzonePct) {
        float m = (float) Math.hypot(x, y);
        if (m <= deadzonePct / 100f) return new float[]{0, 0};
        float k = Math.min(1f, m) / m;
        return new float[]{x * k, y * k};
    }




    public static final int OWNER_NONE = 0, OWNER_FINGER = 1, OWNER_PAD = 2;






    public static boolean padMayWalk(boolean controlsVisible, boolean editMode, boolean paused, boolean assigning,
                                     boolean tour, int owner) {
        return controlsVisible && !editMode && !paused && !assigning && !tour && owner != OWNER_FINGER;
    }





    public static boolean hidesControls(boolean padActive, boolean editMode, boolean idleRuns, boolean playtester, boolean tour) {
        return padActive && !editMode && !idleRuns && !playtester && !tour;
    }


    public static boolean touchEndsPad(int deviceId, int idleDeviceId, boolean playtester) {
        return deviceId != idleDeviceId && !playtester;
    }





    public static boolean firstTouchSwallowed(boolean inGame, boolean windowOpen, boolean onMenuButton) {
        return inGame && !windowOpen && !onMenuButton;
    }
}
