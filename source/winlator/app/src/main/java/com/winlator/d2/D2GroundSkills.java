package com.winlator.d2;

final class D2GroundSkills {
    private D2GroundSkills() {}

    static final int TELEPORT = 54;

    static boolean aimsAtGround(int skill) {
        return skill == TELEPORT;
    }
}
