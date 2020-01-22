package piuk.blockchain.android.ui.campaign

import android.content.DialogInterface
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

abstract class PromoBottomSheet : SlidingModalBottomDialog() {

    interface Host {
        fun onSheetClosed()
    }

    protected val host: Host by lazy {
        parentFragment as? Host
            ?: activity as? Host
            ?: throw IllegalStateException("Host is not a PromoBottomSheet.Host")
    }

    override fun onSheetHidden() {
        host.onSheetClosed()
        super.onSheetHidden()
    }

    override fun onCancel(dialog: DialogInterface) {
        host.onSheetClosed()
        super.onCancel(dialog)
    }
}