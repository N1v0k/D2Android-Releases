package com.winlator.d2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;






public final class D2ModState {
    public static final int PROTOCOL_VERSION = 2;
    public static final int UNIT_PLAYER = 0, UNIT_MONSTER = 1, UNIT_OBJECT = 2, UNIT_ITEM = 4, UNIT_TILE = 5;
    public static final int PKT_STATUS = 1, PKT_STATE = 2, PKT_GOALS = 3, PKT_ITEMS = 4, PKT_MENU = 5, PKT_EFFECTS = 6, PKT_PLAYTEST = 7;


    public static final int UI_INVENTORY = 0x01, UI_CHARACTER = 0x02, UI_MINISKILL = 0x03, UI_SKILLTREE = 0x04,
        UI_CHAT = 0x05, UI_NPCMENU = 0x08, UI_ESCMENU = 0x09, UI_AUTOMAP = 0x0A, UI_HOTKEYS = 0x0B, UI_NPCSHOP = 0x0C,
        UI_ANVIL = 0x0E, UI_QUEST = 0x0F, UI_QUESTLOG = 0x11, UI_WAYPOINT = 0x14, UI_PARTY = 0x16, UI_TRADE = 0x17,
        UI_MESSAGES = 0x18, UI_STASH = 0x19, UI_CUBE = 0x1A, UI_HELP = 0x21, UI_MERC = 0x24, UI_SCROLL = 0x25;





    public static final int UI_QUESTSCROLL = 0x10;




    private static final int[] PANELS = {UI_INVENTORY, UI_CHARACTER, UI_MINISKILL, UI_SKILLTREE, UI_CHAT, UI_NPCMENU,
        UI_ESCMENU, UI_HOTKEYS, UI_NPCSHOP, UI_ANVIL, UI_QUEST, UI_QUESTSCROLL, UI_WAYPOINT, UI_PARTY, UI_TRADE,
        UI_MESSAGES, UI_STASH, UI_CUBE, UI_HELP, UI_MERC, UI_SCROLL};

    public static final int BELT_NONE = 0, BELT_HEAL = 1, BELT_MANA = 2, BELT_REJUV = 3, BELT_OTHER = 4;
    public static final int ITEM_OTHER = 0, ITEM_GOLD = 1, ITEM_POTION = 2, ITEM_RUNE = 3, ITEM_GEM = 4;
    public static final int OBJECT_OTHER = 0, OBJECT_CHEST = 1, OBJECT_SHRINE = 2, OBJECT_WAYPOINT = 3, OBJECT_WELL = 4, OBJECT_PORTAL = 5,

        OBJECT_ENTRANCE = 6,

        OBJECT_CORPSE = 7;


    public static final int MONSTER_CHAMPION = 1, MONSTER_UNIQUE = 2, MONSTER_MINION = 4, MONSTER_SUPERUNIQUE = 8, MONSTER_TOWN_NPC = 16;

    public static final class Monster {
        public final long id;
        public final int x, y, hpPercent, flags;
        public final String name;

        Monster(long id, int x, int y, int hpPercent, int flags, String name) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.hpPercent = hpPercent;
            this.flags = flags;
            this.name = name;
        }

        public boolean isTownNpc() {
            return (flags & MONSTER_TOWN_NPC) != 0;
        }

