package com.winlator.d2;

import com.winlator.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.winlator.renderer.ViewTransformation;
import com.winlator.winhandler.MouseEventFlags;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.XServer;

import java.util.ArrayDeque;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


























@SuppressLint("ViewConstructor")
public class D2TouchOverlay extends View {

    private static final int VK_TAB = 0x09, VK_SHIFT = 0x10, VK_MENU = 0x12, VK_ESCAPE = 0x1B, VK_F1 = 0x70,
        VK_RETURN = 0x0D, VK_UP = 0x26, VK_DOWN = 0x28;
    private static final int VK_1 = 0x31, VK_2 = 0x32, VK_C = 0x43, VK_I = 0x49, VK_O = 0x4F, VK_Q = 0x51, VK_R = 0x52, VK_T = 0x54,
        VK_W = 0x57;
    private static final int KEYEVENTF_KEYUP = 2;

    private static final int KIND_ATTACK = 1, KIND_SKILL = 2, KIND_POTION = 3, KIND_KEY = 4, KIND_PANEL = 5,
        KIND_ESCAPE = 6, KIND_LABELS = 7, KIND_CONTROLS = 8, KIND_MENU = 9, KIND_PLAIN_KEY = 10,
        KIND_EDIT_DONE = 11, KIND_EDIT_RESET = 12, KIND_EDIT_BIGGER = 13, KIND_EDIT_SMALLER = 14, KIND_EDIT_DEBUG = 15,
        KIND_INTERACT = 16, KIND_MENU_TOGGLE = 17, KIND_PAUSE = 18, KIND_PORTAL = 19, KIND_RESPEC = 21, KIND_SHOP_POTIONS = 22, KIND_SORT = 23,
        KIND_RECIPES = 24;

    private static final int ICON_TEXT = -1, ICON_RUN = 100, ICON_ARROW = 101, ICON_LINES = 102, ICON_PAUSE = 103, ICON_PORTAL = 104,
        ICON_CUBE = 105;

    private static final int SKILL_BOOK_OF_TOWNPORTAL = 220;

    private static final int GROUP_GAME = 1, GROUP_MENU = 2, GROUP_HIDDEN = 3, GROUP_EDIT = 4;
    private static final int ROLE_NONE = 0, ROLE_JOYSTICK = 1, ROLE_BUTTON = 2, ROLE_DIRECT = 3, ROLE_DRAG = 4, ROLE_ASSIGN = 5, ROLE_RESUME = 6,
        ROLE_MATERIALS = 7,
        ROLE_RESUME_HINT = 8,
        ROLE_WORLD = 9,
        ROLE_MERC = 10,
        ROLE_EFFECT = 11,
        ROLE_INFO = 12,
        ROLE_TREE_PICK = 13,
        ROLE_RECIPES = 14,
        ROLE_HINT = 15,
        ROLE_INPUT_BOX = 16;
    private static final int ACTION_NONE = 0, ACTION_WALK = 1, ACTION_DIRECT = 2, ACTION_ATTACK = 3, ACTION_SKILL = 4, ACTION_PICKUP = 5,
            ACTION_SELECT = 6,
            ACTION_ATTACK_TAP = 7;

    private static final int TARGET_LIFT = 14;

    private static final float GOLD_RADIUS = 100;

    private static final int[] MONSTER_OFFSETS = {-14, -28, -4, -42};
    private static final int[] ITEM_OFFSETS = {-3, -10, 4, -18};
    private static final int[] OBJECT_OFFSETS = {-10, -24, 0, -38};
    private static final long LONG_PRESS_MS = 450;

    private static final long TRIGGER_TOUCH_WINDOW_MS = 200;

    private static final long HOVER_SETTLE_MS = 80;
    private static final String PREFS = "d2overlay";

    private static final class Button {
        final String id;
        String label;
        final int kind;
        final int vk;
        final int group;
        final boolean rect;
        float cx, cy, r, hw;

        float shiftX, shiftY;

        boolean saved;
        boolean pressed;

        int icon = ICON_TEXT;

        String caption;




        int heldAction = D2TriggerAction.NOTHING, heldVk;

        Button(String id, String label, int kind, int vk, int group, boolean rect) {
            this.id = id;
            this.label = label;
            this.kind = kind;
            this.vk = vk;
            this.group = group;
            this.rect = rect;
        }

        boolean contains(float x, float y, float slop) {
            if (rect) return Math.abs(x - cx) <= hw + slop && Math.abs(y - cy) <= r + slop;
            float dx = x - cx, dy = y - cy;
            return dx * dx + dy * dy <= (r + slop) * (r + slop);
        }
    }

    private static final class Touch {
        int role;
        int deviceId;
        long downTime;
        Button button;
        float downX, downY, x, y;
        boolean moved, longPressed, hoverOnly;

        boolean cancelled;

        boolean cycled;
        float stepX, stepY;

        int infoTap;
        Runnable longPress;
    }

    private interface Step {

        boolean run(long now);
    }

    private final XServer xServer;
    private final WinHandler winHandler;
    private final Runnable openMenu;
    private final SharedPreferences prefs;

    private final D2PointHighlight pointHighlight;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final float density;

    private float dp;

    private float uiFactor = 1;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final RectF noteRect = new RectF();
    private boolean noteRectShown;
    private final Paint iconPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final List<Button> buttons = new ArrayList<>();
    private final SparseArray<Touch> touches = new SparseArray<>();

    private Object skillOwner;
    private boolean skillAimedByDrag;


    private boolean joystickActive;

    private int joystickOwner = D2Gamepad.OWNER_NONE;
    private float idleX, idleY, baseX, baseY, knobX, knobY, joystickRadius;
    private final D2JoystickFollow joystickFollow = new D2JoystickFollow();
    private float dirX = 1, dirY = 0;
    private boolean hasDirection;


    private int action = ACTION_NONE;

    private boolean lmbDown, rmbDown, shiftDown, labelsOn, controlsVisible = false, hiddenByPanel = true;

    private boolean startDone;
    private boolean graphicsDone;

    private boolean modExpected = true;

    private final D2StartupControls.Clock startClock = new D2StartupControls.Clock(SystemClock.uptimeMillis());
    private final Runnable revealStart = this::revealAtStart;

    private boolean altDown;

    private boolean namesSent;

    private boolean mapHack, mapSent;

    private boolean compareEquipped, compareSent;

    private boolean compareCapable, lootCapable;

    private int lootMaskSent;

    private boolean valueLine, valueCapable;

    private final D2MaterialsWindow materialsWindow;
    private boolean materials, materialsCapable, materialsBackupRunning;
    private final float[] materialsMapping = new float[3];
    private File gameDir;
    private long valueSentAt;

    private boolean diffLines, diffCapable, diffSent;

    private int xpBitsSent;
    private long diffSentAt;
    private byte[] valuePayload;
    private boolean valuePayloadLadder;


    private int damageStyle, damageStyleSent;
    private boolean damageCapable, damageDpsSent;
    private boolean editMode, debug;

    private String cutoutDone;

    private volatile D2Hud hud;

    private Button assignButton;

    private boolean assignRight;

    private final RectF assignHint = new RectF();

    private static final Set<Integer> AURA_SKILLS = new HashSet<>(java.util.Arrays.asList(98, 99, 100, 102, 103, 104,
        105, 108, 109, 110, 113, 114, 115, 118, 119, 120, 122, 123, 124, 125));
    private long assignSince;

    private int treePickSkill = -1;
    private long treePickSince;
    private static final long TREE_PICK_MS = 15000, TREE_PICK_CHECK_MS = 1200;

    private boolean pauseWanted, autoPause;

    private long noStateSince;

    private boolean labelsWanted, labelsHeld, alwaysRun, orbNumbers, hidePointer, autoTrade, skillNames;

    private D2InfoStrip infoStrip;

    private File saveDir;

    private final D2Effects effectsView = new D2Effects();

    private boolean npcMenuWasOpen;

    private java.util.Set<String> pinnedMenu = new HashSet<>();
    private boolean pointerWanted;
    private Boolean itemLabelsSetting;

    private boolean runChecked;

    private final D2Retarget retarget = new D2Retarget();
    private final Runnable retargetLater = this::retargetAfterGap;

    private static final Set<Integer> CORPSE_SKILLS = new HashSet<>(java.util.Arrays.asList(70, 74, 80, 95, 131, 142, 150));

    private boolean skillOnCorpses;
    private long corpseClickAt;

    private final Button leftTrigger = new Button("triggerL", "L", KIND_SKILL, VK_F1 + 6, GROUP_GAME, false);
    private final Button rightTrigger = new Button("triggerR", "R", KIND_SKILL, VK_F1 + 7, GROUP_GAME, false);
    private final Paint roundIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final android.graphics.ColorFilter PRESSED_FILTER = new android.graphics.LightingColorFilter(0xFFB0B0B0, 0);
    private final android.graphics.Matrix iconMatrix = new android.graphics.Matrix();
    private final Map<android.graphics.Bitmap, android.graphics.BitmapShader> iconShaders = new HashMap<>();
    private long pauseSentAt;
    private int hudLayout;
    private boolean menuOpen;




    private boolean talkFold, talkFoldDismissed;


    private boolean menuShownOpen() {
        return menuOpen && !talkFold;
    }


    private void foldForTalk(boolean talking) {
        if (!talking) talkFoldDismissed = false;
        boolean fold = talking && menuOpen && !talkFoldDismissed;
        if (fold == talkFold) return;
        talkFold = fold;
        D2Trace.log("menu row " + (fold ? "folded while an NPC talks" : "back as set"));
        if (getWidth() > 0) layoutButtons(getWidth(), getHeight());
        invalidate();
    }
    private long hudSentAt;

    private long hudFirstSentAt;

    static final long HUD_ECHO_MS = 2000;
    private float scale = 1;


    private int targetX = -1, targetY = -1;
    private long atTargetSince;
    private int lastSentFromX = Integer.MIN_VALUE, lastSentFromY = Integer.MIN_VALUE;
    private long lastSendTime;
    private int lastDx, lastDy, stalls;

    private float gainX = 1, gainY = 1;
    private int clickErrorX, clickErrorY;
    private boolean clickMeasured;









    private static final long SYNC_WAIT_MS = 150;
    private static final int SYNC_TOLERANCE = 24;
    private long syncAskedAt;
    private int syncFeedbacks;
    private int syncMouseX = Integer.MIN_VALUE, syncMouseY, syncCount;
    private long syncedPanels = -1;
    private final ArrayDeque<Step> steps = new ArrayDeque<>();
    private boolean pumpScheduled;
    private final Runnable pump = this::pump;


    private float aspect = 1, offsetX, offsetY;


    private final SparseArray<String> triggerDevices = new SparseArray<>();
    private final Set<Integer> fingerDevices = new HashSet<>();
    private final Set<Integer> triggerTouchDevices = new HashSet<>();
    private final SparseArray<String> deviceNames = new SparseArray<>();
    private long lastTriggerDownTime = -10000;
    private int ignoredTriggerTouches;


    private final ArrayDeque<String> events = new ArrayDeque<>();


    private final D2ModLink modLink;
    private D2ModState mod;

    private final D2Idle idle;
    private final D2ResumeHint resumeHint;

    private boolean pausedBeforeFocusLoss;
    private volatile boolean modUpdatePosted;

    private long attackTargetId, skillTargetId;
    private D2ModState.Monster softTarget;

    private final D2BeltRows beltRows = new D2BeltRows();

    private Object padPreview;
    private long padPreviewKey = D2PadTarget.NONE;
    private boolean padPreviewOn;

    private D2ModState.Monster aAttackTarget;


    private Button padAttackOwner;
    private boolean aByPadChoice;
    private Object interactCandidate;
    private Button interactButton;
    private long pickupId, pickupStart, pickupTimeout, pickupClickAt;

    private boolean pickupForHeld;

    private boolean pickupEntrance;

    private boolean pickupCorpse;
    private int pickupLevel;
    private float pickupDirX, pickupDirY;
    private final Map<Long, Long> pickupTried = new HashMap<>();

    private final D2AutoItems autoItems = new D2AutoItems();

    private D2ShopButton shopButton;

    private final D2LabelFilter labelFilter = new D2LabelFilter();

    private String lootScope;
    private long labelFilterSentAt;

    private boolean autoGold, aimAssist, highlightOn, itemsNeedLabels, directWalk;

    private boolean tapMoves;
    private final D2WorldTap.Conditions worldTap = new D2WorldTap.Conditions();
    private int aimRangePct, aimRangeMeleePct, aimConeDeg, potionMode;

    private int aimBlindDeg;





    private float faceX = 1, faceY = 0;
    private boolean hasFacing;

    private int staminaMode, ringSort, autoLifePct, autoManaPct, autoRejuvPct, mercPotionPct;

    private int triggerLeftAction, triggerRightAction;

    private boolean padEnabled = true;
    private int padDeadzone = D2Gamepad.DEADZONE_DEFAULT;
    private int[][] padMap = D2Gamepad.parseMap(null);

    private boolean padActive;

    private boolean padStickActive, padStickHasDirection;
    private float padDirX = 1, padDirY;



    private boolean padLayer2;
    private int padCursorSpeed = D2Gamepad.CURSOR_SPEED_DEFAULT;
    private long padPointerAt, padPointerTick, padPointerFullSince;
    private float padRestX, padRestY;

    private static final int HELD_LEFT_CLICK = -20, HELD_RIGHT_CLICK = -21;

    private final Button[] padOwners = new Button[D2Gamepad.COUNT];
    private final Button skillKeyF5 = new Button("padF5", "F5", KIND_SKILL, VK_F1 + 4, GROUP_GAME, false);
    private final Button skillKeyF6 = new Button("padF6", "F6", KIND_SKILL, VK_F1 + 5, GROUP_GAME, false);

    private final D2GamepadInput pad = new D2GamepadInput(this);
    private boolean unlimitedWanted;

    private boolean joystickVisible = true;
    private int players = 1;
    private boolean playersSent;
    private boolean largeStacks;
    private long stacksSentAt;
    private long playersSentAt;

    private static final long AUTOSAVE_MS = 60_000, SAVE_BEFORE_PAUSE_MS = 300;
    private boolean autosave, respec;

    private boolean sortOn = true;

    private long savedAt;
    private final Runnable sendPause = this::sendPauseNow;

    private long respecArmedUntil;

    private long interactSeenAt;

    private long lastFightAt;

    private String noteText;
    private long noteUntil;





    private static final long SORT_HOLD_MS = 600, SORT_LOCK_MS = 30000;




    private static final long SORT_ANSWER_MS = 2000, SORT_GONE_MS = 5000;

    private int sortWaitFor = -1;
    private long sortLockSince, sortNoDataSince;
    private boolean sortAnswered, sortCursorLock, pauseAfterSort;

    private boolean attackWalking;

    private float confirmX = -1, confirmY = -1;
    private long confirmAt;

    private final D2PointTaps pointTaps = new D2PointTaps();




    private Runnable pendingTap;
    private float pendingTapX, pendingTapY;
    private int pendingTapCellX, pendingTapCellY;

    private long pendingTapWare = -1;

    private int pendingTapPlace = -1;
    private boolean pendingTapHeld;

    private static final java.util.Set<Integer> AMMO_SKILLS = new java.util.HashSet<>(java.util.Arrays.asList(
        0, 2, 4, 6, 7, 11, 12, 15, 16, 20, 21, 22, 25, 26, 27, 31, 35, 140));

    private final D2AutoDrink autoPotions = new D2AutoDrink();

    private volatile java.util.Set<Long> idleIgnored = java.util.Collections.emptySet();


    void ignoreTargets(java.util.Set<Long> ids) {
        idleIgnored = ids != null ? ids : java.util.Collections.emptySet();
    }
    private long aimPreviewUntil;

    private boolean walkDirect;
    private final Runnable walkKeepAlive = this::walkKeepAlive;

    private long interactPinnedId;
    private int interactCount, interactIndex;
    private long pickupToken;

    private boolean buttonPending;
    private int targetMissing;
    private int hoverHits, hoverMisses;

    private boolean directAttack;
    private long skillToken;

    private long skillPressAt = -1;
    private boolean skillPressDirect;

    private long skillWalkToken;

    private long attackPressAt = -1, attackTapToken;

    private boolean skillLeft;

    private final D2LeftRestore leftRestore = new D2LeftRestore();

    private boolean skillFirstPending;

    private long skillEarlyTarget = -1;
    private final Runnable keepAlive = this::keepAlive;

    private Object highlighted;
    private long highlightSentAt;
    private String lastPotion = "–";







    private final D2UsedObjects usedObjects = new D2UsedObjects();

    private final D2OfferCheck offerCheck = new D2OfferCheck();


    private java.util.Set<Long> leftOutObjects() {
        return usedObjects.objectIds(SystemClock.uptimeMillis());
    }
    private int pickupObjectKind = -1;

    private int pickupUnitType = -1;

    private static final long UI_UNKNOWN = -3, UI_OUT_OF_GAME = -2;
    private long appliedUiKey = UI_UNKNOWN;
    private boolean windowFocused = true;
    private final Runnable modWatchdog = this::onModUpdate;

    private final float[][] calibration = new float[24][4];
    private int calibrationCount, calibrationNext, stableUpdates, lastPointerX = -1, lastPointerY = -1, calibratedWidth, calibratedHeight;
    private boolean calibrated;
    private float calibKx, calibBx, calibKy, calibBy;

    private float positionShiftX, positionShiftY;

    public D2TouchOverlay(Context context, XServer xServer, WinHandler winHandler, Runnable openMenu) {
        super(context);
        this.xServer = xServer;
        this.winHandler = winHandler;
        this.openMenu = openMenu;
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        pointHighlight = new D2PointHighlight(context, prefs, this::showNote);
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.density = metrics.density;

        this.dp = density * D2UiScale.overlay(metrics.widthPixels, metrics.heightPixels, density,
            context.getResources().getConfiguration().smallestScreenWidthDp);
        scale = prefs.getFloat("scale", 1);
        debug = com.winlator.BuildConfig.D2_INTERNAL && prefs.getBoolean("debug", false);
        reloadSettings();
        materialsWindow = new D2MaterialsWindow(new MaterialsHost());
        hintView = new D2HintView(context, hintHost(), dp);
        modLink = new D2ModLink((state) -> {
            if (modUpdatePosted) return;
            modUpdatePosted = true;
            handler.post(this::onModUpdate);
        });
        shopButton = new D2ShopButton(context, modLink, autoItems);
        idle = new D2Idle(new D2Idle.Host() {
            @Override public D2ModState state() { return modLink.state(); }
            @Override public D2ModLink link() { return modLink; }
            @Override public String layout() { return spieltestLayout(false); }
            @Override public boolean pauseWanted() { return pauseWanted; }
            @Override public void setPaused(boolean paused) { D2TouchOverlay.this.setPaused(paused); }
            @Override public void dispatch(MotionEvent event) { dispatchTouchEvent(event); }
            @Override public void message(String text) { showNote(text); }
            @Override public String text(int id, Object... args) { return D2TouchOverlay.this.text(id, args); }
            @Override public void redraw() { invalidate(); }
            @Override public com.winlator.d2.test.Fertigkeiten skills() { D2Hud h = hud; return h != null ? h.fertigkeiten : null; }
            @Override public D2Tabellen tabellen() { D2Hud h = hud; return h != null ? h.tabellen : null; }
            @Override public void ignoreTargets(java.util.Set<Long> ids) { D2TouchOverlay.this.ignoreTargets(ids); }

            @Override public boolean pressHotkey(int index) {
                if (index < 0 || index >= 16) return false;
                pressKey(VK_F1 + index);
                return true;
            }
            @Override public java.util.Set<Long> leftOutObjects() { return D2TouchOverlay.this.leftOutObjects(); }
            @Override public D2PointPlan.Build build(int cls, String name) { return pointHighlight.build(cls, name); }
            @Override public boolean buildChosen(int cls, String name) { return pointHighlight.chosen(cls, name); }
            @Override public File saveDir() { return saveDir; }
            @Override public com.winlator.d2.test.Stadtgang.Einkauf einkauf(com.winlator.d2.test.Zustand z) {
                return shopButton != null ? shopButton.einkauf(z) : null;
            }
        });

        resumeHint = new D2ResumeHint(context, modLink, new D2ResumeHint.Host() {
            @Override public void note(String text, long ms) { showNote(text, ms); }
            @Override public String text(int id, Object... args) { return D2TouchOverlay.this.text(id, args); }
            @Override public boolean idle() { return idle.phase() != D2Idle.Phase.OFF; }
            @Override public boolean focused() { return windowFocused; }
            @Override public void arrivalPause() { setPaused(true); }
            @Override public void saveNow() { if (autosave && mod != null && mod.plugyLoaded) save(SystemClock.uptimeMillis()); }
        });
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2 * dp);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.argb(220, 255, 255, 255));
        setClickable(true);
    }


    D2ModLink modLink() {
        return modLink;
    }


    public void reloadSettings() {
        Context context = getContext();
        if (infoStrip == null) infoStrip = new D2InfoStrip(context);
        infoStrip.applySettings();
        autoGold = D2Settings.autoGold(context);
        labelFilter.load(context, lootScope);
        mapFightHide = D2Settings.mapFightHide(context);
        mapFightDelayMs = D2Settings.mapFightDelay(context) * 1000L;
        autoItems.load(context);
        aimAssist = D2Settings.aimAssist(context);
        aimRangePct = D2Settings.aimRange(context);
        aimRangeMeleePct = D2Settings.aimRangeMelee(context);
        aimConeDeg = D2Settings.aimCone(context);
        aimBlindDeg = D2Settings.aimBlind(context);
        highlightOn = D2Settings.highlight(context);
        potionMode = D2Settings.potions(context);
        staminaMode = D2Settings.staminaMode(context);
        ringSort = D2Settings.ringSort(context);
        triggerLeftAction = D2Settings.triggerLeft(context);
        triggerRightAction = D2Settings.triggerRight(context);
        padEnabled = D2Settings.padEnabled(context);
        padDeadzone = D2Settings.padDeadzone(context);
        padCursorSpeed = D2Settings.padCursorSpeed(context);
        padMap = D2Gamepad.parseMap(D2Settings.padMap(context));
        if (!padEnabled) padUsed(false);
        joystickVisible = D2Settings.joystickVisible(context);
        hints.decode(D2Settings.hintsSeen(context));
        int oldPlayers = players;
        players = D2Settings.players(context);

        if (players != oldPlayers && modLink != null && mod != null && playersSent) modLink.players(players);
        autosave = D2Settings.autosave(context);
        respec = D2Settings.respec(context);
        sortOn = D2Settings.sortInventory(context);
        autoLifePct = D2Settings.autoLife(context);
        autoRejuvPct = D2Settings.autoRejuv(context);
        mercPotionPct = D2Settings.mercPotions(context);
        autoManaPct = D2Settings.autoMana(context);
        itemsNeedLabels = D2Settings.itemsNeedLabels(context);
        int oldHud = hudLayout;
        hudLayout = D2Settings.hud(context);
        if (oldHud != hudLayout && getWidth() > 0) layoutButtons(getWidth(), getHeight());
        if (oldHud == D2Settings.HUD_COMPACT && hudLayout != oldHud && modLink != null) modLink.hud(false, true, pointerWanted, unlimitedWanted, namesSent, mapSent, compareSent, damageStyleSent, damageDpsSent, diffSent);
        menuOpen = D2Settings.menuOpen(context);
        autoPause = D2Settings.autoPause(context);
        boolean labelsSetting = D2Settings.itemLabels(context);

        if (itemLabelsSetting == null || labelsSetting != itemLabelsSetting) labelsWanted = labelsSetting;
        itemLabelsSetting = labelsSetting;
        alwaysRun = D2Settings.alwaysRun(context);
        mapHack = D2Settings.mapHack(context);
        compareEquipped = D2Settings.compareEquipped(context);
        windowInfoOn = D2Settings.windowInfo(context);
        compareCapable = D2Settings.compareCapable(context);
        lootCapable = D2Settings.lootCapable(context);
        valueLine = D2Settings.valueLine(context);
        valueCapable = D2Settings.valueCapable(context);
        diffLines = D2Settings.diffLines(context);
        diffCapable = D2Settings.diffCapable(context);
        damageStyle = D2Settings.damageStyle(context);
        materials = D2Settings.materials(context);
        materialsCapable = D2Settings.materialsCapable(context);
        damageCapable = D2Settings.damageCapable(context);
        largeStacks = D2Settings.largeStacks(context);
        orbNumbers = D2Settings.orbNumbers(context);
        hidePointer = D2Settings.hidePointer(context);
        autoTrade = D2Settings.autoTrade(context);
        skillNames = D2Settings.skillNames(context);
        java.util.Set<String> pinned = D2Settings.pinnedMenu(context);
        boolean pinsChanged = !pinned.equals(pinnedMenu);
        pinnedMenu = pinned;
        if (pinsChanged && getWidth() > 0) layoutButtons(getWidth(), getHeight());
        tapMoves = D2Settings.tapMoves(context);
        boolean wasDirect = directWalk;
        directWalk = D2Settings.directWalk(context);
        if (wasDirect && !directWalk && action == ACTION_WALK && walkDirect) {
            stopDirectWalk();
            action = ACTION_NONE;
            if (joystickActive) startWalk();
        }
        invalidate();
    }





    private boolean gridOriginal = true;

    private File modSaves;


    private File materialsKey() {
        return modSaves != null ? modSaves : gameDir;
    }


    public void setGameDir(final File dir, final File saves, boolean originalGrid) {
        if (dir == null) return;
        gridOriginal = originalGrid;
        setGameDir(dir);
        modSaves = null;
        if (saves != null && !saves.equals(D2Saves.saveDir(dir))) {
            modSaves = saves;
            saveDir = saves;
            if (infoStrip != null) infoStrip.setSaveDir(saves);
            resumeHint.setSaveDir(saves);
        }
    }


    public void setGameDir(final File dir) {
        if (dir == null) return;
        saveDir = D2Saves.saveDir(dir);
        gameDir = dir;
        if (infoStrip != null) infoStrip.setSaveDir(saveDir);
        resumeHint.setGameDir(dir);

        D2LevelNames.setCurrent(null);
        recipes.load(dir, handler);
        new Thread(() -> {
            try {
                final D2Hud loaded = D2Hud.load(dir);
                handler.post(() -> {
                    hud = loaded;
                    graphicsDone = true;
                    effectsView.setTexts(getContext(), loaded);

                    if (loaded.language() != null) D2Settings.setGameLanguage(getContext(), loaded.language());
                    D2Style.useGameFrame(loaded);
                    if (getWidth() > 0) layoutButtons(getWidth(), getHeight());
                    revealAtStart();
                    invalidate();
                });
            }
            catch (Throwable e) {
                android.util.Log.w("D2Hud", "game graphics not available", e);
                handler.post(() -> {
                    graphicsDone = true;
                    revealAtStart();
                });
            }
        }, "D2HudLoad").start();
    }


    void showBuildPage(android.app.Activity activity) {
        D2BuildDialog.show(activity, hud, mod, modLink.points(3000), modLink.characterName(), pointHighlight, this::startRespecSpend);
    }



    private final D2RespecSpend respecSpend = new D2RespecSpend();
    private final D2PointSpend.Out respecOut = new D2PointSpend.Out() {
        @Override
        public void send(int kind, int what) {
            D2Trace.log("respec spend: " + (kind == D2Points.KIND_SKILL ? "skill " : "stat ") + what);
            modLink.points(kind, what, -1);
        }

        @Override
        public void log(String text) {
            D2Trace.log(text);
        }
    };
    private String respecNote;
    private long respecNoteAt;
    private int respecShownState = D2RespecSpend.OFF;


    void startRespecSpend() {
        D2ModState s = mod;
        String name = modLink.characterName();
        if (s == null || name == null || s.characterClass < 0) return;
        respecSpend.start(s.characterClass, name);
        respecShownState = D2RespecSpend.OFF;
        D2Trace.log("respec spend: confirmed for " + name);
    }





    private void stepRespecSpend(D2ModState s, long now) {
        int was = respecSpend.state();
        if (was == D2RespecSpend.OFF) return;
        String name = modLink.characterName();
        com.winlator.d2.test.Fertigkeiten table = hud != null ? hud.fertigkeiten : null;
        if (respecSpend.active() && table != null) {
            boolean idleRuns = idle.phase() != D2Idle.Phase.OFF;
            if (idleRuns && was == D2RespecSpend.RUNNING) respecSpend.cancel("idle");
            else {
                boolean ready = name != null && modLink.singlePlayer() && !pauseWanted && windowFocused && !idleRuns
                    && !s.isOpen(D2ModState.UI_ESCMENU);
                D2PointPlan.Build build = name != null && s.characterClass >= 0 ? pointHighlight.build(s.characterClass, name) : null;
                respecSpend.step(build, table, modLink.points(1500), s.characterClass, name, ready, now, respecOut);
            }
        }
        int state = respecSpend.state();
        String text;
        D2PointPlan.Build build = name != null && s.characterClass >= 0 ? pointHighlight.build(s.characterClass, name) : null;
        String buildName = build != null ? text(D2BuildDialog.texts(getContext(), build.id)[0]) : "";
        switch (state) {
            case D2RespecSpend.WAITING:
                text = text(R.string.respec_spend_waiting, buildName);
                break;
            case D2RespecSpend.RUNNING:
                text = text(R.string.respec_spend_running, buildName, respecSpend.skillsSpent(), respecSpend.statsSpent());
                break;
            case D2RespecSpend.DONE:
                text = "no build".equals(respecSpend.why()) ? text(R.string.respec_spend_no_build)
                    : respecSpend.left() > 0 ? text(R.string.respec_spend_left, respecSpend.skillsSpent(), respecSpend.statsSpent(), respecSpend.left())
                    : text(R.string.respec_spend_done, respecSpend.skillsSpent(), respecSpend.statsSpent());
                break;
            default:
                text = text(R.string.respec_spend_cancelled, respecSpend.skillsSpent(), respecSpend.statsSpent());
                break;
        }
        boolean end = !respecSpend.active();
        if (end && respecShownState != state) D2Trace.log("respec spend: " + respecSpend);

        if (!text.equals(respecNote) || (!end && now - respecNoteAt > 1000) || respecShownState != state) {
            respecNote = text;
            respecNoteAt = now;
            showNote(text, end ? 6000 : 2500);
        }
        respecShownState = state;
        if (end) respecSpend.clear();
    }


    private volatile String aimPreviewKey;


    public void showAimPreview(String key) {
        if (!D2AimAssist.previewFor(key)) return;
        aimPreviewKey = key;
        aimPreviewUntil = SystemClock.uptimeMillis() + 2500;
        invalidate();
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ViewTransformation transformation = new ViewTransformation();
        transformation.update(w, h, xServer.screenInfo.width, xServer.screenInfo.height);
        aspect = transformation.aspect;
        boolean fullscreen = xServer.getRenderer() != null && xServer.getRenderer().isFullscreen();
        offsetX = fullscreen ? 0 : transformation.viewOffsetX;
        offsetY = fullscreen ? 0 : transformation.viewOffsetY;
        applyUiScale(w, h);
        layoutButtons(w, h);
    }





    private void applyUiScale(int w, int h) {
        uiFactor = D2UiScale.overlay(w, h, density, getResources().getConfiguration().smallestScreenWidthDp);
        dp = density * uiFactor;
        strokePaint.setStrokeWidth(2 * dp);
        if (hintView != null) hintView.setDp(dp);
    }

    private void layoutButtons(int w, int h) {
        leftTrigger.label = text(R.string.trigger_left);
        rightTrigger.label = text(R.string.trigger_right);
        buttons.clear();
        joystickRadius = 64 * dp;
        idleX = Math.max(0.15f * w, 110 * dp);
        idleY = h - 120 * dp;



        boolean compact = hudLayout == D2Settings.HUD_COMPACT;
        float ax = w - 100 * dp, ay = h - (compact ? 120 : 92) * dp;
        addGame(new Button("attack", text(R.string.btn_attack), KIND_ATTACK, 0, GROUP_GAME, false), w, h, ax, ay, 46);
        float[] skillAngles = {180, 213, 246, 279};
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(skillAngles[i]);
            addGame(new Button("F" + (i + 1), "F" + (i + 1), KIND_SKILL, VK_F1 + i, GROUP_GAME, false), w, h,
                ax + (float) Math.cos(a) * 110 * dp, ay + (float) Math.sin(a) * 110 * dp, 28);
        }

        addGame(new Button("mana", text(R.string.btn_mana), KIND_POTION, VK_2, GROUP_GAME, false), w, h,
            compact ? ax - 171 * dp : w - 30 * dp, compact ? ay - 43 * dp : h - 228 * dp, 25);
        addGame(new Button("heal", text(R.string.btn_heal), KIND_POTION, VK_1, GROUP_GAME, false), w, h,
            compact ? ax - 171 * dp : w - 30 * dp, compact ? ay + 21 * dp : h - 146 * dp, 25);

        addGame(new Button("rejuv", text(R.string.btn_rejuv), KIND_POTION, 0, GROUP_GAME, false), w, h,
            compact ? ax - 143 * dp : w - 30 * dp, compact ? ay - 100 * dp : h - 310 * dp, 22);
        interactButton = new Button("interact", text(R.string.btn_take), KIND_INTERACT, 0, GROUP_GAME, false);
        float[] nextToOrb = compact ? besideManaOrb(w, h, 24 * dp * scale) : null;
        addGame(interactButton, w, h, compact ? nextToOrb[0] : w - 205 * dp, compact ? nextToOrb[1] : h - 30 * dp, 24);



        float half = 20 * dp, gap = 5 * dp, y = 8 * dp + half;
        D2Hud graphics = hud;

        String[] ids = graphics != null
            ? new String[]{"toggle", "pause", "portal", "esc", "map", D2RezeptOeffnen.MENU_ID, "inv", "char", "skills", D2MenuRow.QUESTS, "run", "names", "keys", "drawer"}
            : new String[]{"drawer", "pause", "portal", "esc", "map", D2RezeptOeffnen.MENU_ID, "inv", "char", "skills", "run", "names", "keys"};
        String pause = text(R.string.btn_pause), portal = text(R.string.btn_portal), map = text(R.string.btn_map), inv = text(R.string.btn_inv),
            chr = text(R.string.btn_char), skills = text(R.string.btn_skills), quests = text(R.string.btn_quests), names = text(R.string.btn_names),
            run = text(R.string.btn_run), keysLabel = text(R.string.btn_keys), recipesLabel = text(R.string.menu_label_recipes);
        String[] labels = graphics != null
            ? new String[]{"", pause, portal, "Esc", map, recipesLabel, inv, chr, skills, quests, run, names, keysLabel, "≡"}
            : new String[]{"≡", pause, portal, "Esc", map, recipesLabel, inv, chr, skills, run, names, keysLabel};
        int[] kinds = graphics != null
            ? new int[]{KIND_MENU_TOGGLE, KIND_PAUSE, KIND_PORTAL, KIND_ESCAPE, KIND_KEY, KIND_RECIPES, KIND_PANEL, KIND_PANEL, KIND_PANEL, KIND_PANEL, KIND_KEY, KIND_LABELS, KIND_CONTROLS, KIND_MENU}
            : new int[]{KIND_MENU, KIND_PAUSE, KIND_PORTAL, KIND_ESCAPE, KIND_KEY, KIND_RECIPES, KIND_PANEL, KIND_PANEL, KIND_PANEL, KIND_KEY, KIND_LABELS, KIND_CONTROLS};
        int[] keys = graphics != null
            ? new int[]{0, 0, 0, VK_ESCAPE, VK_TAB, 0, VK_I, VK_C, VK_T, VK_Q, VK_R, 0, 0, 0}
            : new int[]{0, 0, 0, VK_ESCAPE, VK_TAB, 0, VK_I, VK_C, VK_T, VK_R, 0, 0};
        int[] icons = {ICON_ARROW, ICON_PAUSE, ICON_PORTAL, D2Hud.ICON_MENU, D2Hud.ICON_MAP, ICON_CUBE, D2Hud.ICON_INV, D2Hud.ICON_CHAR, D2Hud.ICON_SKILLS, D2Hud.ICON_QUESTS,
            ICON_RUN, ICON_TEXT, ICON_TEXT, ICON_LINES};

        String[] captions = {null, text(R.string.menu_label_pause), text(R.string.menu_label_portal), text(R.string.menu_label_menu),
            text(R.string.menu_label_map), recipesLabel, text(R.string.menu_label_inv), text(R.string.menu_label_char), text(R.string.menu_label_skills),
            text(R.string.menu_label_quests), text(R.string.menu_label_run), null, null, null};


        float rowGap = graphics != null ? menuRowGap(2 * half) : gap;
        Button[] row = new Button[labels.length];
        float[] halfWidths = new float[labels.length];
        int toggle = -1;
        for (int i = 0; i < labels.length; i++) {
            Button b = new Button("menu_" + ids[i], labels[i], kinds[i], keys[i], GROUP_MENU, true);
            float hw = half;
            if (graphics != null) {
                b.icon = icons[i];
                b.caption = captions[i];
                if (b.icon == ICON_TEXT) hw = Math.max(half, menuLabelHalf(graphics, labels[i]));
                if (b.icon == ICON_ARROW) hw = 14 * dp;
            }
            if (b.kind == KIND_MENU_TOGGLE) toggle = i;
            row[i] = b;
            halfWidths[i] = hw;
        }


        float[] rights = D2MenuRow.rightEdges(ids, halfWidths, toggle, pinnedMenu, menuShownOpen() || graphics == null,
            w - 8 * dp - rightShift, rowGap);
        for (int i = 0; i < labels.length; i++) addRect(row[i], rights[i] - halfWidths[i], y, halfWidths[i], half);


        if (graphics != null) {
            int k = menuPixelScale();
            graphics.prepareMenuPictures(square(menuInner(2 * half, 2 * half, k)), menuInner(28 * dp, 2 * half, k),
                SKILL_BOOK_OF_TOWNPORTAL, this);
        }




        float hx = w - 8 * dp - rightShift;
        String[] hiddenLabels = {"Esc", keysLabel, text(R.string.shop_potions_button)};
        for (int i = 0; i < hiddenLabels.length; i++) {
            int kind = i == 0 ? KIND_ESCAPE : i == 1 ? KIND_CONTROLS : KIND_SHOP_POTIONS;
            Button b = new Button(i == 2 ? "hidden_potions" : "hidden" + i, hiddenLabels[i], kind, i == 0 ? VK_ESCAPE : 0, GROUP_HIDDEN, true);
            float hwid = half;
            if (graphics != null) {
                b.icon = i == 0 ? D2Hud.ICON_MENU : ICON_TEXT;
                if (b.icon == ICON_TEXT) hwid = Math.max(half, menuLabelHalf(graphics, hiddenLabels[i]));
            }
            else hwid = i == 0 ? half : (i == 2 ? 3 : 2) * half;
            addRect(b, hx - hwid, y, hwid, half);
            hx -= 2 * hwid + gap;
        }


        respecButton = new Button("respec", RESPEC_LABEL, KIND_RESPEC, 0, GROUP_HIDDEN, true);
        addRect(respecButton, -1000, -1000, 1, 1);
        if (mod != null) placeRespec(mod);

        sortButton = new Button("sort", text(R.string.sort_button), KIND_SORT, 0, GROUP_HIDDEN, true);
        addRect(sortButton, -1000, -1000, 1, 1);
        if (mod != null) placeSort(mod);


        String[] editLabels = {"−", "+", text(R.string.btn_debug), text(R.string.btn_reset), text(R.string.btn_done)};
        int[] editKinds = {KIND_EDIT_SMALLER, KIND_EDIT_BIGGER, KIND_EDIT_DEBUG, KIND_EDIT_RESET, KIND_EDIT_DONE};
        float[] editHalfWidths = {20, 20, 44, 56, 40};
        if (!com.winlator.BuildConfig.D2_INTERNAL) {
            editLabels = new String[]{"−", "+", text(R.string.btn_reset), text(R.string.btn_done)};
            editKinds = new int[]{KIND_EDIT_SMALLER, KIND_EDIT_BIGGER, KIND_EDIT_RESET, KIND_EDIT_DONE};
            editHalfWidths = new float[]{20, 20, 56, 40};
        }
        float total = 0;
        for (float hwid : editHalfWidths) total += 2 * hwid * dp + gap;
        float ex = w / 2f - total / 2;
        for (int i = 0; i < editLabels.length; i++) {
            float hwid = editHalfWidths[i] * dp;
            addRect(new Button("edit" + i, editLabels[i], editKinds[i], 0, GROUP_EDIT, true), ex + hwid, 8 * dp + half, hwid, half);
            ex += 2 * hwid + gap;
        }
        keepClearOfHud(w, h);
        avoidCutout(w, h);
    }





    private void avoidCutout(int w, int h) {
        float[][] rects = cutoutRects();
        cutoutDone = java.util.Arrays.deepToString(rects);
        if (rects == null) return;
        for (Button b : buttons) {
            java.util.List<float[]> others = new java.util.ArrayList<>();
            for (Button o : buttons) {
                if (o != b && o.group == b.group) others.add(new float[]{o.cx - o.hw, o.cy - o.r, o.cx + o.hw, o.cy + o.r});
            }
            float[] c = D2Cutout.clear(b.cx, b.cy, b.hw, b.r, rects, w, h, 6 * dp, others.toArray(new float[0][]));
            b.shiftX = c[0] - b.cx;
            b.shiftY = c[1] - b.cy;
            b.cx = c[0];
            b.cy = c[1];
        }
    }


    private float[][] cutoutRects() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return null;
        int[] at = new int[2];
        getLocationInWindow(at);
        return CutoutApi28.rects(this, at);
    }






    @Override
    public android.view.WindowInsets onApplyWindowInsets(android.view.WindowInsets insets) {
        if (getDisplay() != null) D2Orientation.rotation(getDisplay().getRotation());
        post(() -> {
            if (getWidth() == 0 || buttons.isEmpty() || java.util.Arrays.deepToString(cutoutRects()).equals(cutoutDone)) return;
            for (Button b : buttons) {
                b.cx -= b.shiftX;
                b.cy -= b.shiftY;
                b.shiftX = b.shiftY = 0;
            }
            avoidCutout(getWidth(), getHeight());
            invalidate();
        });
        return super.onApplyWindowInsets(insets);
    }



    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.P)
    private static final class CutoutApi28 {
        static float[][] rects(View view, int[] at) {
            android.view.WindowInsets insets = view.getRootWindowInsets();
            android.view.DisplayCutout cutout = insets != null ? insets.getDisplayCutout() : null;
            if (cutout == null) return null;
            java.util.List<android.graphics.Rect> list = cutout.getBoundingRects();
            float[][] rects = new float[list.size()][];
            for (int i = 0; i < rects.length; i++) {
                android.graphics.Rect r = list.get(i);
                rects[i] = new float[]{r.left - at[0], r.top - at[1], r.right - at[0], r.bottom - at[1]};
            }
            return rects;
        }
    }


    private void addGame(Button button, int w, int h, float cx, float cy, float radiusDp) {
        String saved = prefs.getString("pos_" + button.id, null);
        if (saved != null) {
            try {
                String[] parts = saved.split(",");
                cx = Float.parseFloat(parts[0]) * w;
                cy = Float.parseFloat(parts[1]) * h;
            }
            catch (RuntimeException ignored) {}
        }
        button.cx = cx;
        button.cy = cy;
        button.r = radiusDp * dp * scale;
        button.hw = button.r;
        button.saved = saved != null;
        buttons.add(button);
    }


    private int clearedGameW, clearedGameH;








    private void keepClearOfHud(int w, int h) {
        if (uiFactor <= 1f) return;
        D2ModState s = mod;
        int gw, gh;
        if (s != null && s.gameWidth > 0 && s.gameHeight > 0) {
            gw = s.gameWidth;
            gh = s.gameHeight;
        } else {
            int[] game = D2MenuScale.expectedGame(xServer.screenInfo.width, xServer.screenInfo.height, D2Settings.gameLines(getContext()));
            if (game == null) return;
            gw = game[0];
            gh = game[1];
        }
        clearedGameW = s != null && s.gameWidth > 0 ? gw : 0;
        clearedGameH = s != null && s.gameHeight > 0 ? gh : 0;
        int dw = xServer.screenInfo.width, dh = xServer.screenInfo.height;
        if (dw <= 0 || dh <= 0) return;
        float kd = Math.min(dw / (float) gw, dh / (float) gh), k = kd * aspect;
        float[][] rects = D2UiScale.hudRects(hudLayout == D2Settings.HUD_COMPACT, gw, gh, k,
            (dw - gw * kd) / 2 * aspect + offsetX, (dh - gh * kd) / 2 * aspect + offsetY);
        float margin = 4 * dp, tab = 12 * k - 6 * dp;
        idleY -= D2UiScale.liftClear(idleX, idleY, joystickRadius, 0, rects, margin);
        float lift = 0;
        for (Button b : buttons) if (inDefaultGroup(b, w)) lift = Math.max(lift, D2UiScale.liftClear(b.cx, b.cy, b.r, tab, rects, margin));
        for (Button b : buttons) {
            if (b.group != GROUP_GAME) continue;
            b.cy -= inDefaultGroup(b, w) ? lift : D2UiScale.liftClear(b.cx, b.cy, b.r, tab, rects, margin);
            b.cx = Math.max(b.r, Math.min(w - b.r, b.cx));
            b.cy = Math.max(b.r, Math.min(h - b.r, b.cy));
        }
    }


    private boolean inDefaultGroup(Button b, int w) {
        return b.group == GROUP_GAME && !b.saved && b != interactButton && b.cx > w / 2f;
    }

    private void addRect(Button button, float cx, float cy, float hw, float hh) {
        button.cx = cx;
        button.cy = cy;
        button.hw = hw;
        button.r = hh;
        buttons.add(button);
    }






    private float[] besideManaOrb(int w, int h, float radius) {
        int lines = D2Settings.gameLines(getContext());
        float k = h / (float) (lines == D2Settings.LINES_AUTO ? D2MenuScale.AUTO_LINES : lines);
        float orbX = w / 2f + (D2Hud.CENTER_HALF + 32 + D2Hud.ORB / 2f) * k, orbY = h - (D2Hud.PIECE_H - 8 - D2Hud.ORB / 2f) * k;
        return new float[]{Math.min(w - radius - 4 * dp, orbX + D2Hud.ORB / 2f * k + 6 * dp + radius), Math.min(h - radius - 2 * dp, orbY)};
    }







    private static final String RESPEC_LABEL = "RESPEC";
    private Button respecButton;
    private final float[] respecSpot = new float[4];





    private void placeRespec(D2ModState s) {
        if (respecButton == null || s.gameWidth <= 0 || s.gameHeight <= 0) return;
        float[] m = gameMapping(s);
        D2PageButtons.respec(s.gameWidth, s.gameHeight, respecSpot);
        placeOnGame(respecButton, respecSpot, m);
    }


    private static void placeOnGame(Button button, float[] spot, float[] m) {
        button.hw = spot[2] / 2f * m[0];
        button.r = spot[3] / 2f * m[0];
        button.cx = m[1] + (spot[0] + spot[2] / 2f) * m[0];
        button.cy = m[2] + (spot[1] + spot[3] / 2f) * m[0];
    }

    private Button sortButton;
    private final float[] sortSpot = new float[4];


    private boolean cursorTaken() {
        D2Points points = modLink.points(2000);
        return points == null || points.cursorTaken;
    }


    private void placeSort(D2ModState s) {
        if (sortButton == null || s.gameWidth <= 0 || s.gameHeight <= 0) return;
        float[] m = gameMapping(s);
        D2PageButtons.sort(s.gameWidth, s.gameHeight, sortSpot);
        placeOnGame(sortButton, sortSpot, m);
    }

    private void respecTap() {
        long now = SystemClock.uptimeMillis();
        if (respecArmedUntil < now) {
            respecArmedUntil = now + 4000;
            showNote(text(R.string.note_respec_again));
            postInvalidateDelayed(4100);
            return;
        }
        respecArmedUntil = 0;
        for (int code = D2ModLink.PLUGY_UNASSIGN_STR; code <= D2ModLink.PLUGY_UNASSIGN_VIT; code++) modLink.plugy(code);
        performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        showNote(text(R.string.note_respec_done));
    }

    private boolean respecArmed() {
        return SystemClock.uptimeMillis() < respecArmedUntil;
    }


    private boolean isFreeSlot(Button button) {
        D2ModState s = mod;
        if (button.kind != KIND_SKILL || s == null || s.characterClass < 0) return false;
        int index = button.vk - VK_F1;
        return index >= 0 && index < 16 && s.hotkeySkill[index] < 0;
    }


    private String text(int id, Object... args) {
        return D2Text.get(getContext(), id, args);
    }






    public void onLanguageChanged() {
        noteUntil = 0;
        leftTrigger.label = text(R.string.trigger_left);
        rightTrigger.label = text(R.string.trigger_right);
        if (getWidth() > 0) layoutButtons(getWidth(), getHeight());
        invalidate();
    }





    public boolean sortSettled() {
        return sortWaitFor < 0 && !sortCursorLock;
    }


    private boolean sortCapable() {
        if (!gridOriginal || !sortOn) return false;
        int caps = modLink.capabilities();
        return caps >= 0 && (caps & D2Points.CAP_SORT) != 0;
    }


    private void startSort() {
        D2Points points = modLink.points(2000);
        if (points == null || points.sortCounter < 0 || sortWaitFor >= 0 || pauseWanted) return;
        releaseCurrent();
        sortWaitFor = (points.sortCounter + 1) & 0xFF;
        sortLockSince = SystemClock.uptimeMillis();
        sortAnswered = false;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        D2Trace.log("sort: start, waiting for counter " + sortWaitFor);
        modLink.sortInventory(true);
    }





    private void updateSort(D2ModState s) {
        if (sortWaitFor < 0 && !sortCursorLock) return;
        long now = SystemClock.uptimeMillis();
        if (s != null) sortNoDataSince = 0;
        else if (sortNoDataSince == 0) sortNoDataSince = now;
        boolean gone = s == null && now - sortNoDataSince >= SORT_GONE_MS;
        D2Points points = modLink.points(1000);
        boolean cursorEmpty = points != null && points.sortSettled();
        if (sortWaitFor >= 0) {
            if (points != null && points.sortCounter == sortWaitFor) sortAnswered = true;
            boolean done = sortAnswered && points != null && points.sortCounter == sortWaitFor
                && points.sortState != D2Points.SORT_RUNNING;
            boolean lost = !sortAnswered && now - sortLockSince >= SORT_ANSWER_MS;
            if (!done && !lost && !gone && now - sortLockSince < SORT_LOCK_MS) return;
            int result = done ? points.sortState : -1;
            D2Trace.log("sort: " + (done ? "result " + result + ", " + points.sortMoves + " moves"
                : lost ? "no answer to command 24" : gone ? "out of the game" : "no result in time"));
            sortWaitFor = -1;
            sortCursorLock = !gone && !cursorEmpty;
            if (result == D2Points.SORT_PARTIAL) showNote(text(R.string.sort_partial));
            else if (result == D2Points.SORT_NOT_STARTED || lost) showNote(text(R.string.sort_not_started));
            else if (result == D2Points.SORT_NO_ROOM) showNote(text(R.string.sort_no_room));
            if (sortCursorLock && points != null && points.cursorTaken) {

                showNote(text(R.string.sort_cursor));
                if (s != null && !s.isOpen(D2ModState.UI_INVENTORY)) pressKey(VK_I);
            }
        }
        else if (gone || cursorEmpty) sortCursorLock = false;

        if (sortWaitFor < 0 && !sortCursorLock && pauseAfterSort && !relaunchSaving) {
            pauseAfterSort = false;
            setPaused(true);
        }
    }





    private boolean sortBlocks(float x, float y) {
        if (sortWaitFor >= 0) return true;
        if (!sortCursorLock) return false;
        D2ModState s = mod;
        if (s == null || !s.isOpen(D2ModState.UI_INVENTORY) || s.gameWidth <= 0 || s.gameHeight <= 0) return true;
        float[] m = gameMapping(s);
        return !D2Points.inInventoryWindow(x, y, m[0], m[1], m[2], s.gameWidth, s.gameHeight);
    }

    private void showNote(String text) {
        showNote(text, 2500);
    }

    private void showNote(String text, long ms) {
        noteText = text;
        noteUntil = SystemClock.uptimeMillis() + ms;
        invalidate();
        postInvalidateDelayed(ms + 100);
    }


    private int rejuvAtHand() {
        D2ModItems carried = modLink.items();
        if (carried != null) return carried.belt[2] + (potionMode == D2Settings.POTIONS_ALL ? carried.inventory[2] : 0);
        D2ModState s = mod;
        if (s == null || !s.potionsKnown) return -1;
        int count = 0;
        for (int slot : s.belt) if (slot == D2ModState.BELT_REJUV) count++;
        return count;
    }

    private boolean isVisible(Button button) {
        if (button.group == GROUP_GAME && padHidesControls()) return false;
        if (button.kind == KIND_INTERACT && mod == null && !editMode) return false;

        if (treePickTarget(button) && treePicking()) return true;
        if (button.kind == KIND_SHOP_POTIONS) return !editMode && !controlsVisible && shopButton != null && shopButton.visible(mod);
        if (button.kind == KIND_SORT) {

            D2ModState s = mod;
            return !editMode && !controlsVisible && s != null && D2SortButton.visible(
                s.isOpen(D2ModState.UI_INVENTORY) || s.isOpen(D2ModState.UI_CUBE),
                s.isOpen(D2ModState.UI_STASH) || s.isOpen(D2ModState.UI_NPCSHOP)
                    || s.isOpen(D2ModState.UI_TRADE) || s.isOpen(D2ModState.UI_ESCMENU),
                sortCapable(), modLink.singlePlayer(), pauseWanted, !sortSettled(), cursorTaken());
        }
        if (button.kind == KIND_RESPEC) {
            D2ModState s = mod;

            D2Points points = modLink != null ? modLink.points(1500) : null;
            return !editMode && !controlsVisible && respec && s != null && s.plugyLoaded && s.isOpen(D2ModState.UI_CHARACTER)
                && D2PageButtons.respecShown(points != null ? points.statPoints : -1);
        }

        if (button == interactButton && !editMode && !button.pressed && interactCandidate == null && action != ACTION_PICKUP
            && (mod == null || !mod.npcTalking) && !npcMenuOpen() && SystemClock.uptimeMillis() - interactSeenAt > 800) return false;

        if (button.id.equals("rejuv") && !editMode && !button.pressed && rejuvAtHand() == 0) return false;

        if (button == interactButton && !editMode && npcMenuOpen()) return true;
        if (editMode) return button.group == GROUP_GAME || button.group == GROUP_EDIT;

        if (button == interactButton && !controlsVisible && hiddenByPanel && mod != null
            && mod.panelFlags() == 1L << D2ModState.UI_INVENTORY) return true;
        if (controlsVisible) {
            if (button.group == GROUP_MENU && !menuShownOpen() && hud != null) {
                String id = button.id.substring(5);
                return D2MenuRow.shownFolded(id, button.kind == KIND_MENU_TOGGLE, pinnedMenu.contains(id), questNews());
            }
            return button.group == GROUP_GAME || button.group == GROUP_MENU;
        }
        return button.group == GROUP_HIDDEN;
    }

    private void saveLayout() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Button button : buttons) {
            if (button.group != GROUP_GAME) continue;

            editor.putString("pos_" + button.id, String.format(Locale.ROOT, "%.4f,%.4f", (button.cx - button.shiftX) / getWidth(),
                (button.cy - button.shiftY) / getHeight()));
        }
        editor.putFloat("scale", scale).putBoolean("debug", debug).apply();
    }

    private void resetLayout() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Button button : buttons) {
            if (button.group == GROUP_GAME) editor.remove("pos_" + button.id);
        }
        scale = 1;
        editor.putFloat("scale", 1).apply();
        layoutButtons(getWidth(), getHeight());
    }

    private void changeScale(float factor) {
        scale = Math.max(0.7f, Math.min(1.5f, scale * factor));
        for (Button button : buttons) {
            if (button.group != GROUP_GAME) continue;
            float base = button.kind == KIND_ATTACK ? 46 : (button.kind == KIND_SKILL ? 28 : (button.kind == KIND_INTERACT ? 24 : 25));
            button.r = button.hw = base * dp * scale;
        }
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        D2FrameCounts.overlayDraws.incrementAndGet();
        drawHud(canvas);
        D2ModState ms = mod;

        boolean pointsShown = !editMode && ms != null && ms.gameWidth > 0;
        if (!updatePointsLayer(canvas, pointsShown ? ms : null) && pointsShown && pointHighlight.draw(canvas, hud, ms,
            modLink.points(1500), modLink.characterName(), gameMapping(ms, pointHighlight.mapping), SystemClock.uptimeMillis())) {
            postInvalidateDelayed(80);
        }
        long layerNow = SystemClock.uptimeMillis();
        boolean materialsByMod = updateMaterialsLayer(layerNow);
        if (!editMode && recipes.sendLayer(layerNow, padActive)) postInvalidateDelayed(250);
        boolean stonesByMod = updateStoneLayer(layerNow);
        updateWindowInfoLayer(layerNow);
        if (controlsVisible && !editMode && joystickVisible && !padHidesControls()) {
            boolean shown = joystickActive && joystickOwner == D2Gamepad.OWNER_FINGER;
            float bx = shown ? baseX : idleX, by = shown ? baseY : idleY;

            if ((!materialsByMod || !materialsWindow.covers(bx, by, materialsOn(), layerNow)) && !recipes.covers(bx, by)) {
                drawCircle(canvas, bx, by, joystickRadius, shown ? 40 : 18, shown ? 110 : 60, Color.WHITE);
                drawCircle(canvas, shown ? knobX : bx, shown ? knobY : by, 28 * dp, shown ? 120 : 45, 150, Color.WHITE);
            }
        }
        if (mod != null && !editMode && controlsVisible) drawModMarkers(canvas);
        if (mod != null && controlsVisible && padHidesControls()) drawPadBar(canvas);
        if (padWheelOpen) drawPadWheel(canvas);
        if (SystemClock.uptimeMillis() < aimPreviewUntil) {
            drawAimPreview(canvas);
            postInvalidateDelayed(100);
        }
        if (editMode) {
            fillPaint.setColor(Color.argb(110, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), getHeight(), fillPaint);
            textPaint.setTextSize(15 * dp);
            canvas.drawText(text(R.string.edit_drag), getWidth() / 2f, getHeight() * 0.4f, textPaint);
            if (hud != null && mod != null) {
                textPaint.setTextSize(13 * dp);
                canvas.drawText(text(R.string.edit_assign), getWidth() / 2f, getHeight() * 0.4f + 44 * dp, textPaint);
            }
            textPaint.setTextSize(12 * dp);
            canvas.drawText(text(R.string.edit_size, Math.round(scale * 100)), getWidth() / 2f, getHeight() * 0.4f + 22 * dp, textPaint);
        }
        for (Button button : buttons) {
            if (!isVisible(button)) continue;

            if (materialsByMod && materialsWindow.covers(button.cx, button.cy, materialsOn(), layerNow)) continue;
            if (recipes.covers(button.cx, button.cy)) continue;
            boolean on = button.pressed || (button.kind == KIND_LABELS && labelsOn) || (button.kind == KIND_EDIT_DEBUG && debug)
                || (button.kind == KIND_RESPEC && respecArmed());
            int color = Color.WHITE;
            if (button.kind == KIND_ATTACK) color = Color.rgb(224, 89, 42);
            else if (button.id.equals("heal")) color = Color.rgb(220, 50, 50);
            else if (button.id.equals("mana")) color = Color.rgb(60, 110, 230);
            else if (button.id.equals("rejuv")) color = Color.rgb(170, 60, 190);
            if ((button.kind == KIND_RESPEC || button.kind == KIND_SORT) && stonesByMod) continue;
            if ((button.kind == KIND_RESPEC || button.kind == KIND_SORT) && hud != null && hud.hasGameButton()) {
                rect.set(button.cx - button.hw, button.cy - button.r, button.cx + button.hw, button.cy + button.r);
                hud.drawGameButton(canvas, button.label, button.pressed, button.kind == KIND_RESPEC && respecArmed(), rect);
                continue;
            }
            if (button.rect && (button.group == GROUP_MENU || button.group == GROUP_HIDDEN) && hud != null) {
                drawMenuButton(canvas, button, on);
                continue;
            }
            if (button.rect) {
                rect.set(button.cx - button.hw, button.cy - button.r, button.cx + button.hw, button.cy + button.r);
                if (!drawGameFrame(canvas, rect, Color.argb(on ? 225 : 180, on ? 40 : 10, on ? 32 : 8, on ? 18 : 6))) {
                    fillPaint.setColor(Color.argb(on ? 170 : 90, 20, 12, 10));
                    canvas.drawRoundRect(rect, 7 * dp, 7 * dp, fillPaint);
                    strokePaint.setColor(Color.argb(on ? 230 : 120, 255, 255, 255));
                    canvas.drawRoundRect(rect, 7 * dp, 7 * dp, strokePaint);
                }
                textPaint.setTextSize(button.group == GROUP_EDIT ? 11 * dp : 10 * dp);
            }
            else {
                if (!editMode && isFreeSlot(button)) {
                    drawFreeSlot(canvas, button, on);
                    continue;
                }
                boolean icon = drawSkillIcon(canvas, button, on);
                if (!icon) {
                    int fillAlpha = on ? 150 : (button.kind == KIND_ATTACK ? 70 : 45);
                    if (!drawGameRound(canvas, button, fillAlpha, on ? 230 : 140, color)) {
                        drawCircle(canvas, button.cx, button.cy, button.r, fillAlpha, on ? 230 : 140, color);
                    }
                }
                if (editMode) {
                    strokePaint.setColor(Color.argb(200, 255, 220, 80));
                    canvas.drawCircle(button.cx, button.cy, button.r + 3 * dp, strokePaint);
                }
                textPaint.setTextSize((button.kind == KIND_ATTACK ? 15 : 13) * dp * Math.min(1.2f, scale));
                if (icon) continue;
                if (button.kind == KIND_POTION && drawPotion(canvas, button)) continue;
                if (button.kind == KIND_INTERACT && drawHand(canvas, button)) {
                    drawInteractHint(canvas, button);
                    continue;
                }
            }
            canvas.drawText(button.label, button.cx, button.cy + textPaint.getTextSize() / 3, textPaint);
            if (button.kind == KIND_INTERACT) drawInteractHint(canvas, button);
        }
        if (!editMode) {


            long nowMs = SystemClock.uptimeMillis();
            if (!materialsByMod) materialsWindow.draw(canvas, hud, materialsOn(), nowMs);
            drawInputBox(canvas, ms, nowMs);
            drawSaveWarning(canvas, materialsWindow.saveWarning(nowMs));
            drawPadPointer(canvas);
        }
        if (pauseWanted && !recipes.isOpen()) drawPause(canvas);
        drawIdleBoard(canvas);
        drawRing(canvas);
        drawZeroLifeNote(canvas);
        boolean treePick = treePicking();
        if (assignButton != null || treePick || (noteText != null && SystemClock.uptimeMillis() < noteUntil)) {
            boolean noteShown = noteText != null && SystemClock.uptimeMillis() < noteUntil;

            String text = noteShown ? noteText
                : assignButton == null ? text(R.string.tree_pick_for, treePickName(treePickSkill))
                : (assignButton.kind == KIND_ATTACK
                    ? text(assignRight ? R.string.assign_right : R.string.assign_left)
                    : text(assignRight ? R.string.assign_for_right : R.string.assign_for_left, assignButton.label));
            textPaint.setTextSize(15 * dp);

            int budget = (int) Math.max(120 * dp, Math.min(getWidth() - 64 * dp, 640 * dp));
            java.util.List<String> lines = D2Hints.wrap(text, (t) -> (int) Math.ceil(textPaint.measureText(t)), budget);
            if (lines.isEmpty()) lines = java.util.Collections.singletonList(text);
            float widest = 0;
            for (String line : lines) widest = Math.max(widest, textPaint.measureText(line));
            float w = widest + 32 * dp, lineStep = 20 * dp;
            rect.set(getWidth() / 2f - w / 2, 64 * dp, getWidth() / 2f + w / 2, 100 * dp + (lines.size() - 1) * lineStep);
            if (!drawGameFrame(canvas, rect, Color.argb(225, 10, 8, 6))) {
                fillPaint.setColor(Color.argb(225, 10, 8, 6));
                canvas.drawRoundRect(rect, 8 * dp, 8 * dp, fillPaint);
                strokePaint.setColor(Color.rgb(148, 126, 74));
                canvas.drawRoundRect(rect, 8 * dp, 8 * dp, strokePaint);
            }
            for (int i = 0; i < lines.size(); i++) canvas.drawText(lines.get(i), getWidth() / 2f, 88 * dp + i * lineStep, textPaint);
            if (noteShown) resumeHint.shown(rect);
            noteRect.set(rect);
            noteRectShown = noteShown;


            if (assignButton != null) assignHint.set(rect);
        }
        else noteRectShown = false;
        if (debug) drawDiagnostics(canvas);
        if (teleportHeld) drawTeleportMarker(canvas);
        hintView.draw(canvas, hints.showing(), getWidth());
    }





    private boolean drawPotion(Canvas canvas, Button button) {
        D2Hud graphics = hud;
        D2ModState s = mod;
        if (graphics == null || s == null || !s.potionsKnown) return false;
        if (button.id.equals("rejuv")) {
            int count = Math.max(0, rejuvAtHand());
            android.graphics.Bitmap picture = graphics.rejuvPotion(false);
            if (picture == null) return false;
            float scaleK = menuTextScale() * 1.3f;
            float w = picture.getWidth() * scaleK, h = picture.getHeight() * scaleK;
            RectF where = new RectF(button.cx - w / 2, button.cy - h / 2, button.cx + w / 2, button.cy + h / 2);
            iconPaint.setAlpha(count > 0 ? 255 : 80);


            iconPaint.setColorFilter(REJUV_TINT);
            canvas.drawBitmap(picture, null, where, iconPaint);
            iconPaint.setColorFilter(null);
            iconPaint.setAlpha(255);
            drawTab(canvas, Integer.toString(count), button.cx + button.r * 0.55f, button.cy + button.r * 0.35f);
            return true;
        }
        int k = button.id.equals("heal") ? 0 : 1;
        boolean inventoryToo = potionMode == D2Settings.POTIONS_ALL;
        int tier = s.potionTierBelt[k];
        if (tier == 0 && inventoryToo) tier = s.potionTierInventory[k];
        int count = s.potionCountBelt[k] + (inventoryToo ? s.potionCountInventory[k] : 0);
        android.graphics.Bitmap picture = graphics.potion(k == 0, tier > 0 ? tier : 1);
        if (picture == null) return false;
        float scaleK = menuTextScale() * 1.3f;
        float w = picture.getWidth() * scaleK, h = picture.getHeight() * scaleK;
        RectF where = new RectF(button.cx - w / 2, button.cy - h / 2, button.cx + w / 2, button.cy + h / 2);
        iconPaint.setAlpha(tier > 0 ? 255 : 80);
        canvas.drawBitmap(picture, null, where, iconPaint);
        iconPaint.setAlpha(255);
        drawTab(canvas, Integer.toString(count), button.cx + button.r * 0.55f, button.cy + button.r * 0.35f);
        return true;
    }



    private void drawMenuTab(Canvas canvas, String text, float cx, float top, int k) {
        drawMenuTab(canvas, text, cx, top, k, 8 * dp);
    }





    private void drawMenuTab(Canvas canvas, String text, float cx, float top, float k, float padding) {
        float w = hud.smallTextWidth(text) * k + padding, h = 12 * k;
        float left = Math.round(cx - w / 2), y = Math.round(top);
        rect.set(left, y, left + Math.round(w), y + h);
        fillPaint.setColor(Color.argb(220, 10, 8, 6));
        canvas.drawRoundRect(rect, 3 * dp, 3 * dp, fillPaint);
        if (k == (int) k) hud.drawSmallTextSharp(canvas, text, rect.centerX(), y + h / 2, (int) k);
        else hud.drawSmallText(canvas, text, rect.centerX(), y + h / 2, k);
    }

    private void drawTab(Canvas canvas, String text, float cx, float top) {
        D2Hud graphics = hud;
        float k = menuTextScale();
        if (graphics == null) {
            textPaint.setTextSize(10 * dp);
            float w = textPaint.measureText(text) + 8 * dp;
            if (uiFactor > 1f) cx = D2UiScale.tabInside(cx, w, getWidth());
            rect.set(cx - w / 2, top, cx + w / 2, top + 13 * dp);
            fillPaint.setColor(Color.argb(220, 10, 8, 6));
            canvas.drawRoundRect(rect, 4 * dp, 4 * dp, fillPaint);
            canvas.drawText(text, cx, top + 10 * dp, textPaint);
            return;
        }
        float w = graphics.smallTextWidth(text) * k + 8 * dp, h = 12 * k;
        if (uiFactor > 1f) cx = D2UiScale.tabInside(cx, w, getWidth());
        rect.set(cx - w / 2, top, cx + w / 2, top + h);
        fillPaint.setColor(Color.argb(220, 10, 8, 6));
        canvas.drawRoundRect(rect, 3 * dp, 3 * dp, fillPaint);
        graphics.drawSmallText(canvas, text, cx, top + h / 2, k);
    }


    private final D2InteractHint.MenuCount npcMenuCount = new D2InteractHint.MenuCount();

    private String hintText;
    private int hintIndex, hintCount;
    private boolean hintArrows;





    private void drawInteractHint(Canvas canvas, Button button) {
        if (editMode || ringTouch != null) return;
        D2ModState s = mod;
        boolean menu = npcMenuOpen();


        boolean menuArrowsOnly = menu && !npcMenuCount.known();
        int index = menu ? npcMenuCount.line() : interactIndex;
        int count = menu ? (menuArrowsOnly ? -1 : npcMenuCount.lines()) : interactCount;
        if (count <= 1 && !menuArrowsOnly) return;
        boolean arrows = D2InteractHint.swipeSwitches(menu, s != null && s.npcTalking);
        if (hintText == null || index != hintIndex || count != hintCount || arrows != hintArrows) {
            hintText = menuArrowsOnly ? D2InteractHint.ARROWS : D2InteractHint.text(index, count, arrows);
            hintIndex = index;
            hintCount = count;
            hintArrows = arrows;
        }
        if (hintText == null) return;
        drawTab(canvas, hintText, button.cx, button.cy - button.r - 12 * menuTextScale() - 3 * dp);
    }


    private void drawFreeSlot(Canvas canvas, Button button, boolean pressed) {
        fillPaint.setColor(Color.argb(pressed ? 70 : 28, 12, 10, 8));
        canvas.drawCircle(button.cx, button.cy, button.r, fillPaint);
        float width = strokePaint.getStrokeWidth();
        strokePaint.setStrokeWidth(1.5f * dp);
        strokePaint.setColor(Color.argb(110, 199, 179, 119));
        strokePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{5 * dp, 4 * dp}, 0));
        canvas.drawCircle(button.cx, button.cy, button.r - dp, strokePaint);
        strokePaint.setPathEffect(null);
        strokePaint.setStrokeWidth(width);
        D2Hud graphics = hud;
        if (graphics != null) {
            graphics.drawSmallText(canvas, text(R.string.slot_free), button.cx, button.cy, menuTextScale(), 150);
            return;
        }
        textPaint.setTextSize(11 * dp);
        int alpha = textPaint.getAlpha();
        textPaint.setAlpha(120);
        canvas.drawText(text(R.string.slot_free), button.cx, button.cy + 4 * dp, textPaint);
        textPaint.setAlpha(alpha);
    }





    private boolean drawHand(Canvas canvas, Button button) {
        D2Hud graphics = hud;
        android.graphics.Bitmap hand = graphics != null ? graphics.handIcon() : null;
        if (hand == null) return false;
        float k = button.r * 1.35f / Math.max(hand.getWidth(), hand.getHeight());
        float w = hand.getWidth() * k, h = hand.getHeight() * k;
        RectF where = new RectF(button.cx - w / 2, button.cy - h / 2 - 2 * dp, button.cx + w / 2, button.cy + h / 2 - 2 * dp);
        canvas.drawBitmap(hand, null, where, iconPaint);
        D2ModItems.Item item = labelItem();
        if (item != null) drawNameTab(canvas, graphics, item, button, button.cy + button.r - 6 * dp);
        else drawTab(canvas, button.label, button.cx, button.cy + button.r - 6 * dp);
        return true;
    }





    private final D2ItemLabel.Cache tabLabels = new D2ItemLabel.Cache(), ringLabels = new D2ItemLabel.Cache();

    private D2ModItems.Item tabItem;
    private D2Hud tabFont;
    private int tabBudget = -1, tabTextWidth, tabColour, tabFullWidth;
    private String[] tabLines;

    private D2ModItems.Item[] ringNameItems = new D2ModItems.Item[0];
    private String[] ringNames = new String[0];
    private final HashMap<String, String> baseNames = new HashMap<>();





    private D2ModItems.Item labelItem() {
        D2ModState s = mod;
        Object candidate = interactCandidate;
        if (s == null || editMode || s.npcTalking || npcMenuOpen() || !(candidate instanceof D2ModState.Thing)) return null;
        D2ModState.Thing thing = (D2ModState.Thing) candidate;
        if (thing.kind == D2ModState.ITEM_GOLD || !s.items.contains(thing)) return null;
        D2ModItems details = modLink.items();
        D2ModItems.Item item = details != null && details.namesKnown ? details.find(thing.id) : null;

        return item != null && !item.name.trim().isEmpty() && D2ItemLabel.drawable(item.name) ? item : null;
    }


    private String baseName(D2Hud graphics, String code) {
        String name = baseNames.get(code);
        if (name == null) {
            name = graphics.gameText(code);
            if (name == null) name = "";
            baseNames.put(code, name);
        }
        return name.isEmpty() ? null : name;
    }


    private String itemText(D2Hud graphics, D2ModItems.Item item, int budget) {
        return tabLabels.text(item.name, item.quality, item.runeword, baseName(graphics, item.code), graphics::smallTextWidth,
            budget, D2ItemLabel.VARIANT);
    }








    private void drawNameTab(Canvas canvas, D2Hud graphics, D2ModItems.Item item, Button button, float top) {
        float k = menuTextScale(), pad = 8 * dp, margin = 8 * dp, w = getWidth();


        if (item != tabItem || graphics != tabFont) {
            int widest = 0;
            for (String line : itemText(graphics, item, Integer.MAX_VALUE).split("\n")) widest = Math.max(widest, graphics.smallTextWidth(line));
            tabFullWidth = widest;
            tabColour = D2ItemLabel.colour(item.quality, item.category, item.ethereal, item.socketed, item.runeword);
            tabItem = item;
            tabFont = graphics;
            tabBudget = -1;
        }
        boolean growsRight = D2ItemLabel.tabGrowsRight(button.cx, button.r, w, margin, tabFullWidth * k + pad);
        float maxWidth = D2ItemLabel.tabMaxWidth(button.cx, button.r, w, margin, growsRight);
        int budget = Math.max(1, (int) ((maxWidth - pad) / k));
        if (budget != tabBudget) {
            tabLines = itemText(graphics, item, budget).split("\n");
            int widest = 0;
            for (String line : tabLines) widest = Math.max(widest, graphics.smallTextWidth(line));
            tabTextWidth = widest;
            tabBudget = budget;
        }
        String[] lines = tabLines;
        int colour = tabColour;
        float tabWidth = tabTextWidth * k + pad, lineHeight = 12 * k;
        float left = tabWidth <= 2 * button.r ? button.cx - tabWidth / 2
            : (growsRight ? button.cx - button.r : button.cx + button.r - tabWidth);
        left = Math.max(margin, Math.min(w - margin - tabWidth, left));
        rect.set(left, top, left + tabWidth, top + lineHeight * lines.length);
        fillPaint.setColor(Color.argb(220, 10, 8, 6));
        canvas.drawRoundRect(rect, 3 * dp, 3 * dp, fillPaint);
        for (int i = 0; i < lines.length; i++)
            graphics.drawSmallText(canvas, lines[i], left + tabWidth / 2, top + lineHeight * i + lineHeight / 2, k, 255, colour);
    }





    private boolean drawSkillIcon(Canvas canvas, Button button, boolean pressed) {
        D2Hud graphics = hud;
        D2ModState s = mod;
        if (graphics == null || s == null || s.characterClass < 0) return false;
        int skill;
        boolean active = false;
        int attackRight = button.kind == KIND_ATTACK ? attackRightSkill() : -1;
        if (button.kind == KIND_ATTACK) skill = attackRight >= 0 ? attackRight : s.leftSkill;
        else if (button.kind == KIND_SKILL && button.vk >= VK_F1 && button.vk < VK_F1 + 16) {
            int index = button.vk - VK_F1;
            skill = s.hotkeySkill[index];
            active = skill >= 0 && (s.hotkeyLeft[index] ? skill == s.leftSkill : skill == s.rightSkill);
        }
        else return false;
        android.graphics.Bitmap icon = graphics.skillIcon(skill);
        if (icon == null) return false;


        android.graphics.BitmapShader shader = iconShaders.get(icon);
        if (shader == null) {
            shader = new android.graphics.BitmapShader(icon, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            iconShaders.put(icon, shader);
        }


        float iconR = Math.min(button.r, 30 * dp * scale);
        float width = strokePaint.getStrokeWidth();
        boolean gameRing = graphics.hasRing();
        if (iconR < button.r) {
            fillPaint.setColor(Color.argb(pressed ? 235 : 205, 12, 10, 8));
            canvas.drawCircle(button.cx, button.cy, button.r, fillPaint);
            if (!gameRing) {
                strokePaint.setStrokeWidth(2 * dp);
                strokePaint.setColor(Color.rgb(148, 126, 74));
                canvas.drawCircle(button.cx, button.cy, button.r - dp, strokePaint);
            }
        }
        float k = iconR / (Math.min(icon.getWidth(), icon.getHeight()) / 2f - 0.5f);
        iconMatrix.setScale(k, k);
        iconMatrix.postTranslate(button.cx - icon.getWidth() * k / 2, button.cy - icon.getHeight() * k / 2);
        shader.setLocalMatrix(iconMatrix);
        roundIconPaint.setShader(shader);
        roundIconPaint.setColorFilter(pressed ? PRESSED_FILTER : null);
        canvas.drawCircle(button.cx, button.cy, iconR, roundIconPaint);
        if (gameRing) graphics.drawRing(canvas, button.cx, button.cy, button.r, menuTextScale());
        else {

            strokePaint.setStrokeWidth(1.5f * dp);
            strokePaint.setColor(Color.rgb(90, 74, 48));
            canvas.drawCircle(button.cx, button.cy, iconR, strokePaint);
        }
        if (active) {
            strokePaint.setColor(Color.rgb(236, 201, 127));
            strokePaint.setStrokeWidth(3 * dp);
            canvas.drawCircle(button.cx, button.cy, button.r + 1.5f * dp, strokePaint);
        }
        strokePaint.setStrokeWidth(width);


        String name = skillNames ? graphics.skillName(skill) : null;
        if (name != null && !name.isEmpty()) drawTab(canvas, name, button.cx, button.cy + button.r - 6 * dp);

        int ammo = s.ammunition();
        if (ammo >= 0 && AMMO_SKILLS.contains(skill)) drawTab(canvas, Integer.toString(ammo), button.cx + button.r * 0.6f, button.cy - button.r * 0.95f);

        if (attackRight >= 0) drawTab(canvas, "R", button.cx + button.r * 0.75f, button.cy + button.r * 0.7f);

        if (button.kind == KIND_SKILL && s.hotkeyLeft[button.vk - VK_F1]) drawTab(canvas, "L", button.cx + button.r * 0.75f, button.cy + button.r * 0.7f);
        return true;
    }



    private String zeroLifeNote;
    private long zeroLifeNoteUntil;

    private String zeroLifeCheckedFor;
    private long zeroLifeCheckSince;

    private int zeroLifeTries;
    private long zeroLifeTryAt;







    private void checkZeroLife(D2ModState s, long now) {


        String name = modLink.currentCharacterName();
        if (name == null || !modLink.heroDeathKnown()) return;
        if (!name.equals(zeroLifeCheckedFor)) {
            zeroLifeCheckedFor = name;
            zeroLifeCheckSince = now;
            zeroLifeTries = 0;
        }
        if (zeroLifeCheckSince == 0) return;
        if (s.life > 0 || modLink.heroDead() || s.maxLife <= 0) {

            if (zeroLifeTries > 0 && s.life > 0) showZeroLifeNote(text(R.string.zero_life_potion), now);
            zeroLifeCheckSince = 0;
            return;
        }

        if (pauseWanted || now - zeroLifeTryAt < 1500) return;
        boolean heal = s.potionCountBelt[0] + s.potionCountInventory[0] > 0;
        boolean rejuv = s.beltHas(D2ModState.BELT_REJUV);
        if ((!heal && !rejuv) || zeroLifeTries >= 4 || now - zeroLifeCheckSince > 15000) {
            showZeroLifeNote(text(R.string.zero_life_heal), now);
            D2Trace.log("life 0 after loading: " + (heal || rejuv ? "potions did not help" : "no potion") + ", note");
            zeroLifeCheckSince = 0;
            return;
        }
        zeroLifeTries++;
        zeroLifeTryAt = now;
        modLink.drink(heal ? D2ModLink.DRINK_HEAL : D2ModLink.DRINK_REJUV, false);
        D2Trace.log("life 0 after loading: " + (heal ? "healing" : "rejuvenation") + " potion, try " + zeroLifeTries);
    }

    private void showZeroLifeNote(String text, long now) {
        zeroLifeNote = text;
        zeroLifeNoteUntil = now + 8000;
        invalidate();
    }

    private void drawZeroLifeNote(Canvas canvas) {
        String text = zeroLifeNote;
        if (text == null) return;
        if (SystemClock.uptimeMillis() > zeroLifeNoteUntil) {
            zeroLifeNote = null;
            return;
        }
        D2Hud graphics = hud;
        float k = menuTextScale(), cx = getWidth() / 2f, top = 110 * dp;
        float w = graphics != null ? graphics.textWidth(text) * k : textPaint.measureText(text);
        rect.set(cx - w / 2 - 14 * k, top, cx + w / 2 + 14 * k, top + 30 * k);
        drawGameFrame(canvas, rect, Color.argb(215, 8, 7, 5));
        if (graphics != null) graphics.drawText(canvas, text, cx, top + 15 * k, k);
        else {
            textPaint.setTextSize(14 * dp);
            canvas.drawText(text, cx, top + 20 * k, textPaint);
        }
        postInvalidateDelayed(500);
    }





    private void drawSaveWarning(Canvas canvas, String text) {
        D2ModState s = mod;
        if (text == null || s == null || s.gameHeight <= 0) return;
        D2Hud graphics = hud;
        float k = menuTextScale(), cx = getWidth() / 2f, top = gameMapping(s, materialsMapping)[2] + 44 * k;
        float w = graphics != null ? graphics.textWidth(text) * k : textPaint.measureText(text);
        rect.set(cx - w / 2 - 14 * k, top, cx + w / 2 + 14 * k, top + 26 * k);
        if (!drawGameFrame(canvas, rect, Color.argb(225, 40, 6, 4))) {
            fillPaint.setColor(Color.argb(225, 40, 6, 4));
            canvas.drawRect(rect, fillPaint);
        }
        if (graphics != null) graphics.drawText(canvas, text, cx, top + 13 * k, k, 0xFF4040);
        else {
            textPaint.setTextSize(14 * dp);
            canvas.drawText(text, cx, top + 18 * k, textPaint);
        }
    }


    private boolean materialsOn() {
        return materials && gridOriginal && !idle.running();
    }





    private static boolean layerCanDraw(D2ModState s) {
        return s == null || !s.worldSignal || s.worldDrawn;
    }


    private final D2LayerSender pointsLayer = new D2LayerSender(D2Layer.POINTS);
    private String pointsLayerWhy;





    private boolean updatePointsLayer(Canvas canvas, D2ModState s) {
        D2ModLink link = modLink;
        if (link == null) return false;
        long now = SystemClock.uptimeMillis();
        D2InfoLayer.Art art = hud != null ? hud.layerArt() : null;
        D2Layer list = null;
        if (s != null && art != null && art.frame() != null && link.capable2(D2ModLink.CAP2_LAYERS)) {
            list = new D2Layer();
            pointHighlight.into = list;
            pointHighlight.intoFrame = art.frame();
            try {
                pointHighlight.draw(canvas, hud, s, link.points(1500), link.characterName(), gameMapping(s, pointHighlight.mapping), now);
            }
            finally {
                pointHighlight.into = null;
            }
        }
        int w = s != null ? s.gameWidth : 0, h = s != null ? s.gameHeight : 0;
        boolean byMod = pointsLayer.update(list, w, h, now, link.drawnLayers(), link.drawnLayersAt(), layerCanDraw(s), link::sendLayer);
        String why = list == null || list.isEmpty() ? null : byMod ? "drawn by the mod" : pointsLayer.fallbackReason();
        if (why != null && !why.equals(pointsLayerWhy)) D2Trace.log("points layer: " + why);
        pointsLayerWhy = why;
        if (byMod) postInvalidateDelayed(250);
        return byMod;
    }


    private final D2LayerSender stoneLayer = new D2LayerSender(D2Layer.STONES);
    private String stoneLayerWhy;






    private boolean updateStoneLayer(long now) {
        D2ModLink link = modLink;
        D2ModState s = mod;
        if (link == null) return false;
        D2InfoLayer.Art art = hud != null ? hud.layerArt() : null;
        boolean capable = !editMode && s != null && s.gameWidth > 0 && s.gameHeight > 0 && art != null
            && link.capable2(D2ModLink.CAP2_LAYERS);
        D2Layer list = null;
        if (capable) {
            list = new D2Layer();
            Object[][] stones = {{sortButton, sortSpot}, {respecButton, respecSpot}};
            for (Object[] st : stones) {
                Button b = (Button) st[0];
                float[] spot = (float[]) st[1];
                if (b == null || !isVisible(b) || spot[2] <= 0) continue;
                int colour = b.kind == KIND_RESPEC && respecArmed() ? D2Layer.RED : D2Layer.STONE_TEXT;

                D2MaterialsLayer.stoneFrame(list, art, Math.round(spot[0]), Math.round(spot[1]), D2PageButtons.STONE_W, D2PageButtons.STONE_H);
                D2MaterialsLayer.pageStone(list, art, b.label, Math.round(spot[0]), Math.round(spot[1]), b.pressed, colour);
            }
        }
        int w = s != null ? s.gameWidth : 0, h = s != null ? s.gameHeight : 0;
        boolean byMod = stoneLayer.update(list, w, h, now, link.drawnLayers(), link.drawnLayersAt(), layerCanDraw(s), link::sendLayer);
        String why = list == null || list.isEmpty() ? null : byMod ? "drawn by the mod" : stoneLayer.fallbackReason();
        if (why != null && !why.equals(stoneLayerWhy)) D2Trace.log("stone layer: " + why);
        stoneLayerWhy = why;
        if (byMod) postInvalidateDelayed(250);
        return byMod;
    }




    private final D2WindowInfo windowInfo = new D2WindowInfo();
    private final D2LayerSender windowInfoLayer = new D2LayerSender(D2Layer.WINDOW_INFO);
    private final D2WindowInfo.Open windowOpen = new D2WindowInfo.Open();

    private final int[] windowInfoBox = new int[4];
    private boolean windowInfoOn = true, windowInfoByMod;
    private String windowInfoWhy;

    private final D2WindowInfo.Texts windowInfoTexts = (w, pad, stash) -> {
        switch (w) {
            case SHOP: return text(pad ? R.string.win_info_shop_pad : R.string.win_info_shop);
            case MERC: return text(pad ? R.string.win_info_merc_pad : R.string.win_info_merc);
            case STASH: return text(pad ? R.string.win_info_stash_pad : R.string.win_info_stash);
            case CUBE: return text(pad ? R.string.win_info_cube_pad : R.string.win_info_cube);
            default: return text(pad ? R.string.win_info_inv_pad : stash ? R.string.win_info_inv_stash : R.string.win_info_inv);
        }
    };

    private static D2Hints.Topic windowTopic(D2WindowInfo.Window w) {
        return w == D2WindowInfo.Window.STASH ? D2Hints.Topic.WIN_STASH : w == D2WindowInfo.Window.CUBE ? D2Hints.Topic.WIN_CUBE
            : D2Hints.Topic.WIN_INVENTORY;
    }


    private boolean fillWindowOpen(D2ModState s, long now) {
        D2WindowInfo.Open o = windowOpen;
        boolean on = windowInfoOn && !editMode && s != null && s.gameWidth > 0 && s.gameHeight > 0 && modLink != null
            && modLink.capable2(D2ModLink.CAP2_LAYERS) && !s.isOpen(D2ModState.UI_ESCMENU) && !s.isOpen(D2ModState.UI_TRADE);
        o.inventory = on && D2WindowInfo.inventoryShown(s.isOpen(D2ModState.UI_INVENTORY), s.isOpen(D2ModState.UI_STASH),
            s.isOpen(D2ModState.UI_CUBE));
        o.stash = on && s.isOpen(D2ModState.UI_STASH);

        o.cube = on && s.isOpen(D2ModState.UI_CUBE) && !recipes.isOpen();
        o.merc = on && s.isOpen(D2ModState.UI_MERC);
        o.shop = on && s.isOpen(D2ModState.UI_NPCSHOP);
        o.materialsWindow = o.stash && materialsWindow.windowOpen(materialsOn(), now);
        o.materialsEntry = o.stash && materialsWindow.entry(materialsOn(), now) != null;
        o.pad = padActive;
        return on;
    }


    private void updateWindowInfoLayer(long now) {
        D2ModState s = mod;
        D2ModLink link = modLink;
        if (link == null) return;
        D2InfoLayer.Art art = hud != null ? hud.layerArt() : null;
        D2Layer list = null;
        if (fillWindowOpen(s, now) && art != null) {
            if (s.worldDrawn || !s.worldSignal) {
                D2WindowInfo.Window first = windowInfo.update(windowOpen, w -> hints.seen(windowTopic(w)), now);
                if (first != null) {
                    hints.markSeen(windowTopic(first));
                    if (hints.takeSeenChanged()) D2Settings.setHintsSeen(getContext(), hints.encode());
                    D2Trace.log("window info: first box " + first);
                }
            }
            list = windowInfo.layer(art, windowOpen, windowInfoTexts, s.gameWidth, s.gameHeight, now, windowInfoBox);
        } else {
            windowInfo.hide();
            windowInfoBox[2] = 0;
        }
        int w = s != null ? s.gameWidth : 0, h = s != null ? s.gameHeight : 0;
        windowInfoByMod = windowInfoLayer.update(list, w, h, now, link.drawnLayers(), link.drawnLayersAt(), layerCanDraw(s), link::sendLayer);

        String why = list == null ? null : windowInfoByMod ? "drawn by the mod" : windowInfoLayer.fallbackReason();
        if (why != null && !why.equals(windowInfoWhy)) D2Trace.log("window info layer: " + why);
        windowInfoWhy = why;
        if (list != null) postInvalidateDelayed(250);
    }


    private boolean windowInfoTakes(float x, float y) {
        D2ModState s = mod;
        long now = SystemClock.uptimeMillis();
        if (!windowInfoByMod || !fillWindowOpen(s, now)) return false;
        float[] m = gameMapping(s);
        float lx = (x - m[1]) / m[0] - Math.round((s.gameWidth - 800) / 2f), ly = (y - m[2]) / m[0] - Math.round((s.gameHeight - 600) / 2f);
        boolean taken = windowInfo.tap(lx, ly, windowOpen, now, windowInfoBox[2] > 0 ? windowInfoBox : null);
        if (taken) invalidate();
        return taken;
    }


    private final D2LayerSender infoLayer = new D2LayerSender(D2Layer.INFO);
    private String infoLayerWhy;






    private boolean updateInfoLayer(D2ModState s, D2Hud graphics) {
        D2ModLink link = modLink;
        if (link == null) return false;
        long now = SystemClock.uptimeMillis();
        boolean capable = s != null && s.gameWidth > 0 && s.gameHeight > 0 && graphics != null && graphics.layerArt() != null
            && link.capable2(D2ModLink.CAP2_LAYERS) && link.capable2(D2ModLink.CAP2_LAYERS_BAR);
        D2Layer list = capable ? infoStrip.layer(graphics.layerArt(), s.gameWidth, s.gameHeight, s.staminaKnown && s.stamina >= 0,
            s.isPanelOpen() || beltShown(s)) : null;
        int w = s != null ? s.gameWidth : 0, h = s != null ? s.gameHeight : 0;

        boolean byMod = infoLayer.update(list, w, h, now, link.drawnLayers(), link.drawnLayersAt(), layerCanDraw(s), link::sendLayer);
        String why = list == null || list.isEmpty() ? null : byMod ? "drawn by the mod" : infoLayer.fallbackReason();
        if (why != null && !why.equals(infoLayerWhy)) D2Trace.log("info layer: " + why);
        infoLayerWhy = why;
        return byMod;
    }


    private final D2LayerSender materialsLayer = new D2LayerSender(D2Layer.MATERIALS);
    private String materialsLayerWhy;






    private boolean updateMaterialsLayer(long now) {
        D2ModState s = mod;
        D2ModLink link = modLink;
        if (link == null) return false;
        boolean capable = !editMode && s != null && s.gameWidth > 0 && s.gameHeight > 0 && link.capable2(D2ModLink.CAP2_LAYERS);
        D2Layer list = capable ? materialsWindow.layer(hud, materialsOn(), now) : null;
        int w = s != null ? s.gameWidth : 0, h = s != null ? s.gameHeight : 0;
        boolean byMod = materialsLayer.update(list, w, h, now, link.drawnLayers(), link.drawnLayersAt(), link::sendLayer);
        String why = list == null || list.isEmpty() ? null : byMod ? "drawn by the mod" : materialsLayer.fallbackReason();
        if (why != null && !why.equals(materialsLayerWhy)) D2Trace.log("materials layer: " + why);
        materialsLayerWhy = why;
        if (byMod) postInvalidateDelayed(250);
        return byMod;
    }




    private final D2RezeptHook recipes = new D2RezeptHook(new D2RezeptHook.Host() {
        @Override public D2ModState state() { return mod; }
        @Override public D2ModLink link() { return modLink; }
        @Override public D2Hud graphics() { return hud; }
        @Override public float[] mapping(D2ModState s, float[] into) { return gameMapping(s, into); }
        @Override public boolean ladder(D2ModState s) { return s.plugyLoaded && D2Settings.ladderStarted(getContext()); }
        @Override public void pause(boolean on) { setPaused(on); }
        @Override public boolean paused() { return pauseWanted; }
        @Override public void note(String text) { showNote(text); }
        @Override public String text(int id, Object... args) { return D2TouchOverlay.this.text(id, args); }
        @Override public android.content.Context context() { return getContext(); }
        @Override public void redraw() { invalidate(); }
        @Override public void haptic() { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); }
        @Override public void schedule(Runnable r, long delayMs) { handler.postDelayed(r, delayMs); }
        @Override public void cancel(Runnable r) { handler.removeCallbacks(r); }
    });


    void openRecipeBook(boolean pausedAtMenu) {
        recipes.openFromMenu(pausedAtMenu);
    }


    boolean recipesBack() {
        return recipes.back();
    }






    boolean gameBack() {
        D2ModState s = mod;
        if (s == null || editMode) return false;
        int back = D2GameWindows.back(s.panelFlags(), s.npcTalking, s.isOpen(D2ModState.UI_AUTOMAP), pauseWanted);
        if (back == D2GameWindows.BACK_APP) return false;
        D2Trace.log(back == D2GameWindows.BACK_ESC ? "back: Esc to the game (windows " + Long.toHexString(s.panelFlags()) + ")"
            : "back: Tab to the game (map open)");
        pressKey(back == D2GameWindows.BACK_ESC ? VK_ESCAPE : VK_TAB);
        return true;
    }






    void escapeKey() {
        if (recipes.back()) return;
        D2Trace.log("Esc key to the game");
        pressKey(VK_ESCAPE);
    }





    public boolean materialsNotSaving() {
        return materialsWindow.saveBlocked(modLink.materials());
    }


    private final class MaterialsHost implements D2MaterialsWindow.Host {
        @Override public void send(int order, int arg, int count) { modLink.materials(order, arg, count); }
        @Override public void note(String text) { showNote(text); }
        @Override public String text(int id, Object... args) { return D2TouchOverlay.this.text(id, args); }
        @Override public boolean backedUp() { return gameDir != null && D2Settings.materialsBackedUp(getContext(), materialsKey()); }
        @Override public void startBackup() { backupMaterials(); }
        @Override public void haptic() {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            invalidate();
        }
        @Override public void schedule(Runnable r, long delayMs) { handler.postDelayed(r, delayMs); }
        @Override public void cancel(Runnable r) { handler.removeCallbacks(r); }
        @Override public boolean sorting() { return !sortSettled(); }
        @Override public void sound(String file) { D2Sound.play(getContext(), file); }
        @Override public boolean cursorTaken() { return D2TouchOverlay.this.cursorTaken(); }
        @Override public String recipeLine(int kind) {
            D2Hud h = hud;
            String code = D2Materials.code(kind);
            return recipes.materialsLine(code, D2Materials.name(kind, h != null ? h.gameText(code) : null));
        }
        @Override public void openRecipes(int kind) { recipes.openFor(D2Materials.code(kind)); }
    }








    private void backupMaterials() {
        final File dir = gameDir;
        if (dir == null) {
            materialsWindow.backupDone(false);
            return;
        }
        materialsBackupRunning = true;
        final File saves = saveDir != null ? saveDir : D2Saves.saveDir(dir);
        final File key = materialsKey();
        final Context context = getContext();
        new Thread(() -> {
            boolean ok = false;
            try {
                for (int attempt = 0; attempt < 2 && !ok; attempt++) {
                    long until = SystemClock.uptimeMillis() + 10000;
                    while (System.currentTimeMillis() - D2Saves.newestSaveFile(saves) < 3000 && SystemClock.uptimeMillis() < until) {
                        Thread.sleep(250);
                    }
                    long before = D2Saves.newestSaveFile(saves);
                    File zip = D2Saves.backup(saves, "vor-Materialien");
                    ok = zip != null && D2Saves.newestSaveFile(saves) == before;
                    if (zip != null && !ok && !zip.delete()) D2Trace.log("materials: could not delete the restless backup " + zip);
                    D2Trace.log("materials: backup " + (ok ? "written: " + zip.getName() : "attempt " + (attempt + 1) + " failed"));
                }
            }
            catch (Exception e) {
                D2Trace.log("materials: backup failed: " + e);
            }
            finally {

                final boolean done = ok;
                handler.post(() -> {
                    materialsBackupRunning = false;
                    if (done) D2Settings.setMaterialsBackedUp(context, key);
                    materialsWindow.backupDone(done);
                });
            }
        }, "d2-materials-backup").start();
    }


    private void drawIdleBoard(Canvas canvas) {
        String text = idle.board();
        D2Hud graphics = hud;
        if (text == null) return;
        float k = menuTextScale(), cx = getWidth() / 2f, top = 64 * dp;
        float w = graphics != null ? graphics.textWidth(text) * k : textPaint.measureText(text);
        rect.set(cx - w / 2 - 14 * k, top, cx + w / 2 + 14 * k, top + 30 * k);
        drawGameFrame(canvas, rect, Color.argb(215, 8, 7, 5));
        if (graphics != null) graphics.drawText(canvas, text, cx, top + 15 * k, k);
        else {
            textPaint.setTextSize(14 * dp);
            canvas.drawText(text, cx, top + 20 * k, textPaint);
        }
        postInvalidateDelayed(1000);
    }

    private void drawPause(Canvas canvas) {
        D2ResumeExit exit = resumeHint.exit();

        fillPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), fillPaint);
        D2Hud graphics = hud;
        float cy = getHeight() * 0.42f;
        if (graphics != null) graphics.drawText(canvas, text(R.string.paused), getWidth() / 2f, cy, menuTextScale() * 2);
        else {
            textPaint.setTextSize(30 * dp);
            canvas.drawText(text(R.string.paused), getWidth() / 2f, cy, textPaint);
        }

        String tapLine = text(R.string.tap_to_continue);
        if (graphics != null) graphics.drawText(canvas, tapLine, getWidth() / 2f, cy + 48 * dp, menuTextScale(), 0xD2CEC6);
        else {
            textPaint.setTextSize(14 * dp);
            canvas.drawText(tapLine, getWidth() / 2f, cy + 48 * dp, textPaint);
        }
        exit.draw(canvas, graphics, getWidth() / 2f, cy + 60 * dp, menuTextScale(), dp, padActive, SystemClock.uptimeMillis());
        cy += exit.height(menuTextScale(), dp);

        D2InfoStrip info = infoStrip;
        if (graphics == null || info == null) return;
        String[] lines = info.pauseLines(System.currentTimeMillis(), autosave && mod != null && mod.plugyLoaded,
            modLink != null ? modLink.characterName() : null);
        if (lines.length == 0) return;
        float k = menuTextScale(), w = 0;
        for (String line : lines) w = Math.max(w, graphics.textWidth(line) * k);
        float top = cy + 72 * dp, lineH = 18 * k;
        rect.set(getWidth() / 2f - w / 2 - 14 * k, top, getWidth() / 2f + w / 2 + 14 * k, top + lines.length * lineH + 12 * k);
        drawGameFrame(canvas, rect, Color.argb(215, 8, 7, 5));
        float y = top + 6 * k + lineH / 2;
        for (String line : lines) {
            graphics.drawText(canvas, line, getWidth() / 2f, y, k);
            y += lineH;
        }
        postInvalidateDelayed(1000);
    }


    private void drawHud(Canvas canvas) {
        D2Hud graphics = hud;
        D2ModState s = mod;
        if (graphics != null && s != null && s.durabilityLeftOut() && !editMode) {

            fillPaint.setColor(Color.argb(205, 8, 7, 5));
            strokePaint.setColor(Color.rgb(148, 126, 74));
            graphics.drawDurability(canvas, s.damagedLocation, s.damagedState, getWidth() - 8 * dp, 56 * dp, menuTextScale(), fillPaint, strokePaint);
        }
        if (graphics == null || s == null || s.gameWidth <= 0 || s.gameHeight <= 0) return;
        float[] m = gameMapping(s);
        boolean compact = hudLayout == D2Settings.HUD_COMPACT && s.hudLeftOut();
        if (compact) graphics.draw(canvas, s.gameWidth, s.gameHeight, m[0], m[1], m[2], s.lifePercent, s.manaPercent);

        if (orbNumbers && s.maxLife > 0 && !editMode && (compact || hudLayout == D2Settings.HUD_ORIGINAL)) {

            long open = s.panelFlags();
            graphics.drawOrbNumbers(canvas, s.gameWidth, s.gameHeight, m[0], m[1], m[2], compact, s.life, s.maxLife, s.mana, s.maxMana,
                D2GameWindows.lifeNumbers(open, recipes.isOpen()), D2GameWindows.manaNumbers(open));
        }

        if (infoStrip != null) {
            infoStrip.setPlayers(modLink != null ? modLink.players() : 0);
            D2Points points = modLink != null ? modLink.points(3000) : null;
            infoStrip.setLevel(points != null ? points.level : 0);
            infoStrip.setFacts(modLink != null ? modLink.gridFacts() : null);
            infoStrip.setExperience(modLink != null ? modLink.experience() : null);
        }
        boolean infoShown = infoStrip != null && !editMode && infoStrip.active();

        boolean infoByMod = updateInfoLayer(infoShown ? s : null, graphics);
        if (infoShown && !infoByMod) {
            infoStrip.draw(canvas, graphics, s.gameWidth, s.gameHeight, m[0], m[1], m[2], s.staminaKnown && s.stamina >= 0,
                s.isPanelOpen() || beltShown(s), compact);
        }
        if (infoShown) postInvalidateDelayed(infoByMod ? 500 : 1000);

        long nowMs = SystemClock.uptimeMillis();
        effectsView.update(modLink != null ? modLink.effects() : new D2Effects.Effect[0], nowMs);
        boolean effectsShown = !editMode && effectsView.active(nowMs);

        if (!updateEffectsLayer(effectsShown ? s : null, graphics, m, nowMs) && effectsShown) {
            effectsView.draw(canvas, graphics, s.gameWidth, s.gameHeight, m[0], m[1], m[2], nowMs);
        }
        if (effectsShown) postInvalidateDelayed(250);
    }


    private final D2LayerSender effectsLayer = new D2LayerSender(D2Layer.EFFECTS);
    private String effectsLayerWhy;





    private boolean updateEffectsLayer(D2ModState s, D2Hud graphics, float[] m, long now) {
        D2ModLink link = modLink;
        if (link == null) return false;
        D2LayerArt art = graphics != null ? graphics.layerArt() : null;
        D2Layer list = null;
        boolean shrine = false;
        if (s != null && art != null && link.capable2(D2ModLink.CAP2_LAYERS)) {
            list = new D2Layer();
            if (!effectsView.layer(list, graphics, art, s.gameWidth, s.gameHeight, m[0], m[1], m[2], now)) {
                list = null;
                shrine = true;
            }
        }
        int w = s != null ? s.gameWidth : 0, h = s != null ? s.gameHeight : 0;
        boolean byMod = effectsLayer.update(list, w, h, now, link.drawnLayers(), link.drawnLayersAt(), layerCanDraw(s), link::sendLayer);
        String why = shrine ? "a shrine's picture: drawn by the app"
            : list == null || list.isEmpty() ? null : byMod ? "drawn by the mod" : effectsLayer.fallbackReason();
        if (why != null && !why.equals(effectsLayerWhy)) D2Trace.log("effects layer: " + why);
        effectsLayerWhy = why;
        return byMod;
    }


    private float[] gameMapping(D2ModState s) {
        return gameMapping(s, new float[3]);
    }


    private float[] gameMapping(D2ModState s, float[] m) {
        int dw = xServer.screenInfo.width, dh = xServer.screenInfo.height;
        float k = Math.min(dw / (float) s.gameWidth, dh / (float) s.gameHeight);
        m[0] = k * aspect;
        m[1] = (dw - s.gameWidth * k) / 2 * aspect + offsetX;
        m[2] = (dh - s.gameHeight * k) / 2 * aspect + offsetY;
        return m;
    }


    private float menuTextScale() {
        D2ModState s = mod;
        if (s != null && s.gameHeight > 0) return gameMapping(s)[0];
        if (getHeight() <= 0) return 2 * dp;

        int dw = xServer.screenInfo.width, dh = xServer.screenInfo.height;
        int[] game = D2MenuScale.expectedGame(dw, dh, D2Settings.gameLines(getContext()));
        return D2MenuScale.text(aspect, dw, dh, game != null ? game[0] : 0, game != null ? game[1] : 0, getHeight());
    }





    private boolean drawGameFrame(Canvas canvas, RectF where, int fill) {
        D2Hud graphics = hud;
        if (graphics == null || !graphics.hasFrame()) return false;
        graphics.drawFrame(canvas, where, menuTextScale(), fill);
        return true;
    }





    private int menuPixelScale() {
        return Math.max(1, Math.round(menuTextScale()));
    }


    private static void snap(RectF r, int s) {
        int w = Math.max(2 * D2Frame.LINE, Math.round(r.width() / s)) * s, h = Math.max(2 * D2Frame.LINE, Math.round(r.height() / s)) * s;
        float left = Math.round(r.centerX() - w / 2f), top = Math.round(r.centerY() - h / 2f);
        r.set(left, top, left + w, top + h);
    }






    private void menuFrame(RectF r, int k) {
        D2Hud graphics = hud;
        int h = Math.max(2 * D2Frame.LINE, Math.round(r.height() / k)) * k, inside = h - 2 * D2Frame.LINE * k;
        int pad = D2Frame.menuInset(inside, graphics != null ? graphics.miniIconWidth() : 0, k);
        r.inset(pad, pad);
        snap(r, k);
    }


    private RectF menuInner(float w, float h, int k) {
        RectF r = new RectF(0, 0, w, h);
        menuFrame(r, k);
        r.inset(D2Frame.LINE * k, D2Frame.LINE * k);
        return r;
    }





    private float menuRowGap(float h) {
        int k = menuPixelScale();
        RectF r = new RectF(0, 0, h, h);
        menuFrame(r, k);
        return 4 * k - (h - r.width());
    }


    private float menuFrameDistance(Button b, float x) {
        RectF frame = new RectF(b.cx - b.hw, b.cy - b.r, b.cx + b.hw, b.cy + b.r);
        if (hud != null && hud.hasFrame()) menuFrame(frame, menuPixelScale());
        return Math.max(0, Math.abs(x - frame.centerX()) - frame.width() / 2);
    }


    private float menuLineMax() {
        return 64 * dp;
    }


    private String[] menuLines(D2Hud graphics, String label) {
        int k = menuPixelScale();
        return D2MenuRow.lines(label, t -> graphics.textWidth(t) * k, menuLineMax());
    }


    private float menuLabelHalf(D2Hud graphics, String label) {
        int k = menuPixelScale(), widest = 0;
        for (String line : menuLines(graphics, label)) widest = Math.max(widest, graphics.textWidth(line));
        return widest * Math.max(k, menuTextScale()) / 2 + 7 * dp;
    }


    private void drawMenuButton(Canvas canvas, Button button, boolean on) {
        D2Hud graphics = hud;
        rect.set(button.cx - button.hw, button.cy - button.r, button.cx + button.hw, button.cy + button.r);
        int fill = Color.argb(on ? 235 : 205, on ? 40 : 8, on ? 32 : 7, on ? 18 : 5);

        int k = menuPixelScale();
        boolean framed = graphics.hasFrame();
        if (framed) {
            menuFrame(rect, k);
            graphics.drawFrameSharp(canvas, rect, k, fill);
        }
        else {
            fillPaint.setColor(fill);
            canvas.drawRect(rect, fillPaint);
            strokePaint.setColor(Color.rgb(148, 126, 74));
            canvas.drawRect(rect, strokePaint);
        }

        float inset = framed ? D2Frame.LINE * k : 4 * dp;
        RectF inner = new RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset);
        switch (button.icon) {
            case ICON_TEXT: {

                String[] lines = menuLines(graphics, button.label);
                float step = 11 * k;
                for (int i = 0; i < lines.length; i++) {
                    graphics.drawTextSharp(canvas, lines[i], button.cx, button.cy + (i - (lines.length - 1) / 2f) * step, k);
                }
                break;
            }
            case ICON_RUN:
                graphics.drawRunIcon(canvas, mod == null || mod.running, on, square(inner));
                break;
            case ICON_ARROW:
                graphics.drawArrow(canvas, !menuShownOpen(), on, inner);
                break;
            case ICON_PORTAL: {

                D2ModState s = mod;
                if (!graphics.drawMenuSkillIcon(canvas, SKILL_BOOK_OF_TOWNPORTAL, square(inner), s != null && s.portals == 0 ? 90 : 255)) {
                    graphics.drawTextSharp(canvas, button.label, button.cx, button.cy, k);
                }
                if (s != null && s.portals >= 0) {

                    RectF frame = new RectF(rect);

                    String portals = Integer.toString(s.portals);
                    float tabW = graphics.smallTextWidth(portals) * k + 8 * dp;
                    drawMenuTab(canvas, portals, inner.right - tabW / 2, inner.top, k);
                    rect.set(frame);
                }
                break;
            }
            case ICON_CUBE:
                if (!graphics.drawCubeIcon(canvas, square(inner))) graphics.drawTextSharp(canvas, button.label, button.cx, button.cy, k);
                break;
            case ICON_PAUSE:
                fillPaint.setColor(Color.rgb(199, 179, 119));
                canvas.drawRect(button.cx - 7 * dp, button.cy - 9 * dp, button.cx - 2.5f * dp, button.cy + 9 * dp, fillPaint);
                canvas.drawRect(button.cx + 2.5f * dp, button.cy - 9 * dp, button.cx + 7 * dp, button.cy + 9 * dp, fillPaint);
                break;
            case ICON_LINES:
                strokePaint.setColor(Color.rgb(199, 179, 119));
                for (int i = -1; i <= 1; i++) {
                    float ly = button.cy + i * 6 * dp;
                    canvas.drawLine(button.cx - 8 * dp, ly, button.cx + 8 * dp, ly, strokePaint);
                }
                break;
            default:
                graphics.drawMiniIcon(canvas, button.icon, on, square(inner));
        }


        if (button.id.equals("menu_quests") && questNews()) {

            float size = 2 * Math.min(button.hw, button.r) * 0.42f;
            graphics.drawLevelCross(canvas, new RectF(rect.right - size * 0.8f, rect.top - size * 0.2f,
                rect.right + size * 0.2f, rect.top + size * 0.8f));
        }


        if (button.caption != null && !button.caption.isEmpty()) {
            float kc = menuCaptionScale(graphics, k);
            drawMenuTab(canvas, button.caption, button.cx, rect.bottom - 2 * k, kc, D2UiScale.captionPadding(kc, dp, uiFactor));
        }
    }


    private float menuCaptionScale(D2Hud graphics, int k) {
        if (uiFactor <= 1f) return k;
        float fit = Float.MAX_VALUE;
        for (Button b : buttons) {
            if (b.group != GROUP_MENU || b.caption == null || b.caption.isEmpty() || !isVisible(b)) continue;


            float half = Float.MAX_VALUE;
            for (Button o : buttons) {
                if (o == b || o.group != GROUP_MENU || !isVisible(o) || Math.abs(o.cy - b.cy) > b.r) continue;
                float d = Math.abs(o.cx - b.cx);
                half = Math.min(half, o.caption != null && !o.caption.isEmpty() ? d / 2 : d - o.hw);
            }
            float room = half == Float.MAX_VALUE ? 2 * b.hw : 2 * Math.max(b.hw, half);
            fit = Math.min(fit, D2UiScale.captionFit(graphics.smallTextWidth(b.caption), room));
        }
        return D2UiScale.captionScale(k, fit, uiFactor);
    }


    private boolean questNews() {
        D2ModState s = mod;
        return s != null && D2MenuRow.questNews(s.edgeButtonsHidden, s.isOpen(D2ModState.UI_QUESTLOG));
    }

    private static RectF square(RectF r) {
        float size = Math.min(r.width(), r.height());
        return new RectF(r.centerX() - size / 2, r.centerY() - size / 2, r.centerX() + size / 2, r.centerY() + size / 2);
    }






    private boolean drawGameRound(Canvas canvas, Button button, int fillAlpha, int strokeAlpha, int color) {
        D2Hud graphics = hud;
        if (graphics == null || !graphics.hasRing()) return false;
        float k = menuTextScale(), line = D2Frame.LINE * k;
        fillPaint.setColor(Color.argb(fillAlpha, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawCircle(button.cx, button.cy, button.r - line / 2, fillPaint);
        if (button.kind == KIND_POTION) {
            float width = strokePaint.getStrokeWidth();
            strokePaint.setStrokeWidth(2 * dp);
            strokePaint.setColor(Color.argb(strokeAlpha, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(button.cx, button.cy, button.r - line - dp, strokePaint);
            strokePaint.setStrokeWidth(width);
        }
        graphics.drawRing(canvas, button.cx, button.cy, button.r, k);
        return true;
    }

    private void drawCircle(Canvas canvas, float x, float y, float r, int fillAlpha, int strokeAlpha, int color) {
        fillPaint.setColor(Color.argb(fillAlpha, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawCircle(x, y, r, fillPaint);
        strokePaint.setColor(Color.argb(strokeAlpha, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawCircle(x, y, r, strokePaint);
    }







    private void drawAimPreview(Canvas canvas) {
        D2ModState s = mod;
        float height = s != null ? s.gameHeight : 600;
        float ranged = height * aimRangePct / 100f, melee = height * Math.min(aimRangeMeleePct, aimRangePct) / 100f;
        String key = aimPreviewKey;
        boolean meleeStrong = "aimRangeMelee".equals(key) || (!"aimRange".equals(key) && meleeReach(attackSkillNow()));
        double base = Math.atan2(dirY, dirX), half = Math.toRadians(aimConeDeg);
        Path cone = conePath(s, base, half, Math.max(ranged, melee)), coneInner = conePath(s, base, half, Math.min(ranged, melee));
        fillPaint.setColor(Color.argb(35, 255, 150, 50));
        canvas.drawPath(cone, fillPaint);
        fillPaint.setColor(Color.argb(45, 255, 150, 50));
        canvas.drawPath(coneInner, fillPaint);
        strokePaint.setColor(Color.argb(200, 255, 150, 50));
        canvas.drawPath(cone, strokePaint);
        if (aimBlindDeg > 0) {

            Path blind = conePath(s, Math.atan2(faceY, faceX) + Math.PI, Math.toRadians(aimBlindDeg / 2.0), Math.max(ranged, melee));
            fillPaint.setColor(Color.argb("aimBlind".equals(key) ? 90 : 45, 140, 20, 20));
            canvas.drawPath(blind, fillPaint);
            strokePaint.setColor(Color.argb("aimBlind".equals(key) ? 220 : 110, 170, 30, 30));
            canvas.drawPath(blind, strokePaint);
        }
        float width = strokePaint.getStrokeWidth();
        strokePaint.setStrokeWidth(meleeStrong ? width : 2 * width);
        strokePaint.setColor(Color.argb(meleeStrong ? 70 : 220, 255, 255, 255));
        canvas.drawPath(reachPath(s, ranged), strokePaint);
        strokePaint.setStrokeWidth(meleeStrong ? 2 * width : width);
        strokePaint.setColor(Color.argb(meleeStrong ? 220 : 70, 255, 150, 50));
        canvas.drawPath(reachPath(s, melee), strokePaint);
        strokePaint.setStrokeWidth(width);
    }


    private Path reachPath(D2ModState s, float range) {
        Path path = new Path();
        int steps = 64;
        for (int i = 0; i <= steps; i++) {
            float[] v = previewPoint(s, 2 * Math.PI * i / steps, range);
            if (i == 0) path.moveTo(v[0], v[1]);
            else path.lineTo(v[0], v[1]);
        }
        return path;
    }


    private Path conePath(D2ModState s, double base, double half, float range) {
        Path cone = new Path();
        int steps = 64;
        float[] c = previewPoint(s, 0, 0);
        cone.moveTo(c[0], c[1]);
        for (int i = 0; i <= steps; i++) {
            float[] v = previewPoint(s, base - half + 2 * half * i / steps, range);
            cone.lineTo(v[0], v[1]);
        }
        cone.close();
        return cone;
    }

    private float[] previewPoint(D2ModState s, double angle, float range) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double r = range / Math.sqrt(cos * cos + 4 * sin * sin);
        float gx = (float) (r * cos), gy = (float) (r * sin);
        if (s != null) return gameToView(s.playerX + gx, s.playerY + gy);
        float k = getHeight() / 600f;
        return new float[]{getWidth() / 2f + gx * k, getHeight() / 2f + gy * k};
    }

    private void drawModMarkers(Canvas canvas) {


        if (mod != null && mod.canHighlight() && highlightOn) return;
        D2ModState.Monster current = findMonster(attackTargetId > 0 ? attackTargetId : skillTargetId);
        D2ModState.Monster shown = current != null ? current : softTarget;
        if (shown != null) drawTargetFrame(canvas, shown);
    }





    private boolean fighting() {
        if (action == ACTION_ATTACK || action == ACTION_SKILL) {
            lastFightAt = SystemClock.uptimeMillis();
            return true;
        }
        return SystemClock.uptimeMillis() - lastFightAt < 2500;
    }



    private boolean mapFightHide, mapHiddenByUs, wasAiming, aimStarted;
    private long mapFightDelayMs = 3000, lastAimAt, mapTabAt;






    private void hideMapWhileFighting(D2ModState s) {
        long now = SystemClock.uptimeMillis();
        boolean aiming = action == ACTION_ATTACK || action == ACTION_SKILL;
        if (aiming && !wasAiming) aimStarted = true;
        wasAiming = aiming;
        if (aiming) lastAimAt = now;
        if (!mapFightHide || s == null) {
            mapHiddenByUs = aimStarted = false;
            return;
        }
        if (now - mapTabAt < 500) return;
        boolean open = s.isOpen(D2ModState.UI_AUTOMAP);
        boolean started = aimStarted;
        aimStarted = false;
        if (mapHiddenByUs) {
            if (open) mapHiddenByUs = false;
            else if (!aiming && now - lastAimAt >= mapFightDelayMs) {
                D2Trace.log("map back after fighting");
                mapHiddenByUs = false;
                mapTabAt = now;
                pressKey(VK_TAB);
            }
        }
        else if (started && open) {
            D2Trace.log("map hidden for fighting");
            mapHiddenByUs = true;
            mapTabAt = now;
            pressKey(VK_TAB);
        }
    }


    private void drawTargetFrame(Canvas canvas, D2ModState.Monster monster) {
        float x = 12 * dp, y = 10 * dp, width = 180 * dp;
        rect.set(x, y, x + width, y + 34 * dp);
        fillPaint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRoundRect(rect, 6 * dp, 6 * dp, fillPaint);
        int nameColor = Color.WHITE;
        if ((monster.flags & (D2ModState.MONSTER_UNIQUE | D2ModState.MONSTER_SUPERUNIQUE)) != 0) nameColor = Color.rgb(212, 175, 55);
        else if ((monster.flags & D2ModState.MONSTER_CHAMPION) != 0) nameColor = Color.rgb(100, 149, 237);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(12 * dp);
        int oldColor = textPaint.getColor();
        textPaint.setColor(nameColor);
        canvas.drawText(monster.name.isEmpty() ? "Monster" : monster.name, x + 8 * dp, y + 15 * dp, textPaint);
        textPaint.setColor(oldColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        rect.set(x + 8 * dp, y + 22 * dp, x + width - 8 * dp, y + 28 * dp);
        fillPaint.setColor(Color.argb(160, 60, 10, 10));
        canvas.drawRect(rect, fillPaint);
        rect.right = rect.left + (rect.right - rect.left) * Math.max(0, Math.min(100, monster.hpPercent)) / 100f;
        fillPaint.setColor(Color.argb(230, 200, 30, 30));
        canvas.drawRect(rect, fillPaint);
    }

    private void drawDiagnostics(Canvas canvas) {

        List<String> lines = new ArrayList<>();
        lines.add(modLink.describe());
        D2ModState s = mod;
        if (s != null) {
            float[] expected = desktopToGame(xServer.pointer.getX(), xServer.pointer.getY(), s);
            StringBuilder panels = new StringBuilder();
            for (int i = 1; i < 0x26; i++) if (s.isOpen(i)) panels.append(panels.length() > 0 ? "," : "").append(Integer.toHexString(i));
            StringBuilder belt = new StringBuilder();
            for (int i = 0; i < 4; i++) belt.append(s.belt[i]);
            lines.add(String.format(Locale.ROOT, "Spiel %dx%d Gebiet %d%s  Spieler %d,%d  Maus %d,%d (erwartet %d,%d)  Leben %d%% Mana %d%%",
                s.gameWidth, s.gameHeight, s.levelNo, s.inTown ? " (Stadt)" : "", s.playerX, s.playerY, s.mouseX, s.mouseY,
                Math.round(expected[0]), Math.round(expected[1]), s.lifePercent, s.manaPercent));
            StringBuilder monsters = new StringBuilder();
            for (int i = 0; i < Math.min(3, s.monsters.size()); i++) {
                D2ModState.Monster m = s.monsters.get(i);
                monsters.append(i > 0 ? " | " : "").append(m.name).append(' ').append(m.hpPercent).append("% f").append(m.flags)
                    .append(" @").append(m.x).append(',').append(m.y);
            }
            lines.add(String.format(Locale.ROOT, "Monster roh %d: %s  Maus über: %s  Treffer %d/%d",
                s.rawMonsters, monsters.length() > 0 ? monsters : "–",
                s.hoverType == 0xFF ? "nichts" : ("Typ " + s.hoverType + " #" + s.hoverId), hoverHits, hoverHits + hoverMisses));
            String[] drinkResults = {"noch keiner", "Gürtel", "Inventar", "kein Trank", "kein größerer"};
            lines.add(String.format(Locale.ROOT, "Trank: zuletzt %s, Mod-Antwort %s, Befehle angekommen %d",
                lastPotion, s.lastDrink < drinkResults.length ? drinkResults[s.lastDrink] : "?", s.commandsReceived));
            lines.add(String.format(Locale.ROOT, "Monster %d  Gegenstände %d  Objekte %d  Fenster [%s]  Gürtel unten %s  Ziel %d/%d  Abbildung %s",
                s.monsters.size(), s.items.size(), s.objects.size(), panels, belt, attackTargetId, skillTargetId,
                calibrated ? String.format(Locale.ROOT, "gelernt %.3f/%.3f (%d)", calibKx, calibKy, calibrationCount) : "geschätzt (" + calibrationCount + ")")
                + (positionShiftX != 0 || positionShiftY != 0 ? String.format(Locale.ROOT, "  Positionen verschoben %.0f,%.0f", positionShiftX, positionShiftY) : ""));
            D2ModItems carried = modLink.items();
            if (carried != null) lines.add(carried.describe());
        }
        lines.add(String.format(Locale.ROOT, "Zeiger %d,%d  Ziel %d,%d  Klick-Abweichung %s  Verstärkung %.2f/%.2f  abgeglichen %d",
            xServer.pointer.getX(), xServer.pointer.getY(), targetX, targetY,
            clickMeasured ? clickErrorX + "," + clickErrorY : "–", gainX, gainY, syncCount));
        lines.add("Finger: " + names(fingerDevices) + "   Schultertaste: " + names(triggerDevices)
            + "   gefiltert: " + names(triggerTouchDevices) + " (" + ignoredTriggerTouches + ")");
        lines.addAll(events);

        textPaint.setTextSize(10 * dp);
        float lineHeight = 13 * dp, y = getHeight() * 0.22f;
        rect.set(getWidth() * 0.12f, y - lineHeight, getWidth() * 0.88f, y + lineHeight * (lines.size() - 0.5f));
        fillPaint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(rect, fillPaint);
        for (String line : lines) {
            canvas.drawText(line, getWidth() / 2f, y, textPaint);
            y += lineHeight;
        }
    }

    private String names(Set<Integer> ids) {
        if (ids.isEmpty()) return "–";
        StringBuilder sb = new StringBuilder();
        for (int id : ids) sb.append(sb.length() > 0 ? ", " : "").append(deviceNames.get(id, "?")).append(" #").append(id);
        return sb.toString();
    }

    private String names(SparseArray<String> devices) {
        if (devices.size() == 0) return "–";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < devices.size(); i++) sb.append(i > 0 ? ", " : "").append(devices.valueAt(i)).append(" #").append(devices.keyAt(i));
        return sb.toString();
    }

    private void logEvent(String what, InputDevice device) {
        if (device != null) deviceNames.put(device.getId(), device.getName());
        events.addLast(what + "  –  " + (device != null ? device.getName() + " #" + device.getId() : "?"));
        while (events.size() > 5) events.removeFirst();
    }



    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        InputDevice device = event.getDevice();
        if (device != null) deviceNames.put(device.getId(), device.getName());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (debug) logEvent("Touch " + Math.round(event.getX(index)) + "," + Math.round(event.getY(index)), device);
                if (sortBlocks(event.getX(index), event.getY(index))) break;
                dropEarlySkillClick();
                onDown(event.getPointerId(index), event.getDeviceId(), event.getX(index), event.getY(index));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) onMove(event.getPointerId(i), event.getX(i), event.getY(i), event.getEventTime());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                onUp(event.getPointerId(index), false);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (debug) logEvent("Abbruch (CANCEL)", device);
                for (int i = touches.size() - 1; i >= 0; i--) onUp(touches.keyAt(i), true);
                break;
        }
        invalidate();
        return true;
    }

    private void onDown(int id, int deviceId, float x, float y) {
        Touch touch = new Touch();
        touch.deviceId = deviceId;
        touch.downTime = SystemClock.uptimeMillis();
        touch.downX = touch.x = x;
        touch.downY = touch.y = y;
        touches.put(id, touch);


        if (triggerTouchDevices.contains(deviceId) && !fingerDevices.contains(deviceId)) {
            touch.role = ROLE_NONE;
            ignoredTriggerTouches++;
            return;
        }
        if (padActive && D2Gamepad.touchEndsPad(deviceId, D2Idle.DEVICE_ID, D2SpieltestPort.playing())) {

            boolean hidden = padHidesControls();
            padUsed(false);
            Button menu = findButton(x, y);
            boolean windowOpen = !controlsVisible || pauseWanted || assignButton != null || editMode;
            if (hidden && D2Gamepad.firstTouchSwallowed(mod != null, windowOpen, menu != null && menu.group != GROUP_GAME)) {
                touch.role = ROLE_NONE;
                D2Trace.log("pad: first touch brings the buttons back");
                return;
            }
        }

        if (hintView.contains(x, y)) {
            touch.role = ROLE_HINT;
            return;
        }
        if (respecSpend.active() && noteRectShown && noteRect.contains(x, y)) {

            respecSpend.cancel("tap");
            touch.role = ROLE_NONE;
            return;
        }
        if (resumeHint.hit(x, y, touch.downTime)) {
            touch.role = ROLE_RESUME_HINT;
            return;
        }
        if (!editMode && recipes.takes(x, y, findButton(x, y) != null)) {
            touch.role = ROLE_RECIPES;
            recipes.down(x, y);
            return;
        }
        if (windowInfoTakes(x, y)) {
            touch.role = ROLE_NONE;
            return;
        }
        if (pauseWanted) {
            touch.role = ROLE_RESUME;
            resumeHint.exit().down(x, y, touch.downTime);
            return;
        }
        if (assignButton != null && !editMode) {

            touch.role = ROLE_ASSIGN;
            return;
        }
        if (treePicking()) {

            Button pick = findButton(x, y);
            if (pick != null && treePickTarget(pick)) {
                touch.role = ROLE_TREE_PICK;
                touch.button = pick;
                pick.pressed = true;
                invalidate();
                return;
            }
            endTreePick("a tap elsewhere");
        }

        if (inputBox.open()) {
            touch.role = ROLE_INPUT_BOX;
            inputBox.down(x, y);
            return;
        }
        Button button = findButton(x, y);

        if (!editMode && deviceId != D2Idle.DEVICE_ID && (button == null || button.group != GROUP_MENU) && deathTap()) {
            touch.role = ROLE_NONE;
            return;
        }
        if (editMode) {
            if (button != null && button.group == GROUP_GAME) {
                touch.role = ROLE_DRAG;
                touch.button = button;
                button.pressed = true;
            }
            else if (button != null) {
                touch.role = ROLE_BUTTON;
                touch.button = button;
                button.pressed = true;
            }
            else touch.role = ROLE_NONE;
            return;
        }
        if (materialsWindow.takes(x, y, button != null, materialsOn(), SystemClock.uptimeMillis())) {

            touch.role = ROLE_MATERIALS;
            materialsWindow.down(x, y);
            return;
        }
        if (button == null && effectsView.hitAt(x, y, SystemClock.uptimeMillis()) >= 0) {


            touch.role = ROLE_EFFECT;
            return;
        }
        if (button == null && ringTouch != null) {

            touch.role = ROLE_NONE;
            return;
        }
        if (button != null) {
            touch.role = ROLE_BUTTON;
            touch.button = button;
            button.pressed = true;
            onButtonDown(button, touch);
        }
        else if ((portraitAction = mercPortrait(touch)) == D2WorldTap.PORTRAIT_KEY) {

            touch.role = ROLE_MERC;
        }
        else if (portraitAction == D2WorldTap.PORTRAIT_GAME) {


            touch.role = ROLE_DIRECT;
            startDirect(touch);
        }
        else if ((touch.infoTap = infoTap(touch)) != INFO_NONE) {


            touch.role = ROLE_INFO;
        }
        else if (controlsVisible && inJoystickZone(x, y)) {
            touch.role = ROLE_JOYSTICK;
            if (joystickOwner == D2Gamepad.OWNER_PAD) joystickReleased();
            joystickOwner = D2Gamepad.OWNER_FINGER;
            joystickActive = true;
            hasDirection = false;
            baseX = knobX = x;
            baseY = knobY = y;
            joystickFollow.start(x, y, SystemClock.uptimeMillis());
        }
        else if (!worldTapToGame(touch)) {

            touch.role = ROLE_WORLD;
            D2Trace.log("tap into the world at " + Math.round(x) + "," + Math.round(y) + ": nothing");
        }
        else {
            touch.role = ROLE_DIRECT;

            if (worldTap.tapMoves && !worldTap.onGameControl && worldTap.panels == 0) faceDesktopPoint(toScreen(x, y));

            if (controlsVisible) lastFightAt = SystemClock.uptimeMillis();
            startDirect(touch);
        }
    }





    private boolean worldTapToGame(Touch touch) {
        D2ModState s = modLink.state();
        D2WorldTap.Conditions c = worldTap;
        c.tapMoves = tapMoves;
        c.ownFinger = touch.deviceId == D2Idle.DEVICE_ID;
        c.playtester = D2SpieltestPort.playing();
        c.stateFresh = s != null;
        c.controlsShown = controlsVisible;
        c.panels = s != null ? s.panelFlags() : 0;
        c.npcTalking = s != null && s.npcTalking;
        c.cursorTaken = cursorTaken();
        c.heroDead = s != null && (s.life == 0 || modLink.heroDead());
        c.paused = pauseWanted || (s != null && s.paused());
        c.onGameControl = true;
        if (s != null && s.gameWidth > 0 && s.gameHeight > 0) {
            float[] g = gameAt(touch, s);
            boolean edge = (s.isOpen(D2ModState.UI_QUESTLOG) && !s.edgeButtonsHidden) || s.statPointsLeft;

            boolean ownStrip = hudLayout == D2Settings.HUD_COMPACT && s.hudLeftOut();
            c.onGameControl = D2WorldTap.onGameControl(g[0], g[1], s.gameWidth, s.gameHeight, D2Hud.STRIP, edge,
                s.isOpen(D2WorldTap.UI_MINIPANEL), beltShown(s), ownStrip);
        }
        return D2WorldTap.toGame(c);
    }


    private final D2WorldTap.Portrait portrait = new D2WorldTap.Portrait();

    private long mercKeyAt;


    private int portraitAction;





    private int mercPortrait(Touch touch) {
        D2ModState s = modLink.state();
        D2WorldTap.Portrait c = portrait;
        c.stateFresh = s != null && s.gameWidth > 0 && s.gameHeight > 0;
        if (!c.stateFresh) return D2WorldTap.PORTRAIT_NONE;
        c.panelOpen = s.isPanelOpen();
        c.inventoryOnly = s.panelFlags() == 1L << D2ModState.UI_INVENTORY;
        c.singlePlayer = modLink.singlePlayer();
        c.mercLife = modLink.mercLife();
        D2Points points = modLink.points(2000);
        c.cursorKnown = points != null;
        c.cursorTaken = points == null || points.cursorTaken;
        c.npcTalking = s.npcTalking;
        c.heroDead = s.life == 0 || modLink.heroDead();
        c.ownFinger = touch.deviceId == D2Idle.DEVICE_ID;
        c.playtester = D2SpieltestPort.playing();
        float[] g = gameAt(touch, s);
        return D2WorldTap.mercPortrait(g[0], g[1], c);
    }


    private static final int INFO_NONE = 0, INFO_TAKEN = 1, INFO_ARROW = 2;






    private int infoTap(Touch touch) {
        D2ModState s = modLink.state();
        if (s == null || infoStrip == null || s.gameWidth <= 0 || s.gameHeight <= 0) return INFO_NONE;
        boolean drawn = s.staminaKnown && s.stamina >= 0;
        D2Points points = modLink.points(2000);
        float[] g = gameAt(touch, s);
        boolean cursorTaken = points == null || points.cursorTaken, own = touch.deviceId == D2Idle.DEVICE_ID, tester = D2SpieltestPort.playing();
        boolean boxBlocked = s.isPanelOpen() || beltShown(s);

        if (infoStrip.boxOn() && !s.isOpen(D2WorldTap.UI_MINIPANEL) && D2InfoGrid.takesTap(drawn, points != null, cursorTaken, own, tester,
            D2InfoGrid.inside(D2InfoGrid.arrow(s.gameWidth, s.gameHeight), g[0], g[1]))) return INFO_ARROW;
        boolean on = (infoStrip.gridOn() && D2InfoGrid.onBlock(g[0], g[1], s.gameWidth, s.gameHeight))
            || D2InfoGrid.inside(infoStrip.boxArea(hud, s.gameWidth, s.gameHeight, boxBlocked), g[0], g[1]);
        return D2InfoGrid.takesTap(drawn, points != null, cursorTaken, own, tester, on) ? INFO_TAKEN : INFO_NONE;
    }





    private boolean inJoystickZone(float x, float y) {
        if (x >= getWidth() * 0.4f) return false;
        D2ModState s = mod;
        if (s == null || s.gameHeight <= 0) return y > getHeight() * 0.1f && y < getHeight() * 0.92f;
        float bottom = gameToView(0, s.gameHeight - D2Hud.STRIP)[1];
        if (y >= bottom) return false;

        if ((s.isOpen(D2ModState.UI_QUESTLOG) && !s.edgeButtonsHidden) || s.statPointsLeft) {


            float[] a = gameToView(20, s.gameHeight - 170), b = gameToView(105, s.gameHeight - 85);
            if (x >= a[0] && x <= b[0] && y >= a[1] && y <= b[1]) return false;
        }
        return true;
    }

    private void onMove(int id, float x, float y, long time) {
        Touch touch = touches.get(id);
        if (touch == null) return;
        touch.x = x;
        touch.y = y;
        if (!touch.moved && Math.hypot(x - touch.downX, y - touch.downY) > 12 * dp) touch.moved = true;

        if (touch.role == ROLE_EFFECT && touch.moved && controlsVisible && inJoystickZone(touch.downX, touch.downY)) {

            touch.role = ROLE_JOYSTICK;
            joystickActive = true;
            hasDirection = false;
            baseX = knobX = touch.downX;
            baseY = knobY = touch.downY;
            joystickFollow.start(touch.downX, touch.downY, time);
        }
        if (touch.role == ROLE_DRAG) {
            touch.button.cx = Math.max(touch.button.r, Math.min(getWidth() - touch.button.r, x));
            touch.button.cy = Math.max(touch.button.r, Math.min(getHeight() - touch.button.r, y));
            touch.button.shiftX = touch.button.shiftY = 0;
        }
        else if (touch.role == ROLE_JOYSTICK) {

            joystickFollow.move(x, y, time, joystickRadius, dp);
            baseX = joystickFollow.baseX;
            baseY = joystickFollow.baseY;
            float dx = x - baseX, dy = y - baseY;
            float len = (float) Math.hypot(dx, dy);
            knobX = x;
            knobY = y;
            if (len > 10 * dp) {
                dirX = dx / len;
                dirY = dy / len;
                hasDirection = true;
                onDirectionChanged();
            }
        }
        else if (touch.role == ROLE_BUTTON && touch.button.kind == KIND_INTERACT && ringTouch == touch) {
            moveRing(touch);
        }
        else if (touch.role == ROLE_BUTTON && touch.button.kind == KIND_INTERACT && touch.moved
            && !D2InteractHint.swipeSwitches(npcMenuOpen(), mod != null && mod.npcTalking) && ringTouch == null) {

            if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
            if (openRing(touch)) {
                touch.longPressed = true;
                moveRing(touch);
            }
        }
        else if (touch.role == ROLE_BUTTON && touch.button.kind == KIND_INTERACT && touch.moved && ringTouch == null) {
            float dx = x - touch.stepX, dy = y - touch.stepY, step = 28 * dp;
            if (Math.abs(dx) >= step || Math.abs(dy) >= step) {

                int direction = Math.abs(dx) >= Math.abs(dy) ? (dx > 0 ? 1 : -1) : (dy > 0 ? 1 : -1);
                cycleInteract(direction);
                touch.cycled = true;
                touch.stepX = x;
                touch.stepY = y;
            }
        }
        else if (touch.role == ROLE_BUTTON && touch.button.kind == KIND_SKILL && touch.moved) {
            float dx = x - touch.downX, dy = y - touch.downY;
            float len = (float) Math.hypot(dx, dy);
            if (len > 20 * dp && action == ACTION_SKILL && skillOwner == touch.button && teleportHeld) {

                teleportDragX = dx / len;
                teleportDragY = dy / len;
                teleportDragged = true;
            }
            else if (len > 20 * dp && action == ACTION_SKILL && skillOwner == touch.button && !directAttack && !teleportHeld) {
                skillAimedByDrag = true;
                skillTargetId = 0;

                if (D2SkillAim.shiftNeeded(skillLeft, false)) setShift(true);
                follow(aimPoint(dx / len, dy / len, 0.26f));
            }
        }
        else if (touch.role == ROLE_MATERIALS) {
            materialsWindow.move(x, y);
        }
        else if (touch.role == ROLE_DIRECT) {
            moveDirect(touch);
        }
    }

    private void onUp(int id, boolean cancelled) {
        Touch touch = touches.get(id);
        if (touch == null) return;
        touches.remove(id);
        if (touch.longPress != null) handler.removeCallbacks(touch.longPress);


        if (touch.role != ROLE_NONE && Math.abs(touch.downTime - lastTriggerDownTime) > TRIGGER_TOUCH_WINDOW_MS) {
            fingerDevices.add(touch.deviceId);
        }

        if (touch.role == ROLE_DRAG) {
            touch.button.pressed = false;

            if (!touch.moved && !cancelled && (touch.button.kind == KIND_SKILL || touch.button.kind == KIND_ATTACK)) startAssign(touch.button);
        }
        else if (touch.role == ROLE_ASSIGN) {

            if (cancelled) { }
            else if (assignButton != null && assignHint.contains(touch.x, touch.y)) switchAssignList();
            else assignAt(touch.x, touch.y);
        }
        else if (touch.role == ROLE_RESUME) {
            if (!cancelled && resumeHint.exit().tapped(touch.x, touch.y, touch.downTime)) setPaused(false);
        }
        else if (touch.role == ROLE_TREE_PICK) {
            touch.button.pressed = false;
            if (!cancelled && touch.button.contains(touch.x, touch.y, 4 * dp) && treePicking()) bindTreePick(touch.button);
            else endTreePick(cancelled ? "cancelled" : "finger left the button");
        }
        else if (touch.role == ROLE_RECIPES) {
            recipes.up(touch.downX, touch.downY, touch.x, touch.y, cancelled);
        }
        else if (touch.role == ROLE_MATERIALS) {
            materialsWindow.up(touch.x, touch.y, cancelled);
        }
        else if (touch.role == ROLE_INPUT_BOX) {
            inputBox.up(touch.x, touch.y, cancelled);
        }
        else if (touch.role == ROLE_RESUME_HINT) {
            if (!cancelled) resumeHint.cancel();
        }
        else if (touch.role == ROLE_HINT) {
            if (!cancelled) hints.dismiss(SystemClock.uptimeMillis());
            invalidate();
        }
        else if (touch.role == ROLE_INFO) {

            if (touch.infoTap == INFO_ARROW && !cancelled && !touch.moved && infoStrip != null) {
                infoStrip.toggleBox();
                D2Trace.log("mini panel button: the box " + (infoStrip.boxOpen() ? "unfolded" : "folded"));
                invalidate();
            }
        }
        else if (touch.role == ROLE_EFFECT) {

            if (!cancelled && !touch.moved && effectsView.tap(touch.x, touch.y, SystemClock.uptimeMillis())) invalidate();
        }
        else if (touch.role == ROLE_MERC) {


            long now = SystemClock.uptimeMillis();
            if (!cancelled && !touch.moved && D2WorldTap.mercKeyAllowed(now, mercKeyAt)) {
                D2Trace.log("tap on the mercenary's portrait: his items");
                mercKeyAt = now;
                pressMercKeys();
            }
        }
        else if (touch.role == ROLE_JOYSTICK) {

            if (joystickOwner == D2Gamepad.OWNER_FINGER) joystickReleased();
        }
        else if (touch.role == ROLE_BUTTON) {
            touch.button.pressed = false;
            boolean inside = touch.button.contains(touch.x, touch.y, 12 * dp);
            touch.cancelled = cancelled;
            onButtonUp(touch.button, touch, !cancelled && inside);
        }
        else if (touch.role == ROLE_DIRECT) {
            endDirect(touch, cancelled);
            if (!cancelled && !touch.moved && mod == null) menuTap(touch.downX, touch.downY);
        }
    }



    private java.util.function.Consumer<Boolean> keyboard;
    private boolean keyboardOpened;
    private float menuTapX, menuTapY;
    private long menuTapAt;


    private int rightShift;






    public void setRightShift(int px) {
        if (px == rightShift) return;
        float dx = D2NavInset.moveX(rightShift, px);
        rightShift = px;
        D2Trace.log("top right buttons moved " + px + " px left (navigation bar with the keyboard)");
        if (getWidth() > 0 && !buttons.isEmpty()) {
            for (Button b : buttons) {
                b.cx -= b.shiftX;
                b.cy -= b.shiftY;
                b.shiftX = b.shiftY = 0;

                if ((b.group == GROUP_MENU || b.group == GROUP_HIDDEN) && b != respecButton && b != sortButton) b.cx += dx;
            }
            avoidCutout(getWidth(), getHeight());
        }
        invalidate();
    }


    public boolean keyboardShown() {
        return keyboardOpened;
    }


    public void setKeyboard(java.util.function.Consumer<Boolean> keyboard) {
        this.keyboard = keyboard;
    }


    private void menuTap(float x, float y) {
        int dw = xServer.screenInfo.width, dh = xServer.screenInfo.height;
        float k = Math.min(dw / 800f, dh / 600f);
        menuTapX = ((x - offsetX) / aspect - (dw - 800 * k) / 2) / k;
        menuTapY = ((y - offsetY) / aspect - (dh - 600 * k) / 2) / k;
        menuTapAt = SystemClock.uptimeMillis();
        checkMenuKeyboard();
    }





    private void checkMenuKeyboard() {
        if (keyboard == null) return;
        int[] f = mod == null ? modLink.menuField() : null;
        if (f == null) {
            if (keyboardOpened) keyboard.accept(false);
            keyboardOpened = false;
            return;
        }
        float m = 8;
        boolean hit = menuTapX >= f[0] - m && menuTapX <= f[0] + f[2] + m && menuTapY >= f[1] - m && menuTapY <= f[1] + f[3] + m;
        if (hit && SystemClock.uptimeMillis() - menuTapAt < 1000) {
            menuTapAt = 0;
            keyboardOpened = true;
            keyboard.accept(true);
        }


        if (keyboardOpened && !modLink.menuSecret()) showNote(modLink.menuText() + "_");
    }





    private void startAssign(Button button) {
        D2ModState s = mod;
        if (s == null || s.gameWidth <= 0 || s.gameHeight <= 0) return;
        saveLayout();
        setEditMode(false);
        releaseCurrent();
        assignButton = button;
        assignSince = SystemClock.uptimeMillis();
        assignRight = button.kind != KIND_ATTACK;
        clickSkillButton(s, button.kind == KIND_ATTACK);
        handler.postDelayed(() -> {
            if (assignButton == button && SystemClock.uptimeMillis() - assignSince >= 15000) {
                assignButton = null;
                invalidate();
            }
        }, 15000);
        invalidate();
    }


    private void clickSkillButton(D2ModState s, boolean left) {

        float gx = left ? 141 : s.gameWidth - 141, gy = s.gameHeight - 24;
        int[] p = gameToDesktop(gx - positionShiftX, gy - positionShiftY);
        enqueue(moveTo(p[0], p[1]), settle(60));
        setLmb(true);
        enqueue(waitMs(60));
        setLmb(false);
    }





    private void switchAssignList() {
        D2ModState s = mod;
        if (s == null || s.gameWidth <= 0) return;
        if (assignButton != null && assignButton.kind == KIND_ATTACK && !assignRight && modLink.characterName() == null) {
            showNote(text(R.string.note_right_needs_mod));
            return;
        }
        clickSkillButton(s, !assignRight);
        assignRight = !assignRight;
        clickSkillButton(s, !assignRight);
        final long since = assignSince = SystemClock.uptimeMillis();
        final Button button = assignButton;
        handler.postDelayed(() -> {
            if (assignButton == button && assignSince == since) {
                assignButton = null;
                invalidate();
            }
        }, 15000);
        D2Trace.log("assign: " + (assignButton != null ? assignButton.label : "?") + " from the " + (assignRight ? "right" : "left") + " list");
        invalidate();
    }






    private void updateLootScope(D2ModState s) {
        String scope = D2LabelFilter.scope(s.characterClass, modLink.currentCharacterName());
        if (scope == null || scope.equals(lootScope)) return;
        lootScope = scope;
        labelFilter.load(getContext(), scope);
        D2Trace.log("loot filter of this character: preset " + labelFilter.lootPreset + ", mask " + labelFilter.lootMask());
    }


    public String lootScope() {
        return lootScope;
    }


    private String attackRightKey() {

        String name = modLink.currentCharacterName();
        D2ModState s = mod;
        return name == null || s == null || s.characterClass < 0 ? null : "attackRight." + s.characterClass + "." + name;
    }


    private int attackRightSkill() {
        String key = attackRightKey();
        return key == null ? -1 : prefs.getInt(key, -1);
    }

    private void setAttackRightSkill(int skill) {
        String key = attackRightKey();
        android.util.Log.i("D2Android", "attack right: " + key + " = " + skill);
        D2Trace.log("attack right: " + key + " = " + skill);
        if (key == null) return;
        if (skill < 0) prefs.edit().remove(key).apply();
        else prefs.edit().putInt(key, skill).apply();
    }


    static boolean attackSkillAllowed(int characterClass, int skill) {
        if (skill == 0) return true;
        int[] first = {6, 36, 66, 96, 126, 221, 251};
        return characterClass >= 0 && characterClass < 7 && skill >= first[characterClass] && skill < first[characterClass] + 30
            && !AURA_SKILLS.contains(skill);
    }


    private void takeRightSkill(int before, long since) {
        D2ModState s = mod;
        int now = s != null ? s.rightSkill : -1;
        if (now == before && SystemClock.uptimeMillis() - since < 600) {
            handler.postDelayed(() -> takeRightSkill(before, since), 50);
            return;
        }
        if (now < 0 || s == null) return;
        if (!attackSkillAllowed(s.characterClass, now)) {
            D2Trace.log("assign: right skill " + now + " refused for Angriff");
            showNote(text(R.string.note_skill_not_for_attack));
            return;
        }

        if (!D2Points.ownSkill(modLink.points(3000), now)) {
            D2Trace.log("assign: right skill " + now + " without own points (charges?), refused for Angriff");
            showNote(text(R.string.note_attack_right_no_points));
            return;
        }
        setAttackRightSkill(now);
        D2Trace.log("assign: Angriff = right skill " + now);
        invalidate();
    }

    private void assignAt(float x, float y) {
        assignAtScreen(toScreen(x, y));
    }


    private void assignAtScreen(int[] p) {
        Button button = assignButton;
        assignButton = null;
        if (button == null) return;
        enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS));
        if (button.kind == KIND_SKILL) {
            pressKey(button.vk);
            enqueue(waitMs(60));
        }
        setLmb(true);
        enqueue(waitMs(60));
        setLmb(false);
        if (button.kind == KIND_SKILL && !assignRight && mod != null) {

            leftRestore.pressed(-1, mod.leftSkill);
            queueLeftRestore(false);
        }
        if (button.kind == KIND_ATTACK) {
            android.util.Log.i("D2Android", "assign: Angriff tap in the " + (assignRight ? "right" : "left") + " list");
            if (assignRight) {
                D2ModState s = mod;
                final int before = s != null ? s.rightSkill : -1;
                final long since = SystemClock.uptimeMillis();
                handler.postDelayed(() -> takeRightSkill(before, since), 150);
            }
            else setAttackRightSkill(-1);
            assignRight = false;
        }
        invalidate();
    }

    private Button findButton(float x, float y) {
        Button found = null;
        for (Button button : buttons) {
            if (!isVisible(button) || !button.contains(x, y, 4 * dp)) continue;


            if (found != null && found.group == GROUP_MENU && button.group == GROUP_MENU
                && menuFrameDistance(found, x) < menuFrameDistance(button, x)) continue;
            found = button;
        }
        return found;
    }



    private void onDirectionChanged() {
        if ((action == ACTION_ATTACK || action == ACTION_SKILL) && directAttack) {
            retargetByJoystick(action == ACTION_ATTACK);

            if ((action == ACTION_ATTACK ? attackTargetId : skillTargetId) < 0) attackIdle();
        }
        if (action == ACTION_WALK) {
            if (walkDirect) sendWalk(D2ModLink.PHASE_HOLD);
            else follow(walkPoint());
        }
        else if (action == ACTION_ATTACK && attackTargetId == 0) {

            if (walksWhenNobody()) attackWalkFromPlace();
            else follow(aimPoint(dirX, dirY, 0.12f));
        }


        else if (action == ACTION_SKILL && !skillAimedByDrag && skillTargetId == 0 && !teleportHeld
            && skillPressAt >= 0) follow(aimPoint(dirX, dirY, 0.26f));
        else if (action == ACTION_PICKUP) {

            boolean away = pickupDirX * dirX + pickupDirY * dirY < Math.cos(Math.toRadians(50));
            if (away && SystemClock.uptimeMillis() - pickupStart > 250) finishPickup();
        }
        else if (action == ACTION_SELECT) {

            if (!attackRightHeld) {
                releaseCurrent();
                startWalk();
            }
        }
        else if (action == ACTION_NONE) startWalk();
    }

    private void startWalk() {
        if (!joystickActive || !hasDirection) return;
        action = ACTION_WALK;
        D2ModState s = mod;
        if (directWalk && s != null && s.canWalk()) {

            walkDirect = true;
            sendWalk(D2ModLink.PHASE_DOWN);
            handler.removeCallbacks(walkKeepAlive);
            handler.postDelayed(walkKeepAlive, 200);
            return;
        }
        int[] p = walkPoint();
        enqueue(moveTo(p[0], p[1]), waitMs(15));
        setLmb(true);
    }







    static final float WALK_DISTANCE = 0.30f;


    private void sendWalk(int phase) {
        D2ModState s = mod;
        float distance = (s != null ? s.gameHeight : 600) * WALK_DISTANCE;
        modLink.walk(phase, Math.round(dirX * distance), Math.round(dirY * distance));
    }


    private void walkKeepAlive() {
        if (!walkDirect || (action != ACTION_WALK && !attackWalking)) return;
        sendWalk(D2ModLink.PHASE_HOLD);
        handler.postDelayed(walkKeepAlive, 200);
    }

    private void stopDirectWalk() {
        if (!walkDirect) return;
        walkDirect = false;
        handler.removeCallbacks(walkKeepAlive);
        modLink.walk(D2ModLink.PHASE_UP, 0, 0);
    }

    private void resumeAfterAction() {
        if (attackWalking) stopAttackWalk();
        action = ACTION_NONE;
        if (joystickActive) startWalk();
    }






    private void attackIdle() {
        if (!walksWhenNobody()) {
            if (attackWalking) stopAttackWalk();

            if (action == ACTION_ATTACK) attackInPlace();
            return;
        }
        if (!attackWalking) {
            D2Trace.log("attack: nobody in reach, the joystick walks");
            attackWalking = true;
            walkDirect = true;
            sendWalk(D2ModLink.PHASE_DOWN);
            handler.removeCallbacks(walkKeepAlive);
            handler.postDelayed(walkKeepAlive, 200);
        }
        else sendWalk(D2ModLink.PHASE_HOLD);
    }







    private boolean walksWhenNobody() {
        D2ModState s = mod;
        return D2HeldAttack.walksWhenNobody(aimNow().on, joystickActive, hasDirection, directWalk, s != null && s.canWalk(),
            s != null && s.canActOnUnits());
    }


    private void attackWalkNobody() {
        directAttack = true;
        attackTargetId = -1;
        targetMissing = 0;
        scheduleKeepAlive();
        attackIdle();
    }






    private void attackInPlace() {
        if (action != ACTION_ATTACK || attackTargetId > 0) return;
        if (directAttack) {
            directAttack = false;
            handler.removeCallbacks(keepAlive);
        }
        attackTargetId = 0;
        targetMissing = 0;
        D2Trace.log("attack: nobody in reach, no joystick: in place");
        int[] p = aimPoint(dirX, dirY, 0.12f);
        setShift(true);
        enqueue(moveTo(p[0], p[1]), settle(40));
        setLmb(true);
    }


    private void attackWalkFromPlace() {
        setLmb(false);
        setShift(false);
        D2Trace.log("attack: in place, the joystick pushed: walks");
        attackWalkNobody();
    }

    private void stopAttackWalk() {
        if (attackWalking) D2Trace.log("attack: walking ends");
        attackWalking = false;
        stopDirectWalk();
    }


    private void releaseCurrent() {
        if (action == ACTION_SKILL && skillLeft) queueLeftRestore(skillFirstPending);
        dropAttackRight();
        releaseDirect();
        attackWalking = false;
        stopDirectWalk();
        setLmb(false);
        setRmb(false);
        setShift(false);
        action = ACTION_NONE;
        skillOwner = null;
        attackTargetId = skillTargetId = 0;
        skillOnCorpses = false;
        skillFirstPending = false;
        skillEarlyTarget = -1;
        skillWalkToken++;
        attackTapToken++;
        pickupId = 0;
        buttonPending = false;
    }


    private void onButtonDown(Button button, final Touch touch) {
        D2Trace.log("button down " + button.id + " (action " + action + ")");
        cancelPendingLongPresses();
        switch (button.kind) {
            case KIND_ATTACK:
                pressAttack(button);
                break;
            case KIND_SKILL:

                if (isFreeSlot(button)) {
                    showNote(text(R.string.note_slot_free, button.label, text(R.string.btn_keys)));
                    break;
                }
                startSkill(button.vk, button);
                break;
            case KIND_POTION:
                drinkPotion(button);
                break;
            case KIND_INTERACT:
                if (followRingByFinger(touch)) break;

                touch.stepX = touch.downX;
                touch.stepY = touch.downY;
                touch.longPress = () -> {
                    if (touches.indexOfValue(touch) < 0 || touch.role != ROLE_BUTTON || ringTouch != null) return;
                    if (openRing(touch)) touch.longPressed = true;
                };
                handler.postDelayed(touch.longPress, RING_HOLD_MS);
                break;
            case KIND_PANEL:

                if (button.vk != VK_I || !sortCapable()) break;
                touch.longPress = () -> {
                    if (touches.indexOfValue(touch) < 0 || touch.role != ROLE_BUTTON || touches.size() > 1) return;
                    touch.longPressed = true;
                    startSort();
                };
                handler.postDelayed(touch.longPress, SORT_HOLD_MS);
                break;
        }
    }









    private void queueLeftRestore(boolean waiting) {
        final int token = leftRestore.token();
        enqueue((now) -> {
            leftRestore.ended(SystemClock.uptimeMillis(), waiting, token);
            return true;
        });
    }






    private void restoreLeftNow() {
        if (steps.isEmpty()) {
            sendLeftRestoreNow();
            return;
        }
        enqueue((now) -> {
            sendLeftRestoreNow();
            return true;
        });
    }

    private void sendLeftRestoreNow() {
        int skill = leftRestore.attackNow(SystemClock.uptimeMillis());
        if (skill < 0) return;
        D2Trace.log("left skill " + skill + " back before the attack (left-bound F-button)");
        modLink.selectSkill(true, skill);
    }


    private void stepLeftRestore(D2ModState s, long now) {
        if (pauseWanted) return;
        int saved = leftRestore.saved();
        int skill = leftRestore.step(now, s.leftSkill, action == ACTION_SKILL && skillLeft);
        if (skill == D2LeftRestore.GAVE_UP) D2Trace.log("left skill " + saved + " not back (not selectable?), left stays " + s.leftSkill);
        if (skill < 0) return;
        D2Trace.log("left skill " + skill + " back (left-bound F-button let go)");
        modLink.selectSkill(true, skill);
    }

    private void pressAttack(Button owner) {
        int attackRight = attackRightSkill();
        if (attackRight >= 0) {

            startAttackRight(owner, attackRight);
            return;
        }
        releaseCurrent();
        int restoring = leftRestore.saved();
        if (restoring >= 0) {


            restoreLeftNow();
            D2ModState now = mod;
            if (!steps.isEmpty() || !keyWait.allTaken(SystemClock.uptimeMillis()) || now == null || now.leftSkill != restoring) {
                waitLeftThenAttack(owner, restoring);
                return;
            }
        }
        attackNow(owner);
    }






    private void waitLeftThenAttack(Button owner, int skill) {
        action = ACTION_SELECT;
        hintFoughtAt = SystemClock.uptimeMillis();
        attackRightOwner = owner;
        attackRightHeld = true;
        final long token = ++attackRightToken, since = SystemClock.uptimeMillis();
        handler.postDelayed(new Runnable() {
            private boolean resent;

            @Override
            public void run() {
                if (attackRightToken != token || attackRightOwner != owner || action != ACTION_SELECT) return;
                D2ModState now = mod;
                long t = SystemClock.uptimeMillis(), waited = t - since;

                boolean settled = steps.isEmpty() && keyWait.allTaken(t);
                boolean back = settled && now != null && now.leftSkill == skill;
                if (settled && !back && !resent && now != null && waited >= 200) {

                    resent = true;
                    D2Trace.log("attack: left skill " + skill + " selected again after a left F-key");
                    modLink.selectSkill(true, skill);
                }
                if (!back && waited < 600) {
                    handler.postDelayed(this, 40);
                    return;
                }
                D2Trace.log("attack: left skill " + skill + (back ? " back after " + waited + " ms" : " not back in 600 ms, attacking anyway"));
                boolean held = attackRightHeld;
                attackRightOwner = null;
                action = ACTION_NONE;
                attackNow(owner);
                if (!held) releaseAttack(owner, false);
            }
        }, 40);
    }


    private void attackNow(Button owner) {
        action = ACTION_ATTACK;
        hintFoughtAt = SystemClock.uptimeMillis();
        attackPressAt = -1;

        D2ModState.Monster chosen = aAttackTarget != null ? findMonster(aAttackTarget.id) : null;
        aAttackTarget = null;
        D2ModState.Monster target = chosen != null ? chosen : pickTarget(pressSkill(true));
        heldAttack.pressed(SystemClock.uptimeMillis());
        if (target != null && mod != null && mod.canActOnUnits()) {

            directAttack = true;
            attackTargetId = target.id;
            targetMissing = 0;
            D2Trace.log("attack: direct on " + target.id + " " + target.name);
            modLink.unit(D2ModLink.BUTTON_LEFT, D2ModLink.PHASE_DOWN, D2ModState.UNIT_MONSTER, target.id);
            attackPressAt = SystemClock.uptimeMillis();
            scheduleKeepAlive();
            return;
        }
        if (target == null && walksWhenNobody()) {

            D2Trace.log("attack: nobody in reach, walks with the joystick");
            attackWalkNobody();
            return;
        }
        D2Trace.log(target != null ? "attack: with the mouse on " + target.id + " (no direct clicks)" : "attack: in place, nobody in reach");
        if (target != null) {

            attackTargetId = target.id;
            targetMissing = 0;
            int[] p = monsterPoint(target);
            buttonPending = true;
            enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), hoverSearch(target.id, target.x, target.y, MONSTER_OFFSETS));
        }
        else {

            attackTargetId = 0;
            int[] p = aimPoint(dirX, dirY, 0.12f);
            setShift(true);
            enqueue(moveTo(p[0], p[1]), settle(40));
        }
        setLmb(true);
        enqueue(buttonSent());
    }


    private void releaseAttack(Button owner, boolean cancelled) {
        if (attackRightOwner == owner) {

            if (cancelled) dropAttackRight();
            else attackRightHeld = false;
            return;
        }
        if (action == ACTION_SKILL && skillOwner == owner) {
            endSkill(owner, cancelled);
            return;
        }
        if (action == ACTION_ATTACK) {

            boolean onMonster = directAttack && attackTargetId > 0 && !cancelled;
            releaseDirect();
            setLmb(false);
            setShift(false);
            attackTargetId = 0;
            if (onMonster && D2SkillWalk.defers(joystickActive, false)) resumeAfterAttackTap();
            else resumeAfterAction();
        }
    }













    private void resumeAfterAttackTap() {
        long delay = D2SkillWalk.attackTapDelay(attackPressAt, SystemClock.uptimeMillis());
        if (delay <= 0) {
            resumeAfterAction();
            return;
        }
        action = ACTION_ATTACK_TAP;
        hintFoughtAt = SystemClock.uptimeMillis();
        final long token = ++attackTapToken;
        D2Trace.log("attack: tapped on a monster, the walk waits " + delay + " ms for the swing");
        handler.postDelayed(() -> {
            if (attackTapToken != token || action != ACTION_ATTACK_TAP) return;
            D2Trace.log("attack: walk resumes after the tap");
            resumeAfterAction();
        }, delay);
    }

    private void onButtonUp(Button button, Touch touch, boolean activate) {
        D2Trace.log("button up " + button.id + (activate ? "" : " (cancelled)") + " (action " + action + ")");
        switch (button.kind) {
            case KIND_ATTACK:
                releaseAttack(button, touch.cancelled);
                return;
            case KIND_SKILL:
                endSkill(button, touch.cancelled);
                return;
            case KIND_INTERACT:
                if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
                if (ringTouch == touch) {

                    Object chosen = !touch.cancelled ? ringTarget(ringChoice.target()) : null;
                    if (ringNpc) {
                        npcRingTake(chosen, "finger");
                        return;
                    }
                    closeRing();
                    if (chosen != null) {
                        performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                        startPickup(chosen, 2500);
                    }
                    return;
                }
                if (activate && !touch.cycled && !touch.longPressed) {

                    if (mod != null && mod.npcTalking) pressKey(VK_ESCAPE);
                    else if (npcMenuOpen()) {
                        npcMenuCount.chose(D2ModState.NPC_TALK);
                        pressKey(VK_RETURN);
                    }
                    else interact();
                }
                return;
        }
        if (!activate || touch.longPressed) return;
        windowButtonClick(button);
        switch (button.kind) {
            case KIND_KEY:
            case KIND_PLAIN_KEY:
                pressKey(button.vk);
                break;
            case KIND_PANEL:
                pressKey(button.vk);
                if (controlsVisible) setControlsVisible(false, true);
                else if (hiddenByPanel) setControlsVisible(true, false);
                break;
            case KIND_ESCAPE:
                pressKey(button.vk);

                if (!controlsVisible && appliedUiKey != UI_OUT_OF_GAME) setControlsVisible(true, false);
                break;
            case KIND_LABELS:
                labelsWanted = !labelsWanted;
                syncLabels();
                break;
            case KIND_CONTROLS:

                if (!controlsVisible) setControlsVisible(true, false);
                else setEditMode(true);
                break;
            case KIND_MENU:
                if (openMenu != null) openMenu.run();
                break;
            case KIND_PAUSE:
                setPaused(true);
                break;
            case KIND_RESPEC:
                respecTap();
                break;
            case KIND_SORT:

                if (isVisible(button)) startSort();
                break;
            case KIND_SHOP_POTIONS: {
                String note = shopButton != null ? shopButton.tap(mod) : null;
                if (note != null) showNote(note);
                break;
            }
            case KIND_MENU_TOGGLE:
                if (talkFold) {
                    talkFold = false;
                    talkFoldDismissed = true;
                    layoutButtons(getWidth(), getHeight());
                    break;
                }
                menuOpen = !menuOpen;

                if (menuOpen && mod != null && mod.npcTalking) talkFoldDismissed = true;
                D2Settings.putBoolean(getContext(), D2Settings.KEY_MENU_OPEN, menuOpen);
                layoutButtons(getWidth(), getHeight());
                break;
            case KIND_PORTAL:
                if (mod != null) modLink.portal();
                break;
            case KIND_RECIPES:
                if (mod != null) recipes.openWithCube();
                break;
            case KIND_EDIT_DONE:
                saveLayout();
                setEditMode(false);
                break;
            case KIND_EDIT_RESET:
                resetLayout();
                break;
            case KIND_EDIT_BIGGER:
                changeScale(1.1f);
                break;
            case KIND_EDIT_SMALLER:
                changeScale(1 / 1.1f);
                break;
            case KIND_EDIT_DEBUG:
                debug = !debug;
                break;
        }
    }


    private boolean treePickTarget(Button button) {
        return button.kind == KIND_SKILL && button.group == GROUP_GAME && button.vk >= VK_F1 && button.vk < VK_F1 + 16
            && buttons.contains(button);
    }





    private boolean treePicking() {
        if (treePickSkill < 0) return false;
        D2ModState s = mod;
        if (s != null && s.isOpen(D2ModState.UI_SKILLTREE) && !pauseWanted && !editMode && assignButton == null
            && SystemClock.uptimeMillis() - treePickSince < TREE_PICK_MS) return true;
        endTreePick("over");
        return false;
    }

    private void endTreePick(String why) {
        if (treePickSkill < 0) return;
        D2Trace.log("tree pick: skill " + treePickSkill + " ended (" + why + ")");
        treePickSkill = -1;
        invalidate();
    }


    private int treeSkillAt(Touch touch) {
        D2ModState s = mod;
        D2Hud graphics = hud;
        if (s == null || graphics == null || !s.isOpen(D2ModState.UI_SKILLTREE) || editMode) return -1;
        float[] g = gameAt(touch, s);
        return pointTaps.treeSkillAt(modLink.points(1500), graphics::skillTreePlace, g[0], g[1]);
    }

    private String treePickName(int skill) {
        D2Hud graphics = hud;
        String name = graphics != null ? graphics.skillName(skill) : null;
        return name != null && !name.isEmpty() ? name : text(R.string.tree_pick_skill);
    }


    private boolean treePickPossible(D2Points points) {
        return points != null && points.canSend(D2Points.KIND_KEY) && modLink.singlePlayer() && !pauseWanted;
    }


    private void startTreePick(int skill) {
        D2Points points = modLink.points(1500);
        if (!D2Points.ownSkill(points, skill)) {
            showNote(text(R.string.tree_pick_not_learnt, treePickName(skill)));
            return;
        }

        D2Hud graphics = hud;
        int passive = graphics != null ? graphics.skillPassive(skill) : -1;
        if (passive == 1) {
            showNote(text(R.string.tree_pick_passive, treePickName(skill)));
            return;
        }
        if (passive < 0 || !treePickPossible(points)) {
            showNote(text(R.string.tree_pick_unavailable));
            return;
        }
        treePickSkill = skill;
        treePickSince = SystemClock.uptimeMillis();
        noteUntil = 0;
        D2Trace.log("tree pick: skill " + skill + " waits for an F button");
        postInvalidateDelayed(TREE_PICK_MS + 100);
        invalidate();
    }





    private void bindTreePick(Button button) {
        final int skill = treePickSkill, key = button.vk - VK_F1;
        treePickSkill = -1;
        final String name = treePickName(skill), label = button.label;
        if (!treePickPossible(modLink.points(1500))) {
            showNote(text(R.string.tree_pick_failed, name, label));
            return;
        }
        D2Trace.log("tree pick: skill " + skill + " onto " + label);
        modLink.points(D2Points.KIND_KEY, skill, key, true);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        invalidate();
        handler.postDelayed(() -> {
            D2ModState s = mod;
            boolean ok = s != null && s.hotkeySkill[key] == skill;
            D2Trace.log("tree pick: skill " + skill + " on " + label + (ok ? "" : " not confirmed"));
            showNote(text(ok ? R.string.tree_pick_done : R.string.tree_pick_failed, name, label));
        }, TREE_PICK_CHECK_MS);
    }


    public void startEditMode() {
        if (!editMode) setEditMode(true);
    }

    private void setEditMode(boolean on) {
        editMode = on;
        if (on) {
            cancelRing();
            releaseCurrent();
            joystickActive = false;
            joystickOwner = D2Gamepad.OWNER_NONE;
            for (int i = 0; i < touches.size(); i++) {
                Touch touch = touches.valueAt(i);
                if (touch.role == ROLE_JOYSTICK || touch.role == ROLE_DIRECT) touch.role = ROLE_NONE;
            }
            controlsVisible = true;
            hiddenByPanel = false;
        }
        invalidate();
    }





    private void cancelRing() {
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (touch.button == null || touch.button.kind != KIND_INTERACT) continue;
            if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
            touch.role = ROLE_NONE;
            touch.button.pressed = false;
        }
        if (ringTouch != null) closeRing();
    }

    private void setControlsVisible(boolean visible, boolean byPanel) {
        controlsVisible = visible;
        hiddenByPanel = !visible && byPanel;
        if (!visible) {
            joystickActive = false;
            joystickOwner = D2Gamepad.OWNER_NONE;
            if (!ringNpc) cancelRing();
            for (int i = 0; i < touches.size(); i++) {
                Touch touch = touches.valueAt(i);
                if (touch.role == ROLE_JOYSTICK) touch.role = ROLE_NONE;
            }
            if (action == ACTION_WALK || action == ACTION_ATTACK || action == ACTION_SKILL || action == ACTION_SELECT
                || action == ACTION_ATTACK_TAP) releaseCurrent();
        }
    }





    private void startSkill(int vk, Object owner) {
        startSkill(vk, owner, -1);
    }





    private void startSkill(int vk, Object owner, int rightSkill) {
        D2Trace.log(rightSkill >= 0 ? "attack right: skill " + rightSkill : "skill F" + (vk - VK_F1 + 1));
        teleportHeld = false;
        releaseCurrent();
        action = ACTION_SKILL;
        hintFoughtAt = SystemClock.uptimeMillis();
        skillOwner = owner;
        skillAimedByDrag = false;
        skillWaitsForTarget = false;
        skillPressAt = -1;
        D2ModState state = mod;
        int index = vk - VK_F1;
        int skill;

        final boolean selecting = false;
        if (rightSkill >= 0) {
            skill = rightSkill;
            skillLeft = false;
        }
        else {
            pressKey(vk);
            skill = state != null && index >= 0 && index < 16 ? state.hotkeySkill[index] : -1;

            skillLeft = state != null && index >= 0 && index < 16 && skill >= 0 && state.hotkeyLeft[index];

            if (!skillLeft) rightSelect.rightKey(SystemClock.uptimeMillis());
            else leftRestore.pressed(skill, state.leftSkill);
        }
        if (skillLeft) D2Trace.log("skill F" + (index + 1) + " is bound left: left button");
        if (CORPSE_SKILLS.contains(skill) && state != null && state.canActOnUnits() && state.portals >= 0) {

            skillOnCorpses = true;
            directAttack = true;
            D2ModState.Thing corpse = pickCorpse();
            skillTargetId = corpse != null ? corpse.id : -1;
            corpseClickAt = SystemClock.uptimeMillis();
            final long token = ++skillToken;
            skillFirstPending = true;
            skillEarlyTarget = -1;
            afterSelect(token, rightSkill, selecting, skill, () -> firstSkillCast(token, D2ModLink.PHASE_CLICK, -1));
            return;
        }

        teleportHeld = false;
        if (skill == D2GroundSkills.TELEPORT && !skillLeft) {

            teleportHeld = true;
            teleportPressed = null;
            teleportBlocked = null;
            teleportAim = null;
            teleportPressedAt = 0;
            teleportDragged = false;
            teleportAskedAt = 0;
            teleportSince = SystemClock.uptimeMillis();
            skillTargetId = 0;
            D2Trace.log("skill " + skill + ": teleport with the DLL's target");
            teleportTick(SystemClock.uptimeMillis());
            return;
        }
        skillNow = skill;
        D2ModState.Monster target = D2GroundSkills.aimsAtGround(skill) ? null : pickTarget(skill);
        if (target == null && D2GroundSkills.aimsAtGround(skill)) D2Trace.log("skill " + skill + ": on the ground");
        skillTargetId = target != null ? target.id : 0;
        targetMissing = 0;

        D2Hud graphics = hud;
        skillWaitsForTarget = target == null && graphics != null && graphics.skillAims(skill);
        heldSkill.pressed(SystemClock.uptimeMillis());
        if (target != null && mod != null && mod.canActOnUnits()) {

            directAttack = true;
            final long token = ++skillToken, id = target.id;
            skillFirstPending = true;
            skillEarlyTarget = -1;
            afterSelect(token, rightSkill, selecting, skill, () -> firstSkillCast(token, D2ModLink.PHASE_DOWN, id));
            return;
        }
        buttonPending = target != null;



        if (D2SkillAim.shiftNeeded(skillLeft, target != null)) setShift(true);
        if (target != null) {
            int[] p = monsterPoint(target);
            enqueue(waitMs(30), moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), hoverSearch(target.id, target.x, target.y, MONSTER_OFFSETS));
        }
        else enqueue(waitMs(30), aimGround());
        setSkillMouse(true);

        final long[] downAt = {-1};
        leftDownAt = downAt;
        enqueue((now) -> {
            downAt[0] = now;
            return true;
        });
        enqueue(skillPressed(), buttonSent());
    }


    private Button attackRightOwner;
    private boolean attackRightHeld;
    private long attackRightToken;


    private void dropAttackRight() {
        if (attackRightOwner != null && action == ACTION_SELECT) D2Trace.log("attack right: waiting cast dropped");
        attackRightOwner = null;
        attackRightToken++;
        if (action == ACTION_SELECT) action = ACTION_NONE;
    }


    private final D2RightSelect rightSelect = new D2RightSelect();








    private void startAttackRight(Button button, int skill) {
        releaseCurrent();


        if (!D2Points.ownSkill(modLink.points(3000), skill)) {
            D2Trace.log("attack right: skill " + skill + " without own points (charges?), nothing cast");
            showNote(text(R.string.note_attack_right_no_points));
            return;
        }
        D2ModState s = mod;

        if (s != null && rightSelect.immediate(SystemClock.uptimeMillis(), s.rightSkill == skill)) {
            startSkill(-1, button, skill);
            return;
        }
        modLink.selectSkill(false, skill);
        action = ACTION_SELECT;
        hintFoughtAt = SystemClock.uptimeMillis();
        attackRightOwner = button;
        attackRightHeld = true;
        final long token = ++attackRightToken, since = SystemClock.uptimeMillis();
        Runnable poll = new Runnable() {
            private boolean resent;

            @Override
            public void run() {
                if (attackRightToken != token || attackRightOwner != button || action != ACTION_SELECT) return;
                D2ModState now = mod;
                long t = SystemClock.uptimeMillis();
                boolean shows = now != null && now.rightSkill == skill;
                if (rightSelect.reselect(t, since, shows, resent)) {

                    resent = true;
                    D2Trace.log("attack right: skill " + skill + " selected again after a right F-key");
                    modLink.selectSkill(false, skill);
                    handler.postDelayed(this, 40);
                    return;
                }
                if (rightSelect.taken(t, since, shows)) {
                    boolean held = attackRightHeld;
                    D2Trace.log("attack right: skill " + skill + " taken after " + (SystemClock.uptimeMillis() - since) + " ms");
                    attackRightOwner = null;
                    action = ACTION_NONE;
                    startSkill(-1, button, skill);
                    if (!held) endSkill(button, false);
                }
                else if (SystemClock.uptimeMillis() - since >= 600) {
                    D2Trace.log("attack right: skill " + skill + " not taken in 600 ms, nothing cast");
                    attackRightOwner = null;
                    showNote(text(R.string.note_attack_right_not_taken));
                    resumeAfterAction();
                }
                else handler.postDelayed(this, 40);
            }
        };
        handler.postDelayed(poll, 40);
    }






    private void afterSelect(long token, int rightSkill, boolean selecting, int keySkill, Runnable cast) {
        if (rightSkill < 0) {
            waitForKey(token, keySkill, cast);
            return;
        }
        if (!selecting) {
            handler.post(cast);
            return;
        }
        final long since = SystemClock.uptimeMillis();
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (skillToken != token || !skillFirstPending) return;
                D2ModState s = mod;
                if (s != null && s.rightSkill == rightSkill) {
                    D2Trace.log("attack right: skill " + rightSkill + " taken after " + (SystemClock.uptimeMillis() - since) + " ms");
                    cast.run();
                }
                else if (SystemClock.uptimeMillis() - since >= 600) {
                    D2Trace.log("attack right: skill " + rightSkill + " not taken in 600 ms, nothing cast");
                    skillFirstPending = false;
                    skillEarlyTarget = -1;
                    showNote(text(R.string.note_attack_right_not_taken));
                }
                else handler.postDelayed(this, 40);
            }
        };
        handler.postDelayed(poll, 40);
    }


    private final D2KeyWait keyWait = new D2KeyWait();
    private long lastFKeyTicket, lastFKeyAt;






    private void waitForKey(long token, int keySkill, Runnable cast) {
        final long ticket = lastFKeyTicket, since = lastFKeyAt;
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (skillToken != token || !skillFirstPending) return;
                D2ModState s = mod;
                long now = SystemClock.uptimeMillis();
                if (s != null) keyWait.state(s.leftSkill, s.rightSkill, now);
                int current = s == null ? -1 : (skillLeft ? s.leftSkill : s.rightSkill);
                int how = keyWait.castNow(ticket, keySkill, current, since, now);
                if (how != D2KeyWait.WAIT) {
                    if (keySkill >= 0) D2Trace.log("skill " + keySkill + ": first cast after " + (now - since) + " ms" + (how == D2KeyWait.SHOWN ? "" : current == keySkill ? " (order not shown)" : " (not shown)"));
                    cast.run();
                }
                else handler.postDelayed(this, D2KeyWait.POLL_MS);
            }
        };
        handler.post(poll);
    }





    private void skillDownSent() {
        if (action != ACTION_SKILL) return;
        skillFirstPending = false;
        skillEarlyTarget = -1;
        markSkillPress(true);
        scheduleKeepAlive();
    }


    private int skillButton() {
        return skillLeft ? D2ModLink.BUTTON_LEFT : D2ModLink.BUTTON_RIGHT;
    }

    private void setSkillMouse(boolean down) {
        if (skillLeft) setLmb(down);
        else setRmb(down);
    }





    private void firstSkillCast(long token, int phase, long fallbackId) {
        if (skillToken != token || !skillFirstPending) return;
        skillFirstPending = false;
        int button = skillButton();
        long early = skillEarlyTarget;
        skillEarlyTarget = -1;
        if (early > 0) {


            if (action != ACTION_SKILL || skillOwner != null) return;
            D2Trace.log("skill: let go before the first cast, one click on " + early);
            modLink.unit(button, D2ModLink.PHASE_CLICK, D2ModState.UNIT_MONSTER, early);
            markSkillPress(true);
            resumeAfterSkill();
            return;
        }
        if (action != ACTION_SKILL || !directAttack) return;
        long id = skillTargetId > 0 ? skillTargetId : fallbackId;
        if (id <= 0) return;
        if (phase == D2ModLink.PHASE_CLICK) corpseClickAt = SystemClock.uptimeMillis();
        modLink.unit(button, phase, D2ModState.UNIT_MONSTER, id);
        markSkillPress(true);
        if (phase == D2ModLink.PHASE_DOWN) scheduleKeepAlive();
    }






    private void dropEarlySkillClick() {
        if (skillEarlyTarget <= 0) return;
        D2Trace.log("skill: early click on " + skillEarlyTarget + " dropped by a new touch");
        skillFirstPending = false;
        skillEarlyTarget = -1;
        if (action == ACTION_SKILL && skillOwner == null) resumeAfterAction();
    }


    private void endSkill(Object owner, boolean cancelled) {
        if (action != ACTION_SKILL || skillOwner != owner) return;
        boolean nothingSent = false;
        if (skillFirstPending && directAttack) {

            long target = skillTargetId;
            directAttack = false;
            handler.removeCallbacks(keepAlive);
            if (cancelled || target <= 0) {
                skillFirstPending = false;
                nothingSent = true;
            }
            else {


                skillEarlyTarget = target;
                skillOwner = null;
                skillTargetId = 0;
                skillOnCorpses = false;
                setSkillMouse(false);
                if (skillLeft) {
                    setShift(false);
                    queueLeftRestore(true);
                }
                return;
            }
        }
        releaseDirect();
        skillOwner = null;
        skillTargetId = 0;
        skillOnCorpses = false;
        skillWaitsForTarget = false;


        boolean held = skillLeft && shiftDown && lmbDown;
        if (held) {
            heldReleasesPending++;
            enqueue(leftCastHeld(leftDownAt));
        }
        setSkillMouse(false);
        setRmb(false);
        if (skillLeft) {
            setShift(false);
            if (held) enqueue((now) -> {
                if (heldReleasesPending > 0) heldReleasesPending--;
                return true;
            });
            queueLeftRestore(false);
        }
        if (nothingSent) resumeAfterAction();
        else resumeAfterSkill();
    }











    private void resumeAfterSkill() {
        if (!D2SkillWalk.defers(joystickActive, teleportHeld)) {
            resumeAfterAction();
            return;
        }
        final long token = ++skillWalkToken, since = SystemClock.uptimeMillis();
        enqueue((now) -> {
            if (skillWalkToken != token) return true;
            long delay = D2SkillWalk.resumeDelay(skillPressAt, now, skillPressDirect);
            Runnable resume = () -> {
                if (skillWalkToken != token || action != ACTION_SKILL || skillOwner != null) return;
                D2Trace.log("skill: walk resumes after the cast, " + (SystemClock.uptimeMillis() - since) + " ms after the let-go");
                resumeAfterAction();
            };
            if (delay > 0) handler.postDelayed(resume, delay);
            else resume.run();
            return true;
        });
    }


    private void markSkillPress(boolean direct) {
        skillPressAt = SystemClock.uptimeMillis();
        skillPressDirect = direct;
    }





    private int heldReleasesPending;

    private long[] leftDownAt;


    private Step leftCastHeld(long[] downAt) {
        return (now) -> D2SkillAim.leftReleaseMayGo(downAt != null ? downAt[0] : -1, now);
    }


    private Step skillPressed() {
        final long since = SystemClock.uptimeMillis();
        return (now) -> {
            markSkillPress(false);
            D2Trace.log("skill: mouse button down, " + (now - since) + " ms after it was queued");
            return true;
        };
    }


    private void cancelPendingLongPresses() {
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (touch.role == ROLE_DIRECT && !touch.longPressed && touch.longPress != null) {
                handler.removeCallbacks(touch.longPress);
                touch.longPress = null;
                touch.hoverOnly = !touch.moved;
            }
        }
    }

    private void startDirect(final Touch touch) {
        D2Trace.log("tap into the game at " + Math.round(touch.x) + "," + Math.round(touch.y));

        if (pendingTap != null && !pendingTapHeld) {
            handler.removeCallbacks(pendingTap);
            pendingTapHeld = true;
        }
        else if (pendingTapHeld) dropPendingTap("a held tap's touch did not end as a tap");
        if (action != ACTION_NONE) releaseCurrent();
        action = ACTION_DIRECT;
        int[] p = toScreen(touch.x, touch.y);
        enqueue(moveTo(p[0], p[1]));
        touch.longPress = () -> {
            if (touches.indexOfValue(touch) < 0 || touch.role != ROLE_DIRECT || touch.moved || touch.hoverOnly) return;
            touch.longPressed = true;
            int treeSkill = treeSkillAt(touch);
            if (treeSkill >= 0) {

                touch.hoverOnly = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                startTreePick(treeSkill);
                return;
            }
            int plus = plusAt(touch);
            if (plus >= 0) {

                touch.hoverOnly = true;
                D2Points points = modLink.points(1500);
                spendStat(plus, pointTaps.hold(points != null ? points.statPoints : 0, SystemClock.uptimeMillis()), "long press");
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return;
            }
            if (openBuyBox("long press")) {
                touch.hoverOnly = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return;
            }
            enqueue(settle(HOVER_SETTLE_MS));
            setRmb(true);
        };
        handler.postDelayed(touch.longPress, LONG_PRESS_MS);
    }

    private void moveDirect(Touch touch) {
        if (action != ACTION_DIRECT) return;
        int[] p = toScreen(touch.x, touch.y);
        if (touch.moved && !lmbDown && !rmbDown && !touch.hoverOnly && browsingItems()) {


            if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
            touch.hoverOnly = true;
            follow(p);
        }
        else if (touch.moved && !lmbDown && !rmbDown && !touch.hoverOnly) {

            if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
            enqueue(moveTo(p[0], p[1]), settle(30));
            setLmb(true);
        }
        else follow(p);
    }


    private D2ModState.Thing corpseAt(Touch touch) {
        D2ModState s = mod;
        if (s == null || !s.canActOnUnits() || s.isPanelOpen()) return null;
        for (D2ModState.Thing object : s.objects) {
            if (object.kind != D2ModState.OBJECT_CORPSE) continue;
            int[] p = toScreen(touch.x, touch.y);
            float[] g = desktopToGame(p[0], p[1], s);
            float[] c = thingPoint(object);
            if (Math.abs(g[0] - c[0]) <= 40 && Math.abs(g[1] - c[1]) <= 30) return object;
        }
        return null;
    }


    private boolean npcMenuOpen() {
        D2ModState s = mod;
        return s != null && s.isOpen(D2ModState.UI_NPCMENU) && !s.isOpen(D2ModState.UI_NPCSHOP);
    }




    private final D2Hints hints = new D2Hints();
    private D2HintView hintView;
    private final D2Hints.Facts hintFacts = new D2Hints.Facts();
    private long hintGameSince, hintFoughtAt;
    private int hintLastLife = -1;
    private final List<float[]> hintPanels = new ArrayList<>();


    private void hintTick(D2ModState s) {
        long now = SystemClock.uptimeMillis();
        if (s == null) {
            if (hintGameSince != 0) hints.newGame(now);
            hintGameSince = 0;
            hintLastLife = -1;
            return;
        }

        if (hintGameSince == 0 && uiReady) hintGameSince = now;
        if (hintGameSince == 0) return;
        D2Hints.Facts f = hintFacts;
        D2Hud graphics = hud;
        f.blocked = graphics == null || !graphics.hasFrame() || buttons.isEmpty() || D2SpieltestPort.playing()
            || idle.phase() != D2Idle.Phase.OFF || editMode || ringTouch != null || padWheelOpen || padRingOpen() || assignButton != null
            || !hasWindowFocus() || s.life <= 0 || modLink.heroDead();
        D2ResumeExit exit = resumeHint.exit();
        f.resumeChoice = exit != null && exit.active();
        f.paused = pauseWanted;
        long open = D2Hints.windows(s.panelFlags(), D2ModState.UI_ESCMENU, pauseWanted);
        f.inventoryOpen = (open & 1L << D2ModState.UI_INVENTORY) != 0;
        f.stashOpen = (open & 1L << D2ModState.UI_STASH) != 0;
        f.cubeOpen = (open & 1L << D2ModState.UI_CUBE) != 0;
        f.rightPanel = (open & (1L << D2ModState.UI_INVENTORY | 1L << D2ModState.UI_SKILLTREE)) != 0;
        long left = 1L << D2ModState.UI_STASH | 1L << D2ModState.UI_CUBE | 1L << D2ModState.UI_CHARACTER | 1L << D2ModState.UI_MERC
            | 1L << D2ModState.UI_QUEST;
        f.leftPanel = (open & left) != 0;
        long panels = left | 1L << D2ModState.UI_INVENTORY | 1L << D2ModState.UI_SKILLTREE;
        f.otherWindow = (open & ~panels) != 0 || s.npcTalking || npcMenuOpen();
        f.sortShown = sortButton != null && isVisible(sortButton);
        f.materialsShown = materialsWindow.entry(materialsOn(), now) != null && modLink.singlePlayer();
        f.recipesShown = recipesEntryShown();
        f.merc = modLink.mercLife() >= 0;
        f.pointsLeft = s.statPointsLeft || s.skillPointsLeft;
        D2Points points = modLink.points(2000);
        f.skillPoints = points != null ? points.skillPoints : -1;
        f.takeShown = controlsVisible && interactButton != null && isVisible(interactButton);
        f.padUsed = padActive;

        if (action == ACTION_ATTACK || action == ACTION_SKILL || (hintLastLife > 0 && s.life < hintLastLife)) hintFoughtAt = now;
        hintLastLife = s.life;
        f.foughtAt = hintFoughtAt;
        f.inGameFor = now - hintGameSince;

        if (Float.isNaN(D2Hints.left(getWidth(), (D2HintView.TEXT_BUDGET + 2 * D2HintView.PAD) * menuTextScale(), 8 * dp, hintPanels(s))))
            f.blocked = true;
        D2Hints.Topic before = hints.showing();
        hints.update(f, now);
        if (hints.takeSeenChanged()) D2Settings.setHintsSeen(getContext(), hints.encode());
        D2Hints.Topic after = hints.showing();
        if (after != before) {
            if (after != null) {
                D2Trace.log("hint: " + after);
                postInvalidateDelayed(D2Hints.SHOW_MS + 100);
            }
            invalidate();
        }
    }





    private boolean recipesEntryShown() {
        return false;
    }






    private List<float[]> hintPanels(D2ModState s) {
        hintPanels.clear();
        if (s == null || s.gameWidth <= 0 || s.gameHeight <= 0) return hintPanels;
        float[] m = gameMapping(s);
        float layout = (s.gameWidth - HINT_LAYOUT_W) / 2f;
        if (hintFacts.leftPanel) hintPanels.add(new float[]{m[1] + layout * m[0], m[1] + (layout + HINT_LAYOUT_W / 2f) * m[0]});
        if (hintFacts.rightPanel) hintPanels.add(new float[]{m[1] + (layout + HINT_LAYOUT_W / 2f) * m[0], m[1] + (layout + HINT_LAYOUT_W) * m[0]});
        return hintPanels;
    }


    private static final int HINT_LAYOUT_W = 800;


    public void showHintsAgain() {
        hints.reset();
        D2Settings.setHintsSeen(getContext(), hints.encode());
        hints.takeSeenChanged();
        showNote(text(R.string.hint_again_note));
        D2Trace.log("hint: all again from the menu");
    }

    private D2HintView.Host hintHost() {
        return new D2HintView.Host() {
            @Override public D2Hud hud() { return hud; }
            @Override public float textScale() { return menuTextScale(); }
            @Override public List<float[]> panels() { return hintPanels(mod); }
            @Override public float top() {
                float bottom = 0;
                for (Button b : buttons) if (b.group == GROUP_MENU && b.rect && isVisible(b)) bottom = Math.max(bottom, b.cy + b.r);
                return bottom;
            }
            @Override public D2HintView.Spot spot(D2Hints.Topic topic) { return hintSpot(topic); }
        };
    }


    private D2HintView.Spot hintSpot(D2Hints.Topic topic) {
        switch (topic) {
            case TAKE:
                return interactButton != null && isVisible(interactButton) ? new D2HintView.Spot(interactButton.cx, interactButton.cy, interactButton.r) : null;
            case SORT:
                return sortButton != null && isVisible(sortButton) ? rectSpot(sortButton) : null;
            case MATERIALS: {
                RectF entry = materialsWindow.entry(materialsOn(), SystemClock.uptimeMillis());
                return entry != null ? new D2HintView.Spot(entry) : null;
            }
            case POINTS: {
                Button menu = gameButton("menu_drawer");
                return menu != null && isVisible(menu) ? rectSpot(menu) : null;
            }
            case NEW_SKILL: {
                Button keys = gameButton("menu_keys");
                return keys != null && isVisible(keys) ? rectSpot(keys) : null;
            }
            case MERC: {
                D2ModState s = mod;
                if (s == null || s.gameWidth <= 0) return null;
                float[] a = gameToView(D2WorldTap.MERC_LEFT, D2WorldTap.MERC_TOP), b = gameToView(D2WorldTap.MERC_RIGHT, D2WorldTap.MERC_BOTTOM);
                return new D2HintView.Spot(new RectF(a[0], a[1], b[0], b[1]));
            }
            default:
                return null;
        }
    }

    private static D2HintView.Spot rectSpot(Button b) {
        return new D2HintView.Spot(new RectF(b.cx - b.hw, b.cy - b.r, b.cx + b.hw, b.cy + b.r));
    }


    private static boolean itemWindowOpen(D2ModState s) {
        return s.isOpen(D2ModState.UI_INVENTORY) || s.isOpen(D2ModState.UI_STASH) || s.isOpen(D2ModState.UI_CUBE)
            || s.isOpen(D2ModState.UI_NPCSHOP) || s.isOpen(D2ModState.UI_TRADE) || s.isOpen(D2ModState.UI_MERC);
    }


    private boolean browsingItems() {
        D2ModState s = mod;
        if (s == null) return hiddenByPanel;
        return s.isOpen(D2ModState.UI_INVENTORY) || s.isOpen(D2ModState.UI_STASH) || s.isOpen(D2ModState.UI_CUBE)
            || s.isOpen(D2ModState.UI_NPCSHOP) || s.isOpen(D2ModState.UI_TRADE) || s.isOpen(D2ModState.UI_MERC)
            || s.isOpen(D2ModState.UI_SKILLTREE) || s.isOpen(D2ModState.UI_CHARACTER);
    }







    private boolean needsConfirm(Touch touch) {
        D2ModState s = mod;
        if (s == null) return false;
        boolean skills = s.isOpen(D2ModState.UI_SKILLTREE) && s.skillPointsLeft;
        boolean stats = s.isOpen(D2ModState.UI_CHARACTER) && s.statPointsLeft;
        if (!skills && !stats) return false;
        D2Points points = modLink.points(1500);
        float[] g = gameAt(touch, s);
        int tree = skills ? pointTaps.skillTreeHit(points, g[0], g[1]) : D2PointTaps.OTHER;
        if (tree != D2PointTaps.SKILL_BOX && tree != D2PointTaps.UNKNOWN && !(stats && !statsDirect(points, s))) return false;
        long now = SystemClock.uptimeMillis();
        boolean second = now - confirmAt < 5000 && Math.hypot(touch.x - confirmX, touch.y - confirmY) < 28 * dp;
        if (second) {
            confirmX = confirmY = -1;
            return false;
        }
        confirmX = touch.x;
        confirmY = touch.y;
        confirmAt = now;
        return true;
    }

    private void endDirect(Touch touch, boolean cancelled) {
        if (action != ACTION_DIRECT) {
            if (pendingTapHeld) dropPendingTap("the touch was taken over");
            return;
        }
        int plus = !cancelled && !touch.hoverOnly && !lmbDown && !rmbDown ? plusAt(touch) : -1;
        if (plus >= 0) {


            clickPendingTap();
            D2Points points = modLink.points(1500);
            spendStat(plus, pointTaps.tap(plus, points != null ? points.statPoints : 0, SystemClock.uptimeMillis(),
                touch.downTime, ViewConfiguration.getDoubleTapTimeout()), "tap");
        }
        else if (!cancelled && !touch.hoverOnly && !lmbDown && !rmbDown && needsConfirm(touch)) {


            clickPendingTap();
        }
        else if (!cancelled && !touch.hoverOnly && !lmbDown && !rmbDown && corpseAt(touch) != null) {

            D2ModState.Thing corpse = corpseAt(touch);
            setLmb(false);
            setRmb(false);
            startPickup(corpse, 2500);
            return;
        }
        else if (!cancelled && !touch.hoverOnly && !lmbDown && !rmbDown) {


            D2ModState s = mod;
            boolean second = pendingTap != null && pendingTapHeld;
            float distance = second ? (float) Math.hypot(touch.x - pendingTapX, touch.y - pendingTapY) / dp : Float.MAX_VALUE;

            com.winlator.d2.test.Zustand.Figur.Teil ware = browsingItems() ? wareUnderPointer() : null;
            int what = ware != null ? D2DoubleTap.decideWare(second, distance, pendingTapWare, ware.id)
                : browsingItems() && s != null

                ? D2ItemMoves.decide(D2ItemMoves.context(s.isOpen(D2ModState.UI_NPCSHOP), s.isOpen(D2ModState.UI_TRADE),
                    s.isOpen(D2ModState.UI_STASH)), second, distance, pendingTapPlace, pendingTapCellX, pendingTapCellY,
                    s.hoverItemCategory, s.hoverItemPlace, s.hoverItemX, s.hoverItemY, sellByPacket(), moveByPacket())
                : D2DoubleTap.CLICK;
            if (second) {
                pendingTap = null;
                pendingTapHeld = false;


                if (what != D2DoubleTap.SHIFT_CLICK && what != D2DoubleTap.BUY && what != D2DoubleTap.SELL && what != D2DoubleTap.MOVE) {
                    clickAt(pendingTapX, pendingTapY);
                    int[] p = toScreen(touch.x, touch.y);
                    enqueue(moveTo(p[0], p[1]));
                }
            }
            if (what == D2DoubleTap.SELL || what == D2DoubleTap.MOVE) {



                final boolean sell = what == D2DoubleTap.SELL;
                final int place = s.hoverItemPlace, cx = s.hoverItemX, cy = s.hoverItemY;
                enqueue(settle(HOVER_SETTLE_MS), whenHovered(place, cx, cy, sell ? "sell" : "move", () -> {
                    if (sell) {
                        D2Trace.log("double tap on an own item at a merchant: sell (command 30)");
                        modLink.sell(cx, cy);
                    }
                    else moveItem(mod);
                }));
            }
            else if (what == D2DoubleTap.BUY) {
                D2Trace.log("double tap on a ware: right click (buy one)");
                enqueue(settle(HOVER_SETTLE_MS));
                setRmb(true);
                enqueue(waitMs(50));
                setRmb(false);
            }
            else if (what == D2DoubleTap.SHIFT_CLICK) {
                D2Trace.log("double tap on a potion: Shift + click (into the belt)");
                enqueue(settle(HOVER_SETTLE_MS));
                setShift(true);
                setLmb(true);
                enqueue(waitMs(50));
                setLmb(false);
                setShift(false);
            }
            else if (what == D2DoubleTap.WAIT) {
                pendingTapX = touch.x;
                pendingTapY = touch.y;
                pendingTapCellX = ware != null ? -1 : s.hoverItemX;
                pendingTapCellY = ware != null ? -1 : s.hoverItemY;
                pendingTapWare = ware != null ? ware.id : -1;
                pendingTapPlace = ware != null ? -1 : s.hoverItemPlace;
                pendingTapHeld = false;
                pendingTap = () -> {
                    pendingTap = null;

                    if (action == ACTION_NONE) clickAt(pendingTapX, pendingTapY);
                    else D2Trace.log("tap on a potion dropped: " + action + " meanwhile");
                };
                handler.postDelayed(pendingTap, ViewConfiguration.getDoubleTapTimeout());
            }
            else {

                enqueue(settle(HOVER_SETTLE_MS), measureClick());
                setLmb(true);
                enqueue(waitMs(50));
            }
        }


        if (pendingTapHeld) dropPendingTap("the next touch was no tap");
        setLmb(false);
        setRmb(false);
        resumeAfterAction();
    }


    private void clickPendingTap() {
        if (pendingTap == null || !pendingTapHeld) return;
        pendingTap = null;
        pendingTapHeld = false;
        clickAt(pendingTapX, pendingTapY);
    }


    private int plusAt(Touch touch) {
        D2ModState s = mod;
        if (s == null || !s.isOpen(D2ModState.UI_CHARACTER) || !s.statPointsLeft || s.gameWidth <= 0) return -1;
        D2Points points = modLink.points(1500);
        if (!statsDirect(points, s)) return -1;
        float[] g = gameAt(touch, s);
        return pointTaps.plusHit(points, s.plugyLoaded, s.gameWidth, g[0], g[1]);
    }





    private boolean statsDirect(D2Points points, D2ModState s) {
        return D2PointTaps.statsKnown(points, s.plugyLoaded) && modLink.singlePlayer() && !pauseWanted;
    }


    private float[] gameAt(Touch touch, D2ModState s) {
        int[] p = toScreen(touch.x, touch.y);
        return desktopToGame(p[0], p[1], s);
    }


    private void spendStat(int stat, int n, String how) {
        for (int i = 0; i < n; i++) modLink.points(D2Points.KIND_STAT, stat, 0);
        D2Trace.log("points: " + how + " on \"+\" " + stat + ": " + n + " stat point(s)");
    }


    private void dropPendingTap(String why) {
        if (pendingTap != null) handler.removeCallbacks(pendingTap);
        D2Trace.log("tap on a potion dropped: " + why);
        pendingTap = null;
        pendingTapHeld = false;
    }


    private void clickAt(float x, float y) {
        int[] p = toScreen(x, y);
        enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), measureClick());
        setLmb(true);
        enqueue(waitMs(50));
        setLmb(false);
    }




    private String spieltestLayout() {
        return spieltestLayout(true);
    }


    private String spieltestLayout(boolean screen) {
        try {
            int[] at = new int[2];
            if (screen) getLocationOnScreen(at);
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("t", "layout").put("ms", SystemClock.uptimeMillis()).put("w", getWidth()).put("h", getHeight())
                .put("sx", at[0]).put("sy", at[1]).put("knoepfeSichtbar", controlsVisible).put("bearbeiten", editMode)
                .put("pauseGewollt", pauseWanted).put("joystickAktiv", joystickActive).put("aktion", action)
                .put("joystick", new org.json.JSONArray().put(Math.round(idleX + at[0])).put(Math.round(idleY + at[1])).put(Math.round(joystickRadius)));



            D2ModState aimState = mod;
            D2AimAssist aim = aimNow();
            o.put("traenke", potionMode == D2Settings.POTIONS_KEYS ? "tasten" : potionMode == D2Settings.POTIONS_BELT ? "guertel" : "alle")
                .put("zielhilfe", aim.on).put("zielweite", aimState != null ? aimState.gameHeight * aim.reachPct(meleeReach(attackSkillNow())) / 100.0 : 0)
                .put("zielkegel", aim.coneDeg).put("toterWinkel", aim.blindDeg)

                .put("zielweiteNah", aimState != null ? aimState.gameHeight * aim.meleePct / 100.0 : 0)
                .put("zielweiteFern", aimState != null ? aimState.gameHeight * aim.rangePct / 100.0 : 0);

            int attackRight = attackRightSkill();
            D2ModState s = mod;
            int attackSkill = attackRight >= 0 ? attackRight : s != null ? s.leftSkill : -1;
            D2Hud graphics = hud;
            String attackName = graphics != null ? graphics.skillName(attackSkill) : null;
            o.put("angriff", new org.json.JSONObject().put("seite", attackRight >= 0 ? "rechts" : "links")
                .put("fertigkeit", attackSkill).put("name", attackName != null ? attackName : ""));
            if (s != null && s.gameWidth > 0) {

                float[] g0 = gameToView(0, 0), g1 = gameToView(s.gameWidth, s.gameHeight);
                o.put("spiel", new org.json.JSONArray().put(Math.round(g0[0] + at[0])).put(Math.round(g0[1] + at[1]))
                    .put(Math.round(g1[0] - g0[0])).put(Math.round(g1[1] - g0[1])));
            }
            org.json.JSONObject b = new org.json.JSONObject();
            for (Button button : buttons) {


                b.put(button.id, new org.json.JSONArray().put(Math.round(button.cx + at[0])).put(Math.round(button.cy + at[1]))
                    .put(Math.round(button.hw)).put(Math.round(button.r)).put(button.group).put(isVisible(button) ? 1 : 0));
            }
            o.put("knoepfe", b);
            return o.toString();
        }
        catch (Exception e) {
            return null;
        }
    }

    private void onModUpdate() {
        modUpdatePosted = false;
        D2FrameCounts.modStates.incrementAndGet();
        String rates = D2FrameCounts.due(SystemClock.uptimeMillis());
        if (rates != null) D2Trace.log(rates);
        handler.removeCallbacks(modWatchdog);
        if (D2SpieltestPort.active()) {
            D2SpieltestPort.layout(this::spieltestLayout);

            D2SpieltestPort.leftOut(leftOutObjects());
        }
        pad.playtesterCheck();
        idle.step();
        D2ModState s = modLink.state();
        boolean had = mod != null;
        mod = s;

        if (uiFactor > 1f && s != null && s.gameWidth > 0 && s.gameHeight > 0 && getWidth() > 0 && !editMode
            && (s.gameWidth != clearedGameW || s.gameHeight != clearedGameH)) layoutButtons(getWidth(), getHeight());

        quitLoses = D2InstallFlow.quitLoses(quitLoses, s != null, s != null && autosave && s.plugyLoaded, modLink.reportsWithoutState());
        lastLoses = quitLoses;
        resumeHint.tick(s, SystemClock.uptimeMillis());
        if (s != null) keyWait.state(s.leftSkill, s.rightSkill, SystemClock.uptimeMillis());
        hintTick(s);
        updateSort(s);
        checkMenuKeyboard();
        if (s == null) {

            softTarget = null;
            beltRows.reset();
            zeroLifeCheckedFor = null;
            interactCandidate = null;

            long now = SystemClock.uptimeMillis();
            if (noStateSince == 0) noStateSince = now;


            if (now - noStateSince > 3000 && windowFocused) {
                runChecked = false;
                playersSent = false;
                lastPlugyLoaded = null;
                savedAt = 0;
                hideMapWhileFighting(null);
                autoItems.newGame();
                gameEntry.retrySignal();
                pickupTried.clear();
                usedObjects.clear();
                offerCheck.clear();


                leftRestore.clear();
                D2SpieltestPort.leftOut(null);
                syncLabels();
                if (pauseWanted) {
                    pauseWanted = false;
                    modLink.pause(false);
                    invalidate();
                }
            }
            if (had) {
                if (action == ACTION_ATTACK || action == ACTION_SKILL || action == ACTION_PICKUP || action == ACTION_SELECT
                    || action == ACTION_ATTACK_TAP) releaseCurrent();
                invalidate();
            }


            int without = D2StartupControls.withoutState(modLink.isAlive(), windowFocused);
            if (D2StartupControls.entryWaits(without, windowFocused)) gameEntry.outside();
            if (without != D2StartupControls.KEEP) applyUiKey(without == D2StartupControls.OUT_OF_GAME ? UI_OUT_OF_GAME : UI_UNKNOWN);
            if (modLink.isConnected()) handler.postDelayed(modWatchdog, 1500);
            return;
        }
        noStateSince = 0;
        D2ModItems beltItems = modLink.items();
        beltRows.step(s.mouseX, s.mouseY, s.gameWidth, s.gameHeight, beltItems != null ? beltItems.beltRows : 4);
        lastPlugyLoaded = s.plugyLoaded;
        syncLabels();
        hideMapWhileFighting(s);
        if (!runChecked && alwaysRun && windowFocused && !pauseWanted && s.maxLife >= 0) {

            runChecked = true;
            if (!s.running) pressKey(VK_R);
        }
        if (s.handsKnown) {



            int real = modLink.players();
            long now = SystemClock.uptimeMillis();
            if (real > 0 ? real != players && now - playersSentAt > 2000 : !playersSent && players > 1) {
                modLink.players(players);
                playersSentAt = now;
            }
            playersSent = true;

            D2ModItems stackItems = modLink.items();
            int wantedMax = largeStacks ? 80 : D2ModItems.TOME_MAX;
            if (stackItems != null && stackItems.tpTomeMax > 0 && stackItems.tpTomeMax != wantedMax && now - stacksSentAt > 2000) {
                modLink.stacks(largeStacks);
                stacksSentAt = now;
            }
        }
        if (pauseWanted && hudNowPause() - pauseSentAt > 1000) {
            pauseSentAt = hudNowPause();
            modLink.pause(true);
        }

        long hudNow = SystemClock.uptimeMillis();

        boolean pointer = hidePointer && s.pointerHideable && controlsVisible && !editMode && assignButton == null && !pauseWanted;
        boolean ownHud = hud != null && s.canOwnHud();

        boolean unlimited = staminaMode == D2Settings.STAMINA_UNLIMITED && s.unlimitedStaminaPossible;





        D2ModItems carried = modLink.items();
        updateLootScope(s);
        boolean filter = carried != null && carried.labelFilterPossible && s.itemNamesWithoutAlt && labelFilter.any();
        if (filter && hudNow - labelFilterSentAt > 500) {
            labelFilterSentAt = hudNow;
            modLink.labelFilter(labelFilter.bits(labelsOn), D2LabelFilter.SECONDS, D2LabelFilter.RADIUS,
                labelFilter.lootMask(), labelFilter.lootGold());
            lootMaskSent = labelFilter.lootMask();
        }
        else if (D2LabelFilter.clearLoot(filter, lootMaskSent) && carried != null && carried.labelFilterPossible) {

            modLink.labelFilter(labelFilter.bits(labelsOn), D2LabelFilter.SECONDS, D2LabelFilter.RADIUS, 0, 0);
            lootMaskSent = 0;
        }

        boolean newLoot = filter && labelFilter.dropLabels && carried.newLootVisible && windowFocused;
        boolean names = (labelsOn || newLoot) && s.itemNamesWithoutAlt && (s.labelTipsKept || !itemWindowOpen(s));

        boolean map = mapHack && s.mapHackPossible;


        boolean compare = compareEquipped;
        int caps = modLink.capabilities();
        if (caps >= 0 && ((caps & D2ModLink.CAP_COMPARE) != 0) != compareCapable) {
            compareCapable = (caps & D2ModLink.CAP_COMPARE) != 0;
            D2Settings.setCompareCapable(getContext(), compareCapable);
        }
        if (caps >= 0 && ((caps & D2ModLink.CAP_LOOT) != 0) != lootCapable) {
            lootCapable = (caps & D2ModLink.CAP_LOOT) != 0;
            D2Settings.setLootCapable(getContext(), lootCapable);
        }
        if (caps >= 0 && ((caps & D2ModLink.CAP_VALUE) != 0) != valueCapable) {
            valueCapable = (caps & D2ModLink.CAP_VALUE) != 0;
            D2Settings.setValueCapable(getContext(), valueCapable);
        }



        D2Hud tables = hud;
        if (valueLine && valueCapable && tables != null && hudNow - valueSentAt > 1000) {
            valueSentAt = hudNow;

            boolean ladder = s.plugyLoaded && D2Settings.ladderStarted(getContext());
            if (valuePayload == null || valuePayloadLadder != ladder) {
                valuePayload = D2Wertklasse.regeln(ladder ? tables.wertBasenLadder : tables.wertBasen);
                valuePayloadLadder = ladder;
            }
            modLink.valueRules(valuePayload);
        }

        D2Materials.Snapshot stash = modLink.materials();
        if (!materialsCapable && stash != null && stash.possible() && hudNow - stash.time < D2Materials.STALE_MS) {
            materialsCapable = true;
            D2Settings.setMaterialsCapable(getContext());
        }

        materialsWindow.layout(stash, gameMapping(s, materialsMapping), s.gameWidth, s.gameHeight, s.uiFlags);
        materialsWindow.sharedGold(modLink.sharedGold());
        materialsWindow.update(stash, materialsOn(), hudNow);
        recipes.update(s, stash, hudNow);


        int caps2 = modLink.capabilities2();
        if (caps2 >= 0 && modLink.capable2(D2ModLink.CAP2_DIFF) != diffCapable) {
            diffCapable = modLink.capable2(D2ModLink.CAP2_DIFF);
            D2Settings.setDiffCapable(getContext(), diffCapable);
        }


        boolean diff = diffLines && diffCapable && tables != null;
        if (diff && s.characterClass >= 0 && s.characterClass < D2Breakpoints.CLASS_CODES.length && hudNow - diffSentAt > 1000) {
            diffSentAt = hudNow;
            int attackRight = attackRightSkill();
            int flags = D2Breakpoints.framesFlags(attackRight >= 0 ? attackRight : s.leftSkill);
            int forms = s.characterClass == 5 ? 3 : 1;
            for (int form = 0; form < forms; form++) {
                byte[] payload = D2Breakpoints.frames28(tables.animationen, s.characterClass, form, flags);
                if (payload != null) modLink.frames(payload);
            }
        }
        if (caps >= 0 && ((caps & D2ModLink.CAP_DAMAGE) != 0) != damageCapable) {
            damageCapable = (caps & D2ModLink.CAP_DAMAGE) != 0;
            D2Settings.setDamageCapable(getContext(), damageCapable);
        }
        int numbers = damageStyle;


        boolean gridDps = infoStrip != null && infoStrip.showsDps();
        modLink.setAppDps(true);
        int xpBits = infoStrip != null ? infoStrip.xpBarBits(hudLayout == D2Settings.HUD_COMPACT && s.hudLeftOut()) : 0;
        modLink.setXpBar(xpBits);
        boolean dps = gridDps;
        if ((ownHud || pointer || pointerWanted || unlimited || unlimitedWanted || names || namesSent || map || mapSent
                || compare || compareSent || D2DamageStyle.numbers(numbers) || D2DamageStyle.numbers(damageStyleSent) || dps || damageDpsSent || diff || diffSent
                || D2SpieltestPort.active() || idle.running() || xpBits != 0 || xpBitsSent != 0)
            && (hudNow - hudSentAt > 500 || pointer != pointerWanted || unlimited != unlimitedWanted || names != namesSent
                || map != mapSent || compare != compareSent || numbers != damageStyleSent || dps != damageDpsSent
                || diff != diffSent || xpBits != xpBitsSent)) {

            hudSentAt = hudNow;
            if (hudFirstSentAt == 0) hudFirstSentAt = hudNow;
            pointerWanted = pointer;
            unlimitedWanted = unlimited;
            namesSent = names;
            mapSent = map;
            compareSent = compare;
            damageStyleSent = numbers;
            damageDpsSent = dps;
            diffSent = diff;
            xpBitsSent = xpBits;
            modLink.hud(ownHud && hudLayout == D2Settings.HUD_COMPACT, ownHud, pointer, unlimited, names, map, compare, numbers, dps, diff);
        }
        autoDrink(s, hudNow);
        stepLeftRestore(s, hudNow);
        pointHighlight.stepKeys(s, modLink, hud, pauseWanted, hudNow);
        stepRespecSpend(s, hudNow);

        handler.postDelayed(modWatchdog, 600);
        calibrate(s, hudNow);
        boolean plausible = Math.abs(s.playerX - s.gameWidth / 2f) < s.gameWidth * 0.2f && Math.abs(s.playerY - s.gameHeight / 2f) < s.gameHeight * 0.25f;
        positionShiftX = plausible ? 0 : s.gameWidth / 2f - s.playerX;
        positionShiftY = plausible ? 0 : s.gameHeight / 2f - s.playerY;




        boolean entered = gameEntry.settled(hudNow, s.worldSignal, s.worldDrawn);



        boolean hudApplied = s.edgeButtonsHidden || hudFirstSentAt == 0 || hudNow - hudFirstSentAt >= HUD_ECHO_MS;
        uiReady = entered && graphicsDone && hudApplied;
        if (gameEntry.gaveUp()) D2Trace.log("world signal: no picture of the world for "
            + D2StartupControls.SIGNAL_GRACE_MS / 1000 + " s in the game, the buttons follow the states again");
        if (entered) applyUiKey(D2Hints.windows(s.panelFlags(), D2ModState.UI_ESCMENU, pauseWanted));
        else applyUiKey(UI_OUT_OF_GAME);
        padEscMenu(s, entered);
        updateFacing(s);
        if (action == ACTION_ATTACK && attackTargetId != 0) trackTarget(true);
        else if (action == ACTION_ATTACK && heldAttack.due(hudNow, attackTargetId == 0, buttonPending)) attackComingTarget();
        if (action == ACTION_SKILL && skillOnCorpses) trackCorpse(s);
        else if (action == ACTION_SKILL && skillTargetId != 0 && !skillAimedByDrag) trackTarget(false);
        else if (action == ACTION_SKILL && skillWaitsForTarget && !skillAimedByDrag
            && heldSkill.due(hudNow, skillTargetId == 0, buttonPending)) skillComingTarget();
        if (teleportHeld) teleportTick(hudNow);
        if (action == ACTION_PICKUP) checkPickup(s);
        softTarget = action == ACTION_NONE || action == ACTION_WALK ? pickTarget(attackSkillNow()) : null;
        interactCandidate = pickInteract();
        updatePadPreview();
        if (interactCandidate != null) interactSeenAt = hudNow;
        if (fighting()) postInvalidateDelayed(2600);
        placeRespec(s);
        placeSort(s);


        checkZeroLife(s, hudNow);
        boolean dead = modLink.heroDead();
        if (autosave && s.plugyLoaded && !pauseWanted && !dead && !materialsBackupRunning) {
            if (wasDead) {
                D2Trace.log("hero alive again: save now");
                save(hudNow);
            }
            else if (savedAt == 0) savedAt = hudNow;
            else if (hudNow - savedAt >= AUTOSAVE_MS) save(hudNow);
        }
        wasDead = dead;
        foldForTalk(s.npcTalking);
        if (padEndTalk.escNow(npcAround(s), talkSignature(s), hudNow)) {
            D2Trace.log("pad B: talk still open, Esc again");
            markPadEscSent();
            pressKey(VK_ESCAPE);
        }
        deathScreenSeen(hudNow);
        if (interactButton != null) interactButton.label = s.npcTalking ? text(R.string.take_continue) : (npcMenuOpen() ? text(R.string.take_select) : interactLabel(interactCandidate));

        boolean npcMenu = s.isOpen(D2ModState.UI_NPCMENU);
        if (ringNpc) npcRingLines(s, npcMenu);
        else if (npcFollow.due(npcMenuOpen(), s.npcTalking, s.npcMenuIds, hudNow)) openNpcFollowRing();
        else if (npcMenu && !npcMenuWasOpen && autoTrade) {
            for (int i = 0; i < s.npcMenuIds.length; i++) {
                if (s.npcMenuIds[i] == D2ModState.NPC_TRADE || s.npcMenuIds[i] == D2ModState.NPC_TRADE_REPAIR
                    || s.npcMenuIds[i] == D2ModState.NPC_IDENTIFY) {
                    modLink.npc(i);
                    break;
                }
            }
        }
        if (npcEndTalk.escNow(npcMenuOpen(), s.npcMenuIds, hudNow)) {
            D2Trace.log("npc ring: back in the menu after cancel, Esc ends the talk");
            pressKey(VK_ESCAPE);
        }

        npcMenuWasOpen = npcMenu && (s.npcMenuIds.length > 0 || s.portals < 0);
        npcMenuCount.update(npcMenu, s.npcMenuIds, s.npcTalking || s.isOpen(D2ModState.UI_NPCSHOP));

        boolean equipping = idle.equipping();
        if (!equipping && autoItems.any() && windowFocused && !editMode && controlsVisible
            && (action == ACTION_NONE || action == ACTION_WALK || heldWalksNobody())) autoPickGold(s);
        if (!equipping) refillBelt(s, hudNow);
        if (inputBoxForShop && !s.isOpen(D2ModState.UI_NPCSHOP) && inputBox.open()) {
            inputBox.close();
            inputBoxForShop = false;
        }
        updateBuyProgress();
        String shopNote = shopButton != null ? shopButton.poll() : null;
        if (shopNote != null) showNote(shopNote);
        updateHighlight(s);
        invalidate();
    }






    private void updateHighlight(D2ModState s) {
        if (!s.canHighlight()) return;
        if (!highlightOn) {
            if (highlighted != null) modLink.highlight(0, D2ModLink.NO_UNIT);
            highlighted = null;
            return;
        }
        Object want = null;
        if (controlsVisible && !editMode) {
            long id = action == ACTION_ATTACK ? attackTargetId : (action == ACTION_SKILL ? skillTargetId : 0);
            if (id > 0) want = findMonster(id);


            if (ringTouch != null && !ringNpc) want = D2NpcRing.highlightTarget(ringTarget(ringChoice.target()));
            if (want == null && interactPinnedId != 0 && interactCandidate != null) want = interactCandidate;


            if (want == null && padPreviewOn) want = padPreview;
            else if (padAttackHeld()) {


            }
            else {
                if (want == null && labelsOn && interactCandidate instanceof D2ModState.Thing && s.items.contains(interactCandidate)) want = interactCandidate;
                if (want == null) want = softTarget;
                if (want == null) want = interactCandidate;
            }
        }
        long now = SystemClock.uptimeMillis();
        long wantId = want != null ? thingId(want) : D2ModLink.NO_UNIT;
        long hadId = highlighted != null ? thingId(highlighted) : D2ModLink.NO_UNIT;
        if (wantId == hadId && (want == null || now - highlightSentAt < 300)) {
            highlighted = want;
            return;
        }
        highlighted = want;
        highlightSentAt = now;
        modLink.highlight(want != null ? thingType(want) : 0, wantId);
    }







    private final D2StartupControls.Entry gameEntry = new D2StartupControls.Entry();

    private volatile boolean uiReady;





    boolean uiReady() {
        return uiReady;
    }

    private void applyUiKey(long key) {
        if (key == appliedUiKey || editMode) return;
        D2Trace.log("windows " + Long.toHexString(key));
        appliedUiKey = key;
        if (key == UI_OUT_OF_GAME || key > 0) {
            if (controlsVisible) setControlsVisible(false, true);
        }
        else if (!controlsVisible && hiddenByPanel) {
            setControlsVisible(true, false);
        }
        invalidate();
    }


    private final D2HeldAttack heldAttack = new D2HeldAttack();







    private void attackComingTarget() {
        D2ModState.Monster target = pickTarget(pressSkill(true));
        if (target == null) return;
        setLmb(false);
        setShift(false);
        attackTargetId = target.id;
        targetMissing = 0;
        if (mod != null && mod.canActOnUnits()) {
            directAttack = true;
            D2Trace.log("attack: " + target.id + " " + target.name + " came into reach, direct");
            modLink.unit(D2ModLink.BUTTON_LEFT, D2ModLink.PHASE_DOWN, D2ModState.UNIT_MONSTER, target.id);
            attackPressAt = SystemClock.uptimeMillis();
            scheduleKeepAlive();
            return;
        }
        D2Trace.log("attack: " + target.id + " " + target.name + " came into reach, with the mouse");
        int[] p = monsterPoint(target);
        buttonPending = true;
        enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), hoverSearch(target.id, target.x, target.y, MONSTER_OFFSETS));
        setLmb(true);
        enqueue(buttonSent());
    }


    private final D2HeldAttack heldSkill = new D2HeldAttack();
    private boolean skillWaitsForTarget;








    private void skillComingTarget() {
        D2ModState.Monster target = pickTarget(skillNow);
        if (target == null) return;
        skillWaitsForTarget = false;
        setSkillMouse(false);
        setShift(false);
        skillTargetId = target.id;
        targetMissing = 0;
        if (mod != null && mod.canActOnUnits()) {
            directAttack = true;
            D2Trace.log("skill: " + target.id + " " + target.name + " came into reach, direct");
            modLink.unit(skillButton(), D2ModLink.PHASE_DOWN, D2ModState.UNIT_MONSTER, target.id);
            markSkillPress(true);
            scheduleKeepAlive();
            return;
        }
        D2Trace.log("skill: " + target.id + " " + target.name + " came into reach, with the mouse");
        int[] p = monsterPoint(target);
        buttonPending = true;
        enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), hoverSearch(target.id, target.x, target.y, MONSTER_OFFSETS));
        if (D2SkillAim.shiftNeeded(skillLeft, true)) setShift(true);
        setSkillMouse(true);
        enqueue(skillPressed(), buttonSent());
    }




    private boolean teleportHeld;
    private D2TeleportAim.Answer teleportPressed;
    private long teleportPressedAt, teleportAskedAt, teleportSince;

    private D2TeleportAim.Answer teleportBlocked;












    private void teleportTick(long now) {
        D2ModState s = mod;
        if (action != ACTION_SKILL || skillOwner == null || s == null) {
            teleportHeld = false;
            return;
        }
        if (now - teleportAskedAt >= D2TeleportAim.ASK_MS && modLink.teleportAvailable()) {
            teleportAskedAt = now;
            modLink.teleport(D2TeleportAim.request(teleportDirX(), teleportDirY(), Math.round(s.gameHeight * TELEPORT_REACH)));
        }
        D2TeleportAim.Answer a = modLink.teleportAnswer();
        if (!D2TeleportAim.aimable(a, now, teleportSince)) return;


        if (!a.samePoint(teleportAim) && now - teleportPressedAt >= TELEPORT_CLICK_MS) {
            int[] p = gameToDesktop(a.screenX, a.screenY);
            follow(p);
            teleportAim = a;
        }
        if (!a.samePoint(teleportAim) || !D2TeleportAim.press(a, now, teleportSince, teleportPressed, teleportPressedAt)) return;


        if (!isAtTarget() || atTargetSince == 0 || now - atTargetSince < HOVER_SETTLE_MS) return;
        if (Math.abs(s.mouseX - a.screenX) > 2 || Math.abs(s.mouseY - a.screenY) > 2) return;


        boolean hoverIgnored = s.hoverType != D2ModState.UNIT_TILE && modLink.capable2(D2ModLink.CAP2_TELE_HOVER);
        if (s.hoverType != 0xFF && !hoverIgnored) {
            if (!a.samePoint(teleportBlocked)) {
                D2Trace.log("teleport: no press, the game shows unit type " + s.hoverType + " #" + s.hoverId + " under the cursor");
            }
            teleportBlocked = a;
            return;
        }
        teleportBlocked = null;
        teleportPressed = a;
        teleportPressedAt = now;
        D2Trace.log("teleport: press on " + a.tileX + "," + a.tileY + " from " + a.fromX + "," + a.fromY + " (status " + a.status
            + ", busy spells " + a.busyStarts + ")" + (s.hoverType != 0xFF ? ", unit type " + s.hoverType + " under the cursor" : ""));
        setSkillMouse(true);
        enqueue(waitMs(60));
        setSkillMouse(false);
        invalidate();
    }


    private D2TeleportAim.Answer teleportAim;
    private static final long TELEPORT_CLICK_MS = 100;

    private float teleportDragX, teleportDragY;
    private boolean teleportDragged;

    private float teleportDirX() {
        return teleportDragged ? teleportDragX : dirX;
    }

    private float teleportDirY() {
        return teleportDragged ? teleportDragY : dirY;
    }


    private static final float TELEPORT_REACH = 0.26f;





    private void drawTeleportMarker(Canvas canvas) {
        D2ModState s = mod;
        if (action != ACTION_SKILL || skillOwner == null || s == null) return;
        long now = SystemClock.uptimeMillis();
        D2TeleportAim.Answer a = modLink.teleportAnswer();
        int status = D2TeleportAim.marker(a, now, teleportSince);

        boolean blocked = status != D2TeleportAim.NONE && a.samePoint(teleportBlocked);
        float dx = teleportDirX(), dy = teleportDirY();
        float len = (float) Math.hypot(dx, dy), reach = s.gameHeight * TELEPORT_REACH, k = menuTextScale();
        if (len < 1e-3f) return;

        float[] wish = gameToView(s.playerX + dx / len * reach, s.playerY + dy / len * reach);
        if (status == D2TeleportAim.NONE) D2TeleportMarker.draw(canvas, wish[0], wish[1], k, true, fillPaint, strokePaint);
        else {
            float[] v = gameToView(a.screenX, a.screenY);
            if (status == D2TeleportAim.CORRECTED) D2TeleportMarker.drawCorrection(canvas, wish[0], wish[1], v[0], v[1], k, strokePaint);
            D2TeleportMarker.draw(canvas, v[0], v[1], k, blocked, fillPaint, strokePaint);
        }
        postInvalidateDelayed(50);
    }


    private void trackTarget(boolean attack) {
        if (directAttack) {
            trackDirectTarget(attack);
            return;
        }
        if (buttonPending) return;
        long id = attack ? attackTargetId : skillTargetId;
        D2ModState.Monster monster = id > 0 ? findMonster(id) : null;
        if (monster != null) {
            targetMissing = 0;
            follow(monsterPoint(monster));
            return;
        }

        if (monster == null && id > 0 && ++targetMissing < 3) return;
        targetMissing = 0;
        D2ModState.Monster next = pickTarget(pressSkill(attack));
        if (attack) setLmb(false);
        else setSkillMouse(false);
        if (next == null) {
            if (attack) attackTargetId = -1;
            else skillTargetId = -1;
            return;
        }
        if (attack) attackTargetId = next.id;
        else skillTargetId = next.id;
        int[] p = monsterPoint(next);
        buttonPending = true;
        enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), hoverSearch(next.id, next.x, next.y, MONSTER_OFFSETS));
        if (attack) setLmb(true);
        else {
            setSkillMouse(true);
            enqueue(skillPressed());
        }
        enqueue(buttonSent());
    }





    private void trackDirectTarget(boolean attack) {
        long id = attack ? attackTargetId : skillTargetId;
        if (id > 0 && findMonster(id) != null) {
            targetMissing = 0;
            return;
        }
        if (id > 0 && ++targetMissing < 3) return;
        targetMissing = 0;
        D2ModState.Monster next = pickTarget(pressSkill(attack));
        if (D2HeldAttack.traceTargetChange(id, next != null)) {
            D2Trace.log("target " + id + " gone: next " + (next != null ? next.id + " " + next.name : "none"));
        }
        if (attack) attackTargetId = next != null ? next.id : -1;
        else skillTargetId = next != null ? next.id : -1;
        if (next != null) {
            if (attackWalking) stopAttackWalk();
            modLink.unit(attack ? D2ModLink.BUTTON_LEFT : skillButton(), D2ModLink.PHASE_DOWN, D2ModState.UNIT_MONSTER, next.id);
            if (!attack) skillDownSent();
            else attackPressAt = SystemClock.uptimeMillis();
        }
        else if (!attackWalking) attackIdle();
    }







    private void retargetByJoystick(boolean attack) {
        if (!joystickActive || !hasDirection || (!attack && (skillAimedByDrag || skillOnCorpses))) return;
        long now = SystemClock.uptimeMillis();
        handler.removeCallbacks(retargetLater);
        long wait = retarget.waitMs(now);
        if (wait > 0) {
            handler.postDelayed(retargetLater, wait);
            return;
        }
        D2ModState s = mod;
        if (s == null) return;
        long id = attack ? attackTargetId : skillTargetId;
        D2ModState.Monster next = pickTarget(pressSkill(attack));
        if (next == null || next.id == id) return;
        D2ModState.Monster current = findMonster(id);
        if (current != null && D2Retarget.keeps(current.x - s.playerX, current.y - s.playerY, dirX, dirY, aimNow().coneDeg)) return;
        retarget.changed(now);
        D2Trace.log("retarget by joystick: " + id + " -> " + next.id + " " + next.name);
        if (attack) attackTargetId = next.id;
        else skillTargetId = next.id;
        targetMissing = 0;
        if (attackWalking) stopAttackWalk();
        modLink.unit(attack ? D2ModLink.BUTTON_LEFT : skillButton(), D2ModLink.PHASE_DOWN, D2ModState.UNIT_MONSTER, next.id);
        if (!attack) skillDownSent();
        else attackPressAt = now;
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }


    private void retargetAfterGap() {
        if (directAttack && (action == ACTION_ATTACK || action == ACTION_SKILL)) retargetByJoystick(action == ACTION_ATTACK);
    }


    private void scheduleKeepAlive() {
        handler.removeCallbacks(keepAlive);
        handler.postDelayed(keepAlive, 200);
    }

    private void keepAlive() {
        if (!directAttack || (action != ACTION_ATTACK && action != ACTION_SKILL)) return;
        long id = action == ACTION_ATTACK ? attackTargetId : skillTargetId;
        if (id > 0) modLink.unit(action == ACTION_ATTACK ? D2ModLink.BUTTON_LEFT : skillButton(), D2ModLink.PHASE_HOLD, D2ModState.UNIT_MONSTER, id);
        handler.postDelayed(keepAlive, 200);
    }


    private void releaseDirect() {
        handler.removeCallbacks(retargetLater);
        if (!directAttack) return;
        directAttack = false;
        handler.removeCallbacks(keepAlive);
        long id = action == ACTION_SKILL ? skillTargetId : attackTargetId;
        int button = action == ACTION_SKILL ? skillButton() : D2ModLink.BUTTON_LEFT;
        if (id > 0) modLink.unit(button, D2ModLink.PHASE_UP, D2ModState.UNIT_MONSTER, id);
    }


    private Step buttonSent() {
        return (now) -> {
            buttonPending = false;
            return true;
        };
    }






    private void trackCorpse(D2ModState s) {
        boolean present = false;
        for (D2ModState.Thing corpse : s.corpses) if (corpse.id == skillTargetId) present = true;
        if (!present) {
            D2ModState.Thing next = pickCorpse();
            skillTargetId = next != null ? next.id : -1;
        }
        long now = SystemClock.uptimeMillis();
        if (skillTargetId > 0 && now - corpseClickAt > 400) {
            corpseClickAt = now;
            modLink.unit(skillButton(), D2ModLink.PHASE_CLICK, D2ModState.UNIT_MONSTER, skillTargetId);
            markSkillPress(true);
        }
    }


    private D2ModState.Thing pickCorpse() {
        D2ModState s = mod;
        if (s == null || s.inTown) return null;
        D2AimAssist aim = aimNow();
        float range = s.gameHeight * aim.reachPct(false) / 100f;
        double cone = Math.cos(Math.toRadians(aim.coneDeg));
        D2ModState.Thing best = null;
        double bestScore = Double.MAX_VALUE;
        for (D2ModState.Thing corpse : s.corpses) {
            float dx = corpse.x - s.playerX, dy = corpse.y - s.playerY;
            double distance = isoDistance(dx, dy);
            if (distance > range) continue;
            double score = distance;
            if (joystickActive && hasDirection && distance > 1) {
                double cos = (dx * dirX + dy * dirY) / Math.hypot(dx, dy);
                if (cos < cone) continue;
                score = distance * (1.5 - cos);
            }
            if (score < bestScore) {
                bestScore = score;
                best = corpse;
            }
        }
        return best;
    }






    private void updateFacing(D2ModState s) {
        if (s.inTown) {
            hasFacing = false;
            return;
        }
        if (joystickActive && hasDirection) {
            setFacing(dirX, dirY);
            return;
        }
        float[] game = D2Facing.screen(s.playerDirection);
        if (game != null) {
            setFacing(game[0], game[1]);
            return;
        }
        long id = action == ACTION_ATTACK ? attackTargetId : action == ACTION_SKILL && !skillAimedByDrag ? skillTargetId : 0;
        D2ModState.Monster target = id > 0 ? findMonster(id) : null;
        if (target != null) setFacing(target.x - s.playerX, target.y - s.playerY);
    }

    private void setFacing(float x, float y) {
        float length = (float) Math.hypot(x, y);
        if (length < 1e-3f) return;
        faceX = x / length;
        faceY = y / length;
        hasFacing = true;
    }


    private void faceDesktopPoint(int[] p) {
        D2ModState s = mod;
        if (s == null || s.inTown) return;
        int[] at = gameToDesktop(s.playerX, s.playerY);
        setFacing(p[0] - at[0], p[1] - at[1]);
    }





    private D2AimAssist aimNow() {
        return D2AimAssist.now(aimAssist, aimRangeMeleePct, aimRangePct, aimConeDeg, aimBlindDeg, idle != null && idle.running(),
            D2Settings.AIM_RANGE_MELEE_DEFAULT, D2Settings.AIM_RANGE_DEFAULT, D2Settings.AIM_CONE_DEFAULT);
    }


    private int skillNow = -1;


    private int pressSkill(boolean attack) {
        D2ModState s = mod;
        return attack ? (s != null ? s.leftSkill : -1) : skillNow;
    }


    private int attackSkillNow() {
        int right = attackRightSkill();
        return right >= 0 ? right : pressSkill(true);
    }





    private boolean meleeReach(int skill) {
        D2Hud graphics = hud;
        return D2AimReach.melee(D2AimReach.range(graphics != null ? graphics.fertigkeiten : null, skill), modLink.attackKind());
    }





    private D2ModState.Monster pickTarget(int skill) {
        D2ModState s = mod;
        D2AimAssist aim = aimNow();
        if (s == null || s.inTown || !aim.on) return null;
        float range = s.gameHeight * aim.reachPct(meleeReach(skill)) / 100f;
        double cone = Math.cos(Math.toRadians(aim.coneDeg));
        D2ModState.Monster best = null;
        double bestScore = Double.MAX_VALUE;
        for (D2ModState.Monster m : s.monsters) {
            if (m.isTownNpc()) continue;
            if (idleIgnored.contains(m.id)) continue;
            float dx = m.x - s.playerX, dy = m.y - s.playerY;
            double distance = isoDistance(dx, dy);
            if (distance > range) continue;
            double score = distance;
            if (joystickActive && hasDirection && distance > 1) {
                double cos = (dx * dirX + dy * dirY) / Math.hypot(dx, dy);
                if (cos < cone) continue;
                score = distance * (1.5 - cos);
            }

            else if (hasFacing && D2AimAssist.behind(dx, dy, faceX, faceY, aim.blindDeg)) continue;
            if (m.isSpecial()) score *= 0.9;
            if (score < bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best;
    }







    private static final int ORDER_TAP = 0, ORDER_RANK = 1, ORDER_NEAR = 2;


    private static int[] objectDetails(long[] triples, long id) {
        for (int i = 0; triples != null && i + 2 < triples.length; i += 3) {
            if (triples[i] == id) return new int[]{(int) triples[i + 1], (int) triples[i + 2]};
        }
        return null;
    }


    private void noteLeftOutByUse(D2ModState.Thing object) {
        long now = SystemClock.uptimeMillis();
        String line = offerCheck.leftOutByUse(object.kind, object.id, usedObjects.until(object.kind, object.id), now);
        if (line != null) D2Trace.log(line);
    }

    private List<Object> interactCandidates(int order) {
        boolean byRank = order == ORDER_RANK, near = order == ORDER_NEAR;
        D2ModState s = mod;
        List<Object> list = new ArrayList<>();
        if (s == null) return list;
        float range = s.gameHeight * D2Gamepad.TAKE_REACH_OF_HEIGHT;
        final Map<Object, Double> scores = new HashMap<>();
        D2ModItems details = modLink.items();

        boolean skipHidden = !idle.running();

        if (labelsOn || !itemsNeedLabels || modLink.canTake()) {
            for (D2ModState.Thing item : s.items) {
                double distance = isoDistance(item.x - s.playerX, item.y - s.playerY);
                if (distance > range) continue;
                D2ModItems.Item d = details != null ? details.find(item.id) : null;
                if (skipHidden && d != null && d.hiddenByFilter) continue;
                if (!byRank) {
                    scores.put(item, near ? distance : distance * 0.8);
                    continue;
                }
                int rank = D2InteractRank.item(d != null ? d.category : -1, d != null ? d.quality : -1, item.kind, item.extra);
                scores.put(item, D2InteractRank.key(rank, distance));
            }
        }
        boolean idleNehmen = idle.running() || D2SpieltestPort.playing();
        long[] details12 = idleNehmen ? modLink.objectDetails(2000) : null;
        for (D2ModState.Thing object : s.objects) {
            if (usedObjects.leftOut(object.kind, object.id, SystemClock.uptimeMillis())) {
                noteLeftOutByUse(object);
                continue;
            }


            if (idle.clearing() && (object.kind == D2ModState.OBJECT_PORTAL || object.kind == D2ModState.OBJECT_ENTRANCE)) continue;


            if (idleNehmen && com.winlator.d2.test.Objektwahl.nehmenNie(object.kind, object.extra, objectDetails(details12, object.id))) continue;
            double distance = isoDistance(object.x - s.playerX, object.y - s.playerY);
            if (distance > range) continue;

            if (byRank) scores.put(object, D2InteractRank.key(D2InteractRank.object(object.kind), distance));
            else scores.put(object, !near && object.kind == D2ModState.OBJECT_CORPSE ? -1 : distance);
        }


        for (D2ModState.Monster person : s.monsters) {
            if (!s.inTown && !person.isTownNpc()) continue;
            double distance = isoDistance(person.x - s.playerX, person.y - s.playerY);
            if (distance <= range) scores.put(person, byRank ? D2InteractRank.key(D2InteractRank.person(), distance) : distance);
        }
        list.addAll(scores.keySet());
        Collections.sort(list, (a, b) -> Double.compare(scores.get(a), scores.get(b)));
        return list;
    }



    private static final long RING_HOLD_MS = 250;

    private static final float RING_STEP_DP = 14;

    private Touch ringTouch;
    private final List<Object> ringTargets = new ArrayList<>();

    private final List<Integer> ringTypes = new ArrayList<>();

    private D2RingChoice ringChoice = new D2RingChoice(0);
    private float ringCx, ringCy, ringR;

    private float ringStartX, ringStartY;


    private boolean openRing(Touch touch) {
        if ((mod != null && mod.npcTalking) || editMode) return false;


        if (npcMenuOpen()) return !touch.cycled && mod.npcMenuIds.length > 0 && openNpcRing(touch, null);


        Object primary = pickInteract();
        if (primary instanceof D2ModState.Monster && mod != null && mod.inTown) return openNpcRing(touch, primary);

        List<Object> list = interactCandidates(ringSort == D2Settings.RING_SORT_RANK ? ORDER_RANK : ORDER_NEAR);
        if (list.size() < 2) return false;
        ringTargets.clear();
        ringTargets.addAll(list);
        ringTypes.clear();
        for (Object target : list) ringTypes.add(thingType(target));
        ringNameItems = new D2ModItems.Item[list.size()];
        ringNames = new String[list.size()];
        ringChoice = new D2RingChoice(list.size());


        float w = getWidth(), h = getHeight(), pad = 70 * dp;
        ringR = Math.max(90 * dp, Math.min(160 * dp, Math.min(w, h) / 2f - pad));
        D2ModState s = mod;
        if (s != null && s.gameWidth > 0 && s.gameHeight > 0) {
            float[] m = gameMapping(s);
            ringCx = m[1] + s.gameWidth / 2f * m[0];
            ringCy = m[2] + s.gameHeight / 2f * m[0];
        }
        else {
            ringCx = w / 2f;
            ringCy = h / 2f;
        }
        ringCx = Math.max(ringR + pad, Math.min(w - ringR - pad, ringCx));
        ringCy = Math.max(ringR + pad * 0.6f, Math.min(h - ringR - pad * 0.6f, ringCy));

        ringStartX = touch.downX;
        ringStartY = touch.downY;
        ringTouch = touch;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        D2Trace.log("ring open: " + list.size() + " targets");
        invalidate();
        return true;
    }

    private void closeRing() {
        ringNpc = false;
        ringFollow = null;
        ringNpcIds = null;
        ringNpcTexts = null;
        ringTouch = null;
        ringTargets.clear();
        ringTypes.clear();
        ringChoice = new D2RingChoice(0);
        invalidate();
    }





    private void moveRing(Touch touch) {
        if (ringNpc && ringTargets.isEmpty()) return;
        int changed = ringChoice.move(touch.x - ringStartX, touch.y - ringStartY, RING_STEP_DP * dp);
        if (changed == 2 || (changed == 1 && ringChoice.selected() != D2RingChoice.NONE))
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
    }




    private boolean ringNpc;
    private static final Integer NPC_RING_CANCEL = -1;


    private boolean openNpcRing(Touch touch, Object npc) {
        ringTargets.clear();
        ringTypes.clear();
        ringNpc = true;
        ringChoice = new D2RingChoice(0);
        float w = getWidth(), h = getHeight(), pad = 70 * dp;
        ringR = Math.max(90 * dp, Math.min(160 * dp, Math.min(w, h) / 2f - pad));
        D2ModState s = mod;
        if (s != null && s.gameWidth > 0 && s.gameHeight > 0) {
            float[] m = gameMapping(s);
            ringCx = m[1] + s.gameWidth / 2f * m[0];
            ringCy = m[2] + s.gameHeight / 2f * m[0];
        }
        else {
            ringCx = w / 2f;
            ringCy = h / 2f;
        }
        ringCx = Math.max(ringR + pad, Math.min(w - ringR - pad, ringCx));
        ringCy = Math.max(ringR + pad * 0.6f, Math.min(h - ringR - pad * 0.6f, ringCy));
        ringTouch = touch;
        if (npc == null) {
            D2Trace.log("npc ring: held in his open menu");
            return true;
        }
        D2Trace.log("npc ring: talking to " + thingId(npc) + ", waiting for his menu");
        startPickup(npc, 2500);
        return true;
    }






    private Touch ringFollow;

    private int[] ringNpcIds;
    private String[] ringNpcTexts;
    private final D2NpcRing.Follow npcFollow = new D2NpcRing.Follow();
    private final D2NpcRing.EndTalk npcEndTalk = new D2NpcRing.EndTalk();

    private void openNpcFollowRing() {
        if (ringTouch != null || editMode || pauseWanted || interactButton == null) return;
        Touch touch = new Touch();
        touch.deviceId = D2Idle.DEVICE_ID - 1;
        touch.button = interactButton;
        openNpcRing(touch, null);
        touch.x = ringCx;
        touch.y = ringCy;
        ringFollow = touch;
        padRingTouch = touch;
        D2Trace.log("npc ring: the game's next list follows");
        if (mod != null) npcRingLines(mod, true);
    }


    private boolean followRingByFinger(Touch touch) {
        if (ringFollow == null || ringTouch != ringFollow) return false;
        ringTouch = touch;
        padRingTouch = null;
        ringStartX = touch.downX;
        ringStartY = touch.downY;
        ringChoice = new D2RingChoice(ringTargets.size());
        touch.longPressed = true;
        invalidate();
        return true;
    }


    private void npcRingLines(D2ModState s, boolean npcMenu) {
        if (D2NpcRing.closeOnMenuGone(!ringTargets.isEmpty(), npcMenu)) {

            D2Trace.log("npc ring: the menu closed meanwhile, ring closed");
            closeRing();
            return;
        }
        if (D2NpcRing.closeOnOtherLines(ringNpcIds, ringNpcTexts, s.npcMenuIds, s.npcMenuTexts)) {

            D2Trace.log("npc ring: the game shows other lines, ring closed");
            closeRing();
            return;
        }
        if (!ringTargets.isEmpty() || !npcMenu || s.npcMenuIds.length == 0) return;
        ringNpcIds = s.npcMenuIds.clone();
        ringNpcTexts = s.npcMenuTexts.clone();
        for (int i = 0; i < s.npcMenuIds.length; i++) ringTargets.add(i);
        ringTargets.add(NPC_RING_CANCEL);
        ringNames = new String[ringTargets.size()];
        for (int i = 0; i < s.npcMenuTexts.length; i++) {

            ringNames[i] = D2NpcRing.isResurrect(s.npcMenuIds[i]) ? text(R.string.npc_ring_resurrect) : s.npcMenuTexts[i];
        }
        ringNameItems = new D2ModItems.Item[ringTargets.size()];
        ringChoice = new D2RingChoice(ringTargets.size());

        Touch t = ringTouch;
        ringStartX = t != null && t != padRingTouch ? t.x : ringCx;
        ringStartY = t != null && t != padRingTouch ? t.y : ringCy;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        D2Trace.log("npc ring: " + s.npcMenuIds.length + " lines");
        invalidate();
    }

    private String npcRingName(int index) {
        if (index < 0 || index >= ringTargets.size()) return "";
        Object line = ringTargets.get(index);
        if (NPC_RING_CANCEL.equals(line)) return text(R.string.npc_ring_cancel);
        return ringNames != null && index < ringNames.length && ringNames[index] != null ? D2FontText.map(ringNames[index]) : "?";
    }





    private void npcRingTake(Object chosen, String by) {
        boolean shown = !ringTargets.isEmpty();
        if (ringFollow != null && shown && chosen == null && npcMenuOpen()) {

            D2Trace.log("npc ring: " + by + " nothing chosen, the next list stays");
            ringFollow.x = ringCx;
            ringFollow.y = ringCy;
            ringTouch = ringFollow;
            padRingTouch = ringFollow;
            ringStartX = ringCx;
            ringStartY = ringCy;
            ringChoice = new D2RingChoice(ringTargets.size());
            invalidate();
            return;
        }
        D2ModState s = mod;
        boolean subList = s != null && D2NpcRing.isSubList(s.npcMenuIds);
        closeRing();
        if (!shown) {
            D2Trace.log("npc ring: " + by + " let go before the menu, as a tap");
            return;
        }
        if (chosen instanceof Integer && (Integer) chosen >= 0 && npcMenuOpen()) {
            int line = (Integer) chosen;
            D2Trace.log("npc ring: " + by + " takes line " + line);
            performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            npcFollow.chose(s != null ? s.npcMenuIds : null, SystemClock.uptimeMillis());
            modLink.npc(line);
            return;
        }
        D2Trace.log("npc ring: " + by + " nothing chosen, the talk ends");

        if (subList && npcMenuOpen()) npcEndTalk.start(SystemClock.uptimeMillis());
        if (npcMenuOpen() || (mod != null && mod.npcTalking)) pressKey(VK_ESCAPE);
    }





    private Object ringTarget(int index) {
        if (ringNpc) return index >= 0 && index < ringTargets.size() ? ringTargets.get(index) : null;
        D2ModState s = mod;
        if (s == null || index < 0 || index >= ringTargets.size()) return null;
        Object frozen = ringTargets.get(index);
        long id = thingId(frozen);
        if (frozen instanceof D2ModState.Monster) {
            for (D2ModState.Monster m : s.monsters) if (m.id == id) return m;
            return null;
        }
        int type = ringTypes.get(index);
        for (D2ModState.Thing t : type == D2ModState.UNIT_ITEM ? s.items : s.objects) if (t.id == id && thingType(t) == type) return t;
        return null;
    }


    private String ringName(int index, D2Hud graphics) {
        if (ringNpc) return npcRingName(index);
        Object target = ringTarget(index);
        if (target == null) target = ringTargets.get(index);
        D2ModItems.Item item = ringItem(index);
        if (item != null && graphics != null) {

            if (index < ringNames.length && ringNameItems[index] == item && ringNames[index] != null) return ringNames[index];

            int variant = D2ItemLabel.VARIANT == D2ItemLabel.VARIANT_TWO_LINES ? D2ItemLabel.VARIANT_AFFIXES : D2ItemLabel.VARIANT;
            String text = ringLabels.text(item.name, item.quality, item.runeword, baseName(graphics, item.code), graphics::textWidth,
                RING_NAME_BUDGET, variant);
            if (index < ringNames.length) {
                ringNameItems[index] = item;
                ringNames[index] = text;
            }
            return text;
        }

        String name = item != null ? item.oneLine() : null;
        if (name == null || name.isEmpty()) name = interactLabel(target);
        return name.length() > 16 ? name.substring(0, 15) + "." : name;
    }


    private D2ModItems.Item ringItem(int index) {
        if (ringNpc) return null;
        if (ringTypes.get(index) != D2ModState.UNIT_ITEM) return null;
        Object target = ringTarget(index);
        if (target == null) target = ringTargets.get(index);
        D2ModItems details = modLink.items();
        D2ModItems.Item item = details != null && details.namesKnown ? details.find(((D2ModState.Thing) target).id) : null;
        return item != null && !item.name.trim().isEmpty() && item.category != D2ModItems.CAT_GOLD && D2ItemLabel.drawable(item.name)
            ? item : null;
    }


    private static final int RING_NAME_BUDGET = 150;

    private void drawRing(Canvas canvas) {
        if (ringTouch == null) return;
        if (ringNpc && ringTargets.isEmpty()) {

            if (mod != null && mod.npcTalking) drawRingPlate(canvas, hud, text(R.string.npc_ring_talking), ringCx, ringCy, menuTextScale(), false, -1);
            return;
        }
        D2Hud graphics = hud;
        float k = menuTextScale();
        D2RingChoice choice = ringChoice;
        int places = choice.places(), selected = choice.selected();
        strokePaint.setColor(Color.rgb(148, 126, 74));
        canvas.drawCircle(ringCx, ringCy, ringR, strokePaint);
        for (int p = 0; p < places; p++) {
            int at = choice.at(p);
            if (at == D2RingChoice.NONE) continue;
            double a = Math.toRadians(p * 360.0 / places);
            float x = ringCx + (float) Math.sin(a) * ringR, y = ringCy - (float) Math.cos(a) * ringR;
            boolean on = at == selected;
            if (on) {
                strokePaint.setColor(Color.rgb(255, 220, 120));
                canvas.drawLine(ringCx, ringCy, x, y, strokePaint);
            }
            if (at == D2RingChoice.BACK || at == D2RingChoice.NEXT) {
                drawRingPlate(canvas, graphics, text(at == D2RingChoice.BACK ? R.string.ring_back : R.string.ring_next), x, y, k, on, -1);
                continue;
            }
            D2ModItems.Item item = ringItem(at);
            int colour = item != null ? D2ItemLabel.colour(item.quality, item.category, item.ethereal, item.socketed, item.runeword) : -1;
            drawRingPlate(canvas, graphics, ringName(at, graphics), x, y, k, on, colour);
        }
        if (choice.paged()) {

            int[] page = choice.onPage();
            float y = Math.min(getHeight() - 16 * k, ringCy + ringR + 34 * k);
            drawRingPlate(canvas, graphics, text(R.string.ring_page, page[0] + 1, page[1], choice.count), ringCx, y, k, false, -1);
        }
    }


    private void drawRingPlate(Canvas canvas, D2Hud graphics, String text, float cx, float cy, float k, boolean on, int colour) {
        float w = graphics != null ? graphics.textWidth(text) * k : textPaint.measureText(text);
        rect.set(cx - w / 2 - 10 * k, cy - 13 * k, cx + w / 2 + 10 * k, cy + 13 * k);
        int fill = on ? Color.argb(240, 70, 56, 24) : Color.argb(225, 10, 8, 6);
        if (!drawGameFrame(canvas, rect, fill)) {
            fillPaint.setColor(fill);
            canvas.drawRect(rect, fillPaint);
        }
        if (graphics != null && colour >= 0) graphics.drawText(canvas, text, cx, cy, k, colour);
        else if (graphics != null) graphics.drawText(canvas, text, cx, cy, k);
        else {
            textPaint.setTextSize(13 * dp);
            canvas.drawText(text, cx, cy + 5 * dp, textPaint);
        }
    }


    private Object pickInteract() {
        List<Object> list = interactCandidates(ORDER_TAP);
        interactCount = list.size();
        interactIndex = 0;
        if (list.isEmpty()) {
            interactPinnedId = 0;
            return null;
        }
        if (interactPinnedId != 0) {
            for (int i = 0; i < list.size(); i++) {
                if (thingId(list.get(i)) == interactPinnedId) {
                    interactIndex = i;
                    return list.get(i);
                }
            }
            interactPinnedId = 0;
        }
        return list.get(0);
    }


    private void cycleInteract(int direction) {
        if (npcMenuOpen()) {

            pressKey(direction > 0 ? VK_DOWN : VK_UP);
            npcMenuCount.swipe(direction);
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            return;
        }
        List<Object> list = interactCandidates(ORDER_TAP);
        if (list.isEmpty()) return;
        int current = 0;
        long currentId = interactCandidate != null ? thingId(interactCandidate) : 0;
        for (int i = 0; i < list.size(); i++) if (thingId(list.get(i)) == currentId) current = i;
        int next = ((current + direction) % list.size() + list.size()) % list.size();
        interactCandidate = list.get(next);
        interactPinnedId = thingId(interactCandidate);
        interactCount = list.size();
        interactIndex = next;
        if (interactButton != null) interactButton.label = interactLabel(interactCandidate);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        if (mod != null) updateHighlight(mod);
        invalidate();
    }

    private String interactLabel(Object candidate) {
        if (candidate instanceof D2ModState.Monster) {

            String name = ((D2ModState.Monster) candidate).name;
            return name != null && !name.isEmpty() ? name : text(R.string.take_talk);
        }
        if (candidate instanceof D2ModState.Thing) {
            D2ModState.Thing thing = (D2ModState.Thing) candidate;
            if (mod != null && mod.items.contains(thing)) return thing.kind == D2ModState.ITEM_GOLD ? text(R.string.take_gold) : text(R.string.btn_take);
            switch (thing.kind) {
                case D2ModState.OBJECT_SHRINE: return text(R.string.take_shrine);
                case D2ModState.OBJECT_WELL: return text(R.string.take_well);
                case D2ModState.OBJECT_WAYPOINT: return text(R.string.take_waypoint);
                case D2ModState.OBJECT_PORTAL: return text(R.string.btn_portal);
                case D2ModState.OBJECT_ENTRANCE: return text(R.string.take_enter);
                case D2ModState.OBJECT_CORPSE: return text(R.string.take_corpse);
                default: return text(R.string.take_open);
            }
        }
        return text(R.string.btn_take);
    }






    private void drinkPotion(Button button) {
        boolean heal = button.id.equals("heal");
        D2ModState s = mod;
        if (button.id.equals("rejuv")) {

            if (s == null) return;
            modLink.drink(D2ModLink.DRINK_REJUV, potionMode != D2Settings.POTIONS_ALL);
            autoPotions.manual(D2AutoDrink.REJUV, SystemClock.uptimeMillis());
            lastPotion = "Verjüngung";
            return;
        }



        long now = SystemClock.uptimeMillis();
        int kind = heal ? D2AutoDrink.HEAL : D2AutoDrink.MANA;
        if (autoPotions.tapDropped(kind, now)) {
            D2Trace.log("potion tap dropped: automatic " + (heal ? "healing" : "mana") + " potion just before");
            return;
        }
        autoPotions.manual(kind, now);
        if (s == null || potionMode == D2Settings.POTIONS_KEYS) {
            pressKey(button.vk);
            lastPotion = "Taste";
            return;
        }
        boolean beltOnly = potionMode == D2Settings.POTIONS_BELT;
        lastPotion = beltOnly ? "Mod (Gürtel)" : "Mod";
        modLink.drink(heal ? D2ModLink.DRINK_HEAL : D2ModLink.DRINK_MANA, beltOnly);
    }


    private static final android.graphics.ColorMatrixColorFilter REJUV_TINT = new android.graphics.ColorMatrixColorFilter(new float[]{
        0.85f, 0.15f, 0, 0, 0,
        0.10f, 0.35f, 0, 0, 0,
        0.95f, 0.20f, 0.2f, 0, 0,
        0, 0, 0, 1, 0});


    static final long MERC_POTION_PAUSE_MS = 7000;
    private long mercPotionAt;





    private void autoMercPotion(D2ModState s, long now) {
        int threshold = mercPotionPct;

        if (threshold <= 0 || !modLink.singlePlayer()) return;
        int merc = modLink.mercLife();
        if (merc < 0 || merc >= threshold || now - mercPotionAt < MERC_POTION_PAUSE_MS) return;

        int rejuvs = 0;
        for (int slot : s.belt) if (slot == D2ModState.BELT_REJUV) rejuvs++;
        if (s.potionCountBelt[0] <= 0 && rejuvs < 2) return;
        mercPotionAt = now;
        modLink.potionForMercenary();
        lastPotion = "auto Söldner";
    }




    private void autoDrink(D2ModState s, long now) {


        boolean dead = modLink.heroDeathKnown() ? modLink.heroDead() : s.life <= 0;
        if (!s.staminaKnown || s.inTown || pauseWanted || s.isOpen(D2ModState.UI_ESCMENU) || dead || s.maxLife <= 0) return;
        autoPotions.lifePct = autoLifePct;

        autoPotions.idleLifePct = D2AutoDrink.idleThreshold(idle.running(), potionMode == D2Settings.POTIONS_KEYS, idle.drinkPercent());

        autoPotions.idleManaWanted = D2AutoDrink.idleManaWish(idle.running(), potionMode == D2Settings.POTIONS_KEYS, idle.manaWanted(now));
        autoPotions.rejuvPct = autoRejuvPct;
        autoPotions.manaPct = autoManaPct;
        autoPotions.inventoryToo = potionMode == D2Settings.POTIONS_ALL;
        autoPotions.staminaPotion = staminaMode == D2Settings.STAMINA_POTION;
        D2AutoDrink.Plan plan = autoPotions.decide(D2AutoDrink.Hero.of(s), now);
        sendAutoDrink(plan.life);


        if (plan.mercAllowed) autoMercPotion(s, now);
        sendAutoDrink(plan.mana);
        sendAutoDrink(plan.stamina);
    }

    private void sendAutoDrink(D2AutoDrink.Drink drink) {
        if (drink == null) return;
        modLink.drink(drink.kind, drink.beltOnly, drink.largest, drink.onlyBigger);
        lastPotion = new String[]{"auto Heil", "auto Mana", "auto Verjüngung", "auto Ausdauer"}[drink.kind];
        if (drink.largest) lastPotion = lastPotion + (drink.onlyBigger ? " (größer nachgelegt)" : " (größter)");
    }

    private void interact() {
        Object candidate = interactCandidate != null ? interactCandidate : pickInteract();
        if (candidate == null) return;
        startPickup(candidate, 2500);
    }


    private void startPickup(Object thing, long timeout) {
        releaseCurrent();
        action = ACTION_PICKUP;
        pickupForHeld = false;
        pickupId = thingId(thing);
        if (pickupId == interactPinnedId) interactPinnedId = 0;


        pickupObjectKind = thing instanceof D2ModState.Thing && mod != null && mod.objects.contains(thing) ? ((D2ModState.Thing) thing).kind : -1;
        pickupUnitType = thingType(thing);
        pickupStart = SystemClock.uptimeMillis();
        pickupEntrance = thing instanceof D2ModState.Thing && ((D2ModState.Thing) thing).kind == D2ModState.OBJECT_ENTRANCE
            && mod != null && mod.objects.contains(thing);
        if (pickupEntrance) timeout = Math.max(timeout, 8000);
        pickupCorpse = thing instanceof D2ModState.Thing && ((D2ModState.Thing) thing).kind == D2ModState.OBJECT_CORPSE
            && mod != null && mod.objects.contains(thing);
        if (pickupCorpse) timeout = Math.max(timeout, 6000);
        pickupLevel = mod != null ? mod.levelNo : -1;
        pickupClickAt = pickupStart;
        pickupTimeout = timeout;
        pickupDirX = dirX;
        pickupDirY = dirY;
        final long token = ++pickupToken;
        handler.postDelayed(() -> {
            if (action == ACTION_PICKUP && pickupToken == token) finishPickup();
        }, timeout);
        D2ModState state = mod;
        if (state != null && state.canActOnUnits()) {

            final int type = thingType(thing);
            final long id = pickupId;
            enqueue(waitMs(40), (now) -> {
                takeOrClick(type, id);
                return true;
            });
            return;
        }
        float[] g = thingPoint(thing);
        int[] p = gameToDesktop(g[0], g[1]);

        boolean altHeld = altDown;
        if (altHeld) sendKey(VK_MENU, true);
        D2ModState s = mod;
        boolean item = s != null && thing instanceof D2ModState.Thing && s.items.contains(thing);
        int[] offsets = thing instanceof D2ModState.Monster ? MONSTER_OFFSETS : (item ? ITEM_OFFSETS : OBJECT_OFFSETS);
        float baseY = thing instanceof D2ModState.Monster ? ((D2ModState.Monster) thing).y : ((D2ModState.Thing) thing).y;
        enqueue(moveTo(p[0], p[1]), settle(HOVER_SETTLE_MS), hoverSearch(pickupId, g[0], baseY, offsets));
        setLmb(true);
        enqueue(waitMs(50));
        setLmb(false);
        if (altHeld) sendKey(VK_MENU, false);
    }





    private Step hoverSearch(final long id, final float gx, final float gy, final int[] offsets) {
        final int[] index = {0};
        final long[] since = {0};
        return (now) -> {
            D2ModState s = mod;
            if (s == null) return true;
            if (since[0] == 0) since[0] = now;
            if (s.isHovered(id) && isAtTarget()) {
                hoverHits++;
                return true;
            }
            if (now - since[0] < 120) return false;
            if (++index[0] >= offsets.length) {
                hoverMisses++;
                return true;
            }
            int[] p = gameToDesktop(gx, gy + offsets[index[0]]);
            setTarget(p[0], p[1]);
            since[0] = now;
            return false;
        };
    }

    private void checkPickup(D2ModState s) {
        if (pickupEntrance) {


            long now = SystemClock.uptimeMillis();
            if (s.levelNo != pickupLevel || s.isPanelOpen() || now - pickupStart > pickupTimeout) {
                finishPickup();
                return;
            }
            if (now - pickupClickAt > 700 && s.canActOnUnits()) {
                pickupClickAt = now;
                takeOrClick(D2ModState.UNIT_TILE, pickupId);
            }
            return;
        }
        if (pickupCorpse && SystemClock.uptimeMillis() - pickupClickAt > 700 && s.canActOnUnits()) {
            pickupClickAt = SystemClock.uptimeMillis();
            takeOrClick(D2ModState.UNIT_PLAYER, pickupId);
        }

        if (D2HeldAttack.pickupYields(pickupForHeld, pickTarget(pressSkill(true)) != null)) {
            D2Trace.log("auto gold: a monster in reach, the held attack takes over");
            finishPickup();
            return;
        }

        boolean gone = !D2UsedObjects.isReported(s, pickupUnitType, pickupId);
        if (gone || s.isPanelOpen() || SystemClock.uptimeMillis() - pickupStart > pickupTimeout) finishPickup();
    }


    private void takeOrClick(int type, long id) {
        if (modLink.canTake()) modLink.take(type, id);
        else modLink.unit(D2ModLink.BUTTON_LEFT, D2ModLink.PHASE_CLICK, type, id);
    }

    private void finishPickup() {
        if (action != ACTION_PICKUP) return;
        long now = SystemClock.uptimeMillis();
        usedObjects.expire(now);

        long leftOutUntil = D2UsedObjects.untilAfterTry(pickupObjectKind,
            D2UsedObjects.isReported(mod, D2ModState.UNIT_OBJECT, pickupId), now);
        if (leftOutUntil != D2UsedObjects.NONE) {
            usedObjects.use(pickupObjectKind, pickupId, leftOutUntil);
            if (pickupObjectKind == D2ModState.OBJECT_CHEST) {
                D2Trace.log("pickup: chest " + pickupId + " still closed, left out " + D2UsedObjects.FAILED_TRY_MS + " ms");
            }
        }
        pickupObjectKind = -1;
        pickupUnitType = -1;
        pickupId = 0;
        pickupForHeld = false;
        pickupEntrance = false;
        pickupCorpse = false;
        setLmb(false);
        resumeAfterAction();
        resumeHeldButton();
    }







    private void resumeHeldButton() {
        if (action != ACTION_NONE && action != ACTION_WALK) return;
        D2ModState s = mod;
        if (s == null || s.isPanelOpen() || s.npcTalking || !controlsVisible || editMode || !windowFocused || pauseWanted) {
            D2Trace.log("pickup done: held button not resumed (window, menu or controls hidden)");
            return;
        }
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (touch.role != ROLE_BUTTON || touch.button == null || !touch.button.pressed) continue;
            if (touch.button.kind != KIND_ATTACK && touch.button.kind != KIND_SKILL) continue;
            D2Trace.log("pickup done: " + touch.button.id + " still held, resumes");
            onButtonDown(touch.button, touch);
            return;
        }
    }





    private void refillBelt(D2ModState s, long now) {
        if (pauseWanted || s.life == 0 || s.isOpen(D2ModState.UI_ESCMENU) || s.isOpen(D2ModState.UI_STASH) || s.isOpen(D2ModState.UI_NPCSHOP)
            || s.isOpen(D2ModState.UI_TRADE) || s.isOpen(D2ModState.UI_CUBE)) return;
        int kind = autoItems.beltRefill(s, modLink.items(), now);
        if (kind >= 0) modLink.belt(kind);
    }






    private void autoPickGold(D2ModState s) {
        for (int i = 0; i < touches.size(); i++) if (touches.valueAt(i).role == ROLE_DIRECT) return;

        boolean held = action != ACTION_NONE && action != ACTION_WALK;
        boolean walking = action == ACTION_WALK || attackWalking;
        long now = SystemClock.uptimeMillis();
        for (Iterator<Map.Entry<Long, Long>> it = pickupTried.entrySet().iterator(); it.hasNext(); ) {
            if (now > it.next().getValue()) it.remove();
        }
        D2ModItems items = modLink.items();
        D2ModState.Thing best = null;
        double bestDistance = GOLD_RADIUS;
        for (D2ModState.Thing item : s.items) {
            if (held && item.kind != D2ModState.ITEM_GOLD) continue;
            if (pickupTried.containsKey(item.id) || !autoItems.wants(s, item, items)) continue;
            float dx = item.x - s.playerX, dy = item.y - s.playerY;
            double distance = isoDistance(dx, dy);
            if (distance > bestDistance) continue;
            if (walking && distance > 20 && (dx * dirX + dy * dirY) / Math.hypot(dx, dy) < Math.cos(Math.toRadians(75))) continue;
            best = item;
            bestDistance = distance;
        }
        if (best == null) return;
        pickupTried.put(best.id, now + (best.kind == D2ModState.ITEM_GOLD ? 5000 : 20000));
        if (best.kind != D2ModState.ITEM_GOLD && items != null && items.find(best.id) != null) D2Trace.log("auto pickup " + items.find(best.id).code);
        if (held) D2Trace.log("auto gold while attack is held, nobody in reach");
        startPickup(best, 1500);
        pickupForHeld = held;
    }






    private boolean heldWalksNobody() {
        boolean button = false;
        for (int i = 0; i < touches.size() && !button; i++) {
            Touch touch = touches.valueAt(i);
            button = touch.role == ROLE_BUTTON && touch.button != null && touch.button.pressed && touch.button.kind == KIND_ATTACK;
        }
        return D2HeldAttack.goldWhileHeld(action == ACTION_ATTACK, attackWalking, attackTargetId, button);
    }

    private D2ModState.Monster findMonster(long id) {
        D2ModState s = mod;
        if (s == null || id <= 0) return null;
        for (D2ModState.Monster m : s.monsters) if (m.id == id) return m;
        return null;
    }

    private int thingType(Object thing) {
        if (thing instanceof D2ModState.Monster) return D2ModState.UNIT_MONSTER;
        if (mod != null && mod.items.contains(thing)) return D2ModState.UNIT_ITEM;
        if (thing instanceof D2ModState.Thing && ((D2ModState.Thing) thing).kind == D2ModState.OBJECT_ENTRANCE) return D2ModState.UNIT_TILE;
        if (thing instanceof D2ModState.Thing && ((D2ModState.Thing) thing).kind == D2ModState.OBJECT_CORPSE) return D2ModState.UNIT_PLAYER;
        return D2ModState.UNIT_OBJECT;
    }

    private static long thingId(Object thing) {
        if (!D2NpcRing.isUnit(thing)) return D2ModLink.NO_UNIT;
        return thing instanceof D2ModState.Monster ? ((D2ModState.Monster) thing).id : ((D2ModState.Thing) thing).id;
    }


    private float[] thingPoint(Object thing) {
        if (thing instanceof D2ModState.Monster) {
            D2ModState.Monster m = (D2ModState.Monster) thing;
            return new float[]{m.x, m.y - TARGET_LIFT};
        }
        D2ModState.Thing t = (D2ModState.Thing) thing;
        boolean item = mod != null && mod.items.contains(t);
        return new float[]{t.x, t.y - (item ? 3 : 10)};
    }


    private static double isoDistance(float dx, float dy) {
        return Math.hypot(dx, 2 * dy);
    }

    private int[] monsterPoint(D2ModState.Monster m) {
        return gameToDesktop(m.x, m.y - TARGET_LIFT);
    }





    private int[] gameToDesktop(float gx, float gy) {
        D2ModState s = mod;
        gx += positionShiftX;
        gy += positionShiftY;
        int dw = xServer.screenInfo.width, dh = xServer.screenInfo.height;
        float x, y;
        if (calibrated) {
            x = calibKx * gx + calibBx;
            y = calibKy * gy + calibBy;
        }
        else if (s != null && s.gameWidth > 0 && s.gameHeight > 0) {
            float k = Math.min(dw / (float) s.gameWidth, dh / (float) s.gameHeight);
            x = (dw - s.gameWidth * k) / 2 + gx * k;
            y = (dh - s.gameHeight * k) / 2 + gy * k;
        }
        else {
            x = gx;
            y = gy;
        }
        return new int[]{Math.max(0, Math.min(dw - 1, Math.round(x))), Math.max(0, Math.min(dh - 1, Math.round(y)))};
    }

    private float[] desktopToGame(float x, float y, D2ModState s) {
        if (calibrated) return new float[]{(x - calibBx) / calibKx, (y - calibBy) / calibKy};
        int dw = xServer.screenInfo.width, dh = xServer.screenInfo.height;
        if (s.gameWidth <= 0 || s.gameHeight <= 0) return new float[]{x, y};
        float k = Math.min(dw / (float) s.gameWidth, dh / (float) s.gameHeight);
        return new float[]{(x - (dw - s.gameWidth * k) / 2) / k, (y - (dh - s.gameHeight * k) / 2) / k};
    }





    private void calibrate(D2ModState s, long now) {
        if (s.gameWidth != calibratedWidth || s.gameHeight != calibratedHeight) {
            calibratedWidth = s.gameWidth;
            calibratedHeight = s.gameHeight;
            calibrationCount = calibrationNext = 0;
            calibrated = false;
        }
        int px = xServer.pointer.getX(), py = xServer.pointer.getY();
        if (px == lastPointerX && py == lastPointerY) stableUpdates++;
        else {
            stableUpdates = 0;
            lastPointerX = px;
            lastPointerY = py;
        }
        checkPointerSync(s, now);
        if (stableUpdates != 2 || syncPending(now)) return;
        float[] sample = calibration[calibrationNext];
        sample[0] = px;
        sample[1] = py;
        sample[2] = s.mouseX;
        sample[3] = s.mouseY;
        calibrationNext = (calibrationNext + 1) % calibration.length;
        calibrationCount = Math.min(calibrationCount + 1, calibration.length);
        float[] fx = fitLine(0, 2), fy = fitLine(1, 3);
        calibrated = fx != null && fy != null;
        if (calibrated) {
            calibKx = fx[0];
            calibBx = fx[1];
            calibKy = fy[0];
            calibBy = fy[1];
        }
    }






    private float[] fitLine(int desktopIndex, int gameIndex) {
        if (calibrationCount < 4) return null;
        boolean[] use = new boolean[calibrationCount];
        java.util.Arrays.fill(use, true);
        int left = calibrationCount;
        while (true) {
            float[] fit = fitLine(desktopIndex, gameIndex, use);
            if (fit == null) return null;

            int worst = -1;
            double worstError = 4 * fit[0];
            for (int i = 0; i < calibrationCount; i++) {
                if (!use[i]) continue;
                double error = Math.abs(fit[0] * calibration[i][gameIndex] + fit[1] - calibration[i][desktopIndex]);
                if (error > worstError) {
                    worstError = error;
                    worst = i;
                }
            }
            if (worst < 0) return fit;
            if (--left < 4 || left * 3 < calibrationCount * 2) return null;
            use[worst] = false;
        }
    }

    private float[] fitLine(int desktopIndex, int gameIndex, boolean[] use) {
        double sumG = 0, sumD = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        int n = 0;
        for (int i = 0; i < calibrationCount; i++) {
            if (!use[i]) continue;
            n++;
            sumG += calibration[i][gameIndex];
            sumD += calibration[i][desktopIndex];
            min = Math.min(min, calibration[i][gameIndex]);
            max = Math.max(max, calibration[i][gameIndex]);
        }
        if (n < 4 || max - min < 120) return null;
        double meanG = sumG / n, meanD = sumD / n, cov = 0, var = 0;
        for (int i = 0; i < calibrationCount; i++) {
            if (!use[i]) continue;
            double g = calibration[i][gameIndex] - meanG;
            cov += g * (calibration[i][desktopIndex] - meanD);
            var += g * g;
        }
        double k = cov / var, b = meanD - k * meanG;
        if (k < 0.5 || k > 6) return null;
        return new float[]{(float) k, (float) b};
    }

    private float[] gameToView(float gx, float gy) {
        int[] d = gameToDesktop(gx, gy);
        return new float[]{d[0] * aspect + offsetX, d[1] * aspect + offsetY};
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        D2LaunchGate.screenUp();
        modUpdatePosted = false;
        modLink.start();
        handler.postDelayed(revealStart, D2StartupControls.NO_MOD_MS);
    }


    public void setModExpected(boolean expected) {
        modExpected = expected;
        revealAtStart();
    }





    private void revealAtStart() {
        if (startDone) return;
        if (modLink.isConnected()) {
            startDone = true;
            return;
        }
        handler.removeCallbacks(revealStart);

        if (!startClock.focused()) return;
        long since = startClock.sinceStart(SystemClock.uptimeMillis());
        if (!D2StartupControls.reveal(modExpected, modLink.isAlive(), graphicsDone, since)) {

            long next = D2StartupControls.NO_MOD_MS - since;
            handler.postDelayed(revealStart, next > 0 ? next : 2000);
            return;
        }
        startDone = true;
        D2Trace.log("start: game buttons shown without the mod");
        if (!controlsVisible && hiddenByPanel && !editMode) setControlsVisible(true, false);

        appliedUiKey = UI_UNKNOWN;
        invalidate();
    }








    public boolean onKeyEvent(KeyEvent event) {
        if (inputBox.key(event)) return true;
        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_F7 && keyCode != KeyEvent.KEYCODE_F8) return false;
        if (pauseWanted) return true;
        if (event.getAction() != KeyEvent.ACTION_DOWN && event.getAction() != KeyEvent.ACTION_UP) return true;
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        InputDevice device = event.getDevice();
        if (device != null) triggerDevices.put(device.getId(), device.getName());
        if (down && event.getRepeatCount() > 0) return true;
        if (down) {
            if (debug) logEvent("Taste F" + (keyCode == KeyEvent.KEYCODE_F7 ? 7 : 8), device);
            lastTriggerDownTime = SystemClock.uptimeMillis();
            cancelTriggerTouches(lastTriggerDownTime);
        }
        Button trigger = keyCode == KeyEvent.KEYCODE_F7 ? leftTrigger : rightTrigger;
        int set = keyCode == KeyEvent.KEYCODE_F7 ? triggerLeftAction : triggerRightAction;
        if (editMode) {


            if (down && set == D2TriggerAction.SKILL) startAssign(trigger);
            else if (down) showNote(text(R.string.note_trigger_action, trigger.label, triggerActionName(set)));
            return true;
        }
        if (assignButton != null && down) return true;
        runAction(trigger, set, down);
        invalidate();
        return true;
    }






    private void runAction(Button owner, int set, boolean down) {
        if (!down) {
            int held = owner.heldAction;
            owner.heldAction = D2TriggerAction.NOTHING;
            if (held == D2TriggerAction.KEY) sendKey(owner.heldVk, true);
            else if (held == D2TriggerAction.SKILL || D2TriggerAction.isSkillKey(held)) endSkill(owner, false);
            else if (held == D2TriggerAction.ATTACK) releaseAttack(owner, false);
            else if (held == D2TriggerAction.NAMES) {
                labelsHeld = false;
                syncLabels();
            }
            return;
        }
        int act = D2TriggerAction.resolve(set, controlsVisible, D2SpieltestPort.playing());

        int vk = D2TriggerAction.isSkillKey(set) ? VK_F1 + D2TriggerAction.skillKeyIndex(set) : owner.vk;
        owner.heldAction = D2TriggerAction.held(act) ? act : D2TriggerAction.NOTHING;
        owner.heldVk = vk;
        if (act == D2TriggerAction.KEY) sendKey(vk, false);
        else if (act == D2TriggerAction.SKILL || D2TriggerAction.isSkillKey(act)) {
            Button slot = D2TriggerAction.isSkillKey(act) ? skillKeyButton(D2TriggerAction.skillKeyIndex(act)) : owner;
            if (slot != null && isFreeSlot(slot)) {

                owner.heldAction = D2TriggerAction.NOTHING;
                showNote(text(R.string.note_slot_free, slot.label, text(R.string.btn_keys)));
            }
            else startSkill(vk, owner);
        }
        else if (act == D2TriggerAction.ATTACK) pressAttack(owner);
        else if (act == D2TriggerAction.ATTACK_OR_TAKE) {
            if (attackOrTake()) {
                owner.heldAction = D2TriggerAction.ATTACK;
                padAttackOwner = aByPadChoice ? owner : null;
                pressAttack(owner);
            }
            else owner.heldAction = D2TriggerAction.NOTHING;
        }
        else if (act == D2TriggerAction.NAMES) {

            labelsHeld = true;
            syncLabels();
        }
        else triggerAction(act);
    }





    private boolean attackOrTake() {
        D2ModState s = mod;
        D2AimAssist aim = aimNow();
        boolean inTown = s != null && s.inTown;
        aAttackTarget = null;
        aByPadChoice = false;

        if (padChoiceApplies()) {
            D2PadTarget.Choice choice = padChoice(padPreviewOn ? padPreviewKey : D2PadTarget.NONE);
            D2Trace.log("attack or take: " + (choice.what == D2PadTarget.TAKE ? "take" : "attack") + " (" + choice.why
                + (choice.target != null ? ", " + thingType(choice.target) + " #" + thingId(choice.target) : "") + ")");
            if (choice.what == D2PadTarget.TAKE) {
                startPickup(choice.target, 2500);
                return false;
            }
            aAttackTarget = choice.target instanceof D2ModState.Monster ? (D2ModState.Monster) choice.target : null;
            aByPadChoice = true;
            return true;
        }
        Object close = closeTakeCandidate();
        int skill = pressSkill(true);
        boolean picked = pickTarget(skill) != null, inReach = monsterInAttackReach(skill);
        int what = D2Gamepad.attackOrTake(s != null && s.npcTalking, npcMenuOpen(), inTown, aim.on, attackRightSkill() >= 0,
            picked, inReach, close != null);


        String at = "";
        if (close != null && s != null) {
            float cx = close instanceof D2ModState.Monster ? ((D2ModState.Monster) close).x : ((D2ModState.Thing) close).x;
            float cy = close instanceof D2ModState.Monster ? ((D2ModState.Monster) close).y : ((D2ModState.Thing) close).y;
            at = ", at " + Math.round(isoDistance(cx - s.playerX, cy - s.playerY)) + " px";
        }
        D2Trace.log("attack or take: " + what + " (picked " + picked + ", monster in reach " + inReach + ", close "
            + (close != null) + ", aim " + aim.on + at + ")");
        switch (what) {
            case D2Gamepad.A_SKIP_TALK:
                pressKey(VK_ESCAPE);
                return false;
            case D2Gamepad.A_TAKE_LINE:
                pressKey(VK_RETURN);
                return false;
            case D2Gamepad.A_TAKE:
                startPickup(close, 2500);
                return false;
            default:
                return true;
        }
    }





    private boolean padChoiceApplies() {
        D2ModState s = mod;
        return s != null && !s.inTown && !s.npcTalking && !npcMenuOpen() && aimNow().on && attackRightSkill() < 0;
    }


    private boolean beltShown(D2ModState s) {
        return s.isOpen(D2WorldTap.UI_BELT) || beltRows.open();
    }


    private D2PadTarget.Choice padChoice(long previous) {
        D2ModState s = mod;
        D2AimAssist aim = aimNow();
        List<D2PadTarget.Candidate> monsters = new ArrayList<>(), takes = new ArrayList<>();
        for (D2ModState.Monster m : s.monsters) {
            if (m.isTownNpc() || idleIgnored.contains(m.id)) continue;
            float dx = m.x - s.playerX, dy = m.y - s.playerY;
            boolean behind = hasFacing && D2AimAssist.behind(dx, dy, faceX, faceY, aim.blindDeg);
            monsters.add(new D2PadTarget.Candidate(m, padKey(m), dx, dy, isoDistance(dx, dy), true, m.isSpecial(), behind));
        }
        for (Object thing : interactCandidates(ORDER_TAP)) {
            float x = thing instanceof D2ModState.Monster ? ((D2ModState.Monster) thing).x : ((D2ModState.Thing) thing).x;
            float y = thing instanceof D2ModState.Monster ? ((D2ModState.Monster) thing).y : ((D2ModState.Thing) thing).y;
            float dx = x - s.playerX, dy = y - s.playerY;
            D2PadTarget.Candidate take = new D2PadTarget.Candidate(thing, padKey(thing), dx, dy, isoDistance(dx, dy), false, false, false);
            take.first = thing instanceof D2ModState.Thing && ((D2ModState.Thing) thing).kind == D2ModState.OBJECT_CORPSE
                && s.objects.contains(thing);
            takes.add(take);
        }
        float h = s.gameHeight;
        return D2PadTarget.choose(monsters, takes, joystickActive && hasDirection, dirX, dirY, Math.cos(Math.toRadians(aim.coneDeg)),
            h * aim.reachPct(true) / 100f, h * aim.reachPct(meleeReach(pressSkill(true))) / 100f,
            h * D2Gamepad.TAKE_REACH_OF_HEIGHT, previous);
    }


    private boolean padAttackHeld() {
        Button owner = padAttackOwner;
        return owner != null && action == ACTION_ATTACK && owner.heldAction == D2TriggerAction.ATTACK;
    }


    private boolean padLayerHasAttackOrTake() {
        for (int act : padMap[padLayer2 ? 1 : 0]) if (act == D2TriggerAction.ATTACK_OR_TAKE) return true;
        return false;
    }


    private long padKey(Object thing) {
        return (long) thingType(thing) << 32 | (thingId(thing) & 0xFFFFFFFFL);
    }


    private void updatePadPreview() {
        boolean on = padActive && controlsVisible && !editMode && (action == ACTION_NONE || action == ACTION_WALK) && padChoiceApplies()
            && padLayerHasAttackOrTake();
        D2PadTarget.Choice choice = on ? padChoice(padPreviewOn ? padPreviewKey : D2PadTarget.NONE) : null;
        padPreview = choice != null ? choice.target : null;
        padPreviewKey = choice != null ? choice.key : D2PadTarget.NONE;
        padPreviewOn = on;
    }







    private Object closeTakeCandidate() {
        D2ModState s = mod;
        if (s == null) return null;
        for (Object thing : interactCandidates(ORDER_TAP)) {
            float x = thing instanceof D2ModState.Monster ? ((D2ModState.Monster) thing).x : ((D2ModState.Thing) thing).x;
            float y = thing instanceof D2ModState.Monster ? ((D2ModState.Monster) thing).y : ((D2ModState.Thing) thing).y;
            if (D2Gamepad.aTakes(isoDistance(x - s.playerX, y - s.playerY), s.gameHeight)) return thing;
        }
        return null;
    }





    private boolean monsterInAttackReach(int skill) {
        D2ModState s = mod;
        if (s == null || s.inTown) return false;
        float range = s.gameHeight * aimNow().reachPct(meleeReach(skill)) / 100f;
        for (D2ModState.Monster m : s.monsters) {
            if (m.isTownNpc() || idleIgnored.contains(m.id)) continue;
            if (isoDistance(m.x - s.playerX, m.y - s.playerY) <= range) return true;
        }
        return false;
    }


    private Button skillKeyButton(int index) {
        if (index >= 0 && index < 4) return gameButton("F" + (index + 1));
        if (index == 4) return skillKeyF5;
        if (index == 5) return skillKeyF6;
        if (index == 6) return leftTrigger;
        return index == 7 ? rightTrigger : null;
    }




    public boolean onPadKey(KeyEvent event) {
        return pad.onKey(event);
    }


    public boolean onPadMotion(MotionEvent event) {
        return pad.onMotion(event);
    }


    boolean padEnabled() {
        return padEnabled;
    }

    int padDeadzone() {
        return padDeadzone;
    }


    int padAction(int button, boolean layer2) {
        return padMap[layer2 ? 1 : 0][button];
    }





    void padUsed(boolean used) {
        if (used == padActive) return;
        padActive = used;
        if (used) {
            interactPinnedId = 0;
            interactCandidate = null;
        }
        D2Trace.log(used ? "pad: in use, touch controls hidden" : "pad: touch controls back");
        invalidate();
    }


    private boolean padHidesControls() {
        return D2Gamepad.hidesControls(padActive, editMode, idle.phase() != D2Idle.Phase.OFF, D2SpieltestPort.playing(), false);
    }





    boolean padResume(int button) {
        if (recipes.pad(button)) return true;
        if (!pauseWanted) return false;
        boolean offered = resumeHint.exit().active();
        if (resumeHint.exit().pad(button, SystemClock.uptimeMillis())) setPaused(false);
        return button == D2Gamepad.A || button == D2Gamepad.START || offered;
    }





    void padButton(int button, int set, boolean down) {
        Button owner = padOwners[button];
        if (owner == null) {
            owner = new Button("pad" + D2Gamepad.NAMES[button], D2Gamepad.NAMES[button], KIND_SKILL, 0, GROUP_GAME, false);
            padOwners[button] = owner;
        }
        if (down && editMode) {
            Button slot = D2TriggerAction.isSkillKey(set) ? skillKeyButton(D2TriggerAction.skillKeyIndex(set)) : null;
            if (slot != null) startAssign(slot);
            else showNote(text(R.string.note_trigger_action, owner.label, triggerActionName(set)));
            return;
        }
        if (down && button == D2Gamepad.B && padBack(owner)) return;
        if (!down && (owner.heldAction == HELD_LEFT_CLICK || owner.heldAction == HELD_RIGHT_CLICK)) {
            if (owner.heldAction == HELD_LEFT_CLICK) setLmb(false);
            else setRmb(false);
            owner.heldAction = D2TriggerAction.NOTHING;
            return;
        }


        if (button == D2Gamepad.A && padRingOpen() && owner != padRingOwner) {
            if (down) {
                padRingSwallowA = true;
                padRingTakenByA = true;
                padRingTake("A");
            }
            return;
        }
        if (!down && button == D2Gamepad.A && padRingSwallowA) {
            padRingSwallowA = false;
            return;
        }
        int click = D2Gamepad.pointerClick(button);
        if (down && click != D2Gamepad.CLICK_NONE && padPointerMode()) {
            padClick(owner, click);
            return;
        }
        if (!down && owner == padRingOwner) {
            padTakeRelease(owner);
            return;
        }
        if (down && (pauseWanted || assignButton != null)) return;
        if (down && padTakeHolds(set)) {
            padTakePress(owner, set);
            return;
        }
        D2Trace.log("pad " + owner.label + (down ? " down: action " + set : " up"));
        runAction(owner, set, down);
        invalidate();
    }


    private Button padRingOwner;
    private int padRingSet;
    private Touch padRingTouch;
    private final Runnable padRingHold = this::openPadRing;


    private boolean padTakeHolds(int set) {
        int act = D2TriggerAction.resolve(set, controlsVisible, false);
        D2ModState s = mod;
        boolean inTown = s != null && s.inTown;
        boolean close = act == D2TriggerAction.ATTACK_OR_TAKE && inTown && closeTakeCandidate() != null;
        return D2Gamepad.holdOpensRing(act, inTown, close, s != null && s.npcTalking, npcMenuOpen());
    }

    private void padTakePress(Button owner, int set) {
        D2Trace.log("pad " + owner.label + " down: take, the ring if held");
        handler.removeCallbacks(padRingHold);
        padRingOwner = owner;
        padRingSet = set;
        padRingTakenByA = false;
        owner.heldAction = D2TriggerAction.NOTHING;
        handler.postDelayed(padRingHold, RING_HOLD_MS);
    }


    private void openPadRing() {
        if (padRingOwner == null || ringTouch != null || interactButton == null) return;
        if (!padMayTake()) return;
        Touch touch = new Touch();
        touch.deviceId = D2Idle.DEVICE_ID - 1;
        touch.button = interactButton;
        if (!openRing(touch)) return;
        touch.x = ringCx;
        touch.y = ringCy;
        ringStartX = ringCx;
        ringStartY = ringCy;
        padRingTouch = touch;
        padStick(false, false, dirX, dirY);
    }


    private boolean padRingSwallowA;

    private boolean padRingTakenByA;


    private void padRingTake(String by) {
        Object chosen = ringTarget(ringChoice.target());
        if (ringNpc) {
            padRingTouch = null;
            npcRingTake(chosen, "pad " + by);
            return;
        }
        closeRing();
        padRingTouch = null;
        D2Trace.log("pad " + by + ": ring " + (chosen != null ? "takes " + thingId(chosen) : "closed, nothing chosen"));
        if (chosen != null) {
            performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            startPickup(chosen, 2500);
        }
    }


    private void padTakeRelease(Button owner) {
        handler.removeCallbacks(padRingHold);
        padRingOwner = null;


        if (padRingTakenByA) {
            padRingTakenByA = false;
            return;
        }
        if (padRingTouch != null && ringTouch == padRingTouch) {
            padRingTake(owner.label);
            return;
        }
        if (padRingTouch != null) {
            padRingTouch = null;
            return;
        }
        if (!padMayTake()) return;
        D2Trace.log("pad " + owner.label + ": take (short)");
        if (D2TriggerAction.resolve(padRingSet, controlsVisible, false) == D2TriggerAction.TAKE) triggerAction(D2TriggerAction.TAKE);
        else {
            Object close = closeTakeCandidate();
            if (close != null) startPickup(close, 2500);
        }
        invalidate();
    }



    private final D2InputBoxControl inputBox = new D2InputBoxControl(new D2InputBoxControl.Host() {
        @Override public void invalidate() { D2TouchOverlay.this.invalidate(); }
        @Override public void haptic() { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); }
        @Override public void schedule(Runnable r, long delayMs) { handler.postDelayed(r, delayMs); }
        @Override public void cancel(Runnable r) { handler.removeCallbacks(r); }
        @Override public void keyboard(boolean show) { inputBoxKeyboard(show); }
    });

    private boolean inputBoxForShop;

    private com.winlator.d2.test.Zustand.Figur shopFigur;
    private long shopFigurAt;


    private void drawInputBox(Canvas canvas, D2ModState s, long now) {
        if (!inputBox.open()) {
            inputBox.updateLayer(modLink, null, now);
            return;
        }
        if (s == null || s.gameWidth <= 0) return;
        inputBox.layout(gameMapping(s), s.gameWidth, s.gameHeight, keyboardTop());
        if (!inputBox.updateLayer(modLink, hud != null ? hud.layerArt() : null, now)) inputBox.draw(canvas, hud);
        postInvalidateDelayed(250);
    }


    private static final long BUY_PROGRESS_MAX_MS = 95000;
    private long buyProgressUntil;
    private final Runnable buyProgressTimeout = this::updateBuyProgress;


    private void updateBuyProgress() {
        D2InputBox p = inputBox.progressBox();
        if (p == null) return;
        if (shopButton == null || SystemClock.uptimeMillis() > buyProgressUntil) {
            D2Trace.log("buy progress: closed after " + BUY_PROGRESS_MAX_MS + " ms at " + p.done() + " / " + p.total());
            inputBox.close();
            inputBoxForShop = false;
            return;
        }
        int bought = shopButton.wareBought();
        if (bought >= 0 && bought != p.done()) {
            p.progress(bought);
            invalidate();
        }
        if (shopButton.idle()) {
            D2Trace.log("buy progress: closed at " + p.done() + " / " + p.total());
            handler.removeCallbacks(buyProgressTimeout);
            inputBox.close();
            inputBoxForShop = false;
        }
    }





    private float keyboardTop() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return 0;
        android.view.WindowInsets wi = getRootWindowInsets();
        if (wi == null || !wi.isVisible(android.view.WindowInsets.Type.ime())) return 0;
        int ime = wi.getInsets(android.view.WindowInsets.Type.ime()).bottom;
        if (ime <= 0) return 0;
        int[] at = new int[2];
        getLocationInWindow(at);
        return getRootView().getHeight() - ime - at[1];
    }


    public boolean inputBoxBack() {
        return inputBox.back();
    }


    private void inputBoxKeyboard(boolean show) {
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (show) {
            setFocusableInTouchMode(true);
            requestFocus();
            imm.restartInput(this);
            imm.showSoftInput(this, 0);
        }
        else imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return inputBox.open();
    }

    @Override
    public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo info) {
        return inputBox.open() ? inputBox.inputConnection(this, info) : null;
    }


    private com.winlator.d2.test.Zustand.Figur.Teil wareUnderPointer() {
        D2ModState s = mod;
        if (s == null || !s.isOpen(D2ModState.UI_NPCSHOP) || modLink == null) return null;
        D2ModState state = s;
        com.winlator.d2.test.Zustand.Figur f = figur7();
        return D2ShopGestures.wareAt(f, state.hoverItemPlace, state.hoverItemCategory, state.hoverItemX, state.hoverItemY);
    }


    private com.winlator.d2.test.Zustand.Figur figur7() {
        long at = modLink.figurAt();
        if (at != shopFigurAt) {
            shopFigurAt = at;
            shopFigur = null;
            byte[] data = modLink.figur();
            try {
                String json = data != null ? D2SpieltestPort.figurJson(data, data.length, SystemClock.uptimeMillis()) : null;
                if (json != null) shopFigur = com.winlator.d2.test.Zustand.Figur.parse(new org.json.JSONObject(json));
            }
            catch (org.json.JSONException e) {
                D2Trace.log("type 7 not read: " + e.getMessage());
            }
        }
        return shopFigur;
    }






    private boolean openBuyBox(String by) {
        com.winlator.d2.test.Zustand.Figur.Teil ware = wareUnderPointer();
        if (ware == null || !D2ShopGestures.endless(ware.kategorie) || shopButton == null || !shopButton.idle()) return false;
        D2ModItems items = modLink.items();
        boolean tp = ware.kategorie == D2ModItems.CAT_TP_SCROLL, scroll = tp || ware.kategorie == D2ModItems.CAT_ID_SCROLL;
        int charges = items == null || !scroll ? -1 : tp ? items.tpTome : items.idTome;
        int limit = items == null ? 20 : tp ? items.tpTomeLimit() : (items.idTomeMax > 0 ? items.idTomeMax : 20);
        int cap = D2ShopGestures.capacity(ware, items != null ? items.freeCells() : 0, charges, limit);
        String gameName = hud != null && hud.layerArt() != null ? hud.layerArt().gameText(ware.code) : null;
        final String name = gameName != null ? gameName : ware.code;
        final long id = ware.id;
        D2InputBox box = D2InputBox.number(text(R.string.inputbox_buy_title), name, 1, cap, D2ShopGestures.start(ware, cap),
            new D2InputBox.Listener() {
                @Override public void ok(int number, String t) {
                    inputBoxForShop = false;
                    if (!shopButton.buyWare(id, name, number)) showNote(text(R.string.inputbox_buy_busy));
                    else if (number > 1) {
                        buyProgressUntil = SystemClock.uptimeMillis() + BUY_PROGRESS_MAX_MS;
                        inputBoxForShop = true;
                        inputBox.show(D2InputBox.progress(text(R.string.inputbox_buying_title), name, number));

                        handler.removeCallbacks(buyProgressTimeout);
                        handler.postDelayed(buyProgressTimeout, BUY_PROGRESS_MAX_MS + 100);
                    }
                }

                @Override public void cancel() {
                    inputBoxForShop = false;
                }
            }).hint(D2ShopGestures.stacks(ware.kategorie) ? null : text(R.string.inputbox_room, cap))
            .labels(text(R.string.inputbox_ok), text(R.string.inputbox_back));
        D2Trace.log("ware " + ware.code + " (" + ware.id + ") held by " + by + ": how many, at most " + cap);
        inputBoxForShop = true;
        inputBox.show(box);
        return true;
    }


    private static final long HOVER_CONFIRM_MS = 400;





    private Step whenHovered(int place, int x, int y, String what, Runnable action) {
        final long[] start = {0};
        final D2ModState[] seen = {null, null};
        return (now) -> {
            D2ModState n = mod;
            if (start[0] == 0) {
                start[0] = now;
                seen[0] = n;
                return false;
            }
            if (n != null && n != seen[0] && n != seen[1]) {
                if (seen[1] == null) seen[1] = n;
                else if (n.hoverItemPlace == place && n.hoverItemX == x && n.hoverItemY == y) {
                    action.run();
                    return true;
                }
                else {
                    seen[0] = seen[1];
                    seen[1] = n;
                }
            }
            if (now - start[0] > HOVER_CONFIRM_MS) {
                D2Trace.log("double tap: " + what + " dropped, the pointer does not show the item at place " + place + " cell "
                    + x + "," + y);
                return true;
            }
            return false;
        };
    }


    private boolean sellByPacket() {
        D2ModItems items = modLink.items();
        return items != null && items.sellPossible;
    }


    private boolean moveByPacket() {
        int caps = modLink.capabilities();
        return caps >= 0 && (caps & D2Points.CAP_EQUIP) != 0;
    }






    private void moveItem(D2ModState s) {
        int place = s.hoverItemPlace, to = place == D2ItemMoves.PLACE_STASH ? D2ItemMoves.PLACE_INVENTORY : D2ItemMoves.PLACE_STASH;
        com.winlator.d2.test.Zustand.Figur f = figur7();
        com.winlator.d2.test.Zustand.Figur.Teil item = D2ItemMoves.itemAt(f, place, s.hoverItemX, s.hoverItemY);
        int[] grids = modLink.grids();
        int gw = grids == null ? 0 : grids[to == D2ItemMoves.PLACE_STASH ? D2Points.GRIDS_STASH_W : D2Points.GRIDS_INV_W];
        int gh = grids == null ? 0 : grids[to == D2ItemMoves.PLACE_STASH ? D2Points.GRIDS_STASH_H : D2Points.GRIDS_INV_H];
        if (item != null && !D2ItemMoves.fits(f, to, gw, gh, Math.max(1, item.breite), Math.max(1, item.hoehe))) {
            D2Trace.log("double tap: move " + item.code + " not sent, no room (" + gw + " x " + gh + ")");
            showNote(text(to == D2ItemMoves.PLACE_STASH ? R.string.move_no_room_stash : R.string.move_no_room_inventory));
            return;
        }
        D2Trace.log("double tap with the stash open: move (command 34) from place " + place + " cell " + s.hoverItemX + ","
            + s.hoverItemY);
        modLink.move(place, s.hoverItemX, s.hoverItemY);



        final long id = item != null ? item.id : -1;
        final int x = s.hoverItemX, y = s.hoverItemY, category = s.hoverItemCategory;
        handler.postDelayed(() -> {
            boolean still;
            if (id >= 0) {
                com.winlator.d2.test.Zustand.Figur.Teil t = D2ItemMoves.itemAt(figur7(), place, x, y);
                still = t != null && t.id == id;
            }
            else {
                D2ModState n = mod;
                still = n != null && n.hoverItemPlace == place && n.hoverItemX == x && n.hoverItemY == y && n.hoverItemCategory == category;
            }
            if (still) {
                D2Trace.log("double tap: item at place " + place + " cell " + x + "," + y + " not moved after " + MOVE_CHECK_MS + " ms");
                showNote(text(R.string.move_not_moved));
            }
        }, MOVE_CHECK_MS);
    }


    private static final long MOVE_CHECK_MS = 1500;


    boolean padInputBox(int button, boolean down) {
        return inputBox.pad(button, down);
    }


    boolean padHoldsForBox(int button) {
        if (inputBox.open() || !padPointerMode() || D2Gamepad.pointerClick(button) != D2Gamepad.CLICK_RIGHT) return false;
        com.winlator.d2.test.Zustand.Figur.Teil ware = wareUnderPointer();
        return ware != null && D2ShopGestures.endless(ware.kategorie);
    }


    void padOpenBox() {
        openBuyBox("pad");
    }


    private boolean padMayTake() {
        return controlsVisible && !pauseWanted && assignButton == null && !editMode && !padPointerMode();
    }


    private boolean padRingOpen() {
        return padRingTouch != null && ringTouch == padRingTouch;
    }







    private void padClick(Button owner, int click) {
        D2Trace.log("pad " + owner.label + ": click " + click + " at " + xServer.pointer.getX() + "," + xServer.pointer.getY());
        owner.heldAction = D2TriggerAction.NOTHING;
        D2ModState s = mod;
        if (click == D2Gamepad.CLICK_LEFT && s != null && s.npcTalking) {
            pressKey(VK_ESCAPE);
            return;
        }
        if (click == D2Gamepad.CLICK_LEFT && npcMenuOpen()) {
            pressKey(VK_RETURN);
            return;
        }
        if (click == D2Gamepad.CLICK_LEFT && assignButton != null) {
            assignAtScreen(new int[]{xServer.pointer.getX(), xServer.pointer.getY()});
            return;
        }
        if (D2Gamepad.ownButtonTakes(click) && padClickOwn(click)) return;
        if (click == D2Gamepad.CLICK_LEFT) {
            float[] v = padPointerView();
            if (windowInfoTakes(v[0], v[1])) return;
        }
        targetX = targetY = -1;



        boolean shopOpen = s != null && s.isOpen(D2ModState.UI_NPCSHOP);
        D2ModItems carried = shopOpen ? modLink.items() : null;
        int action = D2Gamepad.padShopClick(owner.label, click, shopOpen, s != null ? s.hoverItemPlace : -1,
            s != null ? s.hoverItemX : -1, s != null ? s.hoverItemY : -1, carried != null && carried.sellPossible);
        if (action == D2Gamepad.CLICK_SELL) modLink.sell(s.hoverItemX, s.hoverItemY);
        if (action != click) return;
        if (click == D2Gamepad.CLICK_LEFT) {
            setLmb(true);
            owner.heldAction = HELD_LEFT_CLICK;
        }
        else {
            setRmb(true);
            owner.heldAction = HELD_RIGHT_CLICK;
        }
    }


    private final D2PadBack.EndTalk padEndTalk = new D2PadBack.EndTalk();

    private long padBackEscAt = -1;





    private void markPadEscSent() {
        enqueue((now) -> {
            padBackEscAt = now;
            padEndTalk.sentNow(now);
            return true;
        });
    }


    private static boolean npcAround(D2ModState s) {
        return s != null && (s.npcTalking || s.isOpen(D2ModState.UI_NPCMENU) || s.isOpen(D2ModState.UI_NPCSHOP));
    }

    private static long talkSignature(D2ModState s) {
        return s == null ? 0 : D2PadBack.signature(s.npcTalking, s.panelFlags(), s.npcMenuIds);
    }






    private boolean padBack(Button owner) {
        D2ModState s = modLink.state();
        boolean npc = npcAround(s);
        long panels = s != null ? s.panelFlags() : 0;
        int what = D2PadBack.action(editMode, pauseWanted, padWheelOpen, padRingOpen(), npc, panels, assignButton != null);
        if (what == D2PadBack.NONE) return false;
        owner.heldAction = D2TriggerAction.NOTHING;
        long now = SystemClock.uptimeMillis();
        boolean sendsEsc = what == D2PadBack.ESC || what == D2PadBack.END_TALK || (what == D2PadBack.RING && npc);
        if (sendsEsc && D2PadBack.repeatSwallowed(padBackEscAt, now, padEndTalk.active())) {
            D2Trace.log("pad B: again right after its Esc, swallowed");
            return true;
        }
        D2Trace.log("pad B: back " + what);
        switch (what) {
            case D2PadBack.WHEEL:
                padWheelCancel();
                return true;
            case D2PadBack.RING:
                closeRing();
                if (!npc) return true;
                break;
            default:
                break;
        }
        padBackEscAt = now;
        if (npc) padEndTalk.start(now, talkSignature(s));
        markPadEscSent();
        pressEscape();
        if (assignButton != null) {
            assignButton = null;
            invalidate();
        }
        return true;
    }






    private boolean padClickOwn(int click) {
        float[] v = padPointerView();
        Button button = padOwnButton(v);
        if (button != null) {
            D2Trace.log("pad: taps " + button.id);
            if (click != D2Gamepad.CLICK_LEFT) return true;
            button.pressed = true;
            postDelayed(() -> {
                button.pressed = false;
                invalidate();
            }, 120);
            Touch touch = new Touch();
            touch.button = button;
            onButtonUp(button, touch, true);
            invalidate();
            return true;
        }
        if (!materialsWindow.takes(v[0], v[1], false, materialsOn(), SystemClock.uptimeMillis())) return false;
        if (click == D2Gamepad.CLICK_LEFT) {
            materialsWindow.down(v[0], v[1]);
            materialsWindow.up(v[0], v[1], false);
            invalidate();
        }
        return true;
    }


    private float[] padPointerView() {
        return D2Gamepad.pointerView(xServer.pointer.getX(), xServer.pointer.getY(), aspect, offsetX, offsetY);
    }


    private Button padOwnButton(float[] v) {
        Button button = findButton(v[0], v[1]);
        return button != null && button.group == GROUP_HIDDEN ? button : null;
    }






    private void drawPadPointer(Canvas canvas) {
        padHandShown = false;
        D2ModState s = mod;
        D2Hud graphics = hud;
        android.graphics.Bitmap hand = graphics != null ? graphics.pointerHand() : null;
        if (!padActive || hand == null || s == null || s.gameHeight <= 0 || !padPointerMode() || cursorTaken()) return;
        float[] v = padPointerView();
        if (!padOverOwn(v)) return;
        float k = gameMapping(s)[0];
        int[] off = graphics.pointerHandOffset();
        float left = v[0] + off[0] * k, top = v[1] + off[1] * k;
        rect.set(left, top, left + hand.getWidth() * k, top + hand.getHeight() * k);
        canvas.drawBitmap(hand, null, rect, iconPaint);
        padHandShown = true;
    }


    private boolean padOverOwn(float[] v) {
        long now = SystemClock.uptimeMillis();
        return padOwnButton(v) != null || materialsWindow.covers(v[0], v[1], materialsOn(), now)
            || effectsView.hitAt(v[0], v[1], now) >= 0;
    }


    private boolean padHandShown;


    private boolean padPointerMode() {
        D2ModState s = modLink.state();
        D2Points points = modLink.points(2000);
        return D2Gamepad.pointerMode(s != null, controlsVisible, s != null ? s.panelFlags() : 0, npcMenuOpen(),
            points != null && points.cursorTaken, assignButton != null, editMode, pauseWanted);
    }


    private final Button padBarAttack = new Button("padBarAttack", "", KIND_ATTACK, 0, GROUP_GAME, false);
    private final Button[] padBarSkills = new Button[8];

    private static final int[] PAD_BAR = {D2Gamepad.Y, D2Gamepad.X, D2Gamepad.B, D2Gamepad.A, D2Gamepad.RB, D2Gamepad.RT};
    private static final float[][] PAD_BAR_AT = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}, {1.9f, -1.6f}, {1.9f, -0.5f}};






    private void drawPadBar(Canvas canvas) {
        Button attack = gameButton("attack");
        if (attack == null) return;
        float r = 21 * dp * scale, step = 2.2f * r;

        float bottom = getHeight();
        D2ModState ms = mod;

        if (ms != null && ms.gameHeight > 0) bottom = Math.min(bottom, gameToView(0, ms.gameHeight - D2Hud.STRIP)[1]);
        float cx = Math.min(attack.cx, getWidth() - step * 1.9f - r - 4 * dp), cy = Math.min(attack.cy, bottom - step - r - 4 * dp);
        for (int i = 0; i < PAD_BAR.length; i++) {
            int b = PAD_BAR[i];
            int act = padMap[padLayer2 ? 1 : 0][b];
            Button icon = null;
            if (act == D2TriggerAction.ATTACK || act == D2TriggerAction.ATTACK_OR_TAKE) icon = padBarAttack;
            else if (D2TriggerAction.isSkillKey(act)) {
                int k = D2TriggerAction.skillKeyIndex(act);
                if (padBarSkills[k] == null) padBarSkills[k] = new Button("padBarF" + (k + 1), "F" + (k + 1), KIND_SKILL, VK_F1 + k, GROUP_GAME, false);
                icon = padBarSkills[k];
            }
            float x = cx + PAD_BAR_AT[i][0] * step, y = cy + PAD_BAR_AT[i][1] * step;
            boolean drawn = false;
            if (icon != null) {
                icon.cx = x;
                icon.cy = y;
                icon.r = r;
                drawn = drawSkillIcon(canvas, icon, false);
            }
            if (!drawn) drawn = drawPadBarPicture(canvas, act, x, y, r);
            if (!drawn) {
                fillPaint.setColor(Color.argb(200, 12, 10, 8));
                canvas.drawCircle(x, y, r, fillPaint);
                float width = strokePaint.getStrokeWidth();
                strokePaint.setStrokeWidth(1.5f * dp);
                strokePaint.setColor(Color.rgb(90, 74, 48));
                canvas.drawCircle(x, y, r, strokePaint);
                strokePaint.setStrokeWidth(width);
                String name = act == D2TriggerAction.NOTHING ? "–" : D2TriggerAction.isSkillKey(act) ? "F" + (D2TriggerAction.skillKeyIndex(act) + 1)
                    : triggerActionName(act);
                drawTab(canvas, name, x, y - 6 * dp);
            }
            drawTab(canvas, D2Gamepad.NAMES[b], x, y - r - 6 * dp);
        }
        if (padLayer2) drawTab(canvas, "LT", cx, cy - step * 1.9f);
    }






    private boolean drawPadBarPicture(Canvas canvas, int act, float x, float y, float r) {
        D2Hud graphics = hud;
        if (graphics == null || (act != D2TriggerAction.PORTAL && act != D2TriggerAction.TAKE)) return false;
        android.graphics.Bitmap hand = act == D2TriggerAction.TAKE ? graphics.handIcon() : null;
        if (act == D2TriggerAction.TAKE && hand == null) return false;
        fillPaint.setColor(Color.argb(200, 12, 10, 8));
        canvas.drawCircle(x, y, r, fillPaint);
        float width = strokePaint.getStrokeWidth();
        strokePaint.setStrokeWidth(1.5f * dp);
        strokePaint.setColor(Color.rgb(90, 74, 48));
        canvas.drawCircle(x, y, r, strokePaint);
        strokePaint.setStrokeWidth(width);
        if (act == D2TriggerAction.PORTAL) {
            float half = r * 0.72f;
            D2ModState s = mod;
            padBarRect.set(x - half, y - half, x + half, y + half);
            return graphics.drawMenuSkillIcon(canvas, SKILL_BOOK_OF_TOWNPORTAL, padBarRect,
                s != null && s.portals == 0 ? 90 : 255);
        }
        float k = r * 1.35f / Math.max(hand.getWidth(), hand.getHeight());
        float w = hand.getWidth() * k, h = hand.getHeight() * k;
        padBarRect.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
        canvas.drawBitmap(hand, null, padBarRect, iconPaint);
        return true;
    }

    private final RectF padBarRect = new RectF();


    void padLayer(boolean held) {
        padLayer2 = held;
        invalidate();
    }






    void padTick(boolean active, boolean hasDirection, float dx, float dy, float lx, float ly, float rx, float ry) {
        long now = SystemClock.uptimeMillis();
        if (padWheelOpen) {
            int entry = D2Gamepad.wheelEntry(rx, ry, padDeadzone);
            if (entry < 0) entry = D2Gamepad.wheelEntry(lx, ly, padDeadzone);
            if (entry != padWheelEntry) {
                padWheelEntry = entry;
                invalidate();
            }
            padStick(false, false, dx, dy);
            return;
        }
        if (padRingOpen()) {
            float[] p = D2Gamepad.ringPoint(lx, ly, padDeadzone);
            padRingTouch.x = ringCx + p[0] * ringR;
            padRingTouch.y = ringCy + p[1] * ringR;
            moveRing(padRingTouch);
            padStick(false, false, dx, dy);
            return;
        }
        if (padPointerMode()) {
            padPointerAt = now;
            padStick(false, false, dx, dy);
            padPointer(lx, ly, rx, ry, now);
            return;
        }
        padPointerTick = padPointerFullSince = 0;
        if (now - padPointerAt < D2Gamepad.WALK_AFTER_POINTER_MS) {
            padStick(false, false, dx, dy);
            return;
        }
        padStick(active, hasDirection, dx, dy);
    }





    private void padPointer(float lx, float ly, float rx, float ry, long now) {
        if (npcMenuOpen()) {
            padPointerTick = 0;
            return;
        }
        long dt = padPointerTick == 0 ? 16 : Math.min(50, now - padPointerTick);
        padPointerTick = now;
        float vmax = D2Gamepad.pointerVmax(xServer.screenInfo.width, padCursorSpeed);
        float ml = (float) Math.hypot(lx, ly), mr = (float) Math.hypot(rx, ry);
        if (D2Gamepad.full(ml)) {
            if (padPointerFullSince == 0) padPointerFullSince = now;
        }
        else padPointerFullSince = 0;
        float vl = D2Gamepad.pointerSpeed(ml, padDeadzone, vmax, padPointerFullSince == 0 ? 0 : now - padPointerFullSince, false);
        float vr = D2Gamepad.pointerSpeed(mr, padDeadzone, vmax, 0, true);
        float vx = (ml > 0 ? lx / ml * vl : 0) + (mr > 0 ? rx / mr * vr : 0);
        float vy = (ml > 0 ? ly / ml * vl : 0) + (mr > 0 ? ry / mr * vr : 0);
        if (vx == 0 && vy == 0) {
            padRestX = padRestY = 0;
            return;
        }
        targetX = targetY = -1;
        padRestX += vx * dt / 1000f / gainX;
        padRestY += vy * dt / 1000f / gainY;
        int mx = (int) padRestX, my = (int) padRestY;
        padRestX -= mx;
        padRestY -= my;
        if (mx == 0 && my == 0) return;
        lastSentFromX = lastSentFromY = Integer.MIN_VALUE;
        winHandler.mouseEvent(MouseEventFlags.MOVE, mx, my, 0);

        if (padHandShown || padOverOwn(padPointerView())) postInvalidateDelayed(20);
    }


    private boolean escMenuSeen;


    private void padEscMenu(D2ModState s, boolean entered) {
        boolean open = entered && s.isOpen(D2ModState.UI_ESCMENU);
        boolean jump = D2Gamepad.escMenuOpened(escMenuSeen, open, pauseWanted);
        escMenuSeen = open;
        if (jump && padActive) padEscMenuTo(s, D2Gamepad.ESC_RETURN);
    }


    private boolean padEscMenuOpen() {
        D2ModState s = mod;
        return s != null && s.isOpen(D2ModState.UI_ESCMENU) && !pauseWanted && s.gameWidth > 0 && s.gameHeight > 0;
    }


    private void padEscMenuTo(D2ModState s, int entry) {
        if (s.gameWidth <= 0 || s.gameHeight <= 0) return;

        float[] g = D2Gamepad.escMenuPoint(entry, s.gameWidth, s.gameHeight);
        int[] p = gameToDesktop(g[0] - positionShiftX, g[1] - positionShiftY);
        D2Trace.log("pad: Esc menu entry " + entry + " at " + p[0] + "," + p[1]);
        enqueue(moveTo(p[0], p[1]));
    }


    void padFixed(int act) {


        if ((pauseWanted || editMode || assignButton != null) && act != D2TriggerAction.APP_MENU) return;
        D2Trace.log("pad: action " + act);
        int resolved = D2TriggerAction.resolve(act, controlsVisible, false);
        if (resolved != D2TriggerAction.NOTHING) triggerAction(resolved);
        invalidate();
    }


    private boolean padWheelOpen;
    private int padWheelEntry = -1;
    private final RectF padWheelRect = new RectF();





    void padWheel(boolean open) {
        if (open) {
            if (pauseWanted || editMode || assignButton != null) return;

            if (ringTouch != null) {
                D2Trace.log("pad: window wheel not over the open ring");
                return;
            }
            padWheelOpen = true;
            padWheelEntry = -1;
            D2Trace.log("pad: window wheel open");
            invalidate();
            return;
        }
        if (!padWheelOpen) return;
        int entry = padWheelEntry;
        padWheelCancel();
        D2Trace.log("pad: window wheel, entry " + entry);
        if (entry < 0 || pauseWanted || editMode) return;
        switch (entry) {
            case D2Gamepad.WHEEL_INVENTORY: triggerAction(D2TriggerAction.INVENTORY); break;
            case D2Gamepad.WHEEL_MAP: triggerAction(D2TriggerAction.MAP); break;
            case D2Gamepad.WHEEL_CHARACTER: padPanelKey(VK_C); break;
            case D2Gamepad.WHEEL_SKILLS: padPanelKey(VK_T); break;
            case D2Gamepad.WHEEL_QUESTS: padPanelKey(VK_Q); break;
            case D2Gamepad.WHEEL_MERC: padPanelKey(VK_O); break;
            case D2Gamepad.WHEEL_PAUSE: setPaused(true); break;
            case D2Gamepad.WHEEL_RECIPES: recipes.openWithCube(); break;
            default: break;
        }
    }





    private void pressMercKeys() {
        D2ModState s = modLink.state();
        boolean inventory = D2WorldTap.mercWithInventory(s != null, s != null && s.isOpen(D2ModState.UI_INVENTORY),
            s != null && s.isOpen(D2ModState.UI_MERC));
        pressKey(VK_O);
        if (!inventory) return;
        D2Trace.log("mercenary's window with the inventory beside it");
        pressKey(VK_I);
    }


    void padWheelCancel() {
        if (!padWheelOpen) return;
        padWheelOpen = false;
        padWheelEntry = -1;
        invalidate();
    }





    private void windowButtonClick(Button button) {
        if (button.group != GROUP_MENU && button.group != GROUP_HIDDEN) return;
        boolean window = button.kind == KIND_PANEL || button.kind == KIND_ESCAPE || (button.kind == KIND_KEY && button.vk == VK_TAB);
        if (!window || mod == null || modLink == null || appliedUiKey == UI_OUT_OF_GAME) return;
        modLink.playSound(D2ModLink.SOUND_BUTTON_CLICK);
    }


    private void padPanelKey(int vk) {
        if (vk == VK_O) pressMercKeys();
        else pressKey(vk);
        if (controlsVisible) setControlsVisible(false, true);
        else if (hiddenByPanel) setControlsVisible(true, false);
    }






    private void drawPadWheel(Canvas canvas) {
        D2Hud graphics = hud;
        float cx = getWidth() / 2f, cy = getHeight() / 2f, ring = Math.min(getWidth(), getHeight()) * 0.28f, r = 26 * dp;
        fillPaint.setColor(Color.argb(140, 0, 0, 0));
        canvas.drawCircle(cx, cy, ring + r + 10 * dp, fillPaint);
        int[] icons = {D2Hud.ICON_INV, D2Hud.ICON_CHAR, D2Hud.ICON_SKILLS, D2Hud.ICON_MAP, D2Hud.ICON_QUESTS, -1, -2, -3};
        String[] names = {text(R.string.btn_inv), text(R.string.btn_char), text(R.string.btn_skills), text(R.string.btn_map),
            text(R.string.btn_quests), text(R.string.pad_wheel_merc), text(R.string.btn_pause), text(R.string.rb_title)};
        for (int i = 0; i < D2Gamepad.WHEEL_COUNT; i++) {
            double a = Math.toRadians(i * 360.0 / D2Gamepad.WHEEL_COUNT);
            float x = cx + (float) Math.sin(a) * ring, y = cy - (float) Math.cos(a) * ring;
            boolean on = i == padWheelEntry;
            fillPaint.setColor(Color.argb(on ? 230 : 200, on ? 40 : 12, on ? 32 : 10, on ? 18 : 8));
            canvas.drawCircle(x, y, r, fillPaint);
            float width = strokePaint.getStrokeWidth();
            strokePaint.setStrokeWidth((on ? 3 : 1.5f) * dp);
            strokePaint.setColor(on ? D2Style.GOLD : Color.rgb(90, 74, 48));
            canvas.drawCircle(x, y, r, strokePaint);
            strokePaint.setStrokeWidth(width);
            float half = r * 0.62f;
            padWheelRect.set(x - half, y - half, x + half, y + half);
            boolean pictured = icons[i] >= 0 && graphics != null;
            if (pictured) graphics.drawMiniIcon(canvas, icons[i], on, padWheelRect);
            else if (icons[i] == -1 && graphics != null) pictured = graphics.drawMercIcon(canvas, padWheelRect);
            else if (icons[i] == -3 && graphics != null) pictured = graphics.drawCubeIcon(canvas, padWheelRect);
            else if (icons[i] == -2) {
                fillPaint.setColor(on ? D2Style.GOLD : Color.argb(220, 230, 220, 200));
                canvas.drawRect(x - 7 * dp, y - 9 * dp, x - 2.5f * dp, y + 9 * dp, fillPaint);
                canvas.drawRect(x + 2.5f * dp, y - 9 * dp, x + 7 * dp, y + 9 * dp, fillPaint);
            }
            if (pictured && !on) continue;
            drawTab(canvas, names[i], x, y + r + 4 * dp);
        }
        drawTab(canvas, D2Gamepad.NAMES[D2Gamepad.VIEW], cx, cy - 6 * dp);
    }


    boolean padInventoryOpen() {
        D2ModState s = mod;
        return s != null && s.isOpen(D2ModState.UI_INVENTORY);
    }


    void padSort() {
        D2ModState s = mod;
        if (pauseWanted || editMode || s == null || !s.isOpen(D2ModState.UI_INVENTORY) || !sortCapable() || !modLink.singlePlayer()
            || cursorTaken()) {
            D2Trace.log("pad: sort not possible now");
            return;
        }
        D2Trace.log("pad: R3 held, sorting");
        startSort();
    }


    void padBelt(int row, boolean mercenary) {
        if (pauseWanted || editMode || row < 0 || row > 3) return;
        if (padRingOpen()) {
            if (row == 0 || row == 1) {
                ringChoice.turn(row == 1 ? 1 : -1);
                invalidate();
            }
            return;
        }
        if (padEscMenuOpen()) {
            if (row == 2 || row == 3) {
                D2ModState s = mod;

                float[] g = desktopToGame(xServer.pointer.getX(), xServer.pointer.getY(), s);
                int at = D2Gamepad.escMenuEntryAt(g[0], g[1], s.gameWidth, s.gameHeight);
                padEscMenuTo(s, D2Gamepad.escMenuStep(at, row == 3 ? -1 : 1));
            }
            return;
        }
        if (npcMenuOpen() && (row == 2 || row == 3)) {
            cycleInteract(row == 3 ? -1 : 1);
            return;
        }
        D2Trace.log("pad: belt " + (row + 1) + (mercenary ? " to the mercenary" : ""));
        boolean shifted = shiftDown;
        if (mercenary && !shifted) setShift(true);
        pressKey(VK_1 + row);
        if (mercenary && !shifted) setShift(false);
    }






    private void padStick(boolean active, boolean hasDirection, float dx, float dy) {
        padStickActive = active;
        padStickHasDirection = hasDirection;
        if (hasDirection) {
            padDirX = dx;
            padDirY = dy;
        }
        boolean may = D2Gamepad.padMayWalk(controlsVisible, editMode, pauseWanted, assignButton != null, false, joystickOwner);
        if (!active || !may) {
            if (joystickOwner == D2Gamepad.OWNER_PAD) joystickReleased();
            return;
        }
        if (joystickOwner != D2Gamepad.OWNER_PAD) {
            joystickOwner = D2Gamepad.OWNER_PAD;
            joystickActive = true;
            this.hasDirection = false;
        }
        if (padStickHasDirection && (!this.hasDirection || dirX != padDirX || dirY != padDirY)) {
            dirX = padDirX;
            dirY = padDirY;
            this.hasDirection = true;
            onDirectionChanged();
        }
    }


    private void joystickReleased() {
        joystickActive = false;
        joystickOwner = D2Gamepad.OWNER_NONE;
        if (action == ACTION_WALK) {
            stopDirectWalk();
            setLmb(false);
            action = ACTION_NONE;
        }

        else if (action == ACTION_ATTACK && directAttack && attackTargetId < 0) attackIdle();
        else if (attackWalking) stopAttackWalk();
    }


    private void pressEscape() {
        pressKey(VK_ESCAPE);
        if (!controlsVisible && appliedUiKey != UI_OUT_OF_GAME) setControlsVisible(true, false);
    }


    private void triggerAction(int act) {
        D2Trace.log("shoulder trigger: action " + act);
        switch (act) {
            case D2TriggerAction.HEAL:
            case D2TriggerAction.MANA:
            case D2TriggerAction.REJUV: {
                Button potion = gameButton(act == D2TriggerAction.HEAL ? "heal" : (act == D2TriggerAction.MANA ? "mana" : "rejuv"));
                if (potion != null) drinkPotion(potion);
                break;
            }
            case D2TriggerAction.TAKE:

                if (mod != null && mod.npcTalking) pressKey(VK_ESCAPE);
                else if (npcMenuOpen()) pressKey(VK_RETURN);
                else interact();
                break;
            case D2TriggerAction.PORTAL:
                if (mod != null) modLink.portal();
                break;
            case D2TriggerAction.MAP:
                pressKey(VK_TAB);
                break;
            case D2TriggerAction.INVENTORY:

                pressKey(VK_I);
                if (controlsVisible) setControlsVisible(false, true);
                else if (hiddenByPanel) setControlsVisible(true, false);
                break;
            case D2TriggerAction.ESC_MENU:
                pressEscape();
                break;
            case D2TriggerAction.APP_MENU:
                if (openMenu != null) openMenu.run();
                break;
            case D2TriggerAction.WEAPON_SWAP:
                pressKey(VK_W);
                break;
            case D2TriggerAction.RUN_TOGGLE:
                pressKey(VK_R);
                break;
            default:
                break;
        }
    }


    private String triggerActionName(int act) {
        switch (act) {
            case D2TriggerAction.HEAL: return text(R.string.trigger_action_heal);
            case D2TriggerAction.MANA: return text(R.string.trigger_action_mana);
            case D2TriggerAction.REJUV: return text(R.string.trigger_action_rejuv);
            case D2TriggerAction.TAKE: return text(R.string.trigger_action_take);
            case D2TriggerAction.PORTAL: return text(R.string.trigger_action_portal);
            case D2TriggerAction.MAP: return text(R.string.trigger_action_map);
            case D2TriggerAction.INVENTORY: return text(R.string.trigger_action_inventory);
            case D2TriggerAction.ATTACK: return text(R.string.trigger_action_attack);
            case D2TriggerAction.NAMES: return text(R.string.trigger_action_names);
            case D2TriggerAction.ATTACK_OR_TAKE: return text(R.string.trigger_action_attack_take);
            case D2TriggerAction.ESC_MENU: return text(R.string.pad_action_esc);
            case D2TriggerAction.APP_MENU: return text(R.string.pad_action_menu);
            case D2TriggerAction.WEAPON_SWAP: return text(R.string.pad_action_weapons);
            case D2TriggerAction.RUN_TOGGLE: return text(R.string.pad_action_run);
            case D2TriggerAction.NOTHING: return text(R.string.pad_action_nothing);
            default:
                if (D2TriggerAction.isSkillKey(act)) return text(R.string.pad_action_skill_key, D2TriggerAction.skillKeyIndex(act) + 1);
                return text(R.string.trigger_action_skill);
        }
    }


    private Button gameButton(String id) {
        for (Button button : buttons) if (button.id.equals(id)) return button;
        return null;
    }






    private void cancelTriggerTouches(long now) {
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (touch.role == ROLE_NONE || now - touch.downTime > TRIGGER_TOUCH_WINDOW_MS || touch.moved) continue;
            boolean fingerDevice = fingerDevices.contains(touch.deviceId);

            boolean menuButton = touch.role == ROLE_BUTTON && touch.button.group != GROUP_GAME;
            if (fingerDevice && touch.role != ROLE_DIRECT && touch.role != ROLE_WORLD && !menuButton) continue;
            if (touch.longPress != null) handler.removeCallbacks(touch.longPress);



            if (touch.role == ROLE_DIRECT) endDirect(touch, true);
            if (touch.role == ROLE_BUTTON) {
                touch.button.pressed = false;
                touch.cancelled = true;
                onButtonUp(touch.button, touch, false);
            }
            if (touch.role == ROLE_JOYSTICK) {
                joystickActive = false;
                joystickOwner = D2Gamepad.OWNER_NONE;
            }

            touch.role = ROLE_NONE;
            ignoredTriggerTouches++;
            if (!fingerDevice && !fingerDevices.isEmpty()) triggerTouchDevices.add(touch.deviceId);
        }
    }


    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean fromTrigger = event.getDevice() != null && triggerDevices.indexOfKey(event.getDevice().getId()) >= 0;
        int action = event.getActionMasked();
        if (debug && action != MotionEvent.ACTION_HOVER_MOVE) {
            logEvent("Maus " + MotionEvent.actionToString(action) + " Knopf " + event.getActionButton(), event.getDevice());
        }
        return fromTrigger;
    }



    private int[] toScreen(float x, float y) {
        int sx = Math.round((x - offsetX) / aspect), sy = Math.round((y - offsetY) / aspect);
        sx = Math.max(0, Math.min(xServer.screenInfo.width - 1, sx));
        sy = Math.max(0, Math.min(xServer.screenInfo.height - 1, sy));
        return new int[]{sx, sy};
    }


    private int[] aimPoint(float dx, float dy, float distanceOfHeight) {
        int w = xServer.screenInfo.width, h = xServer.screenInfo.height;
        float distance = h * distanceOfHeight;
        int x = Math.round(w / 2f + dx * distance), y = Math.round(h / 2f + dy * distance);
        return new int[]{Math.max(0, Math.min(w - 1, x)), Math.max(0, Math.min(h - 1, y))};
    }

    private int[] walkPoint() {
        return aimPoint(dirX, dirY, 0.22f);
    }



    private void setLmb(boolean down) {
        if (lmbDown == down) return;
        lmbDown = down;
        enqueue(mouse(down ? MouseEventFlags.LEFTDOWN : MouseEventFlags.LEFTUP));
    }

    private void setRmb(boolean down) {
        if (rmbDown == down) return;
        rmbDown = down;
        enqueue(mouse(down ? MouseEventFlags.RIGHTDOWN : MouseEventFlags.RIGHTUP));
    }

    private void setShift(boolean down) {
        if (shiftDown == down) return;
        shiftDown = down;
        sendKey(VK_SHIFT, !down);
    }


    private void pressKey(int vk) {
        if (vk >= VK_F1 && vk < VK_F1 + 16) {

            D2ModState s = mod;
            int index = vk - VK_F1;
            lastFKeyAt = SystemClock.uptimeMillis();
            lastFKeyTicket = keyWait.sent(s != null ? s.hotkeySkill[index] : -1, s != null && s.hotkeyLeft[index], lastFKeyAt);
        }
        if (altDown) sendKey(VK_MENU, true);
        sendKey(vk, false);
        sendKey(vk, true);
        if (altDown) sendKey(VK_MENU, false);
    }





    private void syncLabels() {

        boolean inGame = mod != null || !modLink.isConnected();
        boolean want = (labelsWanted || labelsHeld) && windowFocused && inGame;
        boolean alt = want && (mod == null || !mod.itemNamesWithoutAlt);
        if (alt != altDown || want != labelsOn) D2Trace.log("item names " + (want ? "on" : "off") + (alt ? " with Alt" : ""));
        if (alt != altDown) {
            altDown = alt;
            sendKey(VK_MENU, !alt);
        }
        if (want == labelsOn) return;
        labelsOn = want;
        invalidate();
    }

    private void sendKey(final int vk, final boolean up) {
        enqueue((now) -> {
            winHandler.keyboardEvent((byte) vk, up ? KEYEVENTF_KEYUP : 0);
            return true;
        });
    }

    private Step mouse(final int flags) {
        return (now) -> {
            winHandler.mouseEvent(flags, 0, 0, 0);
            return true;
        };
    }

    private Step measureClick() {
        return (now) -> {
            if (targetX >= 0) {
                clickErrorX = xServer.pointer.getX() - targetX;
                clickErrorY = xServer.pointer.getY() - targetY;
                clickMeasured = true;
            }
            return true;
        };
    }

    private Step waitMs(final long ms) {
        final long[] start = {0};
        return (now) -> {
            if (start[0] == 0) start[0] = now;
            return now - start[0] >= ms;
        };
    }


    private Step moveTo(final int x, final int y) {
        final long[] start = {0};
        return (now) -> {
            if (start[0] == 0) {
                start[0] = now;
                setTarget(x, y);
            }
            return isAtTarget() || now - start[0] > 400;
        };
    }


    private Step settle(final long ms) {
        final long[] start = {0};
        return (now) -> {
            if (start[0] == 0) start[0] = now;
            if (targetX < 0) return true;
            if (isAtTarget()) {
                if (atTargetSince == 0) atTargetSince = now;
                return now - atTargetSince >= ms;
            }
            return now - start[0] > 600;
        };
    }







    private Step aimGround() {
        final long[] start = {0}, arrived = {0};
        final int[] at = new int[2];
        return (now) -> {
            if (start[0] == 0) start[0] = now;

            if (arrived[0] != 0 && (targetX != at[0] || targetY != at[1])) arrived[0] = 0;
            if (arrived[0] == 0) {

                if (!skillAimedByDrag) {
                    int[] p = aimPoint(dirX, dirY, 0.26f);
                    setTarget(p[0], p[1]);
                }
                if (targetX >= 0 && isAtTarget()) {
                    arrived[0] = now;
                    at[0] = targetX;
                    at[1] = targetY;
                }
            }
            return D2SkillAim.groundAimDone(now - start[0], arrived[0] == 0 ? -1 : now - arrived[0]);
        };
    }


    private void follow(int[] p) {
        setTarget(p[0], p[1]);
        schedulePump(0);
    }

    private void setTarget(int x, int y) {
        if (x == targetX && y == targetY) return;
        targetX = x;
        targetY = y;
        atTargetSince = 0;
        stalls = 0;
    }

    private void enqueue(Step... newSteps) {
        for (Step step : newSteps) steps.add(step);
        schedulePump(0);
    }

    private void schedulePump(long delay) {
        if (pumpScheduled) return;
        pumpScheduled = true;
        handler.postDelayed(pump, delay);
    }

    private void pump() {
        pumpScheduled = false;
        long now = SystemClock.uptimeMillis();
        driveCursor(now);
        if (targetX >= 0 && isAtTarget()) {
            if (atTargetSince == 0) atTargetSince = now;
        }
        else atTargetSince = 0;
        while (!steps.isEmpty()) {
            if (!steps.peek().run(now)) {
                schedulePump(8);
                return;
            }
            steps.poll();
        }
        if (targetX >= 0 && !isAtTarget()) schedulePump(8);
        if (debug) invalidate();
    }

    private boolean isAtTarget() {
        if (syncPending(SystemClock.uptimeMillis())) return false;
        return Math.abs(targetX - xServer.pointer.getX()) <= 2 && Math.abs(targetY - xServer.pointer.getY()) <= 2;
    }


    private void resyncPointer(long now, String why) {
        syncAskedAt = now;
        syncFeedbacks = winHandler.getCursorFeedbacks();
        syncCount++;

        lastSentFromX = lastSentFromY = Integer.MIN_VALUE;
        stableUpdates = 0;
        D2Trace.log("cursor resync (" + why + ") pointer " + xServer.pointer.getX() + "," + xServer.pointer.getY());
        winHandler.mouseEvent(MouseEventFlags.MOVE, 0, 0, 0);
        schedulePump(SYNC_WAIT_MS);
    }


    private boolean syncPending(long now) {
        if (syncAskedAt == 0) return false;
        if (now - syncAskedAt >= SYNC_WAIT_MS || winHandler.getCursorFeedbacks() != syncFeedbacks) {
            syncAskedAt = 0;
            return false;
        }
        return true;
    }






    private void checkPointerSync(D2ModState s, long now) {
        long panels = s.panelFlags();
        if (syncedPanels != -1 && panels != syncedPanels) resyncPointer(now, "windows " + Long.toHexString(panels));
        syncedPanels = panels;
        if (s.gameWidth <= 0 || s.gameHeight <= 0 || syncPending(now)) return;
        if (targetX >= 0 && !isAtTarget()) return;
        if (stableUpdates < 2) return;
        float[] expected = desktopToGame(xServer.pointer.getX(), xServer.pointer.getY(), s);
        boolean off = Math.abs(expected[0] - s.mouseX) > SYNC_TOLERANCE || Math.abs(expected[1] - s.mouseY) > SYNC_TOLERANCE;
        if (!off || (s.mouseX == syncMouseX && s.mouseY == syncMouseY)) return;
        syncMouseX = s.mouseX;
        syncMouseY = s.mouseY;
        resyncPointer(now, "mouse " + s.mouseX + "," + s.mouseY + " expected " + Math.round(expected[0]) + "," + Math.round(expected[1]));
    }







    private void driveCursor(long now) {
        if (targetX < 0) return;

        if (syncPending(now)) return;
        if (isAtTarget()) {
            stalls = 0;
            return;
        }
        int cx = xServer.pointer.getX(), cy = xServer.pointer.getY();
        boolean feedback = cx != lastSentFromX || cy != lastSentFromY;
        if (!feedback && now - lastSendTime < 50) return;
        if (feedback) {
            if (Math.abs(lastDx) > 8 && lastSentFromX != Integer.MIN_VALUE) gainX = learnGain(gainX, cx - lastSentFromX, lastDx);
            if (Math.abs(lastDy) > 8 && lastSentFromY != Integer.MIN_VALUE) gainY = learnGain(gainY, cy - lastSentFromY, lastDy);
            stalls = 0;
        }
        else if (++stalls > 6) {
            targetX = targetY = -1;
            stalls = 0;
            return;
        }
        int ex = targetX - cx, ey = targetY - cy;
        int dx = Math.abs(ex) <= 6 ? ex : Math.round(ex / gainX);
        int dy = Math.abs(ey) <= 6 ? ey : Math.round(ey / gainY);
        dx = Math.max(-2000, Math.min(2000, dx));
        dy = Math.max(-2000, Math.min(2000, dy));
        winHandler.mouseEvent(MouseEventFlags.MOVE, dx, dy, 0);
        lastSentFromX = cx;
        lastSentFromY = cy;
        lastDx = dx;
        lastDy = dy;
        lastSendTime = now;
    }

    private static float learnGain(float current, int moved, int sent) {
        float gain = moved / (float) sent;
        return gain >= 0.25f && gain <= 4f ? gain : current;
    }


    public void releaseAll() {


        leftRestore.ended(SystemClock.uptimeMillis(), true);


        handler.removeCallbacks(padRingHold);
        padRingOwner = null;
        if (padRingOpen()) closeRing();
        padRingTouch = null;
        pad.releaseAll();
        inputBox.releaseTouches();
        dropAttackRight();
        releaseDirect();
        stopDirectWalk();
        steps.clear();
        if (heldReleasesPending > 0) {

            heldReleasesPending = 0;
            winHandler.mouseEvent(MouseEventFlags.LEFTUP, 0, 0, 0);
            winHandler.keyboardEvent((byte) VK_SHIFT, KEYEVENTF_KEYUP);
        }
        for (int i = 0; i < touches.size(); i++) {
            Touch touch = touches.valueAt(i);
            if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
        }
        touches.clear();
        for (Button button : buttons) button.pressed = false;
        joystickActive = false;
        joystickOwner = D2Gamepad.OWNER_NONE;
        for (Button owner : padOwners) if (owner != null) owner.heldAction = D2TriggerAction.NOTHING;
        leftTrigger.heldAction = rightTrigger.heldAction = D2TriggerAction.NOTHING;
        padLayer2 = false;
        padPointerTick = padPointerFullSince = 0;
        action = ACTION_NONE;
        skillOwner = null;
        attackTargetId = skillTargetId = 0;
        skillFirstPending = false;
        skillEarlyTarget = -1;
        skillWalkToken++;
        attackTapToken++;
        pickupId = 0;
        targetX = targetY = -1;
        if (lmbDown) winHandler.mouseEvent(MouseEventFlags.LEFTUP, 0, 0, 0);
        if (rmbDown) winHandler.mouseEvent(MouseEventFlags.RIGHTUP, 0, 0, 0);
        if (shiftDown) winHandler.keyboardEvent((byte) VK_SHIFT, KEYEVENTF_KEYUP);
        if (altDown) winHandler.keyboardEvent((byte) VK_MENU, KEYEVENTF_KEYUP);
        lmbDown = rmbDown = shiftDown = labelsOn = altDown = false;
        closeRing();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        D2Trace.log("window focus " + hasWindowFocus);
        windowFocused = hasWindowFocus;

        startClock.focus(hasWindowFocus, SystemClock.uptimeMillis());
        if (hasWindowFocus && !startDone) revealAtStart();
        if (!hasWindowFocus && respecSpend.state() == D2RespecSpend.RUNNING) respecSpend.cancel("focus lost");

        if (!hasWindowFocus) {
            idle.onFocus(false);
            pausedBeforeFocusLoss = playerPaused();
        }

        if (!hasWindowFocus) releaseAll();

        if (!hasWindowFocus && autoPause && !editMode && mod != null && mod.canPause()) setPaused(true);
        if (hasWindowFocus) syncLabels();


        if (hasWindowFocus && mod == null) {
            handler.removeCallbacks(modWatchdog);
            handler.postDelayed(modWatchdog, 3000);
        }

        if (hasWindowFocus && pauseWanted) {
            pauseSentAt = SystemClock.uptimeMillis();
            modLink.pause(true);
        }

        if (hasWindowFocus) idle.onFocus(true);
    }


    public String idleQuest() {
        return idle.questPreview();
    }






    void onRealPad() {
        if (idle.phase() != D2Idle.Phase.OFF) idle.stop(text(R.string.idle_key));
        idle.onRealTouch();
    }

    public void onRealKey(android.view.KeyEvent event) {
        if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return;
        if (idle.phase() != D2Idle.Phase.OFF) idle.stop(text(R.string.idle_key));
        idle.onRealTouch();
    }





    public String idleBlocked() {
        if (!com.winlator.BuildConfig.D2_INTERNAL) return "";
        if (idle.running()) return text(R.string.idle_already_running);
        if (playerPaused()) return text(R.string.idle_end_pause_first);
        return idle.blocked();
    }





    private boolean playerPaused() {
        D2ModState s = mod;
        return pauseWanted || (s != null && s.isOpen(D2ModState.UI_ESCMENU));
    }


    public void requestIdle() {
        if (!com.winlator.BuildConfig.D2_INTERNAL) return;
        idle.request(pausedBeforeFocusLoss);
    }





    public void requestIdle(boolean playerPausedAtMenu, int minutes, int retreatPercent) {
        if (!com.winlator.BuildConfig.D2_INTERNAL) return;
        idle.request(playerPausedAtMenu, minutes, retreatPercent);
    }


    public boolean playerPausedNow() {
        return playerPaused();
    }





    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getDeviceId() != D2Idle.DEVICE_ID && idle.phase() != D2Idle.Phase.OFF) idle.stop(text(R.string.idle_touch));
        if (event.getDeviceId() != D2Idle.DEVICE_ID) idle.onRealTouch();
        return super.dispatchTouchEvent(event);
    }

    private static long hudNowPause() {
        return SystemClock.uptimeMillis();
    }



    public boolean gamePaused() {
        return pauseWanted;
    }


    public long lastModStateAt() {
        return modLink.lastStateAt();
    }

    public void setPaused(boolean paused) {
        if (paused == pauseWanted) return;

        if (paused && (sortWaitFor >= 0 || sortCursorLock)) {
            pauseAfterSort = true;
            return;
        }
        if (!paused) pauseAfterSort = false;
        if (paused) {
            if (respecSpend.state() == D2RespecSpend.RUNNING) respecSpend.cancel("pause");
            D2ModState s = mod;
            if (s == null) return;
            releaseCurrent();
            if (!s.canPause()) {
                pressKey(VK_ESCAPE);
                return;
            }
            pauseWanted = true;
            pauseSentAt = SystemClock.uptimeMillis();
            if (ringTouch != null) closeRing();

            if (autosave && s.plugyLoaded) {
                save(pauseSentAt);
                handler.postDelayed(sendPause, SAVE_BEFORE_PAUSE_MS);
            }
            else modLink.pause(true);

            joystickActive = false;
            joystickOwner = D2Gamepad.OWNER_NONE;
            for (int i = 0; i < touches.size(); i++) {
                Touch touch = touches.valueAt(i);
                if (touch.longPress != null) handler.removeCallbacks(touch.longPress);
                if (touch.button != null) touch.button.pressed = false;
                touch.role = ROLE_NONE;
            }
        }
        else {
            pauseWanted = false;
            handler.removeCallbacks(sendPause);
            modLink.pause(false);
            resumeHint.exit().hide();
        }
        invalidate();
    }

    private void sendPauseNow() {
        if (!pauseWanted) return;
        pauseSentAt = SystemClock.uptimeMillis();
        modLink.pause(true);
    }


    private boolean wasDead;

    private long deathScreenSince;
    private Boolean deathHardcore;

    private long deathTapAt;

    private void deathScreenSeen(long now) {
        if (!modLink.heroWaitsForEsc()) {
            deathScreenSince = 0;
            return;
        }
        if (deathScreenSince != 0) return;
        deathScreenSince = now;
        String name = modLink.currentCharacterName();
        deathHardcore = name != null ? D2SaveHeader.hardcore(saveDir, name) : null;
    }





    boolean deathTap() {
        D2ModState s = mod;
        long now = SystemClock.uptimeMillis();
        if (s == null || !D2DeathTap.allowed(modLink.heroWaitsForEsc(), s.life, deathHardcore, now, deathScreenSince,
            deathTapAt, modLink.gameInfoTime())) return false;
        if (!respawn("respawn on death tap")) return false;
        deathTapAt = now;
        return true;
    }

    private void save(long now) {
        savedAt = now;

        if (modLink.heroDead()) {
            D2Trace.log("save skipped: hero dead");
            return;
        }
        modLink.plugy(D2ModLink.PLUGY_SAVE);
        resumeHint.saving(now);
    }



    public void gameLeft() {
        resumeHint.gameLeft();
    }

    public void beforeExit() {
        resumeHint.confirm(SystemClock.uptimeMillis());
    }


    public boolean heroDead() {
        return modLink.heroDead();
    }






    public boolean respawn() {
        return respawn("respawn for saving");
    }


    private boolean respawn(String why) {
        D2ModState s = mod;
        if (!modLink.heroWaitsForEsc() || (s != null && s.isOpen(D2ModState.UI_ESCMENU))) return false;
        D2Trace.log(why);
        pressKey(VK_ESCAPE);
        return true;
    }






    public long onAppPaused() {
        idle.stopInBackground(text(R.string.idle_background));
        releaseAll();
        long now = SystemClock.uptimeMillis();
        if (autoPause && !editMode && mod != null && mod.canPause()) setPaused(true);
        else if (autosave && mod != null && mod.plugyLoaded && !pauseWanted && now - savedAt > 2000) save(now);

        long wait = SAVE_BEFORE_PAUSE_MS + 400 - (SystemClock.uptimeMillis() - savedAt);
        long stopIn = mod != null && mod.plugyLoaded && autosave && wait > 0 ? wait : 0;


        handler.postDelayed(() -> resumeHint.confirm(SystemClock.uptimeMillis()), stopIn);
        return stopIn;
    }


    private static volatile boolean running;


    public boolean canSaveBeforeExit() {
        return autosave && mod != null && mod.plugyLoaded;
    }





    public boolean quitLosesProgress() {
        return quitLoses;
    }





    public Boolean quitLossNow() {
        D2ModState s = modLink.state();
        return D2InstallFlow.quitLossNow(s != null, s != null && autosave && s.plugyLoaded, modLink.reportsWithoutState());
    }






    private boolean quitLoses = true;





    public static boolean quitWouldLose() {
        return lastLoses;
    }

    private static volatile boolean lastLoses = true;





    public boolean saveNow() {
        if (!canSaveBeforeExit()) return false;
        if (pauseWanted) setPaused(false);
        save(SystemClock.uptimeMillis());
        return true;
    }


    public static boolean isGameRunning() {
        return running || relaunchSaving;
    }


    private static volatile boolean relaunchSaving;


    public void endRelaunchSave() {
        if (!relaunchSaving) return;
        relaunchSaving = false;
        running = false;
        modLink.stop();
    }








    public int holdForRelaunch() {
        relaunchSaving = true;
        pauseAfterSort = false;
        D2ModState s = mod;
        return D2Relaunch.saveChance(s != null, s != null && s.plugyLoaded, lastPlugyLoaded);
    }


    private Boolean lastPlugyLoaded;






    public void saveForRelaunch() {
        long now = SystemClock.uptimeMillis();
        if (pauseWanted) setPaused(false);
        if (now - savedAt < 2000) {
            D2Trace.log("relaunch: saved " + (now - savedAt) + " ms ago, no second save");
            return;
        }
        save(now);
    }

    @Override
    protected void onDetachedFromWindow() {

        if (!relaunchSaving) {
            running = false;
            modLink.stop();
        }
        releaseAll();
        handler.removeCallbacksAndMessages(null);
        pumpScheduled = false;
        modUpdatePosted = false;
        super.onDetachedFromWindow();
    }
}
