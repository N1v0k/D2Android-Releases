package com.winlator.d2;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.winlator.R;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;








final class D2RezeptHook {
    interface Host {
        D2ModState state();

        D2ModLink link();

        D2Hud graphics();

        float[] mapping(D2ModState s, float[] into);

        boolean ladder(D2ModState s);

        void pause(boolean on);

        boolean paused();

        void note(String text);

        String text(int id, Object... args);

        Context context();

        void redraw();


        void haptic();

        void schedule(Runnable r, long delayMs);

        void cancel(Runnable r);
    }


    static final long LONG_PRESS_MS = 450;

    static final long INSERT_WAIT_MS = 3500;


    static final int INV_RIGHT = 383, INV_Y = 316, INV_COLUMNS = 10, INV_ROWS = 4, CELL = 29;

    static final float SWIPE = 24;

    private final Host host;
    private volatile D2RezeptDaten daten;
    private D2RezeptBuch book;
    private final D2LayerSender sender = new D2LayerSender(D2Layer.RECIPES);
    private final float[] mapping = new float[3];
    private float scale, viewLeft, viewTop;
    private int ox, oy, gameWidth, gameHeight;
    private boolean placed, cubeOpen, pausedByBook;
    private byte[] lastFigur;

    private String stockHero;
    private List<D2RezeptBestand.Stueck> items;

    private List<D2RezeptBestand.Stueck> pages;
    private Map<String, Integer> lastMaterials;
    private AlertDialog searchDialog;

    private int downHit = D2RezeptBuch.HIT_NONE;
    private boolean longArmed;
    private int[] pendingIds;
    private boolean pendingStash;
    private long pendingUntil;


    private D2RezeptKatalog.Eintrag fetchEntry;
    private final java.util.ArrayDeque<int[]> fetchLeft = new java.util.ArrayDeque<>();
    private int[] fetchSent;
    private int fetchBase;
    private long fetchUntil;
    private String fetchWhat;

    private long cubeWaitUntil;

    private boolean overCube;
    private final Runnable longPress;

    D2RezeptHook(Host host) {
        this.host = host;
        longPress = () -> {
            longArmed = true;
            host.haptic();
        };
    }


    void load(File dir, Handler handler) {
        Context context = host.context();
        D2RezeptTexte texte = new D2RezeptTexte(context);
        new Thread(() -> {
            D2RezeptDaten d = D2RezeptDaten.load(dir, texte);
            if (d == null) D2Trace.log("recipes: the archives lack something, no recipe book");
            handler.post(() -> {
                daten = d;
                book = d != null ? new D2RezeptBuch(d.katalog, texte, d::crop) : null;
                if (d != null) D2Trace.log("recipes: " + d.katalog.all().size() + " entries");
            });
        }, "d2-recipes-load").start();
    }

    boolean isOpen() {
        return book != null && book.isOpen() && placed;
    }



    void update(D2ModState s, D2Materials.Snapshot stash, long now) {
        D2RezeptBuch b = book;
        if (b == null || s == null) return;
        gameWidth = s.gameWidth;
        gameHeight = s.gameHeight;
        host.mapping(s, mapping);
        scale = mapping[0];
        viewLeft = mapping[1];
        viewTop = mapping[2];
        placed = stash != null && stash.origin() && scale > 0 && gameWidth > 0
            && stash.x + D2RezeptBuch.RIM_X >= 0 && stash.x + D2RezeptBuch.RIM_X + D2RezeptBuch.RIM_W <= gameWidth
            && stash.y + D2RezeptBuch.RIM_Y >= 0 && stash.y + D2RezeptBuch.RIM_Y + D2RezeptBuch.RIM_H <= gameHeight;
        if (placed) {
            ox = stash.x;
            oy = stash.y;
        }

        D2Hud graphics = host.graphics();
        D2ModLink drawer = host.link();
        placed &= drawer != null && drawer.capable2(D2ModLink.CAP2_LAYERS) && graphics != null && graphics.layerArt() != null;
        cubeOpen = placed && s.isOpen(D2ModState.UI_CUBE);

        if (cubeOpen && b.isOpen()) overCube = true;
        if (overCube && !cubeOpen && b.isOpen()) close("the cube closed");
        if (!b.isOpen()) overCube = false;

        D2ModLink link = host.link();
        String hero = link != null ? link.characterName() : null;
        if (hero != null && !hero.equals(stockHero)) {
            stockHero = hero;
            items = null;
            pages = null;
        }
        byte[] figur = link != null ? link.figur() : null;
        if (figur != null && figur != lastFigur) {
            lastFigur = figur;
            List<D2RezeptBestand.Stueck> parsed = D2RezeptBestand.parse(figur, figur.length);
            if (parsed != null) {
                items = parsed;
                pages = D2RezeptBestand.parsePages(figur, figur.length);
            }
        }
        checkInsert(now);
        checkCubeWait();
        Map<String, Integer> materials = new HashMap<>();
        if (stash != null) for (int k = 0; k < D2Materials.KINDS; k++) if (stash.stash[k] > 0) materials.put(D2Materials.code(k), stash.stash[k]);

        long figurAt = link != null ? link.figurAt() : 0;
        b.stockAge(items == null ? -2 : figurAt > 0 && now - figurAt < 3000 ? -1 : (int) ((now - figurAt) / 60000));
        boolean ladder = host.ladder(s);
        if (b.bestand().items() != items || !materials.equals(lastMaterials) || b.ladderOn() != ladder) {
            lastMaterials = materials;
            b.bestand(new D2RezeptBestand(items, materials, daten.items, pages), ladder);
        }
        checkFetch(stash);
        if (b.isOpen() && !placed) close("not placed");
    }




