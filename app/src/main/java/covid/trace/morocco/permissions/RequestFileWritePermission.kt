package covid.trace.morocco.permissions

import android.Manifest
import android.os.Bundle
import covid.trace.morocco.R
import covid.trace.morocco.Utils
import covid.trace.morocco.base.BaseActivity
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class RequestFileWritePermission : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWritePermissionAndExecute()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    @AfterPermissionGranted(RC_FILE_WRITE)
    private fun requestWritePermissionAndExecute() {
        val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            Utils.startBluetoothMonitoringService(this)
            finish()
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this, getString(R.string.permission_write_rationale),
                RC_FILE_WRITE, *perms
            )
        }
    }

    companion object {

        private const val RC_FILE_WRITE = 743
    }
}
