package covid.trace.morocco.onboarding

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import covid.trace.morocco.LocaleHelper
import covid.trace.morocco.PreferencesHelper
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.logging.CentralLog
import kotlinx.android.synthetic.main.fragment_otp_new.*
import kotlin.math.floor

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class OTPFragment : OnboardingFragmentInterface() {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var stopWatch: CountDownTimer? = null

    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            startTimer()
        } else {
            resetTimer()
        }
    }

    private fun resetTimer() {
        stopWatch?.cancel()
    }

    override fun getButtonText(): String = resources.getString(R.string.verify)

    override fun becomesVisible() {}

    override fun onButtonClick(view: View) {
        CentralLog.d(TAG, "OnButtonClick 3B")

        val otp = getOtp()
        val onboardActivity = context as OnboardingActivity
        onboardActivity.validateOTP(otp)
    }

    override fun getProgressValue(): Int = 2

    private fun getOtp(): String {
        return otp1.text.toString() + otp2.text.toString() + otp3.text.toString() + otp4.text.toString() + otp5.text.toString() + otp6.text.toString()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_otp_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Utils.firebaseAnalyticsEvent(requireContext(), "otp_screen", "3", "Onboarding third screen")

        resendCode.isEnabled = false

        editTextsListeners()

        val number = PreferencesHelper.getStringPreference("number", "")
        if (!TextUtils.isEmpty(number)) {
            CentralLog.d(TAG, "retreived a saved number $number")
            phoneNumber = number
        }

        language.setOnClickListener {
            Utils.firebaseAnalyticsEvent(
                    requireContext(),
                    "otp_screen_change_language",
                    "21",
                    "Onboarding third screen"
            )

            LocaleHelper.getInstance().switchLocale()
            requireActivity().recreate()
        }

        resendCode.setOnClickListener {
            CentralLog.d(TAG, "resend pressed sent to $phoneNumber")

            Utils.firebaseAnalyticsEvent(
                    requireContext(),
                    "otp_screen_resend_otp",
                    "18",
                    "Onboarding third screen resend otp"
            )

            val onboardingActivity = activity as OnboardingActivity
            onboardingActivity.resendCode(phoneNumber)
            resetTimer()
            startTimer()
        }
    }

    private fun editTextsListeners() {

        otp1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp2.requestFocus()
            }

        })

        otp2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp3.requestFocus()
                if (s?.length == 0) otp1.requestFocus()
            }
        })

        otp3.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp4.requestFocus()
                if (s?.length == 0) otp2.requestFocus()
            }
        })

        otp4.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp5.requestFocus()
                if (s?.length == 0) otp3.requestFocus()
            }
        })

        otp5.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp6.requestFocus()
                if (s?.length == 0) otp4.requestFocus()
            }
        })

        otp6.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 0) otp5.requestFocus()
                if (getOtp().length == 6) {
                    Utils.hideKeyboardFrom(view!!.context, view!!)
                }
            }
        })
    }

    override fun onUpdatePhoneNumber(num: String) {
        CentralLog.d(TAG, "onUpdatePhoneNumber $num")
        phoneNumber = num
        PreferencesHelper.setPreference("number", phoneNumber)
    }

    override fun onError(error: String) {
        if (isDetached) return

        context?.let{
            Utils.firebaseAnalyticsEvent(it, "Otp_error", "15", error)
        }
        tv_error?.text = error
        tv_error?.visibility = View.VISIBLE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    private fun startTimer() {
        stopWatch = object : CountDownTimer(COUNTDOWN_DURATION * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val numberOfMins = floor((millisUntilFinished * 1.0) / 60000)
                val numberOfMinsInt = numberOfMins.toInt()
                val numberOfSeconds = floor((millisUntilFinished / 1000.0) % 60)
                val numberOfSecondsInt = numberOfSeconds.toInt()
                var finalNumberOfSecondsString = ""
                if (numberOfSecondsInt < 10) {
                    finalNumberOfSecondsString = "0$numberOfSecondsInt"
                } else {
                    finalNumberOfSecondsString = "$numberOfSecondsInt"
                }

//                timer?.text = resources.getString(R.string.otp_expiry)+" $numberOfMinsInt:$finalNumberOfSecondsString"
            }

            override fun onFinish() {
//                timer?.text = resources.getString(R.string.otp_expiry)+" 0:00"
                resendCode.isEnabled = true
            }
        }
        stopWatch?.start()
        resendCode?.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWatch?.cancel()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val COUNTDOWN_DURATION = 120L
        const val TAG: String = "OTPFragment"
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}
