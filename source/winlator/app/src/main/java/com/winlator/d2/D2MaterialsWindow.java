package com.winlator.d2;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

import com.winlator.R;

import java.util.Locale;







final class D2MaterialsWindow {

    interface Host {
        void send(int order, int arg, int count);
        void note(String text);
        String text(int id, Object... args);

        boolean backedUp();

        void startBackup();

        void haptic();
        void schedule(Runnable r, long delayMs);
        void cancel(Runnable r);

        boolean sorting();

        void sound(String file);

        boolean cursorTaken();


        String recipeLine(int kind);


        void openRecipes(int kind);
    }

    static final long LONG_PRESS_MS = 400;

    private final Host host;
    private final D2Materials.Orders orders = new D2Materials.Orders();
    private boolean open, backupRunning;
    private int tab = D2Materials.TAB_RUNES, selected = -1, choiceKind = -1;

    private int soundKind = -1;

    private boolean entryDown, closingTap, longFired, outOfCell, pressing;
    private int downHit = D2Materials.HIT_NONE;

    private float scale, viewLeft, viewTop, originX, originY;
    private int gameWidth;
    private D2Materials.Snapshot snapshot;

    private boolean placed;
    private final Paint fill = new Paint(), line = new Paint(), picture = new Paint(Paint.FILTER_BITMAP_FLAG),
        grey = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF rect = new RectF(), at = new RectF();
    private final Runnable longPress = this::onLongPress;

