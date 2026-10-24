package com.winlator.d2;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;







public final class D2Text {
    public static final String AUTO = D2AppLanguage.AUTO, GERMAN = D2AppLanguage.GERMAN, ENGLISH = D2AppLanguage.ENGLISH,
        FRENCH = D2AppLanguage.FRENCH, SPANISH = D2AppLanguage.SPANISH, ITALIAN = D2AppLanguage.ITALIAN;

    private static volatile int generation;

    private D2Text() {}


    public static String language(Context context) {

        return D2AppLanguage.pick(D2Settings.appLanguage(context), D2Settings.gameLanguage(context), Locale.getDefault().getLanguage());
    }


    public static Context wrap(Context context) {
        Locale locale = new Locale(language(context));
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }


    public static Resources resources(Context context) {
        return wrap(context).getResources();
    }

    public static String get(Context context, int id, Object... args) {
        return args.length == 0 ? resources(context).getString(id) : resources(context).getString(id, args);
    }


    public static void choose(Context context, String language) {
        D2Settings.setAppLanguage(context, language);
        generation++;
        D2KeepAliveService.createChannel(context);
        D2KeepAliveService.onLanguageChanged(context);
        Runnable l = listener;
        if (l != null) l.run();
    }





    public static void gameLanguageChanged(Context context) {
        if (!AUTO.equals(D2Settings.appLanguage(context))) return;
        generation++;
        D2KeepAliveService.createChannel(context);
        D2KeepAliveService.onLanguageChanged(context);
        Runnable l = listener;
        if (l != null) l.run();
    }


    private static volatile Runnable listener;

    public static void setListener(Runnable r) {
        listener = r;
    }

    public static int generation() {
        return generation;
    }
}
