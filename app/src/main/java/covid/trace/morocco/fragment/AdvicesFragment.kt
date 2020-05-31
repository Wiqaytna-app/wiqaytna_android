package covid.trace.morocco.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.onboarding.PersonalInfosActivity
import kotlinx.android.synthetic.main.fragment_advices.*

class AdvicesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_advices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.firebaseAnalyticsEvent(requireContext(), "advices_screen", "9", "advices screen")
        setClickListeners()
    }

    private fun setClickListeners() {

        settings.setOnClickListener {
            startActivity(Intent(context, PersonalInfosActivity::class.java))
        }
    }

}
