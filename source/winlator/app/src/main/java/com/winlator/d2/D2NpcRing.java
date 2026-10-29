package com.winlator.d2;







final class D2NpcRing {
    private D2NpcRing() {}


    static boolean closeOnMenuGone(boolean linesShown, boolean menuOpen) {
        return linesShown && !menuOpen;
    }






    static boolean closeOnOtherLines(int[] shownIds, String[] shownTexts, int[] ids, String[] texts) {
        return shownIds != null && (!java.util.Arrays.equals(shownIds, ids) || !java.util.Arrays.equals(shownTexts, texts));
    }






    static final int RESURRECT_ID = 0x1507;


    static boolean isResurrect(int id) {
        return id == RESURRECT_ID;
    }


    static boolean isUnit(Object entry) {
        return entry instanceof D2ModState.Monster || entry instanceof D2ModState.Thing;
    }


    static Object highlightTarget(Object ringChoice) {
        return isUnit(ringChoice) ? ringChoice : null;
    }


    static final long FOLLOW_MS = 1500;


    static boolean isSubList(int[] ids) {
        return ids != null && ids.length > 0 && ids[0] == 0;
    }






    static final class Follow {
        private int[] ids;
        private long until;


        void chose(int[] menuIds, long now) {
            ids = menuIds == null ? new int[0] : menuIds.clone();
            until = now + FOLLOW_MS;
        }

        void cancel() {
            until = 0;
        }

        boolean waiting() {
            return until != 0;
        }


        boolean due(boolean menuOpen, boolean talking, int[] menuIds, long now) {
            if (until == 0) return false;
            if (now > until || talking || !menuOpen) {
                until = 0;
                return false;
            }
            if (menuIds == null || menuIds.length == 0 || java.util.Arrays.equals(ids, menuIds)) return false;
            until = 0;
            return true;
        }
    }





    static final class EndTalk {
        private long until;

        void start(long now) {
            until = now + FOLLOW_MS;
        }


        boolean escNow(boolean menuOpen, int[] menuIds, long now) {
            if (until == 0) return false;
            if (now > until || !menuOpen) {
                until = 0;
                return false;
            }
            if (menuIds == null || menuIds.length == 0 || isSubList(menuIds)) return false;
            until = 0;
            return true;
        }
    }
}
