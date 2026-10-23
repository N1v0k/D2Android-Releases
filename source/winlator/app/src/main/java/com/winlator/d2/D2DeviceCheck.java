package com.winlator.d2;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;






public final class D2DeviceCheck {
    public enum Level { SUPPORTED, UNTESTED, UNSUPPORTED }


    public enum Reason { OK, ABI, PAGES, ADRENO_6XX, ADRENO_NEW, ADRENO_OLD, ADRENO_UNLISTED, NO_KGSL, UNKNOWN_QUALCOMM, GPU }

    public static final long PAGE_SIZE = 4096;






    static final int[] TURNIP_MODELS = {
        605, 608, 610, 612, 613, 615, 616, 618, 619, 620, 621, 623, 630, 640, 643, 644, 650, 660, 663, 680, 690,
        702, 704, 710, 720, 722, 725, 730, 735, 740, 750,
        810, 812, 825, 829, 830, 840,
    };


    static final int[] SUPPORTED_MODELS = {
        710, 720, 722, 725, 730, 735, 740, 750,
        810, 812, 825, 829, 830, 840,
    };

    public static final class Result {
        public final Level level;
        public final Reason reason;

        public final int adreno;

        Result(Level level, Reason reason, int adreno) {
            this.level = level;
            this.reason = reason;
            this.adreno = adreno;
        }

        @Override
        public String toString() {
            return level + " " + reason + (adreno > 0 ? " adreno " + adreno + " turnip " + turnipKnows(adreno) : "");
        }
    }



    private static final Pattern ADRENO = Pattern.compile("adreno\\D{0,12}?(\\d{3,4})(?!\\d)", Pattern.CASE_INSENSITIVE);

    private D2DeviceCheck() {}


    public static int adrenoModel(String renderer) {
        if (renderer == null) return 0;
        Matcher m = ADRENO.matcher(renderer);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }


    public static boolean turnipKnows(int model) {
        return Arrays.binarySearch(TURNIP_MODELS, model) >= 0;
    }






    public static Result classifyAdreno(int model) {
        if (model < 600) return new Result(Level.UNSUPPORTED, Reason.ADRENO_OLD, model);
        if (model < 700) return new Result(Level.UNTESTED, Reason.ADRENO_6XX, model);
        if (model >= 900) return new Result(Level.UNTESTED, Reason.ADRENO_NEW, model);
        if (Arrays.binarySearch(SUPPORTED_MODELS, model) >= 0) return new Result(Level.SUPPORTED, Reason.OK, model);
        return new Result(Level.UNTESTED, Reason.ADRENO_UNLISTED, model);
    }








    public static Result classify(boolean arm64, long pageSize, String glVendor, String glRenderer, boolean kgsl) {
        if (!arm64) return new Result(Level.UNSUPPORTED, Reason.ABI, 0);
        if (pageSize > 0 && pageSize != PAGE_SIZE) return new Result(Level.UNSUPPORTED, Reason.PAGES, 0);
        String vendor = glVendor == null ? "" : glVendor.toLowerCase(Locale.ROOT);
        String renderer = glRenderer == null ? "" : glRenderer;
        int adreno = adrenoModel(renderer);
        boolean qualcomm = kgsl || vendor.contains("qualcomm") || adreno > 0;
        if (adreno > 0) {
            Result model = classifyAdreno(adreno);

            if (!kgsl && model.level != Level.UNSUPPORTED) return new Result(Level.UNTESTED, Reason.NO_KGSL, adreno);
            return model;
        }

        if (qualcomm) return new Result(Level.UNTESTED, Reason.UNKNOWN_QUALCOMM, 0);
        return new Result(Level.UNSUPPORTED, Reason.GPU, 0);
    }
}
