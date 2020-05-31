package covid.trace.morocco.streetpass.persistence

import android.content.Context
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import covid.trace.morocco.BuildConfig
import covid.trace.morocco.notifications.NotificationTemplates

class StreetPassRecordStorage(val context: Context) {

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val recordDao = StreetPassRecordDatabase.getDatabase(context).recordDao()

    suspend fun saveRecord(record: StreetPassRecord) {
        try {
            recordDao.insert(record)
        } catch (exception: Throwable) {
            if (exception is SQLiteFullException) {
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
            recordDao.nukeDb()
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
        }
    }

    fun getAllRecords(): List<StreetPassRecord> {
        return try {
            recordDao.getCurrentRecords()
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
            listOf()
        }
    }

    suspend fun purgeOldRecords(before: Long) {
        try {
            recordDao.purgeOldRecords(before)
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
        }
    }
}
