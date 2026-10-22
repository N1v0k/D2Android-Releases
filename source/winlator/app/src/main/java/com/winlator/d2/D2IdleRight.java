package com.winlator.d2;

final class D2IdleRight {
    interface Actions {
        boolean pressHotkey(int index);
        void selectRight(int skill);
        void trace(String text);
    }

    enum Result { NONE, DONE, FAILED }

    static final long CONFIRM_MS = 600;

    private int skill = -1, key = -1, cls = -1;
    private String name;
    private boolean pending, sent, viaKey, fallbackDone;
    private long until, sentAt, maxMs;

    void remember(int rightSkill, int characterClass, String characterName, int[] hotkeySkill, boolean[] hotkeyLeft) {
        pending = false;
        skill = rightSkill;
        cls = characterClass;
        name = characterName;
        key = -1;
        for (int i = 0; i < 16 && rightSkill >= 0; i++) {
            if (hotkeySkill[i] == rightSkill && !hotkeyLeft[i]) {
                key = i;
                break;
            }
        }
    }

    void owe(long now, long maxMs) {
        if (skill < 0) return;
        pending = true;
        sent = viaKey = fallbackDone = false;
        this.maxMs = maxMs;
        until = -1;
    }

    boolean pending() {
        return pending;
    }

    void takeover(Actions a) {
        if (!pending) return;
        pending = false;
        a.trace("right skill: not restored, the player took over");
    }

    static boolean ownTree(int characterClass, int skill) {
        int[] first = {6, 36, 66, 96, 126, 221, 251};
        return skill == 0 || (characterClass >= 0 && characterClass < 7 && skill >= first[characterClass] && skill < first[characterClass] + 30);
    }

    Result step(long now, boolean focused, boolean haveState, int rightSkill, int characterClass, String characterName,
                int[] hotkeySkill, boolean[] hotkeyLeft, Actions a) {
        if (!pending) return Result.NONE;
        if (focused && until < 0) until = now + maxMs;
        if (until >= 0 && now > until) {
            pending = false;
            a.trace("right skill: not restored, time up");
            return Result.NONE;
        }
        if (!focused || !haveState) return Result.NONE;
        if (characterClass != cls || (name != null && characterName != null && !name.equals(characterName))) {
            pending = false;
            a.trace("right skill: another character, nothing restored");
            return Result.NONE;
        }
        if (rightSkill == skill) {
            pending = false;
            a.trace("right skill " + skill + (sent ? " restored" + (viaKey && !fallbackDone ? " (F" + (key + 1) + ")" : " (command 13)") : " unchanged"));
            return Result.DONE;
        }
        boolean tree = ownTree(cls, skill);
        if (!sent) {
            sent = true;
            sentAt = now;
            if (key >= 0 && hotkeySkill[key] == skill && !hotkeyLeft[key] && a.pressHotkey(key)) {
                viaKey = true;
                a.trace("right skill " + skill + ": F" + (key + 1));
                return Result.NONE;
            }
            if (tree) {
                fallbackDone = true;
                a.selectRight(skill);
                a.trace("right skill " + skill + ": command 13");
                return Result.NONE;
            }
            pending = false;
            a.trace("right skill " + skill + ": no hotkey and not the class's own, not restored");
            return Result.FAILED;
        }
        if (now - sentAt < CONFIRM_MS) return Result.NONE;
        if (viaKey && !fallbackDone && tree) {
            fallbackDone = true;
            sentAt = now;
            a.selectRight(skill);
            a.trace("right skill " + skill + ": F" + (key + 1) + " not taken, command 13");
            return Result.NONE;
        }
        pending = false;
        a.trace("right skill " + skill + ": not taken, not restored (now " + rightSkill + ")");
        return Result.FAILED;
    }
}
