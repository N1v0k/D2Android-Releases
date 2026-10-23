package com.winlator.d2;

import android.content.Context;

import com.winlator.core.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;








public final class D2PlugY {
    private static final String ASSET_DIR = "d2/plugy";

    private D2PlugY() {}

    public static boolean isBundled(Context context) {
        try (InputStream ignored = context.getAssets().open(ASSET_DIR + "/PlugY.dll")) {
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }


    public static void install(Context context, File shadowDir, boolean enabled) throws IOException {
        File dll = new File(shadowDir, "PlugY.dll");
        D2Settings.setRespecStarted(context, false);
        if (!enabled || !isBundled(context)) {

            if (dll.exists() && !dll.delete()) throw new IOException("PlugY.dll lässt sich nicht entfernen");
            return;
        }
        FileUtils.copy(context, ASSET_DIR + "/PlugY.dll", dll);
        File folder = new File(shadowDir, "PlugY");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Cannot create " + folder);
        String[] files = context.getAssets().list(ASSET_DIR + "/PlugY");
        if (files != null) for (String name : files) FileUtils.copy(context, ASSET_DIR + "/PlugY/" + name, new File(folder, name));
        boolean respec = D2Settings.respec(context);
        FileUtils.writeString(new File(shadowDir, "PlugY.ini"), ini(D2Settings.bigStash(context), respec,
            D2Settings.statsPages(context), extras(context)));
        D2Settings.setRespecStarted(context, respec);
    }













    static final int UNASSIGN_KEY = 0x87;





    static String ini(boolean bigStash, boolean respec, boolean statsPages) {
        String unassign = respec ? "1" : "0";
        return "; Written by D2Android (D2PlugY) - changes are overwritten at every start\r\n"
            + "[LAUNCHING]\r\nParam=\r\nLibrary=PlugY.dll\r\n\r\n"
            + "[GENERAL]\r\nActivePlugin=1\r\nDisableBattleNet=1\r\nActiveLogFile=1\r\nDllToLoad=\r\nDllToLoad2=\r\n"
            + "ActiveCommands=1\r\nActiveCheckMemory=1\r\n\r\n"
            + "[WINDOWED]\r\nActiveWindowed=0\r\nRemoveBorder=0\r\nWindowOnTop=0\r\nMaximized=0\r\nSetWindowPos=0\r\n"
            + "X=0\r\nY=0\r\nWidth=0\r\nHeight=0\r\nLockMouseOnStartup=0\r\n\r\n"
            + "[LANGUAGE]\r\nActiveChangeLanguage=0\r\nSelectedLanguage=ENG\r\n\r\n"
            + "[SAVEPATH]\r\nActiveSavePathChange=0\r\nSavePath=Mod PlugY\r\nActiveAutoBackup=1\r\nMaxBackupPerCharacter=10\r\n\r\n"
            + "[MAIN SCREEN]\r\nActiveVersionTextChange=0\r\nVersionText=\r\nColorOfVersionText=0\r\n"
            + "ActivePrintPlugYVersion=1\r\nColorOfPlugYVersion=4\r\n\r\n"
            + "[STASH]\r\nActiveBigStash=" + (bigStash ? 1 : 0) + "\r\nActiveMultiPageStash=1\r\nNbPagesPerIndex=10\r\nNbPagesPerIndex2=100\r\n"
            + "MaxPersonnalPages=0\r\nAutoRenameStashPage=1\r\nPersonalNormalPageColor=0\r\nPersonalIndexPageColor=12\r\n"
            + "PersonalMainIndexPageColor=9\r\nSharedNormalPageColor=4\r\nSharedIndexPageColor=8\r\nSharedMainIndexPageColor=1\r\n"
            + "ActiveSharedStash=1\r\nActiveSharedStashInMultiPlayer=0\r\nSeparateHardcoreStash=1\r\nOpenSharedStashOnLoading=0\r\n"
            + "DisplaySharedSetItemNameInGreen=1\r\nMaxSharedPages=5\r\nActiveSharedGold=1\r\n\r\n"
            + "[STATS POINTS]\r\nActiveStatsUnassignment=" + unassign + "\r\nKeyUsed=" + UNASSIGN_KEY + "\r\nActiveShiftClickLimit=0\r\nLimitValueToShiftClick=10\r\n\r\n"
            + "[SKILLS POINTS]\r\nActiveSkillsUnassignment=" + unassign + "\r\nActiveSkillsUnassignmentOneForOne=0\r\n\r\n"
            + "[WORLD EVENT]\r\nActiveWorldEvent=0\r\nShowCounterInAllDifficulty=0\r\nItemsToSell=The Stone of Jordan\r\nMonsterID=333\r\n"
            + "OwnSOJSoldChargeFor=100\r\nInititalSOJSoldMin=200\r\nInititalSOJSoldMax=3000\r\nTriggerAtEachSOJSoldMin=75\r\n"
            + "TriggerAtEachSOJSoldMax=125\r\nActiveAutoSell=1\r\nTimeBeforeAutoSellMin=0\r\nTimeBeforeAutoSellMax=1200\r\n\r\n"
            + "[UBER QUEST]\r\nActiveUberQuest=0\r\nUberMephistoX=25130\r\nUberMephistoY=5143\r\nUberDiabloX=25139\r\nUberDiabloY=5139\r\n"
            + "UberBaalX=25139\r\nUberBaalY=5135\r\nActiveUberMinions=1\r\nUberMephistoSpawnPercent=80\r\nUberMephistoSpawnRadius=30\r\n"
            + "UberBaalSpawnPercent=30\r\nUberBaalSpawnRadius=30\r\nUberDiabloSpawnPercent=30\r\nUberDiabloSpawnRadius=30\r\n"
            + "ActiveUberDiabloRushTweekAI=1\r\nActiveUberBaalTeleportTweekAI=1\r\nActiveUberBaalChillingArmorTweekAI=1\r\n"
            + "UberBaalChillingArmorTimer=6000\r\n\r\n"
            + "[INTERFACE]\r\nActiveNewStatsInterface=" + (statsPages ? 1 : 0) + "\r\nSelectMainPageOnOpenning=1\r\n"
            + "PrintButtonsBackgroundOnMainStatsPage=1\r\n\r\n"
            + "[EXTRA]\r\nActiveLaunchAnyNumberOfLOD=0\r\nAlwaysRegenMapInSP=0\r\nNBPlayersByDefault=0\r\nActiveDisplayItemLevel=1\r\n"
            + "AlwaysDisplayLifeAndManaValues=0\r\nEnabledTXTFilesWhenMSExcelOpenIt=0\r\nActiveDisplayBaseStatsValue=0\r\n"
            + "ActiveLadderRunewords=1\r\nActiveCowPortalWhenCowKingWasKilled=0\r\nActiveDoNotCloseNihlathakPortal=0\r\n"
            + "MoveCainNearHarrogathWaypoint=0\r\n";
    }








    static final String[] EXTRA_KEYS = {"ActiveSharedGold", "ActiveLadderRunewords", "ActiveDisplayItemLevel",
        "ActiveDisplayBaseStatsValue", "ActiveWorldEvent", "ActiveUberQuest", "ActiveCowPortalWhenCowKingWasKilled",
        "ActiveDoNotCloseNihlathakPortal", "MoveCainNearHarrogathWaypoint"};
    static final boolean[] EXTRA_DEFAULTS = {true, true, true, false, false, false, false, false, false};

    static final int FIRST_GAME_CHANGE = 4;


    static boolean[] extras(Context context) {
        boolean[] on = new boolean[EXTRA_KEYS.length];
        for (int i = 0; i < on.length; i++) on[i] = D2Settings.plugyExtra(context, EXTRA_KEYS[i], EXTRA_DEFAULTS[i]);
        return on;
    }





    static String ini(boolean bigStash, boolean respec, boolean statsPages, boolean[] on) {
        String ini = ini(bigStash, respec, statsPages);
        for (int i = 0; i < EXTRA_KEYS.length; i++) {
            String key = "\r\n" + EXTRA_KEYS[i] + "=";
            int at = ini.indexOf(key);
            if (at < 0) throw new IllegalStateException("PlugY.ini without " + EXTRA_KEYS[i]);
            int end = ini.indexOf("\r\n", at + key.length());
            ini = ini.substring(0, at + key.length()) + (on[i] ? "1" : "0") + ini.substring(end);
        }
        return ini;
    }
}