    D2MaterialsWindow(Host host) {
        this.host = host;
        line.setStyle(Paint.Style.STROKE);
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0);
        grey.setColorFilter(new ColorMatrixColorFilter(m));
        grey.setAlpha(150);
    }





    void layout(D2Materials.Snapshot s, float[] mapping, int gameWidth, int gameHeight, long uiFlags) {
        snapshot = s;
        scale = mapping[0];
        viewLeft = mapping[1];
        viewTop = mapping[2];
        this.gameWidth = gameWidth;
        placed = D2Materials.placed(s, gameWidth, gameHeight, uiFlags) && scale > 0 && !Float.isInfinite(scale);
        if (placed) {
            originX = s.x;
            originY = s.y;
        }
    }





    static boolean visible(D2Materials.Snapshot s, boolean placed, boolean helperOn, long now) {
        return helperOn && placed && s != null && now - s.time < D2Materials.STALE_MS && s.entry();
    }


    private long sharedGold = -1;

    void sharedGold(long gold) {
        sharedGold = gold;
    }






    static boolean sharedGoldShown(D2Materials.Snapshot s, boolean placed, long now) {
        return placed && s != null && now - s.time < D2Materials.STALE_MS && s.open();
    }





    void update(D2Materials.Snapshot s, boolean helperOn, long now) {
        boolean shown = visible(s, placed, helperOn, now);
        if (!shown) orders.clear();
        if (open && (!shown || !s.possible() || s.locked())) close();
        int[] over = orders.finished(s, now);
        if (over != null && s != null) {
            int note = D2Materials.resultNote(over[0], over[1], s.why);
            if (note != D2Materials.NOTE_NONE) host.note(text(note));

            else if (over[1] == D2Materials.RESULT_OK && soundKind >= 0) host.sound(D2Materials.sound(soundKind));
            soundKind = -1;
        }

        if (orders.dropForCursor(host.cursorTaken())) {
            D2Trace.log("materials: waiting orders dropped: an item on the cursor");
            host.note(text(D2Materials.NOTE_CURSOR));
        }
        if (!orders.hasWaiting()) return;
        if (!host.backedUp()) {
            if (!backupRunning) {
                backupRunning = true;
                host.startBackup();
            }
            return;
        }
        int[] next = orders.next(s, now, !backupRunning && !host.sorting());
        if (next != null) {
            soundKind = D2Materials.soundKind(next[0], next[1], s);
            host.send(next[0], next[1], next[2]);
        }
    }


    void backupDone(boolean ok) {
        backupRunning = false;
        if (!ok) {
            orders.clear();
            host.note(host.text(R.string.materials_backup_failed));
        }
    }

    private void close() {
        open = false;
        choiceKind = -1;
        orders.clearWaiting();
    }



    private float lx(float x) {
        return (x - viewLeft) / scale - originX;
    }

    private float ly(float y) {
        return (y - viewTop) / scale - originY;
    }


    private boolean inInventory(float x, float y) {
        float gx = (x - viewLeft) / scale, gy = (y - viewTop) / scale;
        float right = gameWidth - originX;
        return gx >= right - 400 && gx < right && gy >= originY && gy < originY + 600;
    }





    boolean takes(float x, float y, boolean onAppButton, boolean helperOn, long now) {
        D2Materials.Snapshot s = snapshot;
        if (scale <= 0 || !visible(s, placed, helperOn, now)) return false;
        float lx = lx(x), ly = ly(y);
        if (D2Materials.onEntry(lx, ly)) return true;
        if (!open) return false;
        if (D2Materials.inWindow(lx, ly)) return true;
        return !onAppButton && !inInventory(x, y);
    }


    boolean windowOpen(boolean helperOn, long now) {
        return open && visible(snapshot, placed, helperOn, now);
    }


    boolean covers(float x, float y, boolean helperOn, long now) {
        if (scale <= 0 || !visible(snapshot, placed, helperOn, now)) return false;
        float lx = lx(x), ly = ly(y);
        return D2Materials.onEntry(lx, ly) || (open && D2Materials.inWindow(lx, ly));
    }

    void down(float x, float y) {
        float lx = lx(x), ly = ly(y);
        entryDown = D2Materials.onEntry(lx, ly);
        pressing = true;
        closingTap = !entryDown && !D2Materials.inWindow(lx, ly);
        longFired = outOfCell = false;
        downHit = entryDown || closingTap ? D2Materials.HIT_NONE : D2Materials.hit(tab, lx, ly);
        D2Materials.Snapshot s = snapshot;
        if (downHit >= 0 && s != null && s.stash[downHit] > 0
            && (choiceKind < 0 || D2Materials.choiceHit(choiceKind, s.stash[choiceKind], lx, ly) < 0)) {
            host.schedule(longPress, LONG_PRESS_MS);
        }
    }

    void move(float x, float y) {
        if (downHit < 0 || longFired) return;
        if (D2Materials.hit(tab, lx(x), ly(y)) != downHit) {
            outOfCell = true;
            host.cancel(longPress);
        }
    }

    private void onLongPress() {
        if (downHit < 0 || outOfCell) return;
        longFired = true;
        choiceKind = downHit;
        selected = downHit;
        host.haptic();
    }

    void up(float x, float y, boolean cancelled) {
        host.cancel(longPress);
        pressing = false;
        D2Materials.Snapshot s = snapshot;
        float lx = lx(x), ly = ly(y);
        boolean entry = entryDown;
        entryDown = false;
        if (cancelled || s == null) return;
        if (entry) {
            if (!D2Materials.onEntry(lx, ly)) return;
            if (s.locked() || !s.possible()) host.note(text(D2Materials.lockNote(s.why)));
            else if (open) close();
            else open = true;
            return;
        }
        if (closingTap) {
            close();
            return;
        }

        if (choiceKind >= 0) {
            int choice = D2Materials.choiceHit(choiceKind, s.stash[choiceKind], lx, ly);
            if (choice >= 0) {
                order(D2Materials.ORDER_WITHDRAW, choiceKind, D2Materials.choiceCount(choice));
                choiceKind = -1;
                return;
            }
            if (longFired) return;
            choiceKind = -1;
            return;
        }

        if (selected >= 0 && D2Materials.onRecipeLine(lx, ly) && D2Materials.onRecipeLine(lx(x), ly(y)) && host.recipeLine(selected) != null) {
            host.openRecipes(selected);
            return;
        }
        int hit = D2Materials.hit(tab, lx, ly);
        if (hit != downHit || outOfCell) return;
        if (hit == D2Materials.HIT_TAB_RUNES || hit == D2Materials.HIT_TAB_GEMS) {
            tab = hit == D2Materials.HIT_TAB_RUNES ? D2Materials.TAB_RUNES : D2Materials.TAB_GEMS;
        }
        else if (hit == D2Materials.HIT_DEPOSIT_ALL) {
            int carried = 0;
            for (int n : s.inventory) carried += n;
            if (carried == 0) host.note(host.text(R.string.materials_nothing));
            else order(D2Materials.ORDER_DEPOSIT_ALL, 0, 0);
        }
        else if (hit >= 0) {
            selected = hit;
            if (s.stash[hit] > 0) order(D2Materials.ORDER_WITHDRAW, hit, 1);
        }
    }


    private void order(int order, int arg, int count) {
        int note = orders.add(order, arg, count, host.cursorTaken());
        if (note == D2Materials.NOTE_NONE) return;
        D2Trace.log("materials: order " + order + " arg " + arg + " not sent: an item on the cursor");
        host.note(text(note));
    }



    private final D2MaterialsLayer.State layerState = new D2MaterialsLayer.State();





    D2Layer layer(D2Hud hud, boolean helperOn, long now) {
        D2MaterialsLayer.Art art = hud != null ? hud.layerArt() : null;
        if (art == null) return null;
        D2Layer l = new D2Layer();
        D2Materials.Snapshot s = snapshot;
        boolean entry = visible(s, placed, helperOn, now);
        long gold = sharedGoldShown(s, placed, now) ? sharedGold : -1;
        if (scale <= 0 || !entry && gold < 0) return l;
        D2MaterialsLayer.State st = layerState;
        st.entry = entry;
        st.sharedGold = gold;
        st.sharedGoldLabel = host.text(R.string.stash_shared_gold);
        st.open = open;
        st.entryDown = entryDown;
        st.pressing = pressing;
        st.locked = s.locked() || !s.possible();
        st.tab = tab;
        st.selected = selected;
        st.choiceKind = choiceKind;
        st.downHit = downHit;
        st.originX = Math.round(originX);
        st.originY = Math.round(originY);
        st.title = host.text(R.string.materials_title);
        st.tabRunes = host.text(R.string.materials_tab_runes);
        st.tabGems = host.text(R.string.materials_tab_gems);
        st.info = selected >= 0 ? host.text(R.string.materials_info, s.stash[selected], s.inventory[selected])
            : host.text(R.string.materials_hint);
        st.deposit = host.text(R.string.materials_deposit_all);
        st.choiceAll = host.text(R.string.materials_choice_all);
        st.recipeLine = selected >= 0 && choiceKind < 0 ? host.recipeLine(selected) : null;
        D2MaterialsLayer.build(l, art, s, st);
        return l;
    }


    RectF entry(boolean helperOn, long now) {
        if (scale <= 0 || !visible(snapshot, placed, helperOn, now)) return null;
        float x = viewLeft + (originX + D2Materials.ENTRY_X) * scale, y = viewTop + (originY + D2Materials.ENTRY_Y) * scale;
        return new RectF(x, y, x + D2Materials.ENTRY_SIZE * scale, y + D2Materials.ENTRY_SIZE * scale);
    }

    private void toView(float lx, float ly, float w, float h) {
        float x = viewLeft + (originX + lx) * scale, y = viewTop + (originY + ly) * scale;
        rect.set(x, y, x + w * scale, y + h * scale);
    }

    void draw(Canvas canvas, D2Hud hud, boolean helperOn, long now) {
        D2Materials.Snapshot s = snapshot;
        if (hud == null || scale <= 0 || !visible(s, placed, helperOn, now)) return;
        boolean locked = s.locked() || !s.possible();

        toView(D2Materials.ENTRY_X + 1, D2Materials.ENTRY_Y + 1, D2Materials.ENTRY_SIZE - 2, D2Materials.ENTRY_SIZE - 2);
        if (!hud.drawAnvilButton(canvas, open || entryDown, rect, locked ? grey : null)) {
            fill.setColor(Color.argb(220, 20, 18, 16));
            canvas.drawRect(rect, fill);
        }
        if (open) {
            toView(D2Materials.ENTRY_X - 1, D2Materials.ENTRY_Y - 1, D2Materials.ENTRY_SIZE + 2, D2Materials.ENTRY_SIZE + 2);
            line.setStrokeWidth(2 * scale);
            line.setColor(Color.rgb(220, 190, 110));
            canvas.drawRect(rect, line);
        }
        if (!open) return;


        toView(D2Materials.WIN_X, D2Materials.WIN_Y, D2Materials.WIN_W, D2Materials.WIN_H);
        fill.setColor(Color.rgb(9, 8, 7));
        canvas.drawRect(rect, fill);
        float k = scale;


        int room = D2Materials.WIN_W - 2 * D2Materials.TEXT_MARGIN;
        float titleY = viewTop + (originY + D2Materials.WIN_Y + 12) * k;
        String name = selected >= 0 ? D2Materials.name(selected, hud.gameText(D2Materials.code(selected))) : null;
        if (name == null) hud.drawText(canvas, host.text(R.string.materials_title).toUpperCase(Locale.ROOT), rect.centerX(), titleY, k);
        else if (hud.textWidth(name) <= room) hud.drawText(canvas, name, rect.centerX(), titleY, k);
        else hud.drawSmallText(canvas, D2Materials.fit(name, hud::smallTextWidth, room), rect.centerX(), titleY, k);

        float tabsLeft = D2Materials.WIN_X + (D2Materials.WIN_W - 2 * D2Materials.TAB_W - 6) / 2f;
        for (int t = 0; t < 2; t++) {
            boolean on = t == tab;
            toView(tabsLeft + t * (D2Materials.TAB_W + 6), D2Materials.TAB_Y, D2Materials.TAB_W, D2Materials.TAB_H);
            int inside = on ? Color.argb(240, 46, 38, 22) : Color.argb(220, 16, 15, 14);
            if (hud.hasFrame()) hud.drawFrame(canvas, rect, k, inside, on ? 255 : 150);
            else {
                fill.setColor(inside);
                canvas.drawRect(rect, fill);
            }
            String label = host.text(t == 0 ? R.string.materials_tab_runes : R.string.materials_tab_gems).toUpperCase(Locale.ROOT);
            hud.drawSmallText(canvas, label, rect.centerX(), rect.centerY(), k, on ? 255 : 130);
        }

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                int kind = D2Materials.kindAt(tab, column, row);
                if (kind < 0) continue;
                float cx = D2Materials.GRID_X + column * D2Materials.CELL, cy = D2Materials.GRID_Y + row * D2Materials.CELL;
                toView(cx + 1, cy + 1, D2Materials.CELL - 2, D2Materials.CELL - 2);
                fill.setColor(Color.argb(255, 22, 22, 30));
                canvas.drawRect(rect, fill);
                line.setStrokeWidth(Math.max(1, k));
                line.setColor(kind == selected ? Color.rgb(220, 190, 110) : Color.rgb(52, 52, 64));
                canvas.drawRect(rect, line);
                int count = s.stash[kind];
                android.graphics.Bitmap b = hud.materialPicture(kind);
                if (b != null) {
                    float size = (D2Materials.CELL - 6) * k;
                    float w = Math.min(b.getWidth() * k, size), h = Math.min(b.getHeight() * k, size);
                    float px = rect.centerX(), py = rect.centerY() - 2 * k;
                    at.set(px - w / 2, py - h / 2, px + w / 2, py + h / 2);
                    picture.setAlpha(count > 0 ? 255 : 80);
                    canvas.drawBitmap(b, null, at, picture);
                    picture.setAlpha(255);
                }
                else hud.drawSmallText(canvas, D2Materials.code(kind), rect.centerX(), rect.centerY(), k, count > 0 ? 255 : 90);
                String n = Integer.toString(count);
                hud.drawSmallText(canvas, n, rect.right - (hud.smallTextWidth(n) / 2f + 3) * k, rect.bottom - 7 * k, k,
                    count > 0 ? 255 : 110, count > 0 ? 0xFFFFFF : 0x808080);
            }
        }

        String info = selected >= 0 ? host.text(R.string.materials_info, s.stash[selected], s.inventory[selected])
            : host.text(R.string.materials_hint);
        info = D2Materials.fit(info, hud::smallTextWidth, room);
        hud.drawSmallText(canvas, info, viewLeft + (originX + D2Materials.WIN_X + D2Materials.WIN_W / 2f) * k,
            viewTop + (originY + D2Materials.INFO_Y) * k, k, 255, 0xC0C0C0);

        toView(D2Materials.BUTTON_X, D2Materials.BUTTON_Y, D2Materials.BUTTON_W, D2Materials.BUTTON_H);


        String deposit = host.text(R.string.materials_deposit_all);
        if (!hud.drawMediumButton(canvas, deposit, pressing && downHit == D2Materials.HIT_DEPOSIT_ALL, rect)) {
            if (hud.hasFrame()) hud.drawFrame(canvas, rect, k, Color.argb(240, 40, 34, 22));
            hud.drawSmallText(canvas, deposit.toUpperCase(Locale.ROOT), rect.centerX(), rect.centerY(), k);
        }

        if (choiceKind >= 0) {
            int[] box = D2Materials.choiceAt(choiceKind);
            toView(box[0], box[1], 3 * D2Materials.CHOICE_W, D2Materials.CHOICE_H);
            if (hud.hasFrame()) hud.drawFrame(canvas, rect, k, Color.argb(250, 10, 10, 10));
            else {
                fill.setColor(Color.argb(250, 10, 10, 10));
                canvas.drawRect(rect, fill);
            }
            String[] labels = {"1", "3", host.text(R.string.materials_choice_all).toUpperCase(Locale.ROOT)};
            for (int i = 0; i < 3; i++) {
                float cx = viewLeft + (originX + box[0] + (i + 0.5f) * D2Materials.CHOICE_W) * k;
                float cy = viewTop + (originY + box[1] + D2Materials.CHOICE_H / 2f) * k;
                boolean can = i != 1 || s.stash[choiceKind] >= 3;
                if (can) hud.drawText(canvas, labels[i], cx, cy, k);
                else hud.drawText(canvas, labels[i], cx, cy, k, 0x606060);
            }
        }
    }

    private String text(int note) {
        switch (note) {
            case D2Materials.NOTE_INVENTORY_FULL: return host.text(R.string.materials_inventory_full);
            case D2Materials.NOTE_STASH_FULL: return host.text(R.string.materials_stash_full);
            case D2Materials.NOTE_NOT_NOW: return host.text(R.string.materials_not_now);
            case D2Materials.NOTE_BUSY: return host.text(R.string.materials_busy);
            case D2Materials.NOTE_LOCKED_BROKEN: return host.text(R.string.materials_locked_broken);
            case D2Materials.NOTE_LOCKED_UNSAVED: return host.text(R.string.materials_locked_unsaved);
            case D2Materials.NOTE_LOCKED_UNREADABLE: return host.text(R.string.materials_locked_unreadable);
            case D2Materials.NOTE_LOCKED_VERSION: return host.text(R.string.materials_locked_version);
            case D2Materials.NOTE_NOT_SAVED: return host.text(R.string.materials_not_saved);
            case D2Materials.NOTE_NOT_CONFIRMED: return host.text(R.string.materials_not_confirmed);
            case D2Materials.NOTE_CURSOR: return host.text(R.string.materials_cursor);
            default: return host.text(R.string.materials_locked);
        }
    }


    String saveWarning(long now) {
        int note = D2Materials.saveWarning(snapshot, now);
        return note == D2Materials.NOTE_NONE ? null : text(note);
    }


    boolean saveBlocked(D2Materials.Snapshot last) {
        return D2Materials.saveBlocked(last) != D2Materials.NOTE_NONE;
    }
}
