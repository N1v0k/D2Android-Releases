package com.winlator.d2;

final class D2IdleUntold {
    private final long maxAgeMs;
    private String text;
    private long at;

    D2IdleUntold(long maxAgeMs) {
        this.maxAgeMs = maxAgeMs;
    }

    void queue(String text, long now) {
        this.text = text;
        this.at = now;
    }

    String take(long now) {
        String t = text;
        text = null;
        return t != null && now - at < maxAgeMs ? t : null;
    }
}
