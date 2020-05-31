package covid.trace.morocco.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import covid.trace.morocco.Utils
import covid.trace.morocco.logging.CentralLog

class UpgradeReceiver : BroadcastReceiver() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onReceive(context: Context, intent: Intent) {

        try {
            if (Intent.ACTION_MY_PACKAGE_REPLACED != intent.action) return
            // Start your service here.
            context?.let {
                CentralLog.i("UpgradeReceiver", "Starting service from upgrade receiver")
                Utils.startBluetoothMonitoringService(context)
            }
        } catch (e: Exception) {
            crashlytics.recordException(e)
            crashlytics.setCustomKey("receiver","upgrade receiver")
            CentralLog.e("UpgradeReceiver", "Unable to handle upgrade: ${e.localizedMessage}")
        }
    }
}
