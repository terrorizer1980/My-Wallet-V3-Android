package piuk.blockchain.android.ui.thepit

import android.os.Bundle
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog

class PitLaunchBottomDialog : ErrorBottomDialog() {
    override val layout: Int
        get() = R.layout.pit_launch_bottom_dialog

    companion object {
        private const val ARG_CONTENT = "arg_content"
        fun newInstance(content: Content): PitLaunchBottomDialog {
            return PitLaunchBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, content)
                }
            }
        }
    }
}