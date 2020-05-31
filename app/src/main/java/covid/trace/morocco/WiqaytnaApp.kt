package covid.trace.morocco

import android.app.Application
import android.content.Context
import android.os.Build
import com.google.gson.Gson
import covid.trace.morocco.idmanager.TempIDManager
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.RegionArFr
import covid.trace.morocco.models.StatisticsResponse
import covid.trace.morocco.services.BluetoothMonitoringService
import covid.trace.morocco.streetpass.CentralDevice
import covid.trace.morocco.streetpass.PeripheralDevice
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump


class WiqaytnaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext = applicationContext
        PreferencesHelper.init(this)
        regionsData = Gson().fromJson(
            Utils.getJSONString(baseContext),
            Array<RegionArFr>::class.java
        ).toList()
        LocaleHelper.setFont("fonts/Almarai-Regular.ttf")
    }

    override fun attachBaseContext(base: Context?) {
        LocaleHelper.init(base)
        super.attachBaseContext(LocaleHelper.getInstance().setLocale(base))
    }

    companion object {
        var firebaseToken = ""
        var phoneNumber = ""
        var uid = ""
        lateinit var regionsData: List<RegionArFr>
        var statisticsData: StatisticsResponse? = null
        private const val TAG = "TracerApp"
        const val ORG = BuildConfig.ORG

        lateinit var AppContext: Context

        fun thisDeviceMsg(): String {
            BluetoothMonitoringService.broadcastMessage?.let {
                CentralLog.i(TAG, "Retrieved BM for storage: $it")

                if (!it.isValidForCurrentTime()) {

                    val fetch = TempIDManager.retrieveTemporaryID(AppContext)
                    fetch?.let {
                        CentralLog.i(TAG, "Grab New Temp ID")
                        BluetoothMonitoringService.broadcastMessage = it
                    }

                    if (fetch == null) {
                        CentralLog.e(TAG, "Failed to grab new Temp ID")
                    }
                }
            }
            return BluetoothMonitoringService.broadcastMessage?.tempID ?: "Missing TempID"
        }

        fun asPeripheralDevice(): PeripheralDevice {
            return PeripheralDevice(Build.MODEL, "SELF")
        }

        fun asCentralDevice(): CentralDevice {
            return CentralDevice(Build.MODEL, "SELF")
        }
    }
}
