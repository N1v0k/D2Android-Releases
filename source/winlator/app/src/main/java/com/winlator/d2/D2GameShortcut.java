package com.winlator.d2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Icon;

import com.winlator.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;







final class D2GameShortcut {
    static final String ID = "game-icon";

    static final String PROGRAM = "Game.exe";




    static final int SIDE = 288, SAFE = SIDE * 66 / 108;

    private D2GameShortcut() {}


    static String offer(Context context) {
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        if (sm == null || !sm.isRequestPinShortcutSupported()) return context.getString(R.string.settings_shortcut_unsupported);
        Bitmap icon = icon(GameLocator.gameDir(context));
        if (icon == null) return context.getString(R.string.settings_shortcut_no_icon);
        Intent start = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(context, D2LaunchActivity.class.getName())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        CharSequence label = context.getApplicationInfo().loadLabel(context.getPackageManager());
        ShortcutInfo info = new ShortcutInfo.Builder(context, ID).setShortLabel(label).setLongLabel(label)
            .setIcon(Icon.createWithAdaptiveBitmap(icon)).setIntent(start).build();
        try {
            if (sm.requestPinShortcut(info, null)) return context.getString(R.string.settings_shortcut_asked);
        }
        catch (RuntimeException e) {
            D2Trace.log("shortcut with the game's icon refused: " + e);
        }
        return context.getString(R.string.settings_shortcut_unsupported);
    }


    static Bitmap icon(File gameDir) {
        D2ExeIcon.Picture p = picture(gameDir);
        if (p == null) return null;
        Bitmap src = p.png != null ? BitmapFactory.decodeByteArray(p.png, 0, p.png.length)
            : Bitmap.createBitmap(p.argb, p.width, p.height, Bitmap.Config.ARGB_8888);
        if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) return null;
        Bitmap out = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.BLACK);
        int k = Math.max(1, SAFE / Math.max(src.getWidth(), src.getHeight()));
        int w = Math.min(SAFE, src.getWidth() * k), h = Math.min(SAFE, src.getHeight() * k);
        Paint pixels = new Paint();
        pixels.setFilterBitmap(false);
        canvas.drawBitmap(src, null, new Rect((SIDE - w) / 2, (SIDE - h) / 2, (SIDE + w) / 2, (SIDE + h) / 2), pixels);
        return out;
    }


    static D2ExeIcon.Picture picture(File gameDir) {
        if (gameDir == null) return null;
        File[] files = gameDir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (!f.getName().equalsIgnoreCase(PROGRAM) || !f.isFile() || f.length() > D2ExeIcon.MAX_EXE) continue;
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] exe = new byte[(int) f.length()];
                int got = 0;
                while (got < exe.length) {
                    int n = in.read(exe, got, exe.length - got);
                    if (n < 0) break;
                    got += n;
                }
                return D2ExeIcon.read(got == exe.length ? exe : java.util.Arrays.copyOf(exe, got));
            }
            catch (IOException e) {
                D2Trace.log("shortcut: " + f.getName() + " not readable: " + e);
            }
        }
        return null;
    }
}
