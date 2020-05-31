package covid.trace.morocco.models

data class RegionArFr(
    val code_region: String,
    val region_fr: String,
    val region_ar: String,
    val api_key: String,
    val provinces: List<ProvinceArFr>
)