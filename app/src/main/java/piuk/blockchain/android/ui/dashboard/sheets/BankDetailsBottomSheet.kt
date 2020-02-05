package piuk.blockchain.android.ui.dashboard.sheets

import android.view.View
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import kotlinx.android.synthetic.main.dialog_simple_buy_bank_details.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuyUtils
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced

class BankDetailsBottomSheet : SlidingModalBottomDialog() {

    private val prefs: SimpleBuyPrefs by inject()
    private val sbUtils: SimpleBuyUtils by inject()
    private val custodialWalletManager: CustodialWalletManager by inject()

    override val layoutResource = R.layout.dialog_simple_buy_bank_details

    override fun initControls(view: View) {
        sbUtils.inflateSimpleBuyState(prefs)?.let {
            renderState(view, it)
        } ?: onCtaOKClick()
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaOKClick() {
        dismiss()
    }

    private fun onCtaCancelClick(orderId: String) {
        custodialWalletManager.deleteBuyOrder(orderId)
        prefs.clearState()
        onCtaOKClick()
    }

    private fun renderState(view: View, state: SimpleBuyState) {
        with(view) {
            title.text = getString(R.string.simple_buy_pending_buy_sheet_title, state.selectedCryptoCurrency!!)

            if (state.bankAccount != null && state.order.amount != null) {
                bank_details_container.initWithBankDetailsAndAmount(
                    state.bankAccount.details,
                    state.order.amount!!
                )
                secure_transfer.text = getString(
                    R.string.simple_buy_securely_transfer,
                    state.order.amount?.currencyCode ?: ""
                )
                cta_button_ok.setOnClickListenerDebounced { onCtaOKClick() }
                cta_button_cancel.setOnClickListenerDebounced { onCtaCancelClick(state.id!!) }
            } else {
                onCtaOKClick()
            }
        }
    }

    companion object {
        fun newInstance(): BankDetailsBottomSheet =
            BankDetailsBottomSheet()
    }
}
