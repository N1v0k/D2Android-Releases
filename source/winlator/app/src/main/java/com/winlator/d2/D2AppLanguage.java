package com.winlator.d2;





final class D2AppLanguage {
    static final String AUTO = "auto", GERMAN = "de", ENGLISH = "en", FRENCH = "fr", SPANISH = "es", ITALIAN = "it";

    static final String[] LANGUAGES = {GERMAN, ENGLISH, FRENCH, SPANISH, ITALIAN};

    private D2AppLanguage() {}






    static String pick(String chosen, String game, String system) {
        if (known(chosen)) return chosen;
        if (game != null) {
            switch (game) {
                case "deu": return GERMAN;
                case "fra": return FRENCH;
                case "esp": return SPANISH;
                case "ita": return ITALIAN;
                default: return ENGLISH;
            }
        }
        return known(system) ? system : ENGLISH;
    }





    static String gameIso(String game) {
        if (game == null) return null;
        switch (game) {
            case "deu": return GERMAN;
            case "eng": return ENGLISH;
            case "fra": return FRENCH;
            case "esp": return SPANISH;
            case "ita": return ITALIAN;
            case "por": return "pt";
            case "pol": return "pl";
            case "rus": return "ru";
            case "jpn": return "ja";
            case "kor": return "ko";
            case "chi": case "sin": return "zh";
            default: return null;
        }
    }

    private static boolean known(String language) {
        for (String l : LANGUAGES) if (l.equals(language)) return true;
        return false;
    }
}