        public boolean isSpecial() {
            return (flags & (MONSTER_CHAMPION | MONSTER_UNIQUE | MONSTER_SUPERUNIQUE)) != 0;
        }
    }


    public static final class Thing {
        public final long id;
        public final int x, y, kind, extra;

        Thing(long id, int x, int y, int kind, int extra) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.kind = kind;
            this.extra = extra;
        }
    }

    public final int sequence;
    public final int gameWidth, gameHeight, mouseX, mouseY;
    public final long uiFlags;
    public final int levelNo;
    public final boolean inTown;

    public final int flags;
    public final int playerX, playerY, lifePercent, manaPercent;

    public final int hoverType, rawMonsters;

    public final int lastDrink, commandsReceived;
    public final long hoverId;
    public final int[] belt;
    public final List<Monster> monsters;
    public final List<Thing> items;
    public final List<Thing> objects;

    public final int characterClass, leftSkill, rightSkill;

    public final int[] hotkeySkill = new int[16];
    public final boolean[] hotkeyLeft = new boolean[16];

    public final int[] damagedLocation, damagedState;

    public final int life, maxLife, mana, maxMana;

    public final boolean running;

    public final boolean pointerHideable, pointerHidden;

    public final boolean npcTalking;

    public final boolean goldFull;





    public final boolean worldSignal, worldDrawn;

    public final int[] potionTierBelt = new int[2], potionTierInventory = new int[2], potionCountBelt = new int[2], potionCountInventory = new int[2];

    public final boolean potionsKnown;

    public final int portals;

    public final List<Thing> corpses;

    public final int[] npcMenuIds;
    public final String[] npcMenuTexts;

    public final boolean staminaKnown;

    public final int stamina, maxStamina, staminaPotions;

    public final boolean unlimitedStaminaPossible, unlimitedStamina;

    public final boolean healingPotionWorking, manaPotionWorking, staminaPotionWorking;




    public final boolean potionTiersKnown;




    public final int[] potionLargestBelt = new int[2], potionLargestInventory = new int[2], potionTierWorking = new int[2];

    public final int rejuvInventory;


    public final int hoverItemCategory, hoverItemPlace, hoverItemX, hoverItemY;

    public final int playerDirection;

    public final boolean handsKnown;

    public final boolean statPointsLeft, skillPointsLeft;

    public final boolean plugyLoaded;

    public final boolean itemNamesWithoutAlt;

    public final boolean labelTipsKept;

    public final boolean mapHackPossible, mapHackOn;

    public final boolean edgeButtonsHidden;

    public final int quantityRight, quantityLeft;





    public final boolean worldKnown;
    public final double worldX, worldY;
    public final int playerMode, characterLevel;
    public final long experience, gold, goldStash;

    public static final int NPC_TALK = 0xD35, NPC_TRADE = 0xD44, NPC_TRADE_REPAIR = 0xD06, NPC_GAMBLE = 0xD46, NPC_HIRE = 0xD45,

        NPC_IDENTIFY = 0xFB4;

    private D2ModState(ByteBuffer b, int sequence) {
        this.sequence = sequence;
        gameWidth = u16(b);
        gameHeight = u16(b);
        mouseX = b.getShort();
        mouseY = b.getShort();
        long low = b.getInt() & 0xFFFFFFFFL, high = b.getInt() & 0xFFFFFFFFL;
        uiFlags = low | (high << 32);
        levelNo = u16(b);
        inTown = u8(b) != 0;
        flags = u8(b);
        playerX = b.getShort();
        playerY = b.getShort();
        lifePercent = u8(b);
        manaPercent = u8(b);
        hoverType = u8(b);
        hoverId = b.getInt() & 0xFFFFFFFFL;
        lastDrink = u8(b);
        commandsReceived = u8(b);
        belt = new int[16];
        for (int i = 0; i < 16; i++) belt[i] = u8(b);
        rawMonsters = u8(b);
        int count = u8(b);
        List<Monster> monsterList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long id = b.getInt() & 0xFFFFFFFFL;
            int x = b.getShort(), y = b.getShort(), hp = u8(b), flags = u8(b), nameLength = u8(b);
            byte[] name = new byte[nameLength];
            b.get(name);
            monsterList.add(new Monster(id, x, y, hp, flags, new String(name, StandardCharsets.UTF_8)));
        }
        monsters = Collections.unmodifiableList(monsterList);
        items = Collections.unmodifiableList(things(b));
        objects = Collections.unmodifiableList(things(b));


        boolean tail = b.hasRemaining();
        int damaged = 0;
        if (tail) {
            characterClass = u8(b);
            leftSkill = skill(u16(b));
            rightSkill = skill(u16(b));
            for (int i = 0; i < 16; i++) {
                hotkeySkill[i] = skill(u16(b));
                hotkeyLeft[i] = u8(b) != 0;
            }
            damaged = u8(b);
            if (b.remaining() < damaged * 2) throw new java.nio.BufferUnderflowException();
        }
        else {
            characterClass = leftSkill = rightSkill = -1;
            java.util.Arrays.fill(hotkeySkill, -1);
        }
        damagedLocation = new int[damaged];
        damagedState = new int[damaged];
        for (int i = 0; i < damaged; i++) {
            damagedLocation[i] = u8(b);
            damagedState[i] = u8(b);
        }

        if (tail && b.hasRemaining()) {
            life = u16(b);
            maxLife = u16(b);
            mana = u16(b);
            maxMana = u16(b);
            int bits = u8(b);
            running = (bits & 1) != 0;
            pointerHideable = (bits & 2) != 0;
            pointerHidden = (bits & 4) != 0;
            npcTalking = (bits & 8) != 0;
            goldFull = (bits & 64) != 0;
            worldSignal = (bits & 16) != 0;
            worldDrawn = worldSignal && (bits & 32) != 0;
            portals = b.hasRemaining() ? u8(b) : -1;
        }
        else {
            life = maxLife = mana = maxMana = -1;
            running = pointerHideable = pointerHidden = npcTalking = goldFull = false;
            worldSignal = worldDrawn = false;
            portals = -1;
        }
        List<Thing> corpseList = new ArrayList<>();
        if (portals >= 0 && b.hasRemaining()) {
            int n = u8(b);
            for (int i = 0; i < n; i++) {
                long id = b.getInt() & 0xFFFFFFFFL;
                int x = b.getShort(), y = b.getShort();
                corpseList.add(new Thing(id, x, y, 0, 0));
            }
        }
        corpses = Collections.unmodifiableList(corpseList);
        int lines = portals >= 0 && b.hasRemaining() ? u8(b) : 0;
        npcMenuIds = new int[lines];
        npcMenuTexts = new String[lines];
        for (int i = 0; i < lines; i++) {
            npcMenuIds[i] = u16(b);
            byte[] text = new byte[u8(b)];
            b.get(text);
            npcMenuTexts[i] = new String(text, StandardCharsets.UTF_8);
        }

        potionsKnown = portals >= 0 && b.hasRemaining();
        if (potionsKnown && b.remaining() < 8) throw new java.nio.BufferUnderflowException();
        if (potionsKnown) {
            for (int k = 0; k < 2; k++) {
                potionTierBelt[k] = u8(b);
                potionTierInventory[k] = u8(b);
                potionCountBelt[k] = u8(b);
                potionCountInventory[k] = u8(b);
            }
        }

        staminaKnown = potionsKnown && b.hasRemaining();
        if (staminaKnown && b.remaining() < 6) throw new java.nio.BufferUnderflowException();
        boolean tierBit = false, hoverBit = false, directionBit = false;
        if (staminaKnown) {
            stamina = u16(b);
            maxStamina = u16(b);
            staminaPotions = u8(b);
            int bits = u8(b);
            unlimitedStaminaPossible = (bits & 1) != 0;
            unlimitedStamina = (bits & 2) != 0;
            healingPotionWorking = (bits & 4) != 0;
            manaPotionWorking = (bits & 8) != 0;
            staminaPotionWorking = (bits & 16) != 0;
            tierBit = (bits & 32) != 0;
            hoverBit = (bits & 64) != 0;
            directionBit = (bits & 128) != 0;
        }
        else {
            stamina = maxStamina = -1;
            staminaPotions = 0;
            unlimitedStaminaPossible = unlimitedStamina = healingPotionWorking = manaPotionWorking = staminaPotionWorking = false;
        }

        handsKnown = staminaKnown && b.hasRemaining();
        if (handsKnown && b.remaining() < 5) throw new java.nio.BufferUnderflowException();
        if (handsKnown) {
            int points = u8(b);
            statPointsLeft = (points & 1) != 0;
            skillPointsLeft = (points & 2) != 0;
            plugyLoaded = (points & 4) != 0;
            itemNamesWithoutAlt = (points & 8) != 0;
            labelTipsKept = (points & 16) != 0;
            mapHackPossible = (points & 32) != 0;
            mapHackOn = (points & 64) != 0;
            edgeButtonsHidden = (points & 128) != 0;
            int right = u16(b), left = u16(b);
            quantityRight = right == 0xFFFF ? -1 : right;
            quantityLeft = left == 0xFFFF ? -1 : left;
        }
        else {
            statPointsLeft = skillPointsLeft = plugyLoaded = itemNamesWithoutAlt = labelTipsKept = mapHackPossible = mapHackOn = false;
            edgeButtonsHidden = false;
            quantityRight = quantityLeft = -1;
        }


        potionTiersKnown = handsKnown && tierBit;
        int tierBytes = (potionTiersKnown ? 7 : 0) + (handsKnown && hoverBit ? 4 : 0) + (handsKnown && directionBit ? 1 : 0);
        if (b.remaining() < tierBytes) throw new java.nio.BufferUnderflowException();

        worldKnown = handsKnown && b.remaining() - tierBytes >= 20;
        if (worldKnown) {
            int x = u16(b), y = u16(b), fx = u8(b), fy = u8(b);
            worldX = x + fx / 256.0;
            worldY = y + fy / 256.0;
            playerMode = u8(b);
            characterLevel = u8(b);
            experience = b.getInt() & 0xFFFFFFFFL;
            gold = b.getInt() & 0xFFFFFFFFL;
            goldStash = b.getInt() & 0xFFFFFFFFL;
        }
        else {
            worldX = worldY = Double.NaN;
            playerMode = characterLevel = -1;
            experience = gold = goldStash = -1;
        }
        if (potionTiersKnown) {
            for (int k = 0; k < 2; k++) {
                potionLargestBelt[k] = u8(b);
                potionLargestInventory[k] = u8(b);
                potionTierWorking[k] = u8(b);
            }
            rejuvInventory = u8(b);
        }
        else rejuvInventory = -1;

        if (handsKnown && hoverBit) {
            int category = u8(b), place = u8(b), x = u8(b), y = u8(b);
            hoverItemCategory = category == 0xFF ? -1 : category;
            hoverItemPlace = place == 0xFF ? -1 : place;
            hoverItemX = x == 0xFF ? -1 : x;
            hoverItemY = y == 0xFF ? -1 : y;
        }
        else hoverItemCategory = hoverItemPlace = hoverItemX = hoverItemY = -1;

        int direction = handsKnown && directionBit ? u8(b) : 0xFF;
        playerDirection = direction < 64 ? direction : -1;
    }


    public int ammunition() {
        if (quantityRight >= 0 && quantityLeft >= 0) return quantityRight + quantityLeft;
        return quantityRight >= 0 ? quantityRight : quantityLeft;
    }

    private static int skill(int value) {
        return value == 0xFFFF ? -1 : value;
    }

    private static List<Thing> things(ByteBuffer b) {
        int count = u8(b);
        List<Thing> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long id = b.getInt() & 0xFFFFFFFFL;
            int x = b.getShort(), y = b.getShort();
            list.add(new Thing(id, x, y, u8(b), u8(b)));
        }
        return list;
    }

    private static int u8(ByteBuffer b) {
        return b.get() & 0xFF;
    }

    private static int u16(ByteBuffer b) {
        return b.getShort() & 0xFFFF;
    }


    public static int packetType(byte[] data, int length) {
        if (length < 8 || data[0] != 'D' || data[1] != '2' || data[2] != 'A' || data[3] != 'M') return -1;
        if ((data[4] & 0xFF) != PROTOCOL_VERSION) return -1;
        return data[5] & 0xFF;
    }


    public static D2ModState parse(byte[] data, int length) {
        if (packetType(data, length) != PKT_STATE) return null;
        ByteBuffer b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN);
        b.position(6);
        int sequence = u16(b);
        try {
            return new D2ModState(b, sequence);
        }
        catch (RuntimeException e) {
            return null;
        }
    }


    public static Object[] parseStatus(byte[] data, int length) {
        if (packetType(data, length) != PKT_STATUS || length < 10) return null;
        int code = data[8] & 0xFF, textLength = Math.min(data[9] & 0xFF, length - 10);
        return new Object[]{code, new String(data, 10, textLength, StandardCharsets.UTF_8)};
    }





    public static int[] parseMenuField(byte[] data, int length) {
        if (packetType(data, length) != PKT_MENU || length < 17 || (data[8] & 1) == 0) return null;
        int[] field = new int[4];
        for (int i = 0; i < 4; i++) field[i] = (short) ((data[9 + 2 * i] & 0xFF) | (data[10 + 2 * i] & 0xFF) << 8);
        return field;
    }


    public static boolean parseMenuSecret(byte[] data, int length) {
        return packetType(data, length) == PKT_MENU && length >= 9 && (data[8] & 2) != 0;
    }


    public static String parseMenuText(byte[] data, int length) {
        if (packetType(data, length) != PKT_MENU || length < 18 || parseMenuSecret(data, length)) return "";
        int n = Math.min(data[17] & 0xFF, length - 18);
        return new String(data, 18, n, StandardCharsets.UTF_8);
    }


    public boolean isHovered(long id) {
        return hoverType != 0xFF && hoverId == id;
    }


    public boolean canActOnUnits() {
        return (flags & 1) != 0;
    }


    public boolean canOwnHud() {
        return (flags & 8) != 0;
    }


    public boolean hudLeftOut() {
        return (flags & 16) != 0;
    }


    public boolean durabilityLeftOut() {
        return (flags & 128) != 0;
    }


    public boolean canPause() {
        return (flags & 32) != 0;
    }


    public boolean paused() {
        return (flags & 64) != 0;
    }


    public boolean canWalk() {
        return (flags & 4) != 0;
    }


    public boolean canHighlight() {
        return (flags & 2) != 0;
    }

    public boolean isOpen(int uiVar) {
        return (uiFlags & (1L << uiVar)) != 0;
    }


    public boolean isPanelOpen() {
        return panelFlags() != 0;
    }


    public long panelFlags() {
        long flags = 0;
        for (int panel : PANELS) if (isOpen(panel)) flags |= 1L << panel;
        return flags;
    }


    public int beltColumn(int kind) {
        for (int column = 0; column < 4; column++) if (belt[column] == kind) return column;
        return -1;
    }


    public boolean beltEmpty() {
        for (int slot : belt) if (slot != BELT_NONE) return false;
        return true;
    }


    public boolean beltHas(int kind) {
        for (int slot : belt) if (slot == kind) return true;
        return false;
    }
}
