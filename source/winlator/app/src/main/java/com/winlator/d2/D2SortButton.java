package com.winlator.d2;

final class D2SortButton {
    private D2SortButton() {}

    static boolean visible(boolean inventoryOpen, boolean blockingWindow, boolean capable, boolean singlePlayer,
                           boolean paused, boolean sortBusy, boolean cursorTaken) {
        return inventoryOpen && !blockingWindow && capable && singlePlayer && !paused && !sortBusy && !cursorTaken;
    }
}
