package com.winlator.d2;

import com.winlator.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.winlator.XServerHost;
import com.winlator.alsaserver.ALSAClient;
import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.renderer.GLRenderer;
import com.winlator.widget.XServerView;
import com.winlator.winhandler.WinHandler;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.XEnvironment;
import com.winlator.xenvironment.components.ALSAServerComponent;
import com.winlator.xenvironment.components.NetworkInfoUpdateComponent;
import com.winlator.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.xenvironment.components.XServerComponent;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XServer;

import java.io.File;









public class D2GuestScreen implements XServerHost {
    private final Activity activity;
    private final SharedPreferences preferences;
    private final FrameLayout root;
    private final WinHandler winHandler = new WinHandler(this);
    private final XServer xServer;
    private final XServerView xServerView;
    private final TextView loadingView;
    private final boolean audio;
    private XEnvironment environment;

    private D2GuestComponent guest;





    public D2GuestScreen(Activity activity, FrameLayout root, boolean audio, boolean relativeMouse) {
        this.activity = activity;
        this.root = root;
        this.audio = audio;
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        xServer = new XServer(this, new ScreenInfo(D2Runtime.screenSize(activity)));
        xServer.setWinHandler(winHandler);
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            private boolean firstWindow = true;

            @Override
            public void onMapWindow(Window window) {
                if (firstWindow && window.isRenderable() && !window.getClassName().isEmpty()) {
                    firstWindow = false;
                    xServerView.getRenderer().setCursorVisible(true);
                    activity.runOnUiThread(() -> loadingView.setVisibility(View.GONE));
                }

                if (window.attributes.isViewable() && window.isDesktopWindow()) {
                    window.attributes.setViewable(false);
                    if (window.attributes.isEnabled()) window.disableAllDescendants();
                }
            }
        });

        xServerView = new XServerView(activity, xServer);
        GLRenderer renderer = xServerView.getRenderer();
        renderer.setCursorVisible(false);
        xServer.setRenderer(renderer);
        root.addView(xServerView);
        xServer.setRelativeMouseMovement(relativeMouse);

        loadingView = new TextView(activity);
        loadingView.setText(D2Text.get(activity, R.string.launch_starting));
        loadingView.setTextColor(D2Style.GOLD);
        loadingView.setTextSize(18);
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setBackgroundColor(Color.BLACK);
        root.addView(loadingView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        int bannerMargin = Math.round(6 * activity.getResources().getDisplayMetrics().density);
        bannerParams.setMargins(bannerMargin, bannerMargin, 0, 0);
        if (com.winlator.BuildConfig.D2_INTERNAL) root.addView(new D2TestBanner(activity), bannerParams);
    }


    public void addOverlay(View overlay) {
        root.addView(overlay, root.indexOfChild(xServerView) + 1);
    }


    public TextView getLoadingView() {
        return loadingView;
    }

    public WinHandler getWinHandler() {
        return winHandler;
    }





    public void start(String command, EnvVars env, String stdinText, Callback<Integer> onEnded) {
        start(command, env, stdinText, onEnded, null);
    }


    public void start(String command, EnvVars env, String stdinText, Callback<Integer> onEnded, File outputFile) {
        File rootDir = D2Runtime.rootDir(activity);
        String rootPath = rootDir.getPath();
        environment = new XEnvironment(activity, RootFS.find(activity));
        environment.addComponent(new SysVSharedMemoryComponent(xServer, UnixSocketConfig.create(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH)));
        environment.addComponent(new XServerComponent(xServer, UnixSocketConfig.create(rootPath, UnixSocketConfig.XSERVER_PATH)));
        environment.addComponent(new NetworkInfoUpdateComponent());
        if (audio) environment.addComponent(new ALSAServerComponent(UnixSocketConfig.create(rootPath, UnixSocketConfig.ALSA_SERVER_PATH), new ALSAClient.Options()));
        guest = new D2GuestComponent(rootDir, command, env, onEnded, stdinText, outputFile);
        environment.addComponent(guest);
        environment.startEnvironmentComponents();
        winHandler.start();
    }

    public boolean isStarted() {
        return environment != null;
    }


    public void resume() {
        if (environment == null) return;
        xServerView.onResume();
        environment.onResume();
    }


    public void pauseGuest() {
        if (environment != null) environment.onPause();
    }





    public void resumeGuest() {
        if (environment != null) environment.onResume();
    }


    public void pauseView() {
        if (environment != null) xServerView.onPause();
    }


    public void destroy() {
        winHandler.stop();
        if (environment != null) environment.stopEnvironmentComponents();
    }


    public void destroy(String reason) {
        if (guest != null) guest.setStopReason(reason);
        destroy();
    }





    public void destroy(String reason, D2ExitGuard guard) {
        if (guest != null) guest.setStopReason(reason);
        guard.step("WinHandler");
        winHandler.stop();
        if (environment == null) return;
        for (com.winlator.xenvironment.EnvironmentComponent component : environment) {
            guard.step(component.getClass().getSimpleName());
            component.stop();
        }
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return winHandler.onGenericMotionEvent(event);
    }

    public boolean onKeyEvent(KeyEvent event) {
        return !winHandler.onKeyEvent(event) && xServer.keyboard.onKeyEvent(event);
    }



    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public XServer getXServer() {
        return xServer;
    }

    @Override
    public XServerView getXServerView() {
        return xServerView;
    }

    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }
}
