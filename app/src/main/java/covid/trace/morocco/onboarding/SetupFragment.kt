package covid.trace.morocco.onboarding

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import covid.trace.morocco.LocaleHelper
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.logging.CentralLog
import kotlinx.android.synthetic.main.fragment_setup.*


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SetupFragment : OnboardingFragmentInterface() {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private val TAG: String = "SetupFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            Utils.firebaseAnalyticsEvent(it, "setup_screen", "4", "Onboarding fourth screen")
        }

        language.setOnClickListener {
            context?.let {
                Utils.firebaseAnalyticsEvent(
                    it,
                    "setup_screen_change_language",
                    "22",
                    "Onboarding fourth screen"
                )
            }

            LocaleHelper.getInstance().switchLocale()
            requireActivity().recreate()
        }
    }

    override fun getButtonText(): String = resources.getString(R.string.proceed)

    override fun onButtonClick(view: View) {
        CentralLog.d(TAG, "OnButtonClick 2")
        val activity = context as OnboardingActivity?
        activity?.enableBluetooth()
    }

    override fun becomesVisible() {}

    override fun getProgressValue(): Int = 3

    override fun onUpdatePhoneNumber(num: String) {}

    override fun onError(error: String) {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup, container, false)
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}
