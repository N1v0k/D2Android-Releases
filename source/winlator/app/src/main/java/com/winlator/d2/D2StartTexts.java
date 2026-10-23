package com.winlator.d2;


final class D2StartTexts {
    static final int NOW = 0, MINUTES = 1, HOURS = 2, YESTERDAY = 3, DAYS = 4, WEEKS = 5, MONTHS = 6;

    private D2StartTexts() {}


    static int[] ago(long now, long then) {
        long minutes = Math.max(0, now - then) / 60_000;
        if (minutes < 1) return new int[]{NOW, 0};
        if (minutes < 60) return new int[]{MINUTES, (int) minutes};
        long hours = minutes / 60;
        if (hours < 24) return new int[]{HOURS, (int) hours};
        long days = hours / 24;
        if (days < 2) return new int[]{YESTERDAY, 1};
        if (days < 7) return new int[]{DAYS, (int) days};
        if (days < 30) return new int[]{WEEKS, (int) (days / 7)};
        return new int[]{MONTHS, (int) Math.max(1, days / 30)};
    }
}
