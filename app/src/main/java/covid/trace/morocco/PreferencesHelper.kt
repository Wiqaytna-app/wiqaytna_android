package covid.trace.morocco

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {

    const val SHARED_PREF_NAME = "covid_19"
    const val LANGUAGE_KEY = "language_key"
    const val ARABIC_LANGUAGE_CODE = "ar"
    const val FRENCH_LANGUAGE_CODE = "fr"
    const val AGE_RANGE = "AGE_RANGE"
    const val GENDER = "GENDER"
    const val REGION = "REGION"
    const val PROVINCE = "PROVINCE"
    const val INFOS_UPDATE = "INFOS_UPDATE"
    const val STATS_UPDATE = "STATS_UPDATE"

    private var mPreference: SharedPreferences? = null

    fun init(context: Context) {
        mPreference = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setLanguage(language: String?) {
        setPreference(LANGUAGE_KEY, language)
    }

    fun getCurrentLanguage(): String =
        getStringPreference(LANGUAGE_KEY, ARABIC_LANGUAGE_CODE)

    fun setPreference(key: String?, value: String?) {
        mPreference?.put(key, value)
    }

    fun setPreference(key: String?, value: Boolean?) {
        mPreference?.put(key, value)
    }

    fun getStringPreference(key: String?, defValue: String?): String =
        mPreference?.get(key, defValue) as String

    fun getBooleanPreference(key: String?, defValue: Boolean): Boolean =
        mPreference?.get(key, defValue) as Boolean

}