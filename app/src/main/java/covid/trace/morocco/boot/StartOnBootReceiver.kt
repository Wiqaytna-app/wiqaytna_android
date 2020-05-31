package covid.trace.morocco.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import covid.trace.morocco.Utils
import covid.trace.morocco.logging.CentralLog

class StartOnBootReceiver : BroadcastReceiver() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onReceive(context: Context, intent: Intent) {

            try {
                if (Intent.ACTION_BOOT_COMPLETED != intent.action) return
                    CentralLog.d("StartOnBootReceiver", "boot completed received")
                //can i try a scheduled service start here?
                CentralLog.d("StartOnBootReceiver", "Attempting to start service")
                Utils.scheduleStartMonitoringService(context, 500)
            } catch (e: Throwable) {
                crashlytics.recordException(e)
                crashlytics.setCustomKey("receiver","StartOnBoat receiver")
                CentralLog.e("StartOnBootReceiver", e.localizedMessage)
                e.printStackTrace()
            }

    }
}
