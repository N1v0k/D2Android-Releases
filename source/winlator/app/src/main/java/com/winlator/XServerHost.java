package com.winlator;

import android.app.Activity;
import android.content.SharedPreferences;

import com.winlator.widget.XServerView;
import com.winlator.xserver.Window;
import com.winlator.xserver.XServer;

/**
 * D2Android: what the X server and WinHandler need from the activity that shows the game
 * (D2GameActivity). Replaces Winlator's XServerDisplayActivity in their constructors.
 */
public interface XServerHost {
    Activity getActivity();

    XServer getXServer();

    XServerView getXServerView();

    SharedPreferences getPreferences();

    default int getFrameRatingWindowId() {
        return -1;
    }

    default void changeFrameRatingVisibility(Window window, boolean visible) {}
}
