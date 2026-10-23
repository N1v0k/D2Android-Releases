package com.winlator.d2;





final class D2DeathTap {

    static final long LOCK_MS = 1000;




    static final long INFO_AFTER_ESC_MS = 300;

    private D2DeathTap() {
    }









    static boolean allowed(boolean waitsForEsc, int life, Boolean hardcore, long now, long screenSince, long lastEscAt,
                           long infoTime) {
        if (!waitsForEsc || life > 0 || screenSince == 0 || now - screenSince < LOCK_MS) return false;
        if (!Boolean.FALSE.equals(hardcore)) return false;
        return lastEscAt == 0 || now - lastEscAt >= LOCK_MS && infoTime - lastEscAt >= INFO_AFTER_ESC_MS;
    }
}
