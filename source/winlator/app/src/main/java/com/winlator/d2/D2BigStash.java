package com.winlator.d2;

import android.content.Context;
import android.util.Log;

import com.winlator.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;








final class D2BigStash {
    private static final String TAG = "D2BigStash";

    static final int MAX_LINES = 10;

    private D2BigStash() {}


    private static final String[] TABLES = {"weapons", "armor", "misc"};
    private static final long MAX_TABLE = 8L << 20;


    static D2StashEdge.Sizes sizes(File gameDir) {
        return sizes(gameDir, null);
    }






    static D2StashEdge.Sizes sizes(File gameDir, D2ModInfo.Mod mod) {
        java.util.List<byte[]> bins = new ArrayList<>();
        if (mod != null) {
            if (mod.archives) return D2StashEdge.Sizes.fromBins();
            File excel = child(child(child(mod.files(), "data"), "global"), "excel");
            for (String t : TABLES) {
                if (child(excel, t + ".txt") != null) return D2StashEdge.Sizes.fromBins();
                File bin = child(excel, t + ".bin");
                if (bin == null) continue;
                if (bin.length() > MAX_TABLE) return D2StashEdge.Sizes.fromBins();
                try {
                    bins.add(java.nio.file.Files.readAllBytes(bin.toPath()));
                }
                catch (IOException e) {
                    Log.w(TAG, "mod item table not read, unknown sizes", e);
                    return D2StashEdge.Sizes.fromBins();
                }
            }
        }
        try (D2Sprites sprites = new D2Sprites(gameDir)) {
            for (String t : TABLES) bins.add(sprites.read("data\\global\\excel\\" + t + ".bin"));
        }
        catch (IOException | RuntimeException e) {
            Log.w(TAG, "item tables not read, unknown sizes", e);
            return D2StashEdge.Sizes.fromBins();
        }
        return D2StashEdge.Sizes.fromBins(bins.toArray(new byte[0][]));
    }


    private static File child(File dir, String name) {
        File[] entries = dir != null ? dir.listFiles() : null;
        if (entries != null) for (File e : entries) if (e.getName().equalsIgnoreCase(name)) return e;
        return null;
    }


    static String where(Context context, List<D2StashEdge.Place> places) {
        List<String> lines = new ArrayList<>();
        for (D2StashEdge.Place p : places) {
            String owner = p.character != null ? p.character
                : D2Text.get(context, p.hardcore ? R.string.big_stash_shared_hc : R.string.big_stash_shared);
            String line;
            if (p.unreadable) line = D2Text.get(context, R.string.big_stash_where_unreadable, owner);
            else if (p.pages.isEmpty()) line = D2Text.get(context, R.string.big_stash_where_save, owner);
            else line = D2Text.get(context, R.string.big_stash_where_pages, owner, join(p.pages));
            lines.add("• " + line);
        }
        if (lines.size() > MAX_LINES) {
            int rest = lines.size() - MAX_LINES + 1;
            lines = new ArrayList<>(lines.subList(0, MAX_LINES - 1));
            lines.add(D2Text.get(context, R.string.big_stash_more, rest));
        }
        return String.join("\n", lines);
    }

    private static String join(Iterable<Integer> pages) {
        StringBuilder sb = new StringBuilder();
        for (int p : pages) sb.append(sb.length() > 0 ? ", " : "").append(p);
        return sb.toString();
    }







    static String keepOnForStart(Context context, File gameDir, D2ModInfo.Mod mod) {
        boolean plugy = D2Settings.modEnabled(context) && D2Settings.plugy(context);
        if (!D2PlugY.isBundled(context) || plugy && D2Settings.bigStash(context)) return null;
        if (!plugy && !GameShadow.isModBundled(context)) return null;


        if (mod != null && !D2BigInventory.guarded(mod.inventory)) return null;

        File saves = mod != null ? mod.saves() : D2Saves.saveDir(gameDir);
        List<D2StashEdge.Place> places = D2StashEdge.check(saves, sizes(gameDir, mod), plugy);
        if (places.isEmpty()) return null;
        D2Settings.setBigStash(context, true);
        String in = mod != null ? " in mod " + mod.id : "";
        if (plugy) {
            D2ExitLog.note("big stash kept on: items outside 6x8" + in + " at " + places);
            return D2Text.get(context, R.string.big_stash_kept_on_start, where(context, places));
        }
        boolean modWasOff = !D2Settings.modEnabled(context);
        D2Settings.setModEnabled(context, true);
        D2Settings.putBoolean(context, D2Settings.KEY_PLUGY, true);
        D2ExitLog.note("PlugY kept on" + (modWasOff ? " with the mod" : "") + ": items outside 6x8" + in + " at " + places);
        String names = D2Text.get(context, modWasOff ? R.string.stash_guard_mod_plugy : R.string.stash_guard_plugy);
        return D2Text.get(context, R.string.stash_guard_kept_on_start, names, where(context, places));
    }
}
