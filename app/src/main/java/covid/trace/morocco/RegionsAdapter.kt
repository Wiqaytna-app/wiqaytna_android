package covid.trace.morocco

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.Region


class RegionsAdapter(private val regions: Array<Region>) :
    RecyclerView.Adapter<RegionsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var regionName: TextView = itemView.findViewById(R.id.region) as TextView
        var casesNumber: TextView = itemView.findViewById(R.id.number) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionsAdapter.ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)
        val regionView: View = inflater.inflate(R.layout.layout_region_item, parent, false)
        return ViewHolder(regionView)
    }

    override fun getItemCount() = regions.size
    override fun onBindViewHolder(holder: RegionsAdapter.ViewHolder, position: Int) {
        val region = regions[position]
        if (region != null) {
            for (i in WiqaytnaApp.regionsData.indices) {
                CentralLog.d("response region $i", region.region)
                CentralLog.d("local region $i", WiqaytnaApp.regionsData[i].api_key)
                if (TextUtils.equals(region.region, WiqaytnaApp.regionsData[i].api_key)) {
                    val isFrench = TextUtils.equals(
                        PreferencesHelper.getCurrentLanguage(),
                        PreferencesHelper.FRENCH_LANGUAGE_CODE
                    )
                    holder.regionName.text =
                        if (isFrench) WiqaytnaApp.regionsData[i].region_fr else WiqaytnaApp.regionsData[i].region_ar
                    break
                }
            }
            holder.casesNumber.text = region.total
        }
    }
}