    boolean sendLayer(long now, boolean pad) {
        D2RezeptBuch b = book;
        D2ModLink link = host.link();
        D2ModState s = host.state();
        D2Hud graphics = host.graphics();
        if (link == null) return false;
        boolean capable = b != null && placed && s != null && graphics != null && graphics.layerArt() != null
            && link.capable2(D2ModLink.CAP2_LAYERS);
        D2Layer list = capable ? b.layer(graphics.layerArt(), ox, oy, cubeOpen, pad) : null;
        sender.update(list, gameWidth, gameHeight, now, link.drawnLayers(), link.drawnLayersAt(), link::sendLayer);
        return list != null && !list.isEmpty();
    }



    private float lx(float x) {
        return (x - viewLeft) / scale - ox;
    }

    private float ly(float y) {
        return (y - viewTop) / scale - oy;
    }


    boolean covers(float x, float y) {
        D2RezeptBuch b = book;
        if (b == null || !placed || scale <= 0) return false;
        float lx = lx(x), ly = ly(y);
        return b.isOpen() && D2RezeptBuch.inWindow(lx, ly) || b.hit(lx, ly, cubeOpen) == D2RezeptBuch.HIT_ENTRY;
    }


    boolean takes(float x, float y, boolean onAppButton) {
        D2RezeptBuch b = book;
        if (b == null || !placed || scale <= 0) return false;
        float lx = lx(x), ly = ly(y);
        if (b.hit(lx, ly, cubeOpen) != D2RezeptBuch.HIT_NONE) return true;
        if (!b.isOpen()) return false;
        if (D2RezeptBuch.inWindow(lx, ly) || b.pickMode()) return true;

        if (cubeOpen && !host.paused() && D2RezeptBuch.inCorner(lx, ly)) return false;


        if (host.paused()) return true;
        return !onAppButton && !inInventory(x, y);
    }

    private boolean inInventory(float x, float y) {
        float gx = (x - viewLeft) / scale, gy = (y - viewTop) / scale, right = gameWidth - ox;
        return gx >= right - 400 && gx < right && gy >= oy && gy < oy + 600;
    }


    void down(float x, float y) {
        D2RezeptBuch b = book;
        host.cancel(longPress);
        longArmed = false;
        downHit = b != null && placed && scale > 0 && !b.pickMode() ? b.hit(lx(x), ly(y), cubeOpen) : D2RezeptBuch.HIT_NONE;
        if (b != null && b.cardEntry(downHit) != null) host.schedule(longPress, LONG_PRESS_MS);
        if (b != null && downHit == D2RezeptBuch.HIT_INSERT) {
            b.insertPressed(true);
            host.redraw();
        }
    }


