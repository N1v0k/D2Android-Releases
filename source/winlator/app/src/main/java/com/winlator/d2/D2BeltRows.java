package com.winlator.d2;









public final class D2BeltRows {

    static final int LEFT = 24, RIGHT = 145;




    static final int ROW_TOP = 38, ROW_BOTTOM = 10, ROW_HIGH = 31;

    private boolean open;






    public boolean step(int mouseX, int mouseY, int gameWidth, int gameHeight, int rows) {
        if (gameWidth <= 0 || gameHeight <= 0 || rows <= 1) {
            open = false;
            return false;
        }
        int middle = gameWidth / 2;
        boolean column = mouseX >= middle + LEFT && mouseX <= middle + RIGHT;
        boolean bottomRow = column && mouseY >= gameHeight - ROW_TOP && mouseY <= gameHeight - ROW_BOTTOM;
        int top = gameHeight - ROW_BOTTOM - Math.min(rows, 4) * ROW_HIGH;
        boolean unfolded = column && mouseY >= top && mouseY <= gameHeight - ROW_BOTTOM;
        open = bottomRow || (open && unfolded);
        return open;
    }


    public boolean open() {
        return open;
    }


    public void reset() {
        open = false;
    }
}
