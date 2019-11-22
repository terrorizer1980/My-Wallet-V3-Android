package piuk.blockchain.android.ui.send

import com.blockchain.transactions.SendDetails
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails

data class SendConfirmationDetails(
    val sendDetails: SendDetails,
    val fees: CryptoValue,
    val fiatAmount: FiatValue,
    val fiatFees: FiatValue
) {
    val from: AccountReference = sendDetails.from
    val to: String = sendDetails.toAddress
    val toLabel: String = sendDetails.toLabel
    val amount: CryptoValue = sendDetails.value

    val total = amount + fees
    val fiatTotal = fiatAmount + fiatFees

    internal fun toPaymentConfirmationDetails(): PaymentConfirmationDetails {
        return PaymentConfirmationDetails().also {
            it.fromLabel = from.label
            it.toLabel = if (toLabel.isBlank()) to else toLabel

            it.cryptoUnit = amount.symbol()
            it.cryptoAmount = amount.toStringWithoutSymbol()
            it.cryptoFee = fees.toStringWithoutSymbol()
            it.cryptoTotal = total.toStringWithoutSymbol()

            it.fiatUnit = fiatAmount.currencyCode
            it.fiatSymbol = fiatAmount.symbol()
            it.fiatAmount = fiatAmount.toStringWithoutSymbol()
            it.fiatFee = fiatFees.toStringWithoutSymbol()
            it.fiatTotal = fiatTotal.toStringWithoutSymbol()
        }
    }
}
