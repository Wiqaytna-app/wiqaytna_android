package covid.trace.morocco.onboarding

import android.os.Bundle
import android.text.TextUtils
import covid.trace.morocco.LocaleHelper
import covid.trace.morocco.PreferencesHelper
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.base.BaseFragmentActivity
import kotlinx.android.synthetic.main.activity_tou.*

class TouActivity : BaseFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tou)
        Utils.firebaseAnalyticsEvent(baseContext, "CGU_screen", "16", "Terms of use screen")
        initWebView()
        setClickListeners()
    }

    private fun initWebView() {
        if (TextUtils.equals(
                PreferencesHelper.getCurrentLanguage(),
                PreferencesHelper.FRENCH_LANGUAGE_CODE
            )
        ) {
            webView.loadUrl("file:///android_asset/tou.html")
        } else {
            webView.loadUrl("file:///android_asset/tou_AR.html")
        }
    }

    private fun setClickListeners() {
        language.setOnClickListener {
            Utils.firebaseAnalyticsEvent(
                baseContext,
                "CGU_screen_change_language",
                "23",
                "Onboarding fourth screen"
            )
            LocaleHelper.switchLocale()
            recreate()
        }

        back.setOnClickListener {
            finish()
        }
    }
}
