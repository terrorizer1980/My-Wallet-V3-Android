package piuk.blockchain.android.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException

class OSUtil(private val context: Context) {

    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as? ActivityManager ?: return false
        for (s in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == s.service.className) {
                return true
            }
        }
        return false
    }

    fun hasPackage(p: String): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(p, 0)
            true
        } catch (nnfe: NameNotFoundException) {
            false
        }
    }
}
