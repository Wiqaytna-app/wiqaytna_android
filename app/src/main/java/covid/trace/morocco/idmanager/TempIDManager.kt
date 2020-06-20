package covid.trace.morocco.idmanager

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import covid.trace.morocco.BuildConfig
import covid.trace.morocco.Preference
import covid.trace.morocco.Utils
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.notifications.NotificationTemplates
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.bmValidityCheck
import java.io.File
import java.util.*

object TempIDManager {

    private const val TAG = "TempIDManager"
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private fun storeTemporaryIDs(context: Context, packet: String) {
        CentralLog.d(TAG, "[TempID] Storing temporary IDs into internal storage...")
        try {
            val file = File(context.filesDir, "tempIDs")
            file.writeText(packet)
            Utils.firebaseAnalyticsEvent(
                    context,
                    "StoreTempID_Successful",
                    "26",
                    "getTempID successful"
            );
        } catch (exception: Throwable) {
            Utils.firebaseAnalyticsEvent(context, "StoreTempID_Failed", "27", "getTempID failed");
            crashlytics.recordException(exception)
            crashlytics.setCustomKey("error", "can't store tempIDs")
            crashlytics.setCustomKey("function", "storeTemporaryIDs")
            NotificationTemplates.diskFullWarningNotification(
                    context,
                    BuildConfig.SERVICE_FOREGROUND_CHANNEL_ID
            )
        }

    }

    fun retrieveTemporaryID(context: Context): TemporaryID? {
        val file = File(context.filesDir, "tempIDs")
        if (file.exists()) {
            val readback = file.readText()
            CentralLog.d(TAG, "[TempID] fetched broadcastmessage from file:  $readback")
            var tempIDArrayList =
                    convertToTemporaryIDs(
                            readback
                    )
            var tempIDQueue =
                    convertToQueue(
                            tempIDArrayList
                    )
            return getValidOrLastTemporaryID(
                    context,
                    tempIDQueue
            )
        }
        return null
    }

    private fun getValidOrLastTemporaryID(
            context: Context,
            tempIDQueue: Queue<TemporaryID>
    ): TemporaryID? {
        CentralLog.d(TAG, "[TempID] Retrieving Temporary ID")
        var currentTime = System.currentTimeMillis()

        var pop = 0
        while (tempIDQueue.size > 1) {
            val tempID = tempIDQueue.peek()
            tempID.print()

            if (tempID.isValidForCurrentTime()) {
                CentralLog.d(TAG, "[TempID] Breaking out of the loop")
                break
            }

            tempIDQueue.poll()
            pop++
        }

        var foundTempID = tempIDQueue.peek()
        if (foundTempID != null) {
            var foundTempIDStartTime = foundTempID.startTime * 1000
            var foundTempIDExpiryTime = foundTempID.expiryTime * 1000

            CentralLog.d(TAG, "[TempID Total number of items in queue: ${tempIDQueue.size}")
            CentralLog.d(TAG, "[TempID Number of items popped from queue: $pop")
            CentralLog.d(TAG, "[TempID] Current time: ${currentTime}")
            CentralLog.d(TAG, "[TempID] Start time: ${foundTempIDStartTime}")
            CentralLog.d(TAG, "[TempID] Expiry time: ${foundTempIDExpiryTime}")
            CentralLog.d(TAG, "[TempID] Updating expiry time")
            Preference.putExpiryTimeInMillis(
                    context,
                    foundTempIDExpiryTime
            )
        }

        return foundTempID
    }

