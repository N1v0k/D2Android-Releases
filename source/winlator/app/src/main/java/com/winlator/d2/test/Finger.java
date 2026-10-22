package com.winlator.d2.test;

public interface Finger {
    boolean isDown(int finger);

    void down(int finger, float x, float y);

    void move(int finger, float x, float y);

    void up(int finger);

    void releaseAll();
}
