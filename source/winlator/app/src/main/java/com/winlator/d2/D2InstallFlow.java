package com.winlator.d2;

import java.io.File;






final class D2InstallFlow {
    static final long MB = 1024L * 1024, GB = 1024 * MB;




    static final long INSTALLER_BYTES = 2200 * MB;

    static final long GAME_BYTES = 2400 * MB;

    static final long MARGIN_BYTES = 512 * MB;

    static final long FULL_BYTES = 512 * MB;

    private D2InstallFlow() {}





    static long neededAtStart(long installerBytesThere, long reusableGameBytes) {
        return Math.max(0, INSTALLER_BYTES - Math.max(0, installerBytesThere)) + neededBeforeKeys(reusableGameBytes);
    }


    static long neededBeforeKeys(long reusableGameBytes) {
        return Math.max(0, GAME_BYTES - Math.max(0, reusableGameBytes)) + MARGIN_BYTES;
    }

    static boolean enough(long usableBytes, long neededBytes) {
        return usableBytes >= neededBytes;
    }


    static boolean storageFull(long usableBytes) {
        return usableBytes < FULL_BYTES;
    }





    static D2InstallFailure.Kind fetchFailure(boolean refused, boolean access, long usableBytes) {
        if (refused) return D2InstallFailure.Kind.REFUSED;
        if (!access) return D2InstallFailure.Kind.NO_ACCESS;
        if (storageFull(usableBytes)) return D2InstallFailure.Kind.NO_SPACE;
        return D2InstallFailure.Kind.NO_NETWORK;
    }


    enum AfterInstaller {

        OK,

        WRONG_KEY,

        NO_START,

        NO_SPACE,

        UNKNOWN_DIALOG,

        INSTALLER
    }







    static AfterInstaller afterInstaller(String helperResult, boolean noStartChosen, boolean archive, long usableBytes) {
        return afterInstaller(helperResult, noStartChosen, false, archive, usableBytes);
    }


    static AfterInstaller afterInstaller(String helperResult, boolean noStartChosen, boolean unknownChosen, boolean archive, long usableBytes) {
        if (helperResult != null && noStartChosen) return AfterInstaller.NO_START;
        if (helperResult != null && unknownChosen) return AfterInstaller.UNKNOWN_DIALOG;
        if ("wrong-key".equals(helperResult)) return AfterInstaller.WRONG_KEY;
        if (helperResult == null && archive) return AfterInstaller.OK;


        if (storageFull(usableBytes)) return AfterInstaller.NO_SPACE;
        return AfterInstaller.INSTALLER;
    }


    enum AfterPatch { VERIFY, PATCH_FAILED }






    static AfterPatch afterPatch(boolean logSucceeded, boolean gameExeIs113d) {
        return logSucceeded || gameExeIs113d ? AfterPatch.VERIFY : AfterPatch.PATCH_FAILED;
    }


    enum AfterCheck { DONE, REPATCH, CHECK_FAILED }







    static AfterCheck afterCheck(boolean passed, boolean backups, boolean repatchedOnce) {
        if (passed) return AfterCheck.DONE;
        return backups && !repatchedOnce ? AfterCheck.REPATCH : AfterCheck.CHECK_FAILED;
    }






    static boolean discard(D2InstallFailure.Kind kind, boolean awaitsPatch) {
        return kind.discard && !awaitsPatch;
    }





    static D2InstallPlan.Language resumeLanguage(D2InstallPlan.Language stored, D2InstallPlan.Language chosen) {
        return stored != null ? stored : chosen;
    }






    static boolean launcherResumes(boolean unfinishedMarker, boolean usable, boolean saves, boolean checkPassed) {



        return unfinishedMarker && (!usable || !saves && !checkPassed);
    }


    static boolean inRoot(File downloadDir, File dir) {
        File parent = dir != null ? dir.getAbsoluteFile().getParentFile() : null;
        return parent != null && parent.equals(D2InstallPlan.root(downloadDir).getAbsoluteFile());
    }









    enum ResumeNote { NONE, SAME, NEW_FOLDER }






    static ResumeNote resumeNote(boolean interrupted, boolean chosenStoppedWithSaves, boolean finishedOffered) {
        if (finishedOffered) return ResumeNote.NONE;
        if (chosenStoppedWithSaves) return ResumeNote.NEW_FOLDER;
        return interrupted ? ResumeNote.SAME : ResumeNote.NONE;
    }







    static final int MENU_STATUSES = 10;


    static boolean menusWithoutState(boolean everState, int statusesSinceState) {
        return !everState || statusesSinceState >= MENU_STATUSES;
    }





    static Boolean quitLossNow(boolean stateFresh, boolean canSave, boolean menus) {
        if (stateFresh) return !canSave;
        if (menus) return false;
        return null;
    }

    static boolean quitLoses(boolean previous, boolean stateFresh, boolean canSave, boolean menus) {
        Boolean now = quitLossNow(stateFresh, canSave, menus);
        return now != null ? now : previous;
    }
}
