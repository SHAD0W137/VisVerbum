package com.example.visverbum;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import androidx.preference.PreferenceManager;
import com.example.visverbum.ui.options.OptionsFragment;
import java.util.Locale;

public class LocaleHelper {

    public static Context setLocale(Context context) {
        return updateResources(context, getPersistedLocale(context));
    }

    public static Context setNewLocale(Context context, String language) {
        persistLocale(context, language);
        return updateResources(context, language);
    }

    public static String getPersistedLocale(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(OptionsFragment.KEY_APP_LANGUAGE, Locale.getDefault().getLanguage());
    }

    private static void persistLocale(Context context, String language) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(OptionsFragment.KEY_APP_LANGUAGE, language);
        editor.apply();
    }

    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        config.setLocales(localeList);
        res.updateConfiguration(config, res.getDisplayMetrics());
        return context;
    }
}