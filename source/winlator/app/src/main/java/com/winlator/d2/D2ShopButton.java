package com.winlator.d2;

import android.content.Context;
import android.os.SystemClock;

import com.winlator.R;







final class D2ShopButton {
    private final Context context;
    private final D2ModLink link;
    private final D2AutoItems autoItems;
    private final D2PotionShop shop = new D2PotionShop(new java.util.Random().nextInt());

    private boolean lostReported = true;

    D2ShopButton(Context context, D2ModLink link, D2AutoItems autoItems) {
        this.context = context;
        this.link = link;
        this.autoItems = autoItems;
    }





    boolean visible(D2ModState s) {
        D2ModItems items = link.items();
        return s != null && s.isOpen(D2ModState.UI_NPCSHOP) && items != null && items.shopPossible && items.shopJob != null
            && items.shopSellsPotions() && link.singlePlayer() && D2Settings.potionShop(context)
            && !shop.busy(items.shopJob, SystemClock.uptimeMillis());
    }


    String tap(D2ModState s) {
        D2ModItems items = link.items();
        if (s == null || items == null || !visible(s)) return null;
        D2PotionShop.Need need = D2PotionShop.need(s.belt, items.beltRows, autoItems.columnKinds(),
            items.inventory[D2ModItems.KIND_HEAL], items.inventory[D2ModItems.KIND_MANA], D2Settings.shopReserve(context, false),
            D2Settings.shopReserve(context, true), items.freeCells());
        noColumn = noColumnText(need);
        if (need.nothing()) return withNoColumn(D2Text.get(context, R.string.shop_potions_full));
        wareName = null;
        long job = shop.start(SystemClock.uptimeMillis());
        lostReported = false;
        link.refillPotions(job, need, D2Settings.minGold(context));
        return null;
    }






    com.winlator.d2.test.Stadtgang.Einkauf einkauf(com.winlator.d2.test.Zustand z) {
        D2ModItems items = link.items();
        if (items == null || !items.shopPossible) return null;
        return com.winlator.d2.test.Stadtgang.bedarf(z, autoItems.columnKinds(), D2Settings.shopReserve(context, false),
            D2Settings.shopReserve(context, true), D2Settings.minGold(context));
    }


    private String wareName;


    boolean idle() {
        D2ModItems items = link.items();
        return items != null && items.shopPossible && !shop.busy(items.shopJob, SystemClock.uptimeMillis());
    }





    boolean buyWare(long wareId, String name, int count) {
        if (!idle() || count <= 0) return false;
        long job = shop.start(SystemClock.uptimeMillis());
        lostReported = false;
        wareName = name != null ? name : "";
        noColumn = 0;
        link.buyWare(job, wareId, count, 0);
        return true;
    }


    int wareBought() {
        D2ModItems items = link.items();
        return items != null && items.shopJob != null && wareName != null && shop.sentId() != 0
            && items.shopJob.id == shop.sentId() ? items.shopJob.a : -1;
    }


    String poll() {
        D2ModItems items = link.items();
        D2PotionShop.Job job = shop.finished(items != null ? items.shopJob : null);
        if (job != null) {
            lostReported = true;
            String ware = wareName;
            wareName = null;
            String bought = ware != null ? D2Text.get(context, R.string.shop_ware_bought, job.a, ware)
                : D2Text.get(context, R.string.shop_potions_bought, job.a, job.b);
            int reason = reasonText(job.reason);
            return withNoColumn(reason == 0 ? bought : bought + " – " + D2Text.get(context, reason));
        }
        if (!lostReported && items != null && !shop.busy(items.shopJob, SystemClock.uptimeMillis())
            && (items.shopJob == null || items.shopJob.id != shop.sentId())) {
            lostReported = true;
            return D2Text.get(context, R.string.shop_end_lost);
        }
        return null;
    }


    private int noColumn;


    static int noColumnText(D2PotionShop.Need need) {
        if (need.manaNoColumn) return R.string.shop_potions_no_mana_column;
        if (need.healNoColumn) return R.string.shop_potions_no_heal_column;
        return 0;
    }

    private String withNoColumn(String note) {
        return noColumn == 0 ? note : note + " – " + D2Text.get(context, noColumn);
    }

    private static int reasonText(int reason) {
        switch (reason) {
            case D2PotionShop.END_GOLD: return R.string.shop_end_gold;
            case D2PotionShop.END_NO_WARE: return R.string.shop_end_no_ware;
            case D2PotionShop.END_REFUSED: return R.string.shop_end_refused;
            case D2PotionShop.END_CLOSED: return R.string.shop_end_closed;
            case D2PotionShop.END_TIME: return R.string.shop_end_time;
            case D2PotionShop.END_UNAVAILABLE: return R.string.shop_end_unavailable;
            case D2PotionShop.END_INVENTORY_FULL: return R.string.shop_end_inventory_full;
            default: return 0;
        }
    }
}
