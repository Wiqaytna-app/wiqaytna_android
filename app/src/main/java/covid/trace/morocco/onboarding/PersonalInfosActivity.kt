package covid.trace.morocco.onboarding

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import covid.trace.morocco.*
import covid.trace.morocco.base.BaseFragmentActivity
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.RegionArFr
import kotlinx.android.synthetic.main.activity_personal_infos.*

class PersonalInfosActivity : BaseFragmentActivity(), Utils.OnRegionSelected, Utils.OnDialogClick {

    private val ragions = mutableListOf<String>()
    private var crashlytics = FirebaseCrashlytics.getInstance()

    //    private var provinces = mutableListOf<String>()
    private var index: MutableList<Int> = mutableListOf<Int>()
    private var chosenLanguage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_infos)
        Utils.firebaseAnalyticsEvent(baseContext, "settings_screen", "5", "settings screen")
        getRegions()
        setClickListeners()
        initViews()
    }

    private fun initViews() {
        when (PreferencesHelper.getStringPreference(PreferencesHelper.AGE_RANGE, "")) {
            "1" -> firstRange.isChecked = true
            "2" -> secondRange.isChecked = true
            "3" -> thirdRange.isChecked = true
            "4" -> fourthRange.isChecked = true
        }

        when (PreferencesHelper.getStringPreference(PreferencesHelper.GENDER, "")) {
            "male" -> man.isChecked = true
            "female" -> woman.isChecked = true
        }

        when (PreferencesHelper.getCurrentLanguage()) {
            PreferencesHelper.ARABIC_LANGUAGE_CODE -> arabe.isChecked = true
            PreferencesHelper.FRENCH_LANGUAGE_CODE -> french.isChecked = true
        }

        val regionCode = PreferencesHelper.getStringPreference(PreferencesHelper.REGION, "")
        val provinceCode = PreferencesHelper.getStringPreference(PreferencesHelper.PROVINCE, "")
        var selectedRegion: RegionArFr? = null

        if (!TextUtils.isEmpty(regionCode)) {
            WiqaytnaApp.regionsData?.forEach { region1 ->
                if (TextUtils.equals(region1.code_region, regionCode)) {
                    selectedRegion = region1
                    when (PreferencesHelper.getCurrentLanguage()) {
                        PreferencesHelper.FRENCH_LANGUAGE_CODE -> region.setText(region1.region_fr)
                        PreferencesHelper.ARABIC_LANGUAGE_CODE -> region.setText(region1.region_ar)
                    }
                }
            }
        }

        if (selectedRegion != null && !TextUtils.isEmpty(provinceCode)) {
            for (i in 0 until selectedRegion!!.provinces.size) {
                if (TextUtils.equals(provinceCode, selectedRegion!!.provinces[i].code_province)) {
                    if (TextUtils.equals(
                            PreferencesHelper.getCurrentLanguage(),
                            PreferencesHelper.FRENCH_LANGUAGE_CODE
                        )
                    ) {
                        province.setText(selectedRegion!!.provinces[i].province_fr)
                    } else {
                        province.setText(selectedRegion!!.provinces[i].province_ar)
                    }
                }
            }
        }

    }

    private fun setClickListeners() {
        region.setOnClickListener {
            Utils.spinnerAsAnEditText(
                resources.getString(R.string.region),
                this.ragions.toTypedArray(),
                region,
                this@PersonalInfosActivity,
                this@PersonalInfosActivity
            )
        }

        province.setOnClickListener {
            Utils.spinnerAsAnEditText(
                resources.getString(R.string.province),
                getProvinces(region.text.toString()).toTypedArray(),
                province,
                this@PersonalInfosActivity
            )
        }

        close.setOnClickListener {
            finish()
        }

        man.setOnClickListener {
            woman.isChecked = !man.isChecked
        }

        woman.setOnClickListener {
            man.isChecked = !woman.isChecked
        }

        arabe.setOnClickListener {
            french.isChecked = !arabe.isChecked
        }

        french.setOnClickListener {
            arabe.isChecked = !french.isChecked
        }

        firstRange.setOnClickListener {
            if (firstRange.isChecked) {
                secondRange.isChecked = !firstRange.isChecked
                thirdRange.isChecked = !firstRange.isChecked
                fourthRange.isChecked = !firstRange.isChecked
            }
        }

        secondRange.setOnClickListener {
            if (secondRange.isChecked) {
                firstRange.isChecked = !secondRange.isChecked
                thirdRange.isChecked = !secondRange.isChecked
                fourthRange.isChecked = !secondRange.isChecked
            }
        }

        thirdRange.setOnClickListener {
            if (thirdRange.isChecked) {
                secondRange.isChecked = !thirdRange.isChecked
                firstRange.isChecked = !thirdRange.isChecked
                fourthRange.isChecked = !thirdRange.isChecked
            }
        }

        fourthRange.setOnClickListener {
            if (fourthRange.isChecked) {
                secondRange.isChecked = !fourthRange.isChecked
                thirdRange.isChecked = !fourthRange.isChecked
                firstRange.isChecked = !fourthRange.isChecked
            }
        }

        save.setOnClickListener {
            val progressDialog = ProgressDialog(this)
            progressDialog.show()

            var ageRange = ""
            var gender = ""

            if (firstRange.isChecked) ageRange = "1"
            if (secondRange.isChecked) ageRange = "2"
            if (thirdRange.isChecked) ageRange = "3"
            if (fourthRange.isChecked) ageRange = "4"

            var regionCode = ""
            var regionValue = ""
            var selectedRegion: RegionArFr? = null

            var provinceCode = ""
            var provinceValue = ""

            CentralLog.d("loop selected", region.text.toString())
            val size = WiqaytnaApp.regionsData!!.size

            if (!TextUtils.isEmpty(region.text.toString())) {
                for (i in 0 until size) {
                    if (TextUtils.equals(
                            PreferencesHelper.getCurrentLanguage(),
                            PreferencesHelper.FRENCH_LANGUAGE_CODE
                        )
                    ) {
                        if (TextUtils.equals(
                                region.text.toString(),
                                WiqaytnaApp.regionsData!![i].region_fr
                            )
                        ) {
                            regionCode = WiqaytnaApp.regionsData!![i].code_region
                            regionValue = region.text.toString()
                            selectedRegion = WiqaytnaApp.regionsData!![i]
                            break
                        }
                    } else {
                        if (TextUtils.equals(
                                region.text.toString(),
                                WiqaytnaApp.regionsData!![i].region_ar
                            )
                        ) {
                            regionCode = WiqaytnaApp.regionsData!![i].code_region
                            regionValue = WiqaytnaApp.regionsData!![i].region_fr
                            selectedRegion = WiqaytnaApp.regionsData!![i]
                            break
                        }
                    }
                }
            }

            if (selectedRegion != null && !TextUtils.isEmpty(province.text.toString())) {
                for (i in 0 until selectedRegion.provinces.size) {
                    if (TextUtils.equals(
                            PreferencesHelper.getCurrentLanguage(),
                            PreferencesHelper.FRENCH_LANGUAGE_CODE
                        )
                    ) {
                        if (TextUtils.equals(
                                province.text.toString(),
                                selectedRegion.provinces[i].province_fr
                            )
                        ) {
                            provinceCode = selectedRegion.provinces[i].code_province
                            provinceValue = selectedRegion.provinces[i].province_fr
                            break
                        }
                    } else {
                        if (TextUtils.equals(
                                province.text.toString(),
                                selectedRegion.provinces[i].province_ar
                            )
                        ) {
                            provinceCode = selectedRegion.provinces[i].code_province
                            provinceValue = selectedRegion.provinces[i].province_fr
                            break
                        }
                    }
                }
            }


            gender = if (man.isChecked) {
                "male"
            } else {
                "female"
            }

            val language = if (arabe.isChecked) {
                "ar"
            } else {
                "fr"
            }

            setUser(ageRange, provinceCode, regionCode, gender, language)
                .addOnSuccessListener {
                    if (!isFinishing) {
                        progressDialog.dismiss()
                    }
                    PreferencesHelper.setPreference(PreferencesHelper.AGE_RANGE, ageRange)
                    PreferencesHelper.setPreference(PreferencesHelper.GENDER, gender)
                    PreferencesHelper.setPreference(PreferencesHelper.REGION, regionCode)
                    PreferencesHelper.setPreference(PreferencesHelper.PROVINCE, provinceCode)
                    CentralLog.d("current language", "" + PreferencesHelper.getCurrentLanguage())
                    CentralLog.d("selected language", "$language")
                    chosenLanguage = language
                    if (!TextUtils.equals(chosenLanguage, PreferencesHelper.getCurrentLanguage())) {
                        onLanguagePicked(chosenLanguage)
                    } else {
                        finish()
                    }
                }.addOnFailureListener { exception ->
                    crashlytics.recordException(exception)
                    crashlytics.setCustomKey("error", "couldn't update settings data")
                    chosenLanguage = language
                    CentralLog.d("current language", "" + PreferencesHelper.getCurrentLanguage())
                    CentralLog.d("selected language", "$language")
                    if (!isFinishing) {
                        progressDialog.dismiss()
                        Utils.showAlertDialog(
                            resources.getString(R.string.infos_sent),
                            R.drawable.ic_warning,
                            this,
                            this as Utils.OnDialogClick
                        )
                    }
                }
        }
    }

    private fun setUser(
        age: String,
        province: String,
        region: String,
        gender: String,
        lang: String
    ): Task<HttpsCallableResult> {
        // Create the arguments to the callable function.
        val data = hashMapOf(
            "age" to age,
            "provinceID" to province,
            "regionID" to region,
            "gender" to gender,
            "lang" to lang
        )

        CentralLog.d("data sent", data.toString())

        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        return functions
            .getHttpsCallable("updateUser")
            .call(data)
    }

    private fun onLanguagePicked(lang: String) {
        PreferencesHelper.setLanguage(lang)
        if (LocaleHelper.setLocaleIfNeeded(lang)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun getRegions() {
        WiqaytnaApp.regionsData?.forEach { region1 ->
            when (PreferencesHelper.getCurrentLanguage()) {
                PreferencesHelper.FRENCH_LANGUAGE_CODE -> this.ragions.add(region1.region_fr)
                PreferencesHelper.ARABIC_LANGUAGE_CODE -> this.ragions.add(region1.region_ar)
            }
        }

        this.ragions.sort()
    }

    private fun getProvinces(selectedRegion: String?): List<String> {

        val provinces = mutableListOf<String>()

        if (!TextUtils.isEmpty(selectedRegion)) {
            var selectedRegion: RegionArFr? = null

            for (i in WiqaytnaApp.regionsData.indices) {
                if (TextUtils.equals(
                        PreferencesHelper.getCurrentLanguage(),
                        PreferencesHelper.FRENCH_LANGUAGE_CODE
                    )
                ) {
                    if (TextUtils.equals(
                            region.text.toString(),
                            WiqaytnaApp.regionsData[i].region_fr
                        )
                    ) {
                        selectedRegion = WiqaytnaApp.regionsData[i]
                    }
                } else {
                    if (TextUtils.equals(
                            region.text.toString(),
                            WiqaytnaApp.regionsData[i].region_ar
                        )
                    ) {
                        selectedRegion = WiqaytnaApp.regionsData[i]
                    }
                }
            }

            selectedRegion!!.provinces.forEach { province ->
                when (PreferencesHelper.getCurrentLanguage()) {
                    PreferencesHelper.FRENCH_LANGUAGE_CODE -> provinces.add(province.province_fr)
                    PreferencesHelper.ARABIC_LANGUAGE_CODE -> provinces.add(province.province_ar)
                }
            }
        }


        provinces.sort()

        return provinces
    }

    override fun runWhenItemSelected(region: String?) {
        val provincesList = getProvinces(region)
        province.setText("")
        province.setOnClickListener {
            Utils.spinnerAsAnEditText(
                resources.getString(R.string.province),
                provincesList.toTypedArray(),
                province,
                this@PersonalInfosActivity
            )
        }
    }

    override fun okClicked() {
        if (!TextUtils.equals(chosenLanguage, PreferencesHelper.getCurrentLanguage())) {
            onLanguagePicked(chosenLanguage)
        } else {
            finish()
        }
    }

}
