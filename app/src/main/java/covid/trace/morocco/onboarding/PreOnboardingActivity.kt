package covid.trace.morocco.onboarding

import android.content.Intent
import android.os.Bundle
import covid.trace.morocco.LocaleHelper
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.base.BaseFragmentActivity
import kotlinx.android.synthetic.main.main_activity_onboarding.*

class PreOnboardingActivity : BaseFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_onboarding)

        Utils.firebaseAnalyticsEvent(baseContext, "first_screen", "1", "Onboarding first screen")

        btn_onboardingStart.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        language.setOnClickListener {
            Utils.firebaseAnalyticsEvent(baseContext, "first_screen_change_language", "19", "Onboarding third screen resend otp")
            LocaleHelper.getInstance().switchLocale()
            recreate()
        }

        tou.setOnClickListener {
            startActivity(Intent(this, TouActivity::class.java))
        }
    }
}
