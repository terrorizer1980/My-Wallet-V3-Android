package piuk.blockchain.android.ui.confirm

import javax.inject.Inject

import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.UiState

class ConfirmPaymentPresenter @Inject internal constructor()
    : BasePresenter<ConfirmPaymentView>() {

    override fun onViewReady() {
        val paymentDetails = view.paymentDetails

        if (paymentDetails == null) {
            view.closeDialog()
            return
        }

        val contactNote = view.contactNote
        if (contactNote != null) {
            view.contactNote = contactNote
        }

        val contactNoteDescription = view.contactNoteDescription
        if (contactNoteDescription != null) {
            view.contactNoteDescription = contactNoteDescription
        }

        view.setFromLabel(paymentDetails.fromLabel)
        view.setToLabel(paymentDetails.toLabel)
        view.setAmount(
            String.format(
                AMOUNT_FORMAT,
                paymentDetails.cryptoAmount,
                paymentDetails.cryptoUnit,
                paymentDetails.fiatSymbol,
                paymentDetails.fiatAmount
            )
        )
        view.setFee(
            String.format(
                AMOUNT_FORMAT,
                paymentDetails.cryptoFee,
                paymentDetails.cryptoFeeUnit,
                paymentDetails.fiatSymbol,
                paymentDetails.fiatFee
            )
        )

        if (paymentDetails.showCryptoTotal) {
            view.setTotals(
                paymentDetails.cryptoTotal + " " + paymentDetails.cryptoUnit,
                paymentDetails.fiatSymbol + paymentDetails.fiatTotal
            )
        } else {
            view.setFiatTotalOnly(paymentDetails.fiatSymbol + paymentDetails.fiatTotal)
        }

        if (!paymentDetails.warningText.isEmpty()) {
            view.setWarning(paymentDetails.warningText)
            view.setWarningSubText(paymentDetails.warningSubtext)
        }

        view.setUiState(UiState.CONTENT)
    }

    companion object {
        private const val AMOUNT_FORMAT = "%1\$s %2\$s (%3\$s%4\$s)"
    }
}
