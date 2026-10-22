package com.winlator.d2;

final class D2RingChoice {
    static final int MAX_ALL = 8, PER_PAGE = 6, PAGED_PLACES = 8;
    static final int NONE = -1, BACK = -2, NEXT = -3;
    static final int PLACE_NEXT = 2, PLACE_BACK = 6;
    private static final int[] TARGET_PLACES = {0, 1, 3, 4, 5, 7};

    final int count;
    private int page;
    private int selected = NONE;
    private boolean armed = true;

    D2RingChoice(int count) {
        this.count = Math.max(0, count);
    }

    boolean paged() {
        return count > MAX_ALL;
    }

    int pages() {
        return paged() ? (count + PER_PAGE - 1) / PER_PAGE : 1;
    }

    int page() {
        return page;
    }

    int places() {
        return paged() ? PAGED_PLACES : count;
    }

    int[] onPage() {
        if (!paged()) return new int[]{0, count};
        int from = page * PER_PAGE;
        return new int[]{from, Math.min(count, from + PER_PAGE)};
    }

    int at(int place) {
        if (place < 0 || place >= places()) return NONE;
        if (!paged()) return place;
        if (place == PLACE_NEXT) return NEXT;
        if (place == PLACE_BACK) return BACK;
        for (int i = 0; i < TARGET_PLACES.length; i++) {
            if (TARGET_PLACES[i] != place) continue;
            int index = page * PER_PAGE + i;
            return index < count ? index : NONE;
        }
        return NONE;
    }

    int placeOf(float dx, float dy, float min) {
        int n = places();
        if (n <= 0 || Math.hypot(dx, dy) < min) return NONE;
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0) angle += 360;
        return (int) Math.round(angle / (360.0 / n)) % n;
    }

    int selected() {
        return selected;
    }

    int target() {
        return selected >= 0 ? selected : NONE;
    }

    int move(float dx, float dy, float min) {
        int place = placeOf(dx, dy, min);
        if (place == NONE) armed = true;
        int now = at(place);
        int result = 0;
        if ((now == NEXT || now == BACK) && armed) {
            turn(now == NEXT ? 1 : -1);
            armed = false;
            result = 2;
        }
        if (now != selected) {
            selected = now;
            if (result == 0) result = 1;
        }
        return result;
    }

    void turn(int direction) {
        if (!paged()) return;
        int n = pages();
        page = ((page + direction) % n + n) % n;
        selected = NONE;
    }
}
