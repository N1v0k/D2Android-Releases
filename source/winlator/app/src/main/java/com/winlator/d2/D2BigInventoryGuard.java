package com.winlator.d2;

import android.content.Context;

import com.winlator.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;







final class D2BigInventoryGuard {
    private D2BigInventoryGuard() {}






    static List<D2StashEdge.Place> check(File gameDir, D2ModInfo.Mod mod) {
        File saves = mod != null ? mod.saves() : D2Saves.saveDir(gameDir);
        List<D2StashEdge.Place> quick = D2InventoryEdge.check(saves, D2StashEdge.Sizes.fromBins());
        return quick.isEmpty() ? quick : D2InventoryEdge.check(saves, D2BigStash.sizes(gameDir, mod));
    }


    static String where(Context context, List<D2StashEdge.Place> places) {
        List<String> lines = new ArrayList<>();
        for (D2StashEdge.Place p : places) {
            lines.add("• " + (p.unreadable ? D2Text.get(context, R.string.big_stash_where_unreadable, p.character)
                : D2Text.get(context, R.string.big_inv_where, p.character)));
        }
        if (lines.size() > D2BigStash.MAX_LINES) {
            int rest = lines.size() - D2BigStash.MAX_LINES + 1;
            lines = new ArrayList<>(lines.subList(0, D2BigStash.MAX_LINES - 1));
            lines.add(D2Text.get(context, R.string.big_stash_more, rest));
        }
        return String.join("\n", lines);
    }










    static String keepOnForStart(Context context, File gameDir, D2ModInfo.Mod mod) throws java.io.IOException {
        if (!D2BigInventory.guarded(mod != null && mod.inventory)) return null;
        boolean possible = GameShadow.isModBundled(context) && D2BigInventory.languageOk(D2Settings.gameLanguage(context));
        boolean modOn = D2Settings.modEnabled(context);
        if (possible && modOn && D2Settings.bigInventory(context)) return null;
        List<D2StashEdge.Place> places = check(gameDir, mod);
        if (places.isEmpty()) return null;
        String in = mod != null ? " in mod " + mod.id : "";
        if (!possible) {
            D2ExitLog.note("start stopped: items outside 10x4 and no big inventory possible" + in + " at " + places);
            throw new java.io.IOException(D2Text.get(context, R.string.big_inv_start_refused, where(context, places)));
        }
        D2Settings.setBigInventory(context, true);
        if (!modOn) D2Settings.setModEnabled(context, true);
        D2ExitLog.note("big inventory kept on" + (modOn ? "" : " with the mod") + ": items outside 10x4" + in + " at " + places);

        return D2Text.get(context, modOn ? R.string.big_inv_kept_on_start : R.string.big_inv_kept_on_start_mod,
            where(context, places));
    }
}
