package piuk.blockchain.android.ui.swap.ui

import android.os.Bundle
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog

class SwapErrorBottomDialog : ErrorBottomDialog() {
    override val layout: Int
        get() = R.layout.swap_error_bottom_dialog

    companion object {
        private const val ARG_CONTENT = "arg_content"
        fun newInstance(content: Content): SwapErrorBottomDialog {
            return SwapErrorBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, content)
                }
            }
        }
    }
}