    private fun convertToTemporaryIDs(tempIDString: String): Array<TemporaryID>? {
        val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

        try {
            val tempIDResult = gson.fromJson(tempIDString, Array<TemporaryID>::class.java)
            CentralLog.d(
                    TAG,
                    "[TempID] After GSON conversion: ${tempIDResult?.get(0)?.tempID} ${tempIDResult?.get(
                            0
                    )?.startTime}"
            )
            return tempIDResult
        } catch (exception: Throwable) {
            crashlytics.recordException(exception)
            crashlytics.setCustomKey("error", "couldn't parse tempIDString")
            if (tempIDString.length <= 1000) {
                crashlytics.setCustomKey("tempIDString", "$tempIDString")
            } else {
                crashlytics.setCustomKey("tempIDString length","${tempIDString.length}")
                crashlytics.setCustomKey(
                        "tempIDString_first_K",
                        tempIDString.substring(0, 1000)
                )

                if(tempIDString.length <= 2000){
                    crashlytics.setCustomKey(
                            "tempIDString_second_part",
                            tempIDString.substring(1000)
                    )
                } else {
                    crashlytics.setCustomKey(
                            "tempIDString_last_K",
                            tempIDString.substring(tempIDString.length-1000)
                    )
                }
            }
        }

        return null
    }

    private fun convertToQueue(tempIDArray: Array<TemporaryID>?): Queue<TemporaryID> {
        CentralLog.d(TAG, "[TempID] Before Sort: ${tempIDArray?.get(0)}")

        //Sort based on start time
        tempIDArray?.sortBy {
            return@sortBy it.startTime
        }
        CentralLog.d(TAG, "[TempID] After Sort: ${tempIDArray?.get(0)}")

        //Preserving order of array which was sorted
        var tempIDQueue: Queue<TemporaryID> = LinkedList<TemporaryID>()
        if (tempIDArray != null) {
            for (tempID in tempIDArray) {
                tempIDQueue.offer(tempID)
            }
        }

        CentralLog.d(TAG, "[TempID] Retrieving from Queue: ${tempIDQueue.peek()}")
        return tempIDQueue
    }

    fun getTemporaryIDs(context: Context, functions: FirebaseFunctions): Task<HttpsCallableResult> {
        CentralLog.d(TAG, "getTemporaryIDs called")
        return functions.getHttpsCallable("getTempIDs").call().addOnSuccessListener {

            val result: HashMap<String, Any> = it.data as HashMap<String, Any>
            val tempIDs = result["tempIDs"]

            val status = result["status"].toString()

            CentralLog.d(TAG, "tempIDs: $tempIDs")
            CentralLog.d(TAG, "status: $status")

            if (status.toLowerCase().contentEquals("success")) {
                CentralLog.w(TAG, "Retrieved Temporary IDs from Server")
                val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
                val jsonByteArray = gson.toJson(tempIDs).toByteArray(Charsets.UTF_8)
                storeTemporaryIDs(
                        context,
                        jsonByteArray.toString(Charsets.UTF_8)
                )

                val refreshTime = result["refreshTime"].toString()
                var refresh = refreshTime.toLongOrNull() ?: 0

                Preference.putNextFetchTimeInMillis(
                        context,
                        refresh * 1000
                )
                Preference.putLastFetchTimeInMillis(
                        context,
                        System.currentTimeMillis()
                )

                CentralLog.d(TAG, "refresh: ${Preference.getNextFetchTimeInMillis(context)}")

                CentralLog.d(TAG, "lastFetch: ${Preference.getLastFetchTimeInMillis(context)}")
            }

        }
    }

    fun needToUpdate(context: Context): Boolean {
        val nextFetchTime =
                Preference.getNextFetchTimeInMillis(context)
        val currentTime = System.currentTimeMillis()

        val update = currentTime >= nextFetchTime
        CentralLog.i(
                TAG,
                "Need to update and fetch TemporaryIDs? $nextFetchTime vs $currentTime: $update"
        )
        return update
    }

    fun needToRollNewTempID(context: Context): Boolean {
        val expiryTime =
                Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime >= expiryTime
        CentralLog.d(TAG, "[TempID] Need to get new TempID? $expiryTime vs $currentTime: $update")
        return update
    }

    //Can Cleanup, this function always return true
    fun bmValid(context: Context): Boolean {
        val expiryTime =
                Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime < expiryTime

        if (bmValidityCheck) {
            CentralLog.w(TAG, "Temp ID is valid")
            return update
        }

        return true
    }
}
