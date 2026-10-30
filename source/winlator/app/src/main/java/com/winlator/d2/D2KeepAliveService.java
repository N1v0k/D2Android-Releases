package com.winlator.d2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.winlator.R;










public class D2KeepAliveService extends Service {
    private static final String TAG = "D2KeepAlive";
    public static final String CHANNEL_ID = "d2_game";

    private static final String[] OLD_CHANNEL_IDS = {"winlator_foreground_service", "d2_background"};
    private static final int NOTIFICATION_ID = 2;

    public static final String EXTRA_SAVE_AND_EXIT = "d2_save_and_exit";




    public static final String EXTRA_ONLY_SAVED = "d2_nur_gespeichert";




    public static final String EXTRA_THEN_INSTALL = "d2_then_install";





    public static final String ACTION_SAVE_AND_EXIT = "at.d2android.action.SAVE_AND_EXIT";
    private static final String EXTRA_PAUSED = "paused";



    private static volatile boolean shown;
    private static volatile boolean shownPaused;


    public static void onLanguageChanged(Context context) {
        if (shown) start(context, shownPaused);
    }

    public static void start(Context context, boolean paused) {
        Intent intent = new Intent(context, D2KeepAliveService.class).putExtra(EXTRA_PAUSED, paused);
        try {
            ContextCompat.startForegroundService(context, intent);
        }
        catch (RuntimeException e) {
            Log.w(TAG, "cannot start the keep-alive service", e);
        }
    }

    public static void stop(Context context) {
        shown = false;
        context.stopService(new Intent(context, D2KeepAliveService.class));
    }





    public static void createChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        for (String old : OLD_CHANNEL_IDS) {
            if (nm.getNotificationChannel(old) != null) nm.deleteNotificationChannel(old);
        }



        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, D2Text.get(context, R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(D2Text.get(context, R.string.channel_description));
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        D2Store.init(this);
        boolean paused = intent != null && intent.getBooleanExtra(EXTRA_PAUSED, false);
        Notification notification = buildNotification(paused);
        shown = true;
        shownPaused = paused;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            }
            else startForeground(NOTIFICATION_ID, notification);
        }
        catch (RuntimeException e) {
            Log.w(TAG, "startForeground failed", e);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(boolean paused) {
        createChannel(this);
        Intent open = new Intent(this, D2LaunchActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent quit = new Intent(this, D2LaunchActivity.class).setAction(ACTION_SAVE_AND_EXIT).putExtra(EXTRA_SAVE_AND_EXIT, true);
        PendingIntent quitIntent = PendingIntent.getActivity(this, 1, quit,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean saves = D2Settings.modEnabled(this) && D2Settings.plugy(this) && D2Settings.autosave(this);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Diablo II")
            .setContentText(D2Text.get(this, paused ? R.string.notification_paused : R.string.notification_running))
            .setSmallIcon(R.drawable.icon_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(openIntent)
            .addAction(0, D2Text.get(this, saves ? R.string.notification_save_and_quit : R.string.notification_quit), quitIntent)
            .build();
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "task removed, ending the game");

        D2ExitLog.note(D2ExitText.taskRemoved(D2GuestComponent.running()));
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
