package covid.trace.morocco.streetpass.persistence

import android.database.sqlite.SQLiteException
import androidx.lifecycle.LiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics

class StreetPassRecordRepository(private val recordDao: StreetPassRecordDao) {
    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    private val crashlytics = FirebaseCrashlytics.getInstance()
    val allRecords: LiveData<List<StreetPassRecord>> = recordDao.getRecords()

    suspend fun insert(word: StreetPassRecord) {
        try {
            recordDao.insert(word)
        } catch (exception: SQLiteException) {
            crashlytics.recordException(exception)
        }
    }
}
