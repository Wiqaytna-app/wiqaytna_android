package covid.trace.morocco.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import covid.trace.morocco.*
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.Region
import covid.trace.morocco.models.StatisticsResponse
import covid.trace.morocco.onboarding.PersonalInfosActivity
import kotlinx.android.synthetic.main.fragment_statistics.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class StatisticsFragment : Fragment() {

    private var format: NumberFormat? = null

    init {
        format = NumberFormat.getInstance(Locale.FRANCE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            Utils.firebaseAnalyticsEvent(
                it,
                "statistics_screen",
                "8",
                "statistics screen"
            )
        }
        setClickListener()
        if (WiqaytnaApp.statisticsData != null) {
            initViews(WiqaytnaApp.statisticsData!!)
        }
    }

    private fun setClickListener() {
        settings.setOnClickListener {
            startActivity(Intent(context, PersonalInfosActivity::class.java))
        }
    }

    private fun initViews(statistics: StatisticsResponse) {
        if (statistics != null) {
            val data = statistics.data
            val regions: Array<Region>? = data.regions
            healing_all.text = data.covered
            deaths_all.text = data.death
            confirmed_cases_all.text = data.confirmed

            healing.text = data.new_recovered
            deaths.text = data.new_death
            confirmed_cases.text = data.new_confirmed

            regions?.sortByDescending {
                val cleanStringValue = it.total.replace("\\p{C}".toRegex(), "").replace("[^\\x00-\\x7F]".toRegex(), "").replace("[\\p{Cntrl}&&[^\\r\\n\\t]]".toRegex(), "")
                format?.parse(cleanStringValue)?.toDouble()
            }
            val adapter = regions?.let{RegionsAdapter(it)}
            regionsList.adapter = adapter
            regionsList.layoutManager = LinearLayoutManager(context)

            val timeStamp = data.date._seconds * 1000

            if (TextUtils.equals(
                    PreferencesHelper.getCurrentLanguage(),
                    PreferencesHelper.FRENCH_LANGUAGE_CODE
                )
            ) {
                val dateFormat = SimpleDateFormat("dd MMMM yyyy HH'h00'", Locale.FRANCE)
                lastUpdate.text =
                    resources.getString(R.string.last_update_24_hours_ago) + "  " + dateFormat.format(
                        timeStamp
                    )
            } else {
                val day = SimpleDateFormat("dd", Locale("ar", "ma")).format(timeStamp)
                val month = SimpleDateFormat("MMMM", Locale("ar", "ma")).format(timeStamp)
                val year = SimpleDateFormat("yyyy", Locale("ar", "ma")).format(timeStamp)
                val hour = SimpleDateFormat("HH", Locale("ar", "ma")).format(timeStamp)+"h"
                CentralLog.d("update time", "$day $month $year $hour")
                lastUpdate.text =
                        resources.getString(R.string.last_update_24_hours_ago) + "  " + "$day $month $year $hour"
            }
        }

    }

}
