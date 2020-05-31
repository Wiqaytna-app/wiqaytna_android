package covid.trace.morocco.streetpass.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import covid.trace.morocco.streetpass.persistence.StreetPassRecord
import covid.trace.morocco.streetpass.persistence.StreetPassRecordDatabase
import covid.trace.morocco.streetpass.persistence.StreetPassRecordRepository

class RecordViewModel(app: Application) : AndroidViewModel(app) {

    private var repo: StreetPassRecordRepository

    var allRecords: LiveData<List<StreetPassRecord>>

    init {
        val recordDao = StreetPassRecordDatabase.getDatabase(app).recordDao()
        repo = StreetPassRecordRepository(recordDao)
        allRecords = repo.allRecords
    }


}
