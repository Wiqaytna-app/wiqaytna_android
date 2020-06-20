package covid.trace.morocco

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import covid.trace.morocco.WiqaytnaApp.Companion.firebaseToken
import covid.trace.morocco.base.BaseActivity
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.StatisticsResponse
import covid.trace.morocco.onboarding.PreOnboardingActivity

class SplashActivity : BaseActivity(), ProviderInstaller.ProviderInstallListener {

    private val SPLASH_TIME: Long = 4000
    private var needToUpdateApp = false
    private val TAG = "SpalshActivity"
    private lateinit var mHandler: Handler
    private var crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mHandler = Handler()
        getFCMToken()
        ProviderInstaller.installIfNeededAsync(this, this)
        //check if the intent was from notification and its a update notification
        intent.extras?.let {
            val notifEvent: String? = it.getString("event", null)

            notifEvent?.let {
                if (it.equals("update")) {
                    needToUpdateApp = true
                    intent = Intent(Intent.ACTION_VIEW)
                    //Copy App URL from Google Play Store.
                    intent.data = Uri.parse(BuildConfig.STORE_URL)

                    startActivity(intent)
                    finish()
                }
            }
        }

        if (TextUtils.isEmpty(
                        PreferencesHelper.getStringPreference(
                                PreferencesHelper.STATS_UPDATE, ""
                        )
                )
        ) {
            Utils.getStatistics()
        } else {
            val json = PreferencesHelper.getStringPreference(PreferencesHelper.STATS_UPDATE, "")
            val response = Gson().fromJson(json, StatisticsResponse::class.java)
            val currentTimeInSeconds = System.currentTimeMillis() / 1000
            val diff = currentTimeInSeconds - response.data.date._seconds
            val hoursElapsed = diff / 3600
            CentralLog.d("response hours elapsed", hoursElapsed.toString())
            if (hoursElapsed > 2) {
                Utils.getStatistics()
            } else {
                WiqaytnaApp.statisticsData = response
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        if (!needToUpdateApp) {
            mHandler.postDelayed({
                if (!Utils.isEmulator()) {
                    goToNextScreen()
                } else {
                    finish()
                }

            }, SPLASH_TIME)
        }
    }

    private fun goToNextScreen() {
        if (!Preference.isOnBoarded(this)) {
            startActivity(Intent(this, PreOnboardingActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finishAndRemoveTask()
    }

    private fun getFCMToken() {
        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        CentralLog.w(TAG, "failed to get fcm token ${task.exception}")
                        return@addOnCompleteListener
                    } else {
                        // Get new Instance ID token
                        val token = task.result?.token
                        firebaseToken = token.toString()
                        // Log and toast
                        CentralLog.d(TAG, "FCM token: $token")
                    }
                }
    }

    override fun onProviderInstallFailed(p0: Int, p1: Intent?) {
        CentralLog.d(TAG, "provider update failed")
    }

    override fun onProviderInstalled() {
        CentralLog.d(TAG, "provider update suceeded")
    }

}
