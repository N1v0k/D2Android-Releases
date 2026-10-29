package com.winlator.d2;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;







final class D2InputBoxControl {

    interface Host {
        void invalidate();

        void haptic();

        void schedule(Runnable r, long delayMs);

        void cancel(Runnable r);


        void keyboard(boolean show);
    }


    static final long REPEAT_FIRST_MS = 400, REPEAT_MS = 70;

    static final float SLACK = 6;

    private final Host host;
    private final D2LayerSender layer = new D2LayerSender(D2Layer.INPUT_BOX);
    private String layerWhy;
    private D2InputBox box;
    private boolean keyboardShown;
    private float scale, left, top;
    private int gameWidth, gameHeight;
    private int downPart = D2InputBox.HIT_NONE;

    private boolean repeated;
    private final Runnable repeat = new Runnable() {
        @Override public void run() {
            D2InputBox b = box;
            if (b == null || b.closed() || (downPart != D2InputBox.HIT_MINUS && downPart != D2InputBox.HIT_PLUS)) return;
            b.step(downPart == D2InputBox.HIT_PLUS ? 1 : -1);
            repeated = true;
            host.invalidate();
            host.schedule(this, REPEAT_MS);
        }
    };
    private final Paint fill = new Paint(), line = new Paint();

    D2InputBoxControl(Host host) {
        this.host = host;
        line.setStyle(Paint.Style.STROKE);
    }


    boolean open() {
        D2InputBox b = box;
        if (b != null && b.closed()) end();
        return box != null;
    }


    void show(D2InputBox b) {
        if (box != null && !box.closed()) box.cancel();
        end();
        box = b;
        D2Trace.log("input box: open, " + (b.busy() ? "progress 0 / " + b.total()
            : b.number ? "number " + b.min + "-" + b.max + " = " + b.value() : "text"));
        host.invalidate();
    }

    private void end() {
        if (box != null) D2Trace.log("input box: closed");
        box = null;
        downPart = D2InputBox.HIT_NONE;
        host.cancel(repeat);
        if (keyboardShown) {
            keyboardShown = false;
            host.keyboard(false);
        }
    }






    void layout(float[] mapping, int gameWidth, int gameHeight, float keyboardTop) {
        scale = mapping[0];
        left = mapping[1];
        top = mapping[2];
        this.gameWidth = gameWidth;
        this.gameHeight = gameHeight;
        D2InputBox b = box;
        if (b != null) b.lift(D2InputBox.liftFor(keyboardTop > 0 && scale > 0 ? (keyboardTop - top) / scale : Float.NaN, gameHeight));
    }


    void close() {
        if (box == null) return;
        if (!box.closed()) box.finish();
        end();
        host.invalidate();
    }


    D2InputBox progressBox() {
        D2InputBox b = box;
        return open() && b.busy() ? b : null;
    }

    private int partAt(float x, float y) {
        if (box == null || scale <= 0 || gameWidth <= 0) return D2InputBox.HIT_NONE;
        return box.hit((x - left) / scale, (y - top) / scale, gameWidth, gameHeight, SLACK);
    }




    void down(float x, float y) {
        if (!open()) return;
        downPart = partAt(x, y);
        repeated = false;
        box.press(downPart);
        if (downPart == D2InputBox.HIT_MINUS || downPart == D2InputBox.HIT_PLUS) {
            host.haptic();
            host.schedule(repeat, REPEAT_FIRST_MS);
        }
        if (downPart == D2InputBox.HIT_FIELD) {
            keyboardShown = true;
            host.keyboard(true);
        }
        host.invalidate();
    }


    void up(float x, float y, boolean cancelled) {
        if (box == null) return;
        host.cancel(repeat);
        int part = cancelled || repeated ? D2InputBox.HIT_NONE : partAt(x, y);
        downPart = D2InputBox.HIT_NONE;
        repeated = false;
        box.release(part);
        if (box.closed()) end();
        host.invalidate();
    }


    void releaseTouches() {
        host.cancel(repeat);
        if (downPart == D2InputBox.HIT_NONE) return;
        downPart = D2InputBox.HIT_NONE;
        repeated = false;
        if (box != null) box.release(D2InputBox.HIT_NONE);
    }




    boolean back() {
        if (!open()) return false;
        if (box.busy()) return true;
        box.cancel();
        end();
        host.invalidate();
        return true;
    }


    boolean key(KeyEvent event) {
        if (!open()) return false;
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN
            || code == KeyEvent.KEYCODE_VOLUME_MUTE) return false;

