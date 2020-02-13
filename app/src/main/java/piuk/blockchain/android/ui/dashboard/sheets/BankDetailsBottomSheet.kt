package piuk.blockchain.android.ui.dashboard.sheets

import android.view.View
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_simple_buy_bank_details.view.*
import kotlinx.android.synthetic.main.fragment_simple_buy_bank_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced
import timber.log.Timber

class BankDetailsBottomSheet : SlidingModalBottomDialog() {

    private val prefs: SimpleBuyPrefs by inject()
    private val stateFactory: SimpleBuySyncFactory by inject()
    private val custodialWalletManager: CustodialWalletManager by inject()
    private val disposables = CompositeDisposable()

    override val layoutResource = R.layout.dialog_simple_buy_bank_details

    override fun initControls(view: View) =
        stateFactory.currentState()?.let {
            renderState(view, it)
        } ?: closeBecauseError("State not found")

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun closeBecauseError(logMsg: String) {
        Timber.d("Cannot open bank details sheet: $logMsg")
        onCtaOKClick()
    }

    private fun onCtaOKClick() {
        disposables.dispose()
        dismiss()
    }

    private fun onCtaCancelClick(orderId: String) {
        custodialWalletManager.deleteBuyOrder(orderId)
        prefs.clearState()
        onCtaOKClick()
    }

    private fun renderState(view: View, state: SimpleBuyState) {
        with(view) {
            val amount = state.order.amount
            if (amount != null) {
                if (state.bankAccount != null) {
                    renderAccountDetails(view, state.bankAccount, amount)
                } else {
                    disposables += custodialWalletManager.getBankAccountDetails(state.currency)
                        .subscribeBy(
                            onSuccess = { renderAccountDetails(view, it, amount) },
                            onError = { closeBecauseError("Cannot get bank details") }
                        )
                }
            } else {
                closeBecauseError("Invalid amount in SB state")
            }

            title.text = getString(R.string.simple_buy_pending_buy_sheet_title, state.selectedCryptoCurrency!!)
            cta_button_ok.setOnClickListenerDebounced { onCtaOKClick() }
            cta_button_cancel.setOnClickListenerDebounced { onCtaCancelClick(state.id!!) }
        }
    }

    private fun renderAccountDetails(view: View, account: BankAccount, amount: FiatValue) {
        view.bank_details_container.initWithBankDetailsAndAmount(account.details, amount)
        secure_transfer.text = getString(R.string.simple_buy_securely_transfer, amount.currencyCode)
    }

    companion object {
        fun newInstance(): BankDetailsBottomSheet =
            BankDetailsBottomSheet()
    }
}
