package covid.trace.morocco.models

import covid.trace.morocco.status.persistence.StatusRecord
import covid.trace.morocco.streetpass.persistence.StreetPassRecord

data class ExportData(val recordList: List<StreetPassRecord>, val statusList: List<StatusRecord>)