package com.winlator.d2;








public final class D2SkillSets {

    public static final int PLACES = 4;

    public static final int SETS = 2;

    private int set;


    public int set() {
        return set;
    }


    public int toggle() {
        set = (set + 1) % SETS;
        return set;
    }


    public void reset() {
        set = 0;
    }


    public int keyIndex(int place) {
        return keyIndex(place, set);
    }


    public static int keyIndex(int place, int set) {
        if (place < 0 || place >= PLACES || set < 0 || set >= SETS) return -1;
        return place + PLACES * set;
    }


    public String caption() {
        return Integer.toString(set + 1);
    }
}
