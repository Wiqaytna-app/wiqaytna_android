package covid.trace.morocco;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    public static final String LANGUAGE_KEY = "language_key";
    public static final String ARABIC_LANGUAGE_CODE = "ar";
    public static final String FRENCH_LANGUAGE_CODE = "fr";
    public static final String AGE_RANGE = "AGE_RANGE";
    public static final String GENDER = "GENDER";
    public static final String REGION = "REGION";
    public static final String PROVINCE = "PROVINCE";
    public static final String INFOS_UPDATE = "INFOS_UPDATE";
    public static final String STATS_UPDATE = "STATS_UPDATE";
    private static PreferencesHelper sInstance = null;
    private final SharedPreferences mPreference;

    private PreferencesHelper(Context context) {
        mPreference = context.getSharedPreferences("covid_19", Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (sInstance == null)
            sInstance = new PreferencesHelper(context);
    }

    public static void setLanguage(String language) {
        setPreference(LANGUAGE_KEY, language);
    }

    public static String getCurrentLanguage() {
        return getStringPreference(LANGUAGE_KEY, PreferencesHelper.ARABIC_LANGUAGE_CODE);
    }

    public static void setPreference(String key, String value) {
        sInstance.mPreference.edit().putString(key, value).apply();
    }

    public static void setPreference(String key, Boolean value) {
        sInstance.mPreference.edit().putBoolean(key, value).apply();
    }

    public static String getStringPreference(String key, String defValue) {
        return sInstance.mPreference.getString(key, defValue);
    }

    public static boolean getBooleanPreference(String key, boolean defValue) {
        return sInstance.mPreference.getBoolean(key, defValue);
    }
}