    void up(float downX, float downY, float x, float y, boolean cancelled) {
        D2RezeptBuch b = book;
        host.cancel(longPress);
        boolean armed = longArmed;
        longArmed = false;
        if (b != null) b.insertPressed(false);
        if (b == null || cancelled || scale <= 0) return;
        float lx = lx(downX), ly = ly(downY), dy = (y - downY) / scale, dx = (x - downX) / scale;
        if (b.isOpen() && D2RezeptBuch.inWindow(lx, ly) && Math.abs(dy) >= SWIPE && Math.abs(dy) > Math.abs(dx)) {
            b.turn(dy < 0 ? 1 : -1);
            host.redraw();
            return;
        }

        if (armed && Math.abs(dx) < SWIPE && Math.abs(dy) < SWIPE && b.cardEntry(downHit) != null) {
            insert(b.cardEntry(downHit));
            host.redraw();
            return;
        }
        if (b.hit(lx, ly, cubeOpen) == D2RezeptBuch.HIT_INSERT && b.detail() != null) {
            insert(b.detail());
            host.redraw();
            return;
        }
        if (b.pickMode()) {

            if (D2RezeptBuch.inWindow(lx, ly)) {
                if (b.hit(lx, ly, cubeOpen) == D2RezeptBuch.HIT_PICK) b.picked(null);
                host.redraw();
                return;
            }
            b.picked(itemAt(downX, downY));
            host.redraw();
            return;
        }
        int hit = b.hit(lx, ly, cubeOpen);
        if (hit == D2RezeptBuch.HIT_NONE) {
            if (b.isOpen() && !D2RezeptBuch.inWindow(lx, ly) && !inInventory(downX, downY)) close("closing tap");
            return;
        }
        boolean wasOpen = b.isOpen();
        if (b.tap(hit)) search();
        if (wasOpen && !b.isOpen()) unpause();
        host.redraw();
    }


    private D2RezeptBestand.Stueck itemAt(float x, float y) {
        if (items == null) return null;
        float gx = (x - viewLeft) / scale, gy = (y - viewTop) / scale;
        float invX = gameWidth - ox - INV_RIGHT, invY = oy + INV_Y;
        if (gx < invX || gx >= invX + INV_COLUMNS * CELL || gy < invY || gy >= invY + INV_ROWS * CELL) return null;
        int cx = (int) ((gx - invX) / CELL), cy = (int) ((gy - invY) / CELL);
        for (D2RezeptBestand.Stueck s : items) if (s.place == D2RezeptBestand.PLACE_INVENTORY && s.covers(cx, cy)) return s;
        return null;
    }







    boolean pad(int button) {
        D2RezeptBuch b = book;
        if (b == null || !b.isOpen() || !placed) return false;
        switch (button) {
            case D2Gamepad.DPAD_UP: b.padMove(-1); break;
            case D2Gamepad.DPAD_DOWN: b.padMove(1); break;
            case D2Gamepad.LB: b.padTab(-1); break;
            case D2Gamepad.RB: b.padTab(1); break;
            case D2Gamepad.DPAD_LEFT: b.padChip(-1); break;
            case D2Gamepad.DPAD_RIGHT: b.padChip(1); break;
            case D2Gamepad.A:
                if (b.detail() != null && !b.detail().isRuneword()) insert(b.detail());
                else b.padPress();
                break;
            case D2Gamepad.B: back(); break;
            case D2Gamepad.X: search(); break;
            default: return false;
        }
        host.redraw();
        return true;
    }

    boolean back() {
        D2RezeptBuch b = book;
        if (b == null || !b.isOpen()) return false;
        b.back();
        if (!b.isOpen()) unpause();
        host.redraw();
        return true;
    }






    void openFromMenu(boolean playerPausedBefore) {
        D2RezeptBuch b = book;
        if (b == null || !placed) {
            host.note(host.text(R.string.rb_unavailable));
            return;
        }
        b.open();
        if (!playerPausedBefore) {
            if (!host.paused()) host.pause(true);
            pausedByBook = true;
        }
        host.redraw();
    }







    void openWithCube() {
        D2RezeptBuch b = book;
        D2ModState s = host.state();
        D2ModLink link = host.link();
        if (b == null || !placed || s == null || link == null) {
            host.note(host.text(R.string.rb_unavailable));
            return;
        }
        int step = D2RezeptOeffnen.plan(items, cubeOpen, s.isOpen(D2ModState.UI_STASH), host.paused());
        D2Trace.log("recipes: open with the cube, step " + step);
        if (step == D2RezeptOeffnen.BOOK) {
            b.open();
            overCube = true;
            host.redraw();
        }
        else if (step == D2RezeptOeffnen.SEND) {
            link.cubeOpen();
            cubeWaitUntil = android.os.SystemClock.uptimeMillis() + D2RezeptOeffnen.OPEN_WAIT_MS;
        }

        else readOnly(step == D2RezeptOeffnen.READ_STASH ? R.string.rb_open_stash
            : D2RezeptOeffnen.where(items) == D2RezeptBestand.PLACE_INVENTORY ? 0 : R.string.rb_open_none);
    }


