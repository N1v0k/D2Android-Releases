package com.winlator.d2;

public final class D2AutoDrink {
    public static final int HEAL = 0, MANA = 1, REJUV = 2, STAMINA = 3;
    static final long HEAL_GAP_MS = 1000, MANA_GAP_MS = 1000, REJUV_GAP_MS = 500, STAMINA_GAP_MS = 2000;
    static final long REJUV_MISSING_MS = 1000;
    static final long AFTER_REJUV_MS = 300;
    static final int FALLING_PCT = 5;
    static final int FAST_FALL_PCT = 10;
    static final long FAST_FALL_MS = 1000;

    public static final class Drink {
        public final int kind;
        public final boolean beltOnly, largest, onlyBigger;

        Drink(int kind, boolean beltOnly, boolean largest, boolean onlyBigger) {
            this.kind = kind;
            this.beltOnly = beltOnly;
            this.largest = largest;
            this.onlyBigger = onlyBigger;
        }

        @Override
        public String toString() {
            return "drink " + kind + (beltOnly ? " belt" : "") + (largest ? " largest" : "") + (onlyBigger ? " bigger" : "");
        }
    }

    public static final class Plan {
        public Drink life, mana, stamina;
        public boolean mercAllowed;
    }

    public static final class Hero {
        public int life, maxLife, mana, maxMana, stamina, maxStamina, staminaPotions;
        public boolean healWorking, manaWorking, staminaWorking;
        public final int[] countBelt = new int[2], countInventory = new int[2];
        public boolean rejuvBelt;
        public int lastDrink;
        public boolean tiersKnown;
        public final int[] largestBelt = new int[2], largestInventory = new int[2], tierWorking = new int[2];
        public int rejuvInventory = -1;

        public static Hero of(D2ModState s) {
            Hero h = new Hero();
            h.life = s.life;
            h.maxLife = s.maxLife;
            h.mana = s.mana;
            h.maxMana = s.maxMana;
            h.stamina = s.stamina;
            h.maxStamina = s.maxStamina;
            h.staminaPotions = s.staminaPotions;
            h.healWorking = s.healingPotionWorking;
            h.manaWorking = s.manaPotionWorking;
            h.staminaWorking = s.staminaPotionWorking;
            for (int k = 0; k < 2; k++) {
                h.countBelt[k] = s.potionCountBelt[k];
                h.countInventory[k] = s.potionCountInventory[k];
            }
            h.rejuvBelt = s.beltHas(D2ModState.BELT_REJUV);
            h.lastDrink = s.lastDrink;
            h.tiersKnown = s.potionTiersKnown;
            if (h.tiersKnown) {
                for (int k = 0; k < 2; k++) {
                    h.largestBelt[k] = s.potionLargestBelt[k];
                    h.largestInventory[k] = s.potionLargestInventory[k];
                    h.tierWorking[k] = s.potionTierWorking[k];
                }
                h.rejuvInventory = s.rejuvInventory;
            }
            return h;
        }
    }

    public int lifePct, rejuvPct, manaPct;
    public boolean inventoryToo;
    public boolean staminaPotion;
    public int idleLifePct;

    public static int idleThreshold(boolean idleRunning, boolean potionKeys, int brainPct) {
        return idleRunning && potionKeys ? brainPct : 0;
    }

    public boolean idleManaWanted;

    public static boolean idleManaWish(boolean idleRunning, boolean potionKeys, boolean brainTapped) {
        return idleRunning && potionKeys && brainTapped;
    }

    private long lifeAt, rejuvAt, manaAt, staminaAt, rejuvMissingUntil;
    private long lifeDrunkAt, manaDrunkAt;
    private boolean rejuvChecked = true;
    private int lifeRef = -1, manaRef = -1;
    private final long[] lifeTimes = new long[64];
    private final int[] lifeValues = new int[64];
    private int lifeNext, lifeCount;

