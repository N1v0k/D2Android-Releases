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
        if (need.nothing()) return D2Text.get(context, R.string.shop_potions_full);
        long job = shop.start(SystemClock.uptimeMillis());
        lostReported = false;
        link.refillPotions(job, need, D2Settings.minGold(context));
        return null;
    }


    String poll() {
        D2ModItems items = link.items();
        D2PotionShop.Job job = shop.finished(items != null ? items.shopJob : null);
        if (job != null) {
            lostReported = true;
            String bought = D2Text.get(context, R.string.shop_potions_bought, job.a, job.b);
            int reason = reasonText(job.reason);
            return reason == 0 ? bought : bought + " – " + D2Text.get(context, reason);
        }
        if (!lostReported && items != null && !shop.busy(items.shopJob, SystemClock.uptimeMillis())
            && (items.shopJob == null || items.shopJob.id != shop.sentId())) {
            lostReported = true;
            return D2Text.get(context, R.string.shop_end_lost);
        }
        return null;
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