    private void readOnly(int note) {
        openFromMenu(host.paused());
        if (note != 0 && book != null && book.isOpen()) host.note(host.text(note));
    }


    private void checkCubeWait() {
        if (cubeWaitUntil == 0) return;
        D2RezeptBuch b = book;
        if (cubeOpen && b != null) {
            cubeWaitUntil = 0;
            D2Trace.log("recipes: the cube opened, the book over it");
            b.open();
            overCube = true;
            host.redraw();
        }
        else if (android.os.SystemClock.uptimeMillis() > cubeWaitUntil) {
            cubeWaitUntil = 0;
            D2Trace.log("recipes: the cube did not open, the book alone");
            int where = D2RezeptOeffnen.where(items);
            readOnly(where == D2RezeptBestand.PLACE_STASH ? R.string.rb_open_stash
                : where == D2RezeptBestand.PLACE_INVENTORY ? 0 : R.string.rb_open_none);
        }
    }


    void openFor(String code) {
        D2RezeptBuch b = book;
        if (b == null || !placed) return;
        b.openFor(code);
        host.redraw();
    }


    String materialsLine(String code, String name) {
        D2RezeptBuch b = book;
        return b != null && placed ? b.materialsLine(code, name) : null;
    }

    private void close(String why) {
        D2RezeptBuch b = book;
        if (b != null) b.close();
        D2Trace.log("recipes: closed (" + why + ")");
        unpause();
    }

    private void unpause() {
        if (!pausedByBook) return;
        pausedByBook = false;
        host.pause(false);
    }








    void insert(D2RezeptKatalog.Eintrag e) {
        D2RezeptBuch b = book;
        D2ModLink link = host.link();
        if (b == null || e == null || link == null || pendingIds != null || fetchEntry != null) return;
        int caps = link.capabilities();
        if (caps < 0 || (caps & D2Points.CAP_EQUIP) == 0) {
            host.note(host.text(R.string.rb_insert_unavailable));
            return;
        }
        if (!cubeOpen) {
            host.note(host.text(R.string.rb_insert_cube_closed));
            return;
        }
        if (host.paused()) {
            host.note(host.text(R.string.rb_insert_paused));
            return;
        }
        if (items == null) {
            host.note(host.text(R.string.rb_stale_none));
            return;
        }
        D2ModItems carried = link.items();
        int invW = carried != null ? carried.gridWidth() : D2ModItems.INVENTORY_WIDTH;
        int invH = carried != null ? carried.gridHeight() : D2ModItems.INVENTORY_HEIGHT;
        D2RezeptEinlegen.Plan p = D2RezeptEinlegen.plan(e, b.bestand(), invW, invH);
        String what = p.zutat >= 0 ? p.count + "x " + e.zutaten.get(p.zutat).label : "";
        D2Trace.log("recipes: insert \"" + e.name + "\": outcome " + p.outcome + (p.zutat >= 0 ? " (" + what + ")" : ""));
        switch (p.outcome) {
            case D2RezeptEinlegen.OK:
                sendCube(link, p);
                return;
            case D2RezeptEinlegen.FETCH:
                startFetch(link, e, p, what);
                return;
            case D2RezeptEinlegen.RUNEWORD: host.note(host.text(R.string.rb_insert_runeword)); return;
            case D2RezeptEinlegen.MISSING: host.note(host.text(R.string.rb_insert_missing, what)); return;
            case D2RezeptEinlegen.IN_MATERIALS: host.note(host.text(R.string.rb_insert_materials, what)); return;
            case D2RezeptEinlegen.ON_PAGE:
                host.note(host.text(p.shared ? R.string.rb_insert_page_shared : R.string.rb_insert_page, what, p.page));
                return;
            case D2RezeptEinlegen.CHOOSE: host.note(host.text(R.string.rb_insert_choose, e.zutaten.get(p.zutat).label)); return;
            case D2RezeptEinlegen.CUBE_FULL: host.note(host.text(R.string.rb_insert_cube_full)); return;
            default: host.note(host.text(R.string.rb_insert_inventory_full));
        }
    }

    private void sendCube(D2ModLink link, D2RezeptEinlegen.Plan p) {
        link.cube(p.ids);
        pendingIds = p.ids;
        pendingStash = p.fromStash;
        pendingUntil = android.os.SystemClock.uptimeMillis() + INSERT_WAIT_MS;
    }








