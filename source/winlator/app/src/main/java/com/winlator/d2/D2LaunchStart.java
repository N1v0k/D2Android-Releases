package com.winlator.d2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;





public final class D2LaunchStart {
    public enum Step {

        ASK_STORAGE,

        CHECK_FOLDER,

        NEWS,




        UPDATE_WAITING,

        LAUNCH
    }

    private D2LaunchStart() {}







    public static List<Step> steps(boolean storage, boolean rebuilt, boolean news, boolean updateShown) {
        if (!storage) return Collections.singletonList(Step.ASK_STORAGE);

        if (rebuilt) return Collections.singletonList(Step.CHECK_FOLDER);


        if (news) return Arrays.asList(Step.CHECK_FOLDER, Step.NEWS);
        if (updateShown) return Collections.singletonList(Step.UPDATE_WAITING);
        return Collections.singletonList(Step.LAUNCH);
    }






    public static boolean installAfterQuit(boolean extra, boolean rebuilt, boolean fromHistory) {
        return extra && !rebuilt && !fromHistory;
    }
}
