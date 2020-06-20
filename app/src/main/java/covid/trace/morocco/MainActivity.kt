package covid.trace.morocco

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.iid.FirebaseInstanceId
import covid.trace.morocco.base.BaseFragmentActivity
import covid.trace.morocco.fragment.AdvicesFragment
import covid.trace.morocco.fragment.ForUseFragment
import covid.trace.morocco.fragment.HomeFragment
import covid.trace.morocco.fragment.StatisticsFragment
import covid.trace.morocco.logging.CentralLog
import kotlinx.android.synthetic.main.activity_main_new.*

class MainActivity : BaseFragmentActivity() {

    private val TAG = "MainActivity"
    private var crashlytics = FirebaseCrashlytics.getInstance()

    // navigation
    private var mNavigationLevel = 0
    var LAYOUT_MAIN_ID = 0
    private var selected = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)

        Utils.startBluetoothMonitoringService(this)

        LAYOUT_MAIN_ID = R.id.content

        val mOnNavigationItemSelectedListener =
                BottomNavigationView.OnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.navigation_home -> {

                            if (selected != R.id.navigation_home) {
                                openFragment(
                                        LAYOUT_MAIN_ID, HomeFragment(),
                                        HomeFragment::class.java.name
                                )
                            }
                            selected = R.id.navigation_home
                            return@OnNavigationItemSelectedListener true
                        }


                        R.id.navigation_news -> {

                            if (selected != R.id.navigation_news) {
                                openFragment(
                                        LAYOUT_MAIN_ID, StatisticsFragment(),
                                        StatisticsFragment::class.java.name
                                )
                            }
                            selected = R.id.navigation_news
                            return@OnNavigationItemSelectedListener true
                        }


                        R.id.navigation_upload -> {

                            if (selected != R.id.navigation_upload) {
                                openFragment(
                                        LAYOUT_MAIN_ID, ForUseFragment(),
                                        ForUseFragment::class.java.name
                                )
                            }

                            selected = R.id.navigation_upload
                            return@OnNavigationItemSelectedListener true
                        }

                        R.id.navigation_help -> {
                            if (selected != R.id.navigation_help) {
                                openFragment(
                                        LAYOUT_MAIN_ID, AdvicesFragment(),
                                        AdvicesFragment::class.java.name
                                )
                            }
                            selected = R.id.navigation_help
                            return@OnNavigationItemSelectedListener true
                        }
                    }
                    false
                }

        nav_view.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        goToSelectedItem()

        getFCMToken()

        if (!PreferencesHelper.getBooleanPreference(PreferencesHelper.INFOS_UPDATE, false)) {
            setUser(PreferencesHelper.getCurrentLanguage())
                    .addOnSuccessListener {
                        CentralLog.d("language upadte", "updated")
                        PreferencesHelper.setPreference(PreferencesHelper.INFOS_UPDATE, true)
                    }.addOnFailureListener { exception ->
                        crashlytics.recordException(exception)
                        crashlytics.setCustomKey("error", "couldn't send choosen language info")
                        CentralLog.d("language upadte", "not updated")
                        PreferencesHelper.setPreference(PreferencesHelper.INFOS_UPDATE, false)
                    }
        }
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
                        // Log and toast
                        CentralLog.d(TAG, "FCM token: $token")
                    }
                }


    }

    fun goToSelectedItem() {
        val selectedString = PreferencesHelper.getStringPreference("selected", "-1")

        when(selectedString.toInt()){
            R.id.navigation_home -> nav_view.selectedItemId = R.id.navigation_home
            R.id.navigation_news -> nav_view.selectedItemId = R.id.navigation_news
            R.id.navigation_help -> nav_view.selectedItemId = R.id.navigation_help
            R.id.navigation_upload -> nav_view.selectedItemId = R.id.navigation_upload
            else -> nav_view.selectedItemId = R.id.navigation_home
        }
    }

    fun openFragment(
            containerViewId: Int,
            fragment: Fragment,
            tag: String
    ) {
        try { // pop all fragments
            supportFragmentManager.popBackStackImmediate(
                    LAYOUT_MAIN_ID,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            mNavigationLevel = 0
            val transaction =
                    supportFragmentManager.beginTransaction()
            transaction.replace(containerViewId, fragment, tag)
            transaction.commit()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun setUser(lang: String): Task<HttpsCallableResult> {
        // Create the arguments to the callable function.
        val data = hashMapOf("lang" to lang)
        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        return functions
                .getHttpsCallable("updateUser")
                .call(data)
    }

    override fun onStop() {
        super.onStop()
        PreferencesHelper.setPreference("selected", "$selected")
    }
}