    private void startFetch(D2ModLink link, D2RezeptKatalog.Eintrag e, D2RezeptEinlegen.Plan p, String what) {
        D2Materials.Snapshot m = link.materials();
        fetchWhat = what;
        if (m == null || !m.possible() || m.locked()) {
            host.note(host.text(R.string.rb_insert_materials, what));
            return;
        }
        fetchLeft.clear();
        for (Map.Entry<String, Integer> f : p.fetch.entrySet()) {
            int kind = D2Materials.kindOf(f.getKey());
            if (kind < 0) {
                host.note(host.text(R.string.rb_insert_materials, what));
                return;
            }
            fetchLeft.add(new int[]{kind, f.getValue()});
        }
        fetchEntry = e;
        nextFetch(link, m);
    }

    private void nextFetch(D2ModLink link, D2Materials.Snapshot m) {
        fetchSent = fetchLeft.poll();
        fetchUntil = android.os.SystemClock.uptimeMillis() + D2Materials.ANSWER_MS;
        if (fetchSent == null) return;
        fetchBase = m.stash[fetchSent[0]];
        D2Trace.log("recipes: fetch " + fetchSent[1] + "x " + D2Materials.code(fetchSent[0]) + " from the materials");
        link.materials(D2Materials.ORDER_WITHDRAW, fetchSent[0], fetchSent[1]);
    }

    private void endFetch(String why) {
        D2Trace.log("recipes: fetch ended: " + why);
        fetchEntry = null;
        fetchSent = null;
        fetchLeft.clear();
        host.note(host.text(R.string.rb_insert_materials, fetchWhat));
    }


    private void checkFetch(D2Materials.Snapshot m) {
        if (fetchEntry == null) return;
        D2ModLink link = host.link();
        D2RezeptBuch b = book;
        if (link == null || b == null || !cubeOpen) {
            endFetch("the cube or the book closed");
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (fetchSent != null) {
            if (m != null && m.stash[fetchSent[0]] <= fetchBase - fetchSent[1]) nextFetch(link, m);
            else if (now > fetchUntil) endFetch("withdrawal not seen (stash out of reach?)");
            return;
        }
        D2ModItems carried = link.items();
        int invW = carried != null ? carried.gridWidth() : D2ModItems.INVENTORY_WIDTH;
        int invH = carried != null ? carried.gridHeight() : D2ModItems.INVENTORY_HEIGHT;
        D2RezeptEinlegen.Plan p = D2RezeptEinlegen.plan(fetchEntry, b.bestand(), invW, invH);
        if (p.outcome == D2RezeptEinlegen.OK) {
            D2Trace.log("recipes: fetched, insert \"" + fetchEntry.name + "\"");
            fetchEntry = null;
            sendCube(link, p);
        }
        else if (now > fetchUntil) endFetch("not in the stock after the withdrawal (outcome " + p.outcome + ")");
    }


    private void checkInsert(long now) {
        if (pendingIds == null) return;
        if (D2RezeptEinlegen.inCube(items, pendingIds)) {
            pendingIds = null;
            D2Trace.log("recipes: insert done");
            host.note(host.text(R.string.rb_insert_done));

            if (book != null && book.isOpen()) close("inserted");
        }
        else if (android.os.SystemClock.uptimeMillis() > pendingUntil) {
            pendingIds = null;
            D2Trace.log("recipes: insert not seen in the cube");
            host.note(host.text(pendingStash ? R.string.rb_insert_failed_stash : R.string.rb_insert_failed));
        }
    }




    private void search() {
        D2RezeptBuch b = book;
        Context context = host.context();
        if (b == null || searchDialog != null && searchDialog.isShowing()) return;

        if (!host.paused()) pausedByBook = true;
        EditText field = new EditText(context);
        field.setSingleLine(true);
        field.setText(b.query());
        field.setSelection(field.getText().length());
        field.setHint(D2Text.get(context, R.string.rb_search_hint));
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI | android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        D2Style.text(field);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                b.search(s.toString());
                host.redraw();
            }
        });
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (searchDialog != null) searchDialog.dismiss();
            return true;
        });
        searchDialog = new AlertDialog.Builder(context).setView(field).create();
        android.view.Window w = searchDialog.getWindow();
        if (w != null) {
            w.setGravity(android.view.Gravity.TOP);
            w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        searchDialog.show();
        field.requestFocus();
    }
}
