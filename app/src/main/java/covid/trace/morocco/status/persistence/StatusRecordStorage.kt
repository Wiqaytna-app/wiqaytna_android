package covid.trace.morocco.status.persistence

import android.content.Context
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import covid.trace.morocco.BuildConfig
import covid.trace.morocco.notifications.NotificationTemplates
import covid.trace.morocco.streetpass.persistence.StreetPassRecordDatabase

class StatusRecordStorage(val context: Context) {

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val statusDao = StreetPassRecordDatabase.getDatabase(context).statusDao()

    suspend fun saveRecord(record: StatusRecord) {
        try {
            statusDao.insert(record)
        } catch (exception: Throwable) {
            if(exception is SQLiteFullException){
                NotificationTemplates.diskFullWarningNotification(
                    context,
                    BuildConfig.SERVICE_FOREGROUND_CHANNEL_ID
                )
            }
            crashlytics.recordException(exception)
        }
    }

    fun nukeDb() {
        try {
            statusDao.nukeDb()
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
        }

    }

    fun getAllRecords(): List<StatusRecord> {
        return try {
            statusDao.getCurrentRecords()
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
            listOf()
        }
    }

    suspend fun purgeOldRecords(before: Long) {
        try {
            statusDao.purgeOldRecords(before)
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
        }
    }
}
