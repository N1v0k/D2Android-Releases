package com.winlator.d2;

import java.util.List;









final class D2RezeptRaster {

    static final int PANEL_FROM_RIGHT = 400, PANEL_TOP = 60;

    static final int GRID_LEFT = 19, GRID_TOP_SMALL = 255, GRID_TOP_BIG = 145, COLUMNS = 10, ROWS_SMALL = 4, ROWS_BIG = 8,
        CELL = 29;

    private D2RezeptRaster() {}


    static int gridTop(int columns, int rows) {
        if (columns != COLUMNS) return -1;
        return rows == ROWS_SMALL ? GRID_TOP_SMALL : rows == ROWS_BIG ? GRID_TOP_BIG : -1;
    }





    static int[] cell(float gx, float gy, int gameWidth, int ox, int oy, int columns, int rows) {
        int top = gridTop(columns, rows);
        if (top < 0) return null;
        float left = gameWidth - ox - PANEL_FROM_RIGHT + GRID_LEFT, y0 = oy + PANEL_TOP + top;
        if (gx < left || gx >= left + columns * CELL || gy < y0 || gy >= y0 + rows * CELL) return null;
        return new int[]{(int) ((gx - left) / CELL), (int) ((gy - y0) / CELL)};
    }


    static D2RezeptBestand.Stueck itemAt(List<D2RezeptBestand.Stueck> items, float gx, float gy, int gameWidth, int ox,
                                         int oy, int columns, int rows) {
        int[] c = items == null ? null : cell(gx, gy, gameWidth, ox, oy, columns, rows);
        if (c == null) return null;
        for (D2RezeptBestand.Stueck s : items) if (s.place == D2RezeptBestand.PLACE_INVENTORY && s.covers(c[0], c[1])) return s;
        return null;
    }
}
