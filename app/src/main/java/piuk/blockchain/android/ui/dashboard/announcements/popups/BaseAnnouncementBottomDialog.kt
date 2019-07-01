package piuk.blockchain.android.ui.dashboard.announcements.popups

import android.content.DialogInterface
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.sunriver.ui.BaseAirdropBottomDialog
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

abstract class BaseAnnouncementBottomDialog(content: Content) : BaseAirdropBottomDialog(content) {

    protected val dismissKey: String
        get() = arguments?.get(ARG_DISMISS_KEY) as? String ?: ""

    protected val analytics: Analytics by inject()
    protected val dismissRecorder: DismissRecorder by inject()

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        dismissRecorder[dismissKey].dismiss(DismissRule.DismissForever)
    }

    companion object {
        const val ARG_DISMISS_KEY = "ARG_DISMISS_KEY"
    }
}