package com.winlator.d2;

import java.util.Arrays;





public final class D2InteractHint {
    private D2InteractHint() {}





    public static boolean swipeSwitches(boolean npcMenuOpen, boolean npcTalking) {
        return npcMenuOpen || npcTalking;
    }





    public static int menuLines(int sentLines) {
        return sentLines > 0 ? sentLines + 1 : 0;
    }





    public static int menuLine(int line, int direction, int lines) {
        if (lines <= 0) return 0;
        return ((line + direction) % lines + lines) % lines;
    }


    public static final String ARROWS = "< >";







    public static final class MenuCount {
        private int line;
        private boolean known = true;
        private int[] ids;


        public void update(boolean open, int[] sentIds, boolean shopOrTalk) {
            if (!open) {
                reset(null);
                return;
            }
            if (ids == null || !Arrays.equals(ids, sentIds)) {
                reset(sentIds);
                return;
            }
            if (shopOrTalk) line = 0;
        }

        private void reset(int[] sentIds) {
            line = 0;
            known = true;
            ids = sentIds == null ? null : sentIds.clone();
        }


        public void swipe(int direction) {
            line = menuLine(line, direction, lines());
        }


        public void chose(int talkId) {
            if (ids != null && line < ids.length && ids[line] == talkId) known = false;
        }

        public int line() {
            return line;
        }


        public int lines() {
            return menuLines(ids == null ? 0 : ids.length);
        }


        public boolean known() {
            return known;
        }
    }





    public static String text(int index, int count, boolean swipeSwitches) {
        if (count <= 1) return null;
        String n = (index + 1) + "/" + count;
        return swipeSwitches ? "< " + n + " >" : n;
    }
}
