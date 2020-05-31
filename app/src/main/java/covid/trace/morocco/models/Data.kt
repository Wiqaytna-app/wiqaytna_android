package covid.trace.morocco.models

data class Data(
    val _id: String,
    val date: Date,
    val total_tested: String, val confirmed: String,
    val covered: String, val death: String,
    val new_confirmed: String, val new_death: String,
    val new_recovered: String,
    val regions: Array<Region>
)