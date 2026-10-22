package com.winlator.d2;

final class D2NavInset {
    private D2NavInset() {}

    static int rightShift(boolean imeVisible, int navRight) {
        return imeVisible && navRight > 0 ? navRight : 0;
    }

    static float moveX(int oldShift, int newShift) {
        return oldShift - newShift;
    }
}
