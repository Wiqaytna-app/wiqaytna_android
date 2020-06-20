package covid.trace.morocco

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import covid.trace.morocco.bluetooth.gatt.*
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.StatisticsResponse
import covid.trace.morocco.scheduler.Scheduler
import covid.trace.morocco.services.BluetoothMonitoringService
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_ADVERTISE_REQ_CODE
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_BM_UPDATE
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_HEALTH_CHECK_CODE
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_PURGE_CODE
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_SCAN_REQ_CODE
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_START
import covid.trace.morocco.status.Status
import covid.trace.morocco.streetpass.ACTION_DEVICE_SCANNED
import covid.trace.morocco.streetpass.ConnectablePeripheral
import covid.trace.morocco.streetpass.ConnectionRecord
import kotlinx.android.synthetic.main.layout_dialog.view.*
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*


object Utils {

    private const val TAG = "Utils"

    const val faqArURL = "https://www.wiqaytna.ma/Default.aspx#nav-wiqaytna"
    const val faqFrURL = "https://www.wiqaytna.ma/Default_Fr.aspx#nav-wiqaytna"

    fun getRequiredPermissions(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun getBatteryOptimizerExemptionIntent(packageName: String): Intent {
        val intent = Intent()
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:$packageName")
        return intent
    }

    fun canHandleIntent(batteryExemptionIntent: Intent, packageManager: PackageManager?): Boolean {
        packageManager?.let {
            return batteryExemptionIntent.resolveActivity(packageManager) != null
        }
        return false
    }

    fun getDate(milliSeconds: Long): String {
        val dateFormat = "dd/MM/yyyy HH:mm:ss.SSS"
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getTime(milliSeconds: Long): String {
        val dateFormat = "h:mm a"
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun startBluetoothMonitoringService(context: Context) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_START.index
        )

        context.startService(intent)
    }

    fun scheduleStartMonitoringService(context: Context, timeInMillis: Long) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_START.index
        )

