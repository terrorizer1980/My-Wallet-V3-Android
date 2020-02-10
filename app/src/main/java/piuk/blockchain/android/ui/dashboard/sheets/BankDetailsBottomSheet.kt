package piuk.blockchain.android.ui.dashboard.sheets

import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.View
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.ui.urllinks.MODULAR_TERMS_AND_CONDITIONS
import kotlinx.android.synthetic.main.dialog_simple_buy_bank_details.view.*
import kotlinx.android.synthetic.main.fragment_simple_buy_bank_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced

class BankDetailsBottomSheet : SlidingModalBottomDialog() {

    private val prefs: SimpleBuyPrefs by inject()
    private val stringUtils: StringUtils by inject()
    private val sbStateFacatory: SimpleBuySyncFactory by inject()
    private val custodialWalletManager: CustodialWalletManager by inject()

    override val layoutResource = R.layout.dialog_simple_buy_bank_details

    override fun initControls(view: View) =
        sbStateFacatory.currentState()?.let { renderState(view, it) } ?: onCtaOKClick()

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

                val linksMap = mapOf<String, Uri>(
                    "modular_terms_and_conditions" to Uri.parse(MODULAR_TERMS_AND_CONDITIONS)
                )
                if (state.currency == "GBP") {
                    bank_deposit_instruction.text =
                        stringUtils.getStringWithMappedLinks(R.string.recipient_name_must_match_gbp,
                            linksMap,
                            requireActivity())
                    bank_deposit_instruction.movementMethod = LinkMovementMethod.getInstance()
                } else {
                    bank_deposit_instruction.text = getString(R.string.recipient_name_must_match_eur)
                }
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
