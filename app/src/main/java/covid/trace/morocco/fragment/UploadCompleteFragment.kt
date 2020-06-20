package covid.trace.morocco.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import covid.trace.morocco.MainActivity
import covid.trace.morocco.PreferencesHelper
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import kotlinx.android.synthetic.main.fragment_upload_uploadcomplete.*

class UploadCompleteFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_uploadcomplete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let{
            Utils.firebaseAnalyticsEvent(it, "upload_succeeded", "12", "upload succeeded")
        }

        uploadCompleteFragmentActionButton.setOnClickListener {
            goBackToHome()
        }
    }

    private fun goBackToHome() {
        var parentActivity = activity as MainActivity
        PreferencesHelper.setPreference("selected", R.id.navigation_home.toString())
        parentActivity.goToSelectedItem()
    }
}