        Scheduler.scheduleServiceIntent(
            PENDING_START,
            context,
            intent,
            timeInMillis
        )
    }

    fun scheduleBMUpdateCheck(context: Context, bmCheckInterval: Long) {

        cancelBMUpdateCheck(context)

        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_UPDATE_BM.index
        )

        Scheduler.scheduleServiceIntent(
            PENDING_BM_UPDATE,
            context,
            intent,
            bmCheckInterval
        )
    }

    fun cancelBMUpdateCheck(context: Context) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_UPDATE_BM.index
        )

        Scheduler.cancelServiceIntent(PENDING_BM_UPDATE, context, intent)
    }

    fun stopBluetoothMonitoringService(context: Context) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_STOP.index
        )
        cancelNextScan(context)
        cancelNextHealthCheck(context)
        context.stopService(intent)
    }

    fun cancelNextScan(context: Context) {
        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_SCAN.index
        )
        Scheduler.cancelServiceIntent(PENDING_SCAN_REQ_CODE, context, nextIntent)
    }

    fun cancelNextAdvertise(context: Context) {
        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_ADVERTISE.index
        )
        Scheduler.cancelServiceIntent(PENDING_ADVERTISE_REQ_CODE, context, nextIntent)
    }

    fun scheduleNextHealthCheck(context: Context, timeInMillis: Long) {
        //cancels any outstanding check schedules.
        cancelNextHealthCheck(context)

        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_SELF_CHECK.index
        )
        //runs every XXX milliseconds - every minute?
        Scheduler.scheduleServiceIntent(
            PENDING_HEALTH_CHECK_CODE,
            context,
            nextIntent,
            timeInMillis
        )
    }

    private fun cancelNextHealthCheck(context: Context) {
        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_SELF_CHECK.index
        )
        Scheduler.cancelServiceIntent(PENDING_HEALTH_CHECK_CODE, context, nextIntent)
    }

    fun scheduleRepeatingPurge(context: Context, intervalMillis: Long) {
        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_PURGE.index
        )

        Scheduler.scheduleRepeatingServiceIntent(
            PENDING_PURGE_CODE,
            context,
            nextIntent,
            intervalMillis
        )
    }

    fun broadcastDeviceScanned(
        context: Context,
        device: BluetoothDevice,
        connectableBleDevice: ConnectablePeripheral
    ) {
        val intent = Intent(ACTION_DEVICE_SCANNED)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        intent.putExtra(CONNECTION_DATA, connectableBleDevice)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun broadcastDeviceProcessed(context: Context, deviceAddress: String) {
        val intent = Intent(ACTION_DEVICE_PROCESSED)
        intent.putExtra(DEVICE_ADDRESS, deviceAddress)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }


    fun broadcastStreetPassReceived(context: Context, streetpass: ConnectionRecord) {
        val intent = Intent(ACTION_RECEIVED_STREETPASS)
        intent.putExtra(STREET_PASS, streetpass)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun broadcastStatusReceived(context: Context, statusRecord: Status) {
        val intent = Intent(ACTION_RECEIVED_STATUS)
        intent.putExtra(STATUS, statusRecord)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun getDateFromUnix(unix_timestamp: Long): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH)
        val date = sdf.format(unix_timestamp)
        return date.toString()
    }

    fun hideKeyboardFrom(
        context: Context,
        view: View
    ) {
        val imm = context.getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun isBluetoothAvailable(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null &&
                bluetoothAdapter.isEnabled && bluetoothAdapter.state == BluetoothAdapter.STATE_ON
    }

    fun spinnerAsAnEditText(
        title: String?,
        items: Array<String?>,
        editText: EditText,
        context: Context?
    ) {

        if (items == null || editText == null || context == null) {
            CentralLog.d("Spinner", "Something is null")
            return
        }

        val dialog = AlertDialog.Builder(context)
        dialog.setTitle(title)
        dialog.setItems(items) { _, which ->
            editText.setText(items[which])
        }
        dialog.create().show()
    }

    fun spinnerAsAnEditText(
        title: String?,
        items: Array<String?>,
        editText: EditText,
        context: Context?,
        selected: OnRegionSelected
    ) {

        if (items == null || editText == null || context == null) {
            CentralLog.d("Spinner", "Something is null")
            return
        }

        val dialog = AlertDialog.Builder(context)
        dialog.setTitle(title)
        dialog.setItems(items) { _, which ->
            editText.setText(items[which])
            selected.runWhenItemSelected(items[which])
        }
        dialog.create().show()
    }

    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }

    fun firebaseAnalyticsEvent(context: Context, eventName:String, id:String, description:String){
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id)
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, description)
        CentralLog.d("FirebaseAnalyticsEvent", eventName)
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    fun showAlertDialog(
        message: String,
        icon: Int,
        context: Context?,
        onDialogClick: OnDialogClick?
    ) {
        if (context == null) {
            CentralLog.d(TAG, "context is null")
            return
        }
        val dialogLayout = LayoutInflater.from(context).inflate(R.layout.layout_dialog, null)

        dialogLayout.icon.setImageDrawable(context.getDrawable(icon))

        dialogLayout.cancel.visibility = View.GONE

        dialogLayout.message.text = message

        val dialog = AlertDialog.Builder(context)
            .setView(dialogLayout)
            .create()

        dialogLayout.ok.setOnClickListener {
            onDialogClick?.okClicked()
            dialog.dismiss()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    fun getStatistics() {
        var crashlytics = FirebaseCrashlytics.getInstance()
        FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
                .getHttpsCallable("stats")
                .call()
                .continueWith { task ->
                    if (task.isSuccessful) {
                        CentralLog.d("getStats response", "${task.result!!.data}")
                        val json: String = Gson().toJson(task.result!!.data)
                        PreferencesHelper.setPreference(PreferencesHelper.STATS_UPDATE, json)
                        val response = Gson().fromJson(json, StatisticsResponse::class.java)
                        WiqaytnaApp.statisticsData = response
                    } else {
                        if (task.exception != null) {
                            crashlytics.recordException(task.exception!!)
                        }
                        crashlytics.setCustomKey("error", "couldn't get the latest stats")
                        CentralLog.d("getStats response", task.exception.toString())
                    }
                }
    }

    fun getJSONString(context: Context): String {
        val stringBuilder = StringBuilder()
        try {
            val input = context.resources.openRawResource(R.raw.regions)
            val isr = InputStreamReader(input)
            val inputBuffer = CharArray(100)
            var charRead: Int
            while (isr.read(inputBuffer).also { charRead = it } > 0) {
                stringBuilder.append(String(inputBuffer, 0, charRead))
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
        return stringBuilder.toString()
    }

    interface OnRegionSelected {
        fun runWhenItemSelected(region: String?)
    }

    interface OnDialogClick {
        fun okClicked()
    }

}