    public Plan decide(Hero h, long now) {
        Plan plan = new Plan();
        boolean beltOnly = !inventoryToo;
        boolean fastFall = rememberLife(h, now);
        if (!h.healWorking && now - lifeAt > HEAL_GAP_MS) lifeRef = -1;
        else if (h.healWorking && lifeRef < 0) lifeRef = h.life;
        if (!h.manaWorking && now - manaAt > MANA_GAP_MS) manaRef = -1;
        else if (h.manaWorking && manaRef < 0) manaRef = h.mana;
        boolean holdOthers = !h.tiersKnown && now - rejuvAt < AFTER_REJUV_MS;
        if (!rejuvChecked && now - rejuvAt > 200 && now - rejuvAt < 1000 && h.lastDrink == 3) {
            rejuvChecked = true;
            rejuvMissingUntil = now + REJUV_MISSING_MS;
        }
        boolean lowLife = lifePct > 0 && h.life * 100 < lifePct * h.maxLife;
        int healPct = Math.max(lifePct, idleLifePct);
        boolean lowHeal = healPct > 0 && h.life * 100 < healPct * h.maxLife;
        boolean emergency = rejuvPct > 0 && h.life * 100 < rejuvPct * h.maxLife;
        if (lowHeal || emergency) {
            int healing = h.countBelt[0] + (inventoryToo ? h.countInventory[0] : 0);
            boolean rejuvAtHand = h.rejuvBelt || (inventoryToo && h.rejuvInventory != 0);
            boolean rejuv = rejuvAtHand && now > rejuvMissingUntil && now - rejuvAt > REJUV_GAP_MS;
            if (rejuv && (emergency || (lowLife && healing == 0 && !h.healWorking))) {
                rejuvAt = now;
                rejuvChecked = false;
                plan.life = new Drink(REJUV, beltOnly, false, false);
            }
            else if (lowHeal && healing > 0 && now - rejuvAt >= AFTER_REJUV_MS && now - lifeAt > HEAL_GAP_MS) {
                boolean low = rejuvPct > 0 ? emergency : h.life * 200 < lifePct * h.maxLife;
                boolean falling = h.healWorking && lifeRef >= 0 && (lifeRef - h.life) * 100 >= FALLING_PCT * h.maxLife;
                boolean pressure = h.tiersKnown && (low || falling || fastFall);
                Drink drink = null;
                if (!h.healWorking) drink = new Drink(HEAL, beltOnly, pressure, false);
                else if (pressure && h.tierWorking[0] > 0 && largestAllowed(h, 0) > h.tierWorking[0]) drink = new Drink(HEAL, beltOnly, true, true);
                if (drink != null) {
                    lifeAt = now;
                    lifeDrunkAt = now;
                    lifeRef = h.life;
                    plan.life = drink;
                }
            }
        }
        plan.mercAllowed = plan.life == null && !holdOthers;
        boolean lowMana = manaPct > 0 && h.mana * 100 < manaPct * h.maxMana;
        if (!holdOthers && h.maxMana > 0 && (lowMana || idleManaWanted) && now - manaAt > MANA_GAP_MS) {
            int mana = h.countBelt[1] + (inventoryToo ? h.countInventory[1] : 0);
            if (mana > 0) {
                boolean falling = h.manaWorking && manaRef >= 0 && (manaRef - h.mana) * 100 >= FALLING_PCT * h.maxMana;
                boolean pressure = h.tiersKnown && (h.mana * 200 < manaPct * h.maxMana || falling);
                Drink drink = null;
                if (!h.manaWorking) drink = new Drink(MANA, beltOnly, pressure, false);
                else if (pressure && h.tierWorking[1] > 0 && largestAllowed(h, 1) > h.tierWorking[1]) drink = new Drink(MANA, beltOnly, true, true);
                if (drink != null) {
                    manaAt = now;
                    manaDrunkAt = now;
                    manaRef = h.mana;
                    plan.mana = drink;
                }
            }
        }
        if (!holdOthers && staminaPotion && h.maxStamina > 0 && h.stamina * 10 <= h.maxStamina && h.staminaPotions > 0
            && !h.staminaWorking && now - staminaAt > STAMINA_GAP_MS) {
            staminaAt = now;
            plan.stamina = new Drink(STAMINA, false, false, false);
        }
        return plan;
    }

    private int largestAllowed(Hero h, int k) {
        return inventoryToo ? Math.max(h.largestBelt[k], h.largestInventory[k]) : h.largestBelt[k];
    }

    private boolean rememberLife(Hero h, long now) {
        lifeTimes[lifeNext] = now;
        lifeValues[lifeNext] = h.life;
        lifeNext = (lifeNext + 1) % lifeTimes.length;
        if (lifeCount < lifeTimes.length) lifeCount++;
        int highest = h.life;
        for (int i = 0; i < lifeCount; i++) {
            if (now - lifeTimes[i] <= FAST_FALL_MS && lifeValues[i] > highest) highest = lifeValues[i];
        }
        return h.maxLife > 0 && (highest - h.life) * 100 >= FAST_FALL_PCT * h.maxLife;
    }

    public boolean tapDropped(int kind, long now) {
        if (kind == HEAL) return now - lifeDrunkAt < HEAL_GAP_MS;
        if (kind == MANA) return now - manaDrunkAt < MANA_GAP_MS;
        return false;
    }

    public void manual(int kind, long now) {
        if (kind == HEAL) {
            lifeAt = now;
            lifeRef = -1;
        }
        else if (kind == MANA) {
            manaAt = now;
            manaRef = -1;
        }
        else if (kind == REJUV) {
            rejuvAt = now;
            rejuvChecked = false;
        }
    }
}
