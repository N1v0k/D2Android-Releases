package com.winlator.d2;

import java.util.Set;





public final class D2MenuRow {
    private D2MenuRow() {}





    public static String[] lines(String label, java.util.function.ToIntFunction<String> width, float max) {
        if (width.applyAsInt(label) <= max || label.indexOf(' ') < 0) return new String[]{label};
        int best = -1;
        for (int i = label.indexOf(' '); i >= 0; i = label.indexOf(' ', i + 1)) {
            if (best < 0 || Math.abs(i - label.length() / 2) < Math.abs(best - label.length() / 2)) best = i;
        }
        return new String[]{label.substring(0, best), label.substring(best + 1)};
    }


    public static final String QUESTS = "quests";






    public static boolean questNews(boolean edgeButtonsHidden, boolean questLogButton) {
        return edgeButtonsHidden && questLogButton;
    }


    public static boolean shownFolded(String id, boolean toggle, boolean pinned, boolean questNews) {
        return toggle || pinned || (questNews && QUESTS.equals(id));
    }








    public static float[] rightEdges(String[] ids, float[] halfWidths, int toggle, Set<String> pinned, boolean open,
                                     float right, float gap) {
        return rightEdges(ids, halfWidths, toggle, pinned, open, right, gap, -Float.MAX_VALUE, new int[ids.length]);
    }






    public static float[] rightEdges(String[] ids, float[] halfWidths, int toggle, Set<String> pinned, boolean open,
                                     float right, float gap, float left, int[] lines) {
        float[] edges = new float[ids.length];
        float x = right;
        int line = 0;
        int quests = -1;
        for (int i = 0; i < ids.length; i++) {
            boolean placed = open || i == toggle || pinned.contains(ids[i]);
            if (!placed && QUESTS.equals(ids[i])) {
                quests = i;
                continue;
            }
            if (placed && x < right && x - 2 * halfWidths[i] < left) {
                line++;
                x = right;
            }
            edges[i] = x;
            lines[i] = line;
            if (placed) x -= 2 * halfWidths[i] + gap;
        }
        if (quests >= 0) {
            if (x < right && x - 2 * halfWidths[quests] < left) {
                line++;
                x = right;
            }
            edges[quests] = x;
            lines[quests] = line;
        }
        return edges;
    }
}
