package covid.trace.morocco.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import covid.trace.morocco.*
import covid.trace.morocco.logging.CentralLog
import covid.trace.morocco.models.ExportData
import covid.trace.morocco.notifications.NotificationTemplates
import covid.trace.morocco.status.persistence.StatusRecord
import covid.trace.morocco.status.persistence.StatusRecordStorage
import covid.trace.morocco.streetpass.persistence.StreetPassRecord
import covid.trace.morocco.streetpass.persistence.StreetPassRecordStorage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_upload_enterpin.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class EnterPinFragment : Fragment() {
    private var TAG = "UploadFragment"
    private var crashlytics = FirebaseCrashlytics.getInstance()

    private var disposeObj: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_enterpin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let{
            Utils.firebaseAnalyticsEvent(it, "upload_screen_2", "11", "upload screen 2")
        }

        if (TextUtils.equals(
                PreferencesHelper.getCurrentLanguage(),
                PreferencesHelper.ARABIC_LANGUAGE_CODE
            )
        ) {
            back.rotation = 180F
        }

        back.setOnClickListener {
            val parentActivity: MainActivity = activity as MainActivity
            parentActivity.openFragment(
                parentActivity.LAYOUT_MAIN_ID,
                ForUseFragment(),
                ForUseFragment::class.java.name
            )
        }

        editTextsListeners()

        enterPinActionButton.setOnClickListener {
            enterPinFragmentErrorMessage.visibility = View.INVISIBLE
            turnOnLoadingProgress()

            var observableStreetRecords = Observable.create<List<StreetPassRecord>> {
                val result = StreetPassRecordStorage(WiqaytnaApp.AppContext).getAllRecords()
                it.onNext(result)
            }
            var observableStatusRecords = Observable.create<List<StatusRecord>> {
                val result = StatusRecordStorage(WiqaytnaApp.AppContext).getAllRecords()
                it.onNext(result)
            }

            disposeObj = Observable.zip(observableStreetRecords, observableStatusRecords,

                BiFunction<List<StreetPassRecord>, List<StatusRecord>, ExportData> { records, status ->
                    ExportData(
                        records,
                        status
                    )
                }

            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { exportedData ->
                    CentralLog.d(TAG, "records: ${exportedData.recordList}")
                    CentralLog.d(TAG, "status: ${exportedData.statusList}")

                    getUploadToken(getPin()).addOnSuccessListener {
                        val response = it.data as HashMap<String, String>
                        try {
                            val uploadToken = response["token"]
                            CentralLog.d(TAG, "uploadToken: $uploadToken")
                            var task = writeToInternalStorageAndUpload(
                                WiqaytnaApp.AppContext,
                                exportedData.recordList,
                                exportedData.statusList,
                                uploadToken
                            )
                            task.addOnFailureListener { exception ->
                                crashlytics.recordException(exception)
                                crashlytics.setCustomKey("error", "failed to upload")
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                crashlytics.setCustomKey("uid", "$uid")
                                CentralLog.d(TAG, "failed to upload")
                                if (!isDetached) {
                                    turnOffLoadingProgress()
                                    Toast.makeText(
                                        context, resources.getString(R.string.server_problem),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }.addOnSuccessListener {
                                CentralLog.d(TAG, "uploaded successfully")
                                if (!isDetached) {
                                    turnOffLoadingProgress()
                                    navigateToUploadComplete()
                                }
                            }
                        } catch (e: Throwable) {
                            crashlytics.recordException(e)
                            crashlytics.setCustomKey("error", "Failed to upload data")
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            crashlytics.setCustomKey("uid", "$uid")
                            context?.let {context ->
                                NotificationTemplates.diskFullWarningNotification(
                                    context,
                                    BuildConfig.SERVICE_FOREGROUND_CHANNEL_ID
                                )
                            }

                            CentralLog.d(TAG, "Failed to upload data: ${e.message}")
                            if (!isDetached) {
                                turnOffLoadingProgress()
                                Toast.makeText(
                                    context, resources.getString(R.string.server_problem),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }.addOnFailureListener { exception ->
                        CentralLog.d(TAG, "Invalid code")
                        crashlytics.recordException(exception)
                        crashlytics.setCustomKey("error", "upload : Invalid code")
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        crashlytics.setCustomKey("uid", "$uid")
                        if (!isDetached) {
                            turnOffLoadingProgress()
                            enterPinFragmentErrorMessage?.visibility = View.VISIBLE
                        }
                    }
                }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        disposeObj?.dispose()
    }

    private fun getUploadToken(uploadCode: String): Task<HttpsCallableResult> {
        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        return functions
            .getHttpsCallable("getUploadToken")
            .call(uploadCode)
    }

    private fun writeToInternalStorageAndUpload(
        context: Context,
        deviceDataList: List<StreetPassRecord>,
        statusList: List<StatusRecord>,
        uploadToken: String?
    ): UploadTask {
        var date = Utils.getDateFromUnix(System.currentTimeMillis())
        var gson = Gson()

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        var updatedDeviceList = deviceDataList.map {
            it.timestamp = it.timestamp / 1000
            return@map it
        }

        var updatedStatusList = statusList.map {
            it.timestamp = it.timestamp / 1000
            return@map it
        }

        var map: MutableMap<String, Any> = HashMap()
        map["token"] = uploadToken as Any
        map["records"] = updatedDeviceList as Any
        map["events"] = updatedStatusList as Any

        val mapString = gson.toJson(map)

        val fileName = "StreetPassRecord_${manufacturer}_${model}_$date.json"
        val fileOutputStream: FileOutputStream

        val uploadDir = File(context.filesDir, "upload")

        if (uploadDir.exists()) {
            uploadDir.deleteRecursively()
        }

        uploadDir.mkdirs()
        val fileToUpload = File(uploadDir, fileName)
//        fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
        fileOutputStream = FileOutputStream(fileToUpload)

        fileOutputStream.write(mapString.toByteArray())
        fileOutputStream.close()

        CentralLog.i(TAG, "File wrote: ${fileToUpload.absolutePath}")

        return uploadToCloudStorage(context, fileToUpload)
    }

    private fun uploadToCloudStorage(context: Context, fileToUpload: File): UploadTask {
        CentralLog.d(TAG, "Uploading to Cloud Storage")

        val bucketName = BuildConfig.FIREBASE_UPLOAD_BUCKET
        val storage = FirebaseStorage.getInstance("gs://${bucketName}")
        var storageRef = storage.getReferenceFromUrl("gs://${bucketName}")

        val dateString = SimpleDateFormat("yyyyMMdd").format(Date())
        var streetPassRecordsRef =
            storageRef.child("streetPassRecords/$dateString/${fileToUpload.name}")

        val fileUri: Uri =
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                fileToUpload
            )

        var uploadTask = streetPassRecordsRef.putFile(fileUri)
        uploadTask.addOnCompleteListener {
            try {
                fileToUpload.delete()
                CentralLog.i(TAG, "upload file deleted")
            } catch (e: Exception) {
                CentralLog.e(TAG, "Failed to delete upload file")
            }
        }
        return uploadTask
    }

    private fun editTextsListeners() {

        otp1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp2?.requestFocus()
            }

        })

        otp2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp3?.requestFocus()
                if (s?.length == 0) otp1?.requestFocus()
            }

        })

        otp3.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp4?.requestFocus()
                if (s?.length == 0) otp2?.requestFocus()
            }

        })

        otp4.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp5?.requestFocus()
                if (s?.length == 0) otp3?.requestFocus()
            }

        })

        otp5.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) otp6?.requestFocus()
                if (s?.length == 0) otp4?.requestFocus()
            }

        })

        otp6.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 0) otp5?.requestFocus()
                if (getPin().length == 6) {
                    Utils.hideKeyboardFrom(view!!.context, view!!)
                }
            }

        })
    }

    private fun turnOnLoadingProgress() {
        if (uploadPageFragmentLoadingProgressBarFrame != null)
            uploadPageFragmentLoadingProgressBarFrame.visibility = View.VISIBLE
    }

    private fun turnOffLoadingProgress() {
        if (uploadPageFragmentLoadingProgressBarFrame != null)
            uploadPageFragmentLoadingProgressBarFrame.visibility = View.INVISIBLE
    }

    private fun navigateToUploadComplete() {
        val parentActivity: MainActivity = activity as MainActivity
        parentActivity.openFragment(
            parentActivity.LAYOUT_MAIN_ID,
            UploadCompleteFragment(),
            UploadCompleteFragment::class.java.name
        )
    }

    private fun getPin(): String {
        return otp1.text.toString() + otp2.text.toString() + otp3.text.toString() + otp4.text.toString() + otp5.text.toString() + otp6.text.toString()
    }

    private fun showAlertDialog(message: String) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(
                "ok",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.dismiss()
                }).create()

        dialog.show()
    }
}
