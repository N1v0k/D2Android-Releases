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

    private static boolean known(String language) {
        for (String l : LANGUAGES) if (l.equals(language)) return true;
        return false;
    }
}
