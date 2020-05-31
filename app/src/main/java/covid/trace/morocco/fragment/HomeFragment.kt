package covid.trace.morocco.fragment

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.Spannable
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import covid.trace.morocco.*
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.StatisticsResponse
import covid.trace.morocco.onboarding.OnboardingActivity
import covid.trace.morocco.onboarding.PersonalInfosActivity
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_new_home.*
import kotlinx.android.synthetic.main.home_setupe_incomplete.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_ENABLE_BT = 123
private const val PERMISSION_REQUEST_ACCESS_LOCATION = 456

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"

    private var mIsBroadcastListenerRegistered = false
    private var counter = 0

    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showSetup()

        Preference.registerListener(requireActivity().applicationContext, listener)

        showNonEmptyAnnouncement()

        if (WiqaytnaApp.statisticsData != null) {
            initViews(WiqaytnaApp.statisticsData!!)
        }

        settings.setOnClickListener {
            startActivity(Intent(context, PersonalInfosActivity::class.java))
        }

        faq.setOnClickListener {

            Utils.firebaseAnalyticsEvent(requireContext(), "open_faq", "13", "open faq")


            val url = if (TextUtils.equals(
                    PreferencesHelper.getCurrentLanguage(),
                    PreferencesHelper.FRENCH_LANGUAGE_CODE
                )
            ) {
                Utils.faqFrURL
            } else {
                Utils.faqArURL
            }
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.new_blue))
            builder.addDefaultShareMenuItem()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        Preference.registerListener(requireActivity().applicationContext, listener)

        return view
    }

    private fun initViews(response: StatisticsResponse) {
        val data = response.data
        confirmed_cases.text = data.new_confirmed
        recovered.text = data.new_recovered
        deaths.text = data.new_death
        val timeStamp = response.data.date._seconds * 1000
        if (TextUtils.equals(
                PreferencesHelper.getCurrentLanguage(),
                PreferencesHelper.FRENCH_LANGUAGE_CODE
            )
        ) {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH'h00'", Locale.FRANCE)
            updateDate.text = dateFormat.format(timeStamp)
        } else {
            val day = SimpleDateFormat("dd", Locale("ar", "ma")).format(timeStamp)
            val month = SimpleDateFormat("MMMM", Locale("ar", "ma")).format(timeStamp)
            val year = SimpleDateFormat("yyyy", Locale("ar", "ma")).format(timeStamp)
            val hour = SimpleDateFormat("HH", Locale("ar", "ma")).format(timeStamp)
            CentralLog.d("update time", "$day $month $year الساعة $hour")
            updateDate.text = "$day $month $year الساعة $hour"
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        share_card_view.setOnClickListener { shareThisApp() }
        animation_view.setOnClickListener {
            if (BuildConfig.DEBUG && ++counter == 2) {
                counter = 0
                var intent = Intent(context, PeekActivity::class.java)
                context?.startActivity(intent)
            }
        }
        //goes to permissions page
        btn_restart_app_setup.setOnClickListener {
            var intent = Intent(context, OnboardingActivity::class.java)
            intent.putExtra("page", 3)
            context?.startActivity(intent)
        }

        btn_announcement_close.setOnClickListener {
            clearAndHideAnnouncement()
        }

        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf("ShareText" to getString(R.string.share_message)))
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(activity as Activity) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    CentralLog.d(TAG, "Remote config fetch - success: $updated")
                } else {
                    CentralLog.d(TAG, "Remote config fetch - failed")
                }
            }
    }

    private fun isShowRestartSetup(): Boolean {
        if (canRequestBatteryOptimizerExemption()) {
            location_card_view.visibility = if (iv_location.isSelected) View.GONE else View.VISIBLE
            if (iv_bluetooth.isSelected && iv_location.isSelected && iv_battery.isSelected) return false
        } else {
            location_card_view.visibility = View.GONE
            if (iv_bluetooth.isSelected && iv_location.isSelected) return false
        }
        return true
    }

    private fun canRequestBatteryOptimizerExemption(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Utils.canHandleIntent(
            Utils.getBatteryOptimizerExemptionIntent(
                WiqaytnaApp.AppContext.packageName
            ), WiqaytnaApp.AppContext.packageManager
        )
    }

    fun showSetup() {
        view_setup.isVisible = isShowRestartSetup()
        view_complete.isVisible = !isShowRestartSetup()
        if (view_setup.isVisible) {
            Utils.firebaseAnalyticsEvent(
                requireContext(),
                "home_screen_setup_incomplete",
                "7",
                "home screen setup incomplete"
            )
        } else {
            Utils.firebaseAnalyticsEvent(requireContext(), "home_screen", "6", "home screen")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mIsBroadcastListenerRegistered) {
            // bluetooth on/off
            var f = IntentFilter()
            f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            requireActivity().registerReceiver(mBroadcastListener, f)
            mIsBroadcastListenerRegistered = true
        }

        view?.let {
            //location permission
            val perms = Utils.getRequiredPermissions()
            iv_location.isSelected =
                EasyPermissions.hasPermissions(activity as MainActivity, *perms)
            if (iv_location.isSelected) {
                iv_location.visibility = View.GONE
            } else {
                iv_location.visibility = View.VISIBLE
            }

            //push notification
            iv_push.isSelected =
                NotificationManagerCompat.from(activity as MainActivity).areNotificationsEnabled()

            if (iv_push.isSelected) {
                push_card_view.visibility = View.GONE
            } else {
                push_card_view.visibility = View.VISIBLE
            }

            bluetoothAdapter?.let {
                iv_bluetooth.isSelected = !it.isDisabled

                if (iv_bluetooth.isSelected) {
                    bluetooth_card_view.visibility = View.GONE
                } else {
                    bluetooth_card_view.visibility = View.VISIBLE
                }
            }

            //battery ignore list
            val powerManager =
                (activity as MainActivity).getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
            val packageName = (activity as MainActivity).packageName

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                battery_card_view.visibility = View.VISIBLE
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    iv_battery.isSelected = false
                    battery_card_view.visibility = View.VISIBLE
                    CentralLog.d(TAG, "Not on Battery Optimization whitelist")
                } else {
                    iv_battery.isSelected = true
                    battery_card_view.visibility = View.GONE
                    CentralLog.d(TAG, "On Battery Optimization whitelist")
                }
            } else {
                battery_card_view.visibility = View.GONE
            }

            showSetup()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mIsBroadcastListenerRegistered) {
            requireActivity().unregisterReceiver(mBroadcastListener)
            mIsBroadcastListenerRegistered = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Preference.unregisterListener(requireActivity().applicationContext, listener)
    }

    private fun shareThisApp() {
        Utils.firebaseAnalyticsEvent(
            requireContext(),
            FirebaseAnalytics.Event.SHARE,
            "14",
            "share the app"
        )
        var newIntent = Intent(Intent.ACTION_SEND)
        newIntent.type = "text/plain"
        newIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        var shareMessage = remoteConfig.getString("ShareText")
        newIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(newIntent, "choose one"))
    }

    private val mBroadcastListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                var state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_OFF) {
                    iv_bluetooth.isSelected = false
                    bluetooth_card_view.visibility = View.VISIBLE
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    iv_bluetooth.isSelected = false
                    bluetooth_card_view.visibility = View.VISIBLE
                } else if (state == BluetoothAdapter.STATE_ON) {
                    iv_bluetooth.isSelected = true
                    bluetooth_card_view.visibility = View.GONE
                }

                showSetup()
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            (activity as MainActivity).getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private fun enableBluetooth() {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.let {
            if (it.isDisabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    @AfterPermissionGranted(PERMISSION_REQUEST_ACCESS_LOCATION)
    fun setupPermissionsAndSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = Utils.getRequiredPermissions()
            if (EasyPermissions.hasPermissions(activity as MainActivity, *perms)) {
                // Already have permission, do the thing
            } else {
                // Do not have permissions, request them now
                if (!isDetached) {
                    EasyPermissions.requestPermissions(
                        this, getString(R.string.permission_location_rationale),
                        PERMISSION_REQUEST_ACCESS_LOCATION, *perms
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            iv_bluetooth.isSelected = resultCode == Activity.RESULT_OK
            if (iv_bluetooth.isSelected) {
                bluetooth_card_view.visibility = View.GONE
            } else {
                bluetooth_card_view.visibility = View.VISIBLE
            }
        }
        showSetup()
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        CentralLog.d(TAG, "[onRequestPermissionsResult]requestCode $requestCode")
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> {
                iv_location.isSelected = permissions.isNotEmpty()
                if (iv_location.isSelected) {
                    iv_location.visibility = View.GONE
                } else {
                    iv_location.visibility = View.VISIBLE
                }
            }
        }

        showSetup()
    }

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "ANNOUNCEMENT" -> showNonEmptyAnnouncement()
            }
        }

    private fun clearAndHideAnnouncement() {
        view_announcement.isVisible = false
        Preference.putAnnouncement(requireActivity().applicationContext, "")
    }

    private fun showNonEmptyAnnouncement() {
        val new = Preference.getAnnouncement(requireActivity().applicationContext)
        if (new.isEmpty()) return
        CentralLog.d(TAG, "FCM Announcement Changed to $new!")
        tv_announcement.text = HtmlCompat.fromHtml(new, HtmlCompat.FROM_HTML_MODE_COMPACT)
        tv_announcement.movementMethod = object : LinkMovementMethod() {
            override fun onTouchEvent(
                widget: TextView?,
                buffer: Spannable?,
                event: MotionEvent?
            ): Boolean {
                if (event?.action == MotionEvent.ACTION_UP && widget != null && buffer != null) {
                    val x = event.x - widget.totalPaddingLeft + widget.scrollX
                    val y = event.y - widget.totalPaddingTop + widget.scrollY
                    val layout = widget.layout
                    val line = layout.getLineForVertical(y.toInt())
                    val off = layout.getOffsetForHorizontal(line, x)

                    val link: Array<out URLSpan> = buffer.getSpans(off, off, URLSpan::class.java)
                    if (link.isNotEmpty()) {
                        clearAndHideAnnouncement()
                    }
                }
                return super.onTouchEvent(widget, buffer, event)
            }
        }
        view_announcement.isVisible = true
    }
}
