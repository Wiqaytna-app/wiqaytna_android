package covid.trace.morocco.onboarding

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import covid.trace.morocco.*
import covid.trace.morocco.R
import covid.trace.morocco.WiqaytnaApp.Companion.firebaseToken
import covid.trace.morocco.WiqaytnaApp.Companion.uid
import covid.trace.morocco.base.BaseFragmentActivity
import covid.trace.morocco.idmanager.TempIDManager
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.services.BluetoothMonitoringService
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_otp_new.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import kotlin.properties.Delegates

private const val REQUEST_ENABLE_BT = 123
private const val PERMISSION_REQUEST_ACCESS_LOCATION = 456
private const val BATTERY_OPTIMISER = 789

class OnboardingActivity : BaseFragmentActivity(),
    SetupFragment.OnFragmentInteractionListener,
    RegisterNumberFragment.OnFragmentInteractionListener,
    OTPFragment.OnFragmentInteractionListener {

    // [START declare_auth]
    //private var uid: String = ""
    private lateinit var auth: FirebaseAuth
    // [END declare_auth]

    private var crashlytics = FirebaseCrashlytics.getInstance()

    private var TAG: String = "OnboardingActivity"
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var bleSupported = false
    private var speedUp = false
    private var resendingCode = false

    private val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
    private var credential: PhoneAuthCredential by Delegates.notNull()
    private var verificationId: String by Delegates.notNull()
    private var resendToken: PhoneAuthProvider.ForceResendingToken by Delegates.notNull()
    private val phoneNumberVerificationCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(receivedCredential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                CentralLog.d(TAG, "onVerificationCompleted: $receivedCredential")
                credential = receivedCredential
                signInWithPhoneAuthCredential(credential)
                speedUp = true
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    CentralLog.d(TAG, "FirebaseAuthInvalidCredentialsException", e)
//                    alertDialog(getString(R.string.verification_failed))
                    updatePhoneNumberError(getString(R.string.invalid_number))

                } else if (e is FirebaseTooManyRequestsException) {
                    CentralLog.d(TAG, "FirebaseTooManyRequestsException", e)
                    alertDialog(getString(R.string.too_many_requests))
                }

                enableFragmentbutton()

                CentralLog.d(TAG, "On Verification failure: ${e.message}")
                onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
            }

            override fun onCodeSent(
                receivedVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                verificationId = receivedVerificationId
                resendToken = token

                CentralLog.d(TAG, "onCodeSent: $receivedVerificationId")
                if (resendingCode) {
                    onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                } else {
                    navigateToNextPage()
                }
            }
        }

    private fun enableFragmentbutton() {
        val interfaceObject: OnboardingFragmentInterface? = pagerAdapter?.getItem(pager.currentItem)
        interfaceObject?.enableButton()
    }

    private fun alertDialog(desc: String?) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(desc)
            .setCancelable(false)
            .setPositiveButton(
                getString(R.string.ok)
            ) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    CentralLog.d(TAG, "signInWithCredential:success")

                    if (BluetoothMonitoringService.broadcastMessage == null || TempIDManager.needToUpdate(
                            applicationContext
                        )
                    ) {
                        getTemporaryID()
                    }
                } else {
                    // Sign in failed, display a message and update the UI
                    CentralLog.d(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                        updateOTPError(getString(R.string.invalid_otp))
                    } else if (task.exception is FirebaseAuthInvalidUserException) {
                        alertDialog(getString(R.string.invalid_user))
                    }
                    onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                }
            }
    }

    private fun getTemporaryID(): Task<HttpsCallableResult> {
        return TempIDManager.getTemporaryIDs(this, functions)
            .addOnCompleteListener {
                if (!isFinishing) {
                    CentralLog.d(TAG, "Retrieved Temporary ID successfully")
                    onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                    navigateTo(pager.currentItem + 2)
                    Preference.putHandShakePin(
                        WiqaytnaApp.AppContext,
                        uid.substring(0, 5)
                    )
                }
            }.addOnFailureListener { exception ->
                crashlytics.recordException(exception)
                crashlytics.setCustomKey("error", "Couldn't Retrieve Temporary ID")
                if (!isFinishing) {
                    CentralLog.d(TAG, "Couldn't Retrieve Temporary ID")
                    onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                }
            }
    }

    private var mIsOpenSetting = false
    private var mIsResetup = false

    // [START on_start_check_user]
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    // [END on_start_check_user]
    private fun updateUI(user: FirebaseUser?) {
        val isSignedIn = user != null
        // Status text
        if (isSignedIn) {
            uid = user!!.uid
            CentralLog.i(TAG, uid)
        } else {
            uid = ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        pager.adapter = pagerAdapter

        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // [END initialize_auth]

        tabDots.setupWithViewPager(pager, true)

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                CentralLog.d(TAG, "OnPageScrollStateChanged")
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                CentralLog.d(TAG, "OnPageScrolled")
            }

            override fun onPageSelected(position: Int) {
                CentralLog.d(TAG, "position: $position")
                val onboardingFragment: OnboardingFragmentInterface =
                    pagerAdapter!!.getItem(position)
                onboardingFragment.becomesVisible()
                when (position) {
                    0 -> {
                        Preference.putCheckpoint(
                            baseContext,
                            position
                        )
                    }
                    1 -> {
                        //Cannot put check point at this page without triggering OTP
                    }
                    2 -> {
                        Preference.putCheckpoint(
                            baseContext,
                            position
                        )
                    }
                    3 -> {
                        Preference.putCheckpoint(
                            baseContext,
                            position
                        )
                    }
                    4 -> {
                        Preference.putCheckpoint(
                            baseContext,
                            position
                        )
                    }
                }
            }
        })

        //disable swiping
        pager.setPagingEnabled(false)
        pager.offscreenPageLimit = 5

        val extras = intent.extras
        if (extras != null) {
            mIsResetup = true
            val page = extras.getInt("page", 0)
            navigateTo(page)
        } else {
            val checkPoint = Preference.getCheckpoint(this)
            navigateTo(checkPoint)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mIsOpenSetting) {
            Handler().postDelayed(Runnable { setupPermissionsAndSettings() }, 1000)
        }
    }

    override fun onBackPressed() {

        // come back to OTP from Setup
        if (pager.currentItem == 3) {
            pager.currentItem = pager.currentItem - 2
            pagerAdapter!!.notifyDataSetChanged()
            return
        }

        if (pager.currentItem > 0 && pager.currentItem != 2) {
            navigateToPreviousPage()
            return
        }
        super.onBackPressed()
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    fun enableBluetooth() {
        CentralLog.d(TAG, "[enableBluetooth]")
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.let {
            if (it.isDisabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(
                    enableBtIntent,
                    REQUEST_ENABLE_BT
                )
            } else {
                setupPermissionsAndSettings()
            }
        }
    }

    @AfterPermissionGranted(PERMISSION_REQUEST_ACCESS_LOCATION)
    fun setupPermissionsAndSettings() {
        CentralLog.d(TAG, "[setupPermissionsAndSettings]")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = Utils.getRequiredPermissions()

            if (EasyPermissions.hasPermissions(this, *perms)) {
                // Already have permission, do the thing
                initBluetooth()
                excludeFromBatteryOptimization()
            } else {
                // Do not have permissions, request them now
                if (!isFinishing) {
                    EasyPermissions.requestPermissions(
                        this, getString(R.string.permission_location_rationale),
                        PERMISSION_REQUEST_ACCESS_LOCATION, *perms
                    )
                }
            }
        } else {
            initBluetooth()
            goToHomeScreen()
        }
    }

    private fun goToHomeScreen() {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(baseContext)
        Preference.putCheckpoint(baseContext, 0)
        Preference.putIsOnBoarded(baseContext, true)
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "P1234")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Onboard Completed for Android Device")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initBluetooth() {
        checkBLESupport()
    }

    private fun checkBLESupport() {
        CentralLog.d(TAG, "[checkBLESupport] ")
        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported) {
            bleSupported = false
            Utils.stopBluetoothMonitoringService(this)
        } else {
            bleSupported = true
        }
    }

    private fun excludeFromBatteryOptimization() {
        CentralLog.d(TAG, "[excludeFromBatteryOptimization] ")
        val powerManager =
            this.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        val packageName = this.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent =
                Utils.getBatteryOptimizerExemptionIntent(
                    packageName
                )

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                CentralLog.d(TAG, "Not on Battery Optimization whitelist")
                //check if there's any activity that can handle this
                if (Utils.canHandleIntent(
                        intent,
                        packageManager
                    )
                ) {
                    this.startActivityForResult(
                        intent,
                        BATTERY_OPTIMISER
                    )
                } else {
                    //no way of handling battery optimizer
                    goToHomeScreen()
                }
            } else {
                CentralLog.d(TAG, "On Battery Optimization whitelist")
                goToHomeScreen()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        CentralLog.d(TAG, "requestCode $requestCode resultCode $resultCode")
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                CentralLog.d(TAG, "request to enable bluetooth canceled")
                Toast.makeText(baseContext, resources.getString(R.string.setup_app_permission),Toast.LENGTH_LONG).show()
                return
            } else {
                setupPermissionsAndSettings()
            }
        } else if (requestCode == BATTERY_OPTIMISER) {
            if (resultCode != Activity.RESULT_CANCELED) {
                Handler().postDelayed({
                    goToHomeScreen()
                }, 1000)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        CentralLog.d(TAG, "[onRequestPermissionsResult] requestCode $requestCode")
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> {
                for (x in permissions.indices) {
                    val permission = permissions[x]
                    if (grantResults[x] == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val showRationale = shouldShowRequestPermissionRationale(permission)
                            if (!showRationale) {

                                // build alert dialog
                                val dialogBuilder = AlertDialog.Builder(this)
                                // set message of alert dialog
                                dialogBuilder.setMessage(getString(R.string.open_location_setting))
                                    // if the dialog is cancelable
                                    .setCancelable(false)
                                    // positive button text and action
                                    .setPositiveButton(
                                        getString(R.string.ok),
                                        DialogInterface.OnClickListener { _, _ ->
                                            CentralLog.d(TAG, "user also CHECKED never ask again")
                                            mIsOpenSetting = true
                                            val intent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            val uri: Uri =
                                                Uri.fromParts("package", packageName, null)
                                            intent.data = uri
                                            startActivity(intent)

                                        })
                                    // negative button text and action
                                    .setNegativeButton(
                                        getString(R.string.cancel),
                                        DialogInterface.OnClickListener { dialog, _ ->
                                            dialog.cancel()
                                        }).show()
                            } else if (Manifest.permission.WRITE_CONTACTS.equals(permission)) {
                                CentralLog.d(TAG, "user did not CHECKED never ask again")
                            } else {
                                excludeFromBatteryOptimization()
                            }
                        }
                    } else if (grantResults[x] == PackageManager.PERMISSION_GRANTED) {
                        excludeFromBatteryOptimization()
                    }
                }
            }
        }
    }

    fun navigateToNextPage() {
        CentralLog.d(TAG, "Navigating to next page")
        onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
        if (!speedUp) {
            pager.currentItem = pager.currentItem + 1
            pagerAdapter!!.notifyDataSetChanged()
        } else {
            pager.currentItem = pager.currentItem + 2
            pagerAdapter!!.notifyDataSetChanged()
            speedUp = false
        }
    }

    fun navigateToPreviousPage() {
        CentralLog.d(TAG, "Navigating to previous page")
        if (mIsResetup) {
            if (pager.currentItem >= 4) {
                pager.currentItem = pager.currentItem - 1
                pagerAdapter!!.notifyDataSetChanged()
            } else {
                finish()
            }
        } else {
            pager.currentItem = pager.currentItem - 1
            pagerAdapter!!.notifyDataSetChanged()
        }
    }

    private fun navigateTo(page: Int) {
        CentralLog.d(TAG, "Navigating to page")
        pager.currentItem = page
        pagerAdapter!!.notifyDataSetChanged()
    }

    fun requestForOTP(phoneNumber: String) {
        onboardingActivityLoadingProgressBarFrame.visibility = View.VISIBLE
        speedUp = false
        resendingCode = false
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    CentralLog.d(TAG, "signInAnonymously:success")
                    val user = auth.currentUser
                    sendPostRequest(user!!.uid, phoneNumber)
                        .addOnSuccessListener {
                            navigateToNextPage()
                            updateUI(user)
                        }
                        .addOnFailureListener { exception ->
                            crashlytics.recordException(exception)
                            crashlytics.setCustomKey("error", "getOTPCode:failure")
                            crashlytics.setCustomKey("phone", phoneNumber)

                            CentralLog.d(TAG, "${exception.message}")
                            onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                            CentralLog.w("getOTPCode:failure", task.exception.toString())
                            Toast.makeText(
                                baseContext, resources.getString(R.string.server_problem),
                                Toast.LENGTH_SHORT
                            ).show()
                            updateUI(null)
                        }
                } else {
                    // If sign in fails, display a message to the user.
                    onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                    CentralLog.w("signInAnonymously:failure", task.exception.toString())
                    task.exception?.let{
                        crashlytics.recordException(it)
                        crashlytics.setCustomKey("error", "signInAnonymously failed")
                    }

                    Toast.makeText(
                        baseContext, resources.getString(R.string.server_problem),
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun sendPostRequest(uid: String, phoneNumber: String): Task<HttpsCallableResult> {

        val data = hashMapOf(
            "phoneNumber" to phoneNumber,
            "token" to firebaseToken,
            "os" to "ANDROID"
        )
        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        return functions
            .getHttpsCallable("getOTPCode")
            .call(data)
    }

    private fun resendPostRequest(uid: String, phoneNumber: String): Task<HttpsCallableResult> {

        val data = hashMapOf(
            "phoneNumber" to phoneNumber,
            "token" to firebaseToken,
            "os" to "ANDROID"
        )
        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        return functions
            .getHttpsCallable("resendOTP")
            .call(data)
    }

    fun validateOTP(otp: String) {
        if (TextUtils.isEmpty(otp) || otp.length < 6) {
            updateOTPError(getString(R.string.must_be_six_digit))
            return
        }
        onboardingActivityLoadingProgressBarFrame.visibility = View.VISIBLE

        val data = hashMapOf(
            "OTP" to otp
        )
        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        functions
            .getHttpsCallable("verifyOTP")
            .call(data)
            .addOnFailureListener { exception ->
                crashlytics.recordException(exception)
                crashlytics.setCustomKey("error", "verifyOTP Failed")
                if (!isFinishing) {
                    CentralLog.e("FF", exception.toString())
                    onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                    updateOTPError(getString(R.string.unknown_error))
                }
            }
            .addOnSuccessListener {
                if (it.data.toString() == "true") {
                    CentralLog.d("VerifyOTP", "Success")
                    tv_error.visibility = View.INVISIBLE
                    if (BluetoothMonitoringService.broadcastMessage == null || TempIDManager.needToUpdate(
                            applicationContext
                        )
                    ) {
                        getTemporaryID()
                    } else {
                        if (!isFinishing) {
                            CentralLog.d(TAG, "Retrieved Temporary ID successfully")
                            onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                            navigateTo(pager.currentItem + 2)
                            Preference.putHandShakePin(
                                WiqaytnaApp.AppContext,
                                uid.substring(0, 5)
                            )
                        }
                    }
                } else {
                    if (!isFinishing) {
                        updateOTPError(getString(R.string.invalid_otp))
                        onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
                        CentralLog.d("VERIFYOTP", "VERIFICATION FALSE")
                    }
                }
            }
    }

    fun resendCode(phoneNumber: String) {
        onboardingActivityLoadingProgressBarFrame.visibility = View.VISIBLE
        speedUp = false
        resendingCode = true
        val user = auth.currentUser
        resendPostRequest(user!!.uid, phoneNumber).addOnSuccessListener {
            onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
        }.addOnFailureListener { exception ->
            crashlytics.recordException(exception)
            crashlytics.setCustomKey("error", "couldn't resend the OTP")
            crashlytics.setCustomKey("phone", phoneNumber)
            Toast.makeText(
                baseContext, resources.getString(R.string.server_problem),
                Toast.LENGTH_SHORT
            ).show()
            onboardingActivityLoadingProgressBarFrame.visibility = View.GONE
        }
    }

    fun updatePhoneNumber(num: String) {
        val onboardingFragment: OnboardingFragmentInterface = pagerAdapter!!.getItem(1)
        onboardingFragment.onUpdatePhoneNumber(num)
    }

    fun updatePhoneNumberError(error: String) {
        val registerNumberFragment: OnboardingFragmentInterface = pagerAdapter!!.getItem(0)
        registerNumberFragment.onError(error)
    }

    private fun updateOTPError(error: String) {
        val onboardingFragment: OnboardingFragmentInterface = pagerAdapter!!.getItem(1)
        onboardingFragment.onError(error)
    }

    override fun onFragmentInteraction(uri: Uri) {
        CentralLog.d(TAG, "########## fragment interaction: $uri")
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {

        val fragmentMap: MutableMap<Int, OnboardingFragmentInterface> = HashMap()

        override fun getCount(): Int = 5

        override fun getItem(position: Int): OnboardingFragmentInterface {
            return fragmentMap.getOrPut(position, { createFragAtIndex(position) })
        }

        private fun createFragAtIndex(index: Int): OnboardingFragmentInterface {
            return when (index) {
                0 -> return RegisterNumberFragment()
                1 -> return OTPFragment()
//                2 -> return TOUFragment()
                3 -> return SetupFragment()
//                4 -> return SetupCompleteFragment()
                else -> {
                    RegisterNumberFragment()
                }
            }
        }

    }

}

