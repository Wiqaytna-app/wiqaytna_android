package covid.trace.morocco;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.StringDef;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;


public class LocaleHelper {

    private static final String LANGUAGE_PREF_KEY = "language_pref_key";
    public static final String FRENCH = "fr";
    public static final String ARABIC = "ar";

    private static LocaleHelper instance;

    public static LocaleHelper getInstance() {
        return instance;
    }

    private SharedPreferences sharedPreferences;
    private String currentLocale;

    public String getCurrentLocale() {
        return currentLocale;
    }

    private LocaleHelper() {
    }

    public static void init(Context context) {
        instance = new LocaleHelper();
        instance.sharedPreferences = context.getSharedPreferences("covid_19", Context.MODE_PRIVATE);
        instance.currentLocale = instance.sharedPreferences.getString(PreferencesHelper.LANGUAGE_KEY, ARABIC);
    }

    public Context setLocale(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            return updateLocale(context, currentLocale);
        }
        return updateLocaleLegacy(context, currentLocale);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Context updateLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    private Context updateLocaleLegacy(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());

        return context;
    }

    public boolean setLocaleIfNeeded(@Language String language) {
        if (language.equals(currentLocale)) {
            return false;
        }
        currentLocale = language;
        sharedPreferences.edit()
                .putString(PreferencesHelper.LANGUAGE_KEY, language)
                .apply();
        return true;
    }

    public void switchLocale() {
        setLocaleIfNeeded(currentLocale.equals(FRENCH) ? ARABIC : FRENCH);
        String font = currentLocale.equals(FRENCH) ? "fonts/Barlow-Regular.ttf" : "fonts/Almarai-Regular.ttf";
        setFont(font);
    }

    public static void setFont(String fontPath) {
        ViewPump.init(
                ViewPump.builder()
                        .addInterceptor(
                                new CalligraphyInterceptor(
                                        new CalligraphyConfig.Builder()
                                                .setDefaultFontPath(fontPath)
                                                .setFontAttrId(R.attr.fontPath)
                                                .build()
                                )
                        )
                        .build()
        );
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({FRENCH, ARABIC})
    private @interface Language {
    }
}
