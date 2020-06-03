package covid.trace.morocco.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import covid.trace.morocco.MainActivity
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.onboarding.PersonalInfosActivity
import kotlinx.android.synthetic.main.fragment_upload_foruse.*

class ForUseFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_foruse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let {
            Utils.firebaseAnalyticsEvent(it, "upload_screen_1", "10", "upload screen")
        }

        forUseFragmentActionButton.setOnClickListener {
            goToUploadFragment()
        }

        language.setOnClickListener {
            startActivity(Intent(context, PersonalInfosActivity::class.java))
        }
    }


    private fun goToUploadFragment() {
        val parentActivity: MainActivity = activity as MainActivity
        parentActivity.openFragment(
            parentActivity.LAYOUT_MAIN_ID,
            EnterPinFragment(),
            EnterPinFragment::class.java.name
        )
    }
}
