package com.winlator.d2;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;








final class D2SettingsLook {
    final D2GameFont font16, formal10, font24, font30, exocet10;
    final Bitmap wideStone, mediumStone, backdrop;




    final int[] plainUp, plainDown;

    final int scale;

    private static volatile D2SettingsLook loaded;
    private static boolean loading;
    private static final List<Runnable> waiting = new ArrayList<>();

    private D2SettingsLook(D2Sprites sprites, int scale) throws java.io.IOException {
        this.scale = scale;
        font16 = need(D2GameFont.load(sprites, "font16"));
        formal10 = need(D2GameFont.load(sprites, "fontformal10"));
        font24 = need(D2GameFont.load(sprites, "font24"));
        font30 = need(D2GameFont.load(sprites, "font30"));
        exocet10 = need(D2GameFont.load(sprites, "fontexocet10"));
        D2Sprites.Frame[] wide = sprites.dc6(D2StartArt.FRONT_END + "widebuttonblank.dc6", "Sky");
        D2Sprites.Frame[] medium = sprites.dc6(D2StartArt.FRONT_END + "mediumbuttonblank.dc6", "Sky");


        D2Sprites.Frame[] tiles = sprites.dc6(D2StartArt.FRONT_END + "TCPIPscreen.dc6", "Sky");
        if (tiles == null) tiles = sprites.dc6(D2StartArt.FRONT_END + "gameselectscreenEXP.dc6", "Sky");
        if (wide == null || wide.length < 2 || medium == null || medium.length < 1 || tiles == null) throw new java.io.IOException("stones");
        Bitmap left = bitmap(wide[0]), right = bitmap(wide[1]);
        wideStone = Bitmap.createBitmap(left.getWidth() + right.getWidth(), Math.max(left.getHeight(), right.getHeight()), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(wideStone);
        c.drawBitmap(left, 0, 0, null);
        c.drawBitmap(right, left.getWidth(), 0, null);
        mediumStone = bitmap(medium[0]);
        D2Sprites.Frame[] plain = sprites.dc6("data\\global\\ui\\PANEL\\buysellbtn.dc6");
        boolean square = plain != null && plain.length >= 2 && squareStone(plain[0]) && squareStone(plain[1]);
        plainUp = square ? plain[0].argb : null;
        plainDown = square ? plain[1].argb : null;
        int[] screen = D2StartArt.tiled(tiles, D2StartArt.SCREEN_W, D2StartArt.SCREEN_H);
        for (int i = 0; i < screen.length; i++) screen[i] = D2StartArt.dim(screen[i], 35);
        backdrop = Bitmap.createBitmap(screen, D2StartArt.SCREEN_W, D2StartArt.SCREEN_H, Bitmap.Config.ARGB_8888);
        D2Sprites.Frame[] g = D2StartArt.gems(sprites);
        gems = new Bitmap[g.length];
        for (int i = 0; i < g.length; i++) gems[i] = g[i] != null ? bitmap(g[i]) : null;
    }


    final Bitmap[] gems;

    private static D2GameFont need(D2GameFont font) throws java.io.IOException {
        if (font == null) throw new java.io.IOException("font");
        return font;
    }

    private static boolean squareStone(D2Sprites.Frame f) {
        return f.width == D2StoneButton.PLAIN_SQUARE && f.height == D2StoneButton.PLAIN_SQUARE;
    }

    private static Bitmap bitmap(D2Sprites.Frame f) {
        return Bitmap.createBitmap(f.argb, f.width, f.height, Bitmap.Config.ARGB_8888);
    }


    static D2SettingsLook get() {
        return loaded;
    }






    static void load(Context context, Runnable done) {
        Handler main = new Handler(Looper.getMainLooper());
        synchronized (waiting) {
            if (loaded != null) {
                if (done != null) main.post(done);
                return;
            }
            if (done != null) waiting.add(done);
            if (loading) return;
            loading = true;
        }
        Context app = context.getApplicationContext();
        int scale = Math.max(1, Math.round(app.getResources().getDisplayMetrics().density));
        new Thread(() -> {
            D2SettingsLook look = null;
            File dir = GameLocator.gameDir(app);
            if (dir != null) {
                try (D2Sprites sprites = new D2Sprites(dir)) {
                    look = new D2SettingsLook(sprites, scale);
                }
                catch (Throwable e) {
                    D2Trace.log("settings keep the app look: " + e);
                }
            }
            List<Runnable> run;
            synchronized (waiting) {
                loaded = look;
                loading = false;
                run = new ArrayList<>(waiting);
                waiting.clear();
            }
            if (look != null) for (Runnable r : run) main.post(r);
        }, "SettingsLook").start();
    }






    static final int HINT_RGB = 0xB0B0B0, HINT_GAIN = 120, TITLE_GAIN = 145;


    static Bitmap text(D2GameFont font, String text, int rgb) {
        return text(font, text, rgb, 100);
    }


    static Bitmap text(D2GameFont font, String text, int rgb, int gainPercent) {
        D2GameFont.Img img = font.render(text, rgb, gainPercent);
        return Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888);
    }
}
