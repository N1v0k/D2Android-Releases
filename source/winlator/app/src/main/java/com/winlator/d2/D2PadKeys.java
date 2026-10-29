package com.winlator.d2;






final class D2PadKeys {
    private D2PadKeys() {}

    static final int NONE = 0, UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4, A = 5, B = 6, X = 7, Y = 8, LB = 9, RB = 10,
        START = 11, VIEW = 12;


    static final int KEYCODE_BACK = 4, KEYCODE_DPAD_UP = 19, KEYCODE_DPAD_DOWN = 20, KEYCODE_DPAD_LEFT = 21,
        KEYCODE_DPAD_RIGHT = 22, KEYCODE_BUTTON_A = 96, KEYCODE_BUTTON_B = 97, KEYCODE_BUTTON_X = 99,
        KEYCODE_BUTTON_Y = 100, KEYCODE_BUTTON_L1 = 102, KEYCODE_BUTTON_R1 = 103, KEYCODE_BUTTON_START = 108,
        KEYCODE_BUTTON_SELECT = 109, KEYCODE_MENU = 82;
    static final int FLAG_FALLBACK = 0x400;
    static final int SOURCE_DPAD = 0x00000201, SOURCE_GAMEPAD = 0x00000401, SOURCE_JOYSTICK = 0x01000010;


    static boolean fromPad(int source) {
        return (source & SOURCE_GAMEPAD) == SOURCE_GAMEPAD || (source & SOURCE_JOYSTICK) == SOURCE_JOYSTICK
            || (source & SOURCE_DPAD) == SOURCE_DPAD;
    }


    static int action(int keyCode) {
        switch (keyCode) {
            case KEYCODE_DPAD_UP: return UP;
            case KEYCODE_DPAD_DOWN: return DOWN;
            case KEYCODE_DPAD_LEFT: return LEFT;
            case KEYCODE_DPAD_RIGHT: return RIGHT;
            case KEYCODE_BUTTON_A: return A;

            case KEYCODE_BUTTON_B: case KEYCODE_BACK: return B;
            case KEYCODE_BUTTON_X: return X;
            case KEYCODE_BUTTON_Y: return Y;
            case KEYCODE_BUTTON_L1: return LB;
            case KEYCODE_BUTTON_R1: return RB;
            case KEYCODE_BUTTON_START: case KEYCODE_MENU: return START;
            case KEYCODE_BUTTON_SELECT: return VIEW;
            default: return NONE;
        }
    }





    static int ours(int keyCode, int source, int flags) {
        if (!fromPad(source)) return NONE;
        int action = action(keyCode);

        if ((flags & FLAG_FALLBACK) != 0 && !direction(action)) return NONE;
        return action;
    }

    static boolean direction(int action) {
        return action >= UP && action <= RIGHT;
    }
}
