package com.winlator.d2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;





public final class D2Quests {
    public static final int PKT_QUESTS = 9;

    public static final int HOEHLE_DES_BOESEN = 1;


    public final int schwierigkeit;
    private final int[] woerter;

    private D2Quests(int schwierigkeit, int[] woerter) {
        this.schwierigkeit = schwierigkeit;
        this.woerter = woerter;
    }


    public static D2Quests parse(byte[] data, int length) {
        if (data == null || length < 10 || length > data.length || (data[5] & 0xFF) != PKT_QUESTS) return null;
        int n = data[9] & 0xFF;
        if (length < 10 + 2 * n) return null;
        int[] w = new int[n];
        for (int i = 0; i < n; i++) w[i] = (data[10 + 2 * i] & 0xFF) | (data[11 + 2 * i] & 0xFF) << 8;
        return new D2Quests(data[8] & 0xFF, w);
    }


    public int wort(int i) {
        return i >= 0 && i < woerter.length ? woerter[i] : -1;
    }

    public int anzahl() {
        return woerter.length;
    }


    public int[] woerter() {
        return woerter.clone();
    }


    public static String json(D2Quests q, int seq, long now) {
        try {
            JSONArray a = new JSONArray();
            for (int w : q.woerter) a.put(w);
            return new JSONObject().put("t", "quest").put("ms", now).put("seq", seq).put("schwierigkeit", q.schwierigkeit)
                .put("woerter", a).toString();
        }
        catch (JSONException e) {
            return null;
        }
    }
}
