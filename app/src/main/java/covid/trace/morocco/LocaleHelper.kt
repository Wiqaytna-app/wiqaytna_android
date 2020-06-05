package covid.trace.morocco

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringDef
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

object LocaleHelper {

    private const val SHARED_PREF_NAME = "covid_19"
    private const val PATH_FONT_FR = "fonts/Barlow-Regular.ttf"
    private const val PATH_FONT_AR = "fonts/Almarai-Regular.ttf"
    private const val LANGUAGE_PREF_KEY = "language_pref_key"
    const val FRENCH = "fr"
    const val ARABIC = "ar"

    private var sharedPreferences: SharedPreferences? = null
    var currentLocale: String? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        currentLocale =  sharedPreferences?.get(PreferencesHelper.LANGUAGE_KEY, ARABIC)
    }

    fun setLocale(context: Context?): Context? {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            updateLocale(context, currentLocale)
        } else updateLocaleLegacy(context, currentLocale)
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun updateLocale(context: Context?, language: String?): Context? {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context?.resources?.configuration
        config?.setLocale(locale)
        return context?.createConfigurationContext(config)
    }

    private fun updateLocaleLegacy(context: Context?, language: String?): Context? {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val res = context?.resources
        val config = res?.configuration
        config?.locale = locale
        res?.updateConfiguration(config, res.displayMetrics)
        return context
    }

    fun setLocaleIfNeeded(@Language language: String): Boolean {
        if (language == currentLocale) {
            return false
        }
        currentLocale = language
        sharedPreferences?.put(PreferencesHelper.LANGUAGE_KEY, language)
        return true
    }

    fun switchLocale() {
        setLocaleIfNeeded(if (currentLocale == FRENCH) ARABIC else FRENCH)
        val font =
            if (currentLocale == FRENCH) PATH_FONT_FR else PATH_FONT_AR
        setFont(font)
    }

    fun setFont(fontPath: String?) {
        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath(fontPath)
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                )
                .build()
        )
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(FRENCH, ARABIC)
    private annotation class Language
}