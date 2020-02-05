package piuk.blockchain.android.ui.dashboard.sheets

import android.content.DialogInterface
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

abstract class DashboardBottomSheet : SlidingModalBottomDialog() {

    interface Host {
        fun onSheetClosed()
    }

    protected open val host: Host by lazy {
        parentFragment as? Host
            ?: activity as? Host
            ?: throw IllegalStateException("Host is not a DashboardBottomSheet.Host")
    }

    override fun onSheetHidden() {
        host.onSheetClosed()
        super.onSheetHidden()
    }

    override fun onCancel(dialog: DialogInterface) {
        host.onSheetClosed()
        super.onCancel(dialog)
    }

    override fun dismiss() {
        host.onSheetClosed()
        super.dismiss()
    }
}