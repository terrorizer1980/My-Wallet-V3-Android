package piuk.blockchain.android.ui.thepit

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.wallet_mercury_linking_botton_dialog.*
import piuk.blockchain.android.R
import com.blockchain.ui.dialog.ErrorBottomDialog
import piuk.blockchain.androidcoreui.utils.extensions.goneIf

class PitStateBottomDialog : ErrorBottomDialog() {
    @Parcelize
    data class StateContent(val content: Content, val isLoading: Boolean) : Parcelable

    override val layout: Int
        get() = R.layout.wallet_mercury_linking_botton_dialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isLoading = arguments?.getBoolean(ARG_LOADING, false) ?: false
        state_loading.goneIf(!isLoading)
        dialog_icon.goneIf(isLoading)
    }

    companion object {
        private const val ARG_CONTENT = "arg_content"
        private const val ARG_LOADING = "arg_loading"
        fun newInstance(stateContent: StateContent): PitStateBottomDialog {
            return PitStateBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, stateContent.content)
                    putBoolean(ARG_LOADING, stateContent.isLoading)
                }
            }
        }
    }
}