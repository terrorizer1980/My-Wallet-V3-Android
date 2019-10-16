package piuk.blockchain.androidcoreui.utils

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener

class CameraPermissionListener(
    private val analytics: Analytics,
    private val granted: () -> Unit,
    private val denied: () -> Unit = {}
) : BasePermissionListener() {

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        granted()
        analytics.logEvent(AnalyticsEvents.CameraSystemPermissionApproved)
        super.onPermissionGranted(response)
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        denied()
        analytics.logEvent(AnalyticsEvents.CameraSystemPermissionDeclined)
        super.onPermissionDenied(response)
    }
}