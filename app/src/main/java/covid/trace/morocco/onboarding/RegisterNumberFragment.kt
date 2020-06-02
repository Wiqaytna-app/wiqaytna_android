package covid.trace.morocco.onboarding

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import covid.trace.morocco.*
import covid.trace.morocco.WiqaytnaApp.Companion.phoneNumber
import covid.trace.morocco.logging.CentralLog
import kotlinx.android.synthetic.main.fragment_register_number_new.*

class RegisterNumberFragment : OnboardingFragmentInterface() {
    private var listener: OnFragmentInteractionListener? = null
    private val TAG: String = "RegisterNumberFragment"
    private val countryCode = "+212"
    private val pattern = "[0]*[6-7][0-9]{8}".toRegex()

    private var mView: View? = null

    override fun getButtonText(): String = resources.getString(R.string.get_otp)

    override fun becomesVisible() {
        CentralLog.d(TAG, "becomes visible")
        val myActivity = this as OnboardingFragmentInterface
        myActivity.enableButton()
    }

    override fun onButtonClick(buttonView: View) {
        CentralLog.d(TAG, "OnButtonClick")
        if (pattern.matches(phone_number.text.toString())) {
            disableButtonAndRequestOTP()
        } else {
            phone_number_error.visibility = View.VISIBLE
        }

    }

    override fun getProgressValue(): Int = 1

    private fun disableButtonAndRequestOTP() {
        var myactivity = this as OnboardingFragmentInterface
        myactivity.disableButton()
        requestOTP()
    }

    private fun requestOTP() {
        mView?.let { view ->
            phone_number_error.visibility = View.INVISIBLE
            var numberText: String

            if (phone_number.text.toString().length == 10) {
                numberText = phone_number.text.toString().substring(1)
                CentralLog.d("used number", "$numberText")
            } else {
                numberText = phone_number.text.toString()
                CentralLog.d("used number", "$numberText")
            }

            val fullNumber = "$countryCode${numberText}"
            phoneNumber = fullNumber
            CentralLog.d(TAG, "The value retrieved: ${fullNumber}")

            val onboardActivity = context as OnboardingActivity
            Preference.putPhoneNumber(
                WiqaytnaApp.AppContext, fullNumber
            )
            onboardActivity.updatePhoneNumber(fullNumber)
            onboardActivity.requestForOTP(fullNumber)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CentralLog.i(TAG, "View created")

        context?.let{
            Utils.firebaseAnalyticsEvent(
                it,
                "phone_number_screen",
                "2",
                "Onboarding second screen"
            )
        }


        mView = view

        phone_number.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                phone_number_error.visibility = View.GONE
            }
        })

        phone_number.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                Utils.hideKeyboardFrom(view.context, view)
                if (pattern.matches(phone_number.text.toString())) {
                    disableButtonAndRequestOTP()
                } else {
                    phone_number_error.visibility = View.VISIBLE
                }
                true
            } else {
                false
            }
        }

        language.setOnClickListener {
            context?.let{
                Utils.firebaseAnalyticsEvent(
                    it,
                    "phone_number_screen_change_language",
                    "20",
                    "Onboarding second screen"
                )
            }

            LocaleHelper.getInstance().switchLocale()
            requireActivity().recreate()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        CentralLog.i(TAG, "Making view")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register_number_new, container, false)
    }


    override fun onUpdatePhoneNumber(num: String) {
        CentralLog.d(TAG, "onUpdatePhoneNumber $num")
    }

    override fun onError(error: String) {
        if (!isDetached) {
            phone_number_error.let {
                phone_number_error.visibility = View.VISIBLE
                phone_number_error.text = error
            }
            CentralLog.e(TAG, "error: $error")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        mView = null

        CentralLog.i(TAG, "Detached??")
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}