        if (code == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) back();
            return true;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (code == KeyEvent.KEYCODE_DEL) box.backspace();
        else if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) box.ok();
        else {
            int c = event.getUnicodeChar();
            if (c > 0) box.type((char) c);
        }
        if (box.closed()) end();
        host.invalidate();
        return true;
    }





    boolean pad(int button, boolean down) {
        if (!open()) return false;
        if (!down) return true;
        int b;
        switch (button) {
            case D2Gamepad.DPAD_LEFT: b = D2InputBox.PAD_LEFT; break;
            case D2Gamepad.DPAD_RIGHT: b = D2InputBox.PAD_RIGHT; break;
            case D2Gamepad.DPAD_UP: b = D2InputBox.PAD_UP; break;
            case D2Gamepad.DPAD_DOWN: b = D2InputBox.PAD_DOWN; break;
            case D2Gamepad.A: b = D2InputBox.PAD_A; break;
            case D2Gamepad.B: b = D2InputBox.PAD_B; break;
            default: return true;
        }
        box.pad(b);
        if (box.closed()) end();
        host.invalidate();
        return true;
    }


    InputConnection inputConnection(View view, EditorInfo info) {
        D2InputBox b = box;
        if (b == null) return null;
        info.inputType = b.number ? InputType.TYPE_CLASS_NUMBER
            : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        info.imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new BaseInputConnection(view, false) {
            @Override public boolean commitText(CharSequence text, int newCursorPosition) {
                for (int i = 0; box != null && i < text.length(); i++) box.type(text.charAt(i));
                after();
                return true;
            }

            @Override public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                for (int i = 0; box != null && i < Math.max(1, beforeLength); i++) box.backspace();
                after();
                return true;
            }

            @Override public boolean performEditorAction(int actionCode) {
                if (box != null) box.ok();
                after();
                return true;
            }

            @Override public boolean sendKeyEvent(KeyEvent event) {
                key(event);
                return true;
            }

            private void after() {
                if (box != null && box.closed()) end();
                host.invalidate();
            }
        };
    }







    boolean updateLayer(D2ModLink link, D2MaterialsLayer.Art art, long now) {
        D2Layer list = null;
        boolean capable = link != null && art != null && gameWidth > 0 && gameHeight > 0 && link.capable2(D2ModLink.CAP2_LAYERS);
        if (open() && capable) {
            list = new D2Layer();
            box.build(list, art, gameWidth, gameHeight);
        }
        if (link == null) return false;
        boolean byMod = layer.update(list, gameWidth, gameHeight, now, link.drawnLayers(), link.drawnLayersAt(), link::sendLayer);
        String why = list == null || list.isEmpty() ? null : byMod ? "drawn by the mod" : layer.fallbackReason();
        if (why != null && !why.equals(layerWhy)) D2Trace.log("input box layer: " + why);
        layerWhy = why;
        return byMod && open();
    }






    void draw(Canvas canvas, D2Hud hud) {
        if (!open() || hud == null || scale <= 0) return;
        D2InputBox b = box;
        int ox = D2InputBox.originX(gameWidth), oy = b.top(gameHeight);
        RectF r = view(ox, oy, D2InputBox.W, D2InputBox.H);
        int fillColour = Color.rgb(10, 10, 10);
        if (hud.hasFrame()) hud.drawFrame(canvas, r, scale, fillColour);
        else {
            fill.setColor(fillColour);
            canvas.drawRect(r, fill);
        }
        int[] f = b.rect(D2InputBox.HIT_FIELD, gameWidth, gameHeight);
        RectF field = view(f[0], f[1], f[2], f[3]);

        if (b.subtitle != null && !b.subtitle.isEmpty()) {
            hud.drawText(canvas, b.subtitle, r.centerX(), top + (oy + D2InputBox.SUB_BOTTOM - 8) * scale, scale);
        }
        drawLeft(canvas, hud, b.title, f[0] + 1, oy + D2InputBox.LABEL_BOTTOM - 8, GOLD);
        fill.setColor(Color.BLACK);
        canvas.drawRect(field, fill);
        line.setColor(Color.rgb(123, 112, 77));
        line.setStrokeWidth(Math.max(1, scale));
        canvas.drawRect(field, line);
        drawLeft(canvas, hud, b.busy() ? b.shown() : b.shown() + "_", f[0] + D2InputBox.TEXT_LEFT, f[1] + D2InputBox.FIELD_H / 2, 0xF0F0F0);
        if (b.busy()) {
            int[] q = b.bar(gameWidth, gameHeight);
            RectF bar = view(q[0], q[1], q[2], q[3]);
            fill.setColor(Color.BLACK);
            canvas.drawRect(bar, fill);
            canvas.drawRect(bar, line);
            fill.setColor(0xFF000000 | GOLD);
            canvas.drawRect(view(q[0] + 2, q[1] + 2, b.barFill(q[2] - 4), q[3] - 4), fill);
            return;
        }
        if (b.number) {
            square(canvas, hud, b, D2InputBox.HIT_MINUS, "-");
            square(canvas, hud, b, D2InputBox.HIT_PLUS, "+");
        }
        if (b.hint() != null && !b.hint().isEmpty()) drawLeft(canvas, hud, b.hint(), f[0] + 1, oy + D2InputBox.HINT_BOTTOM - 8, GOLD);
        button(canvas, hud, b, D2InputBox.HIT_OK, b.okLabel());
        button(canvas, hud, b, D2InputBox.HIT_BACK, b.backLabel());
    }


    private static final int GOLD = 0xC7B377;


    private void drawLeft(Canvas canvas, D2Hud hud, String text, int x, int y, int rgb) {
        if (text == null || text.isEmpty()) return;
        hud.drawText(canvas, text, left + x * scale + hud.textWidth(text) * scale / 2, top + y * scale, scale, rgb);
    }

    private void square(Canvas canvas, D2Hud hud, D2InputBox b, int part, String sign) {
        int[] q = b.rect(part, gameWidth, gameHeight);
        RectF r = view(q[0], q[1], q[2], q[3]);
        boolean down = b.pressed() == part;
        hud.drawBlankButton(canvas, down, r, null);

        float shift = down ? scale : 0;
        hud.drawStoneSign(canvas, "+".equals(sign), r.centerX() + shift, r.centerY() + shift, scale);
    }

    private void button(Canvas canvas, D2Hud hud, D2InputBox b, int part, String label) {
        int[] q = b.rect(part, gameWidth, gameHeight);
        RectF r = view(q[0], q[1], q[2], q[3]);
        if (!hud.drawMediumButton(canvas, label, b.pressed() == part, r)) {
            fill.setColor(Color.rgb(40, 34, 26));
            canvas.drawRect(r, fill);
            hud.drawText(canvas, label, r.centerX(), r.centerY(), scale);
        }
    }

    private RectF view(int x, int y, int w, int h) {
        return new RectF(left + x * scale, top + y * scale, left + (x + w) * scale, top + (y + h) * scale);
    }
}
