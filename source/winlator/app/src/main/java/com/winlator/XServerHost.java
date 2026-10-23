package com.winlator;

import android.app.Activity;
import android.content.SharedPreferences;

import com.winlator.widget.XServerView;
import com.winlator.xserver.Window;
import com.winlator.xserver.XServer;





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
