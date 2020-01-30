package piuk.blockchain.android.ui.campaign

import android.view.View
import kotlinx.android.synthetic.main.dialog_custodial_intro.view.*
import piuk.blockchain.android.R

class CustodyWalletIntroSheet : PromoBottomSheet() {

    override val layoutResource: Int = R.layout.dialog_custodial_intro

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() {
        dismiss()
        onSheetHidden()
    }

    companion object {
        fun newInstance(): CustodyWalletIntroSheet =
            CustodyWalletIntroSheet()
    }
}
