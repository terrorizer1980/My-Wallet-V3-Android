package piuk.blockchain.android.ui.account

import android.os.Parcelable
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.Currency

@Parcelize
data class PaymentConfirmationDetails(
    val crypto: CryptoCurrency,
    val fromLabel: String = "",
    val toLabel: String = "",
    val fiatUnit: String = "",
    val cryptoAmount: String = "",
    val fiatAmount: String = "",
    val cryptoFee: String = "",
    val fiatFee: String = "",
    val cryptoTotal: String = "",
    val fiatTotal: String = "",
    val btcSuggestedFee: String = "",
    val isLargeTransaction: Boolean = false,
    val hasConsumedAmounts: Boolean = false,
    val showCryptoTotal: Boolean = true,
    val warningText: String = "",
    val warningSubtext: String = ""
) : Parcelable {

    val fiatSymbol: String
        get() = Currency.getInstance(fiatUnit).symbol

    @IgnoredOnParcel
    var cryptoFeeUnit: String = ""
        get() = if (field.isEmpty()) crypto.displayTicker else field

    override fun toString(): String {
        return "PaymentConfirmationDetails{" +
            "fromLabel='$fromLabel'" +
            ", toLabel='$toLabel'" +
            ", cryptoUnit='${crypto.displayTicker}'" +
            ", fiatUnit='$fiatUnit'" +
            ", cryptoAmount='$cryptoAmount'" +
            ", fiatAmount='$fiatAmount'" +
            ", cryptoFee='$cryptoFee'" +
            ", fiatFee='$fiatFee'" +
            ", cryptoTotal='$cryptoTotal'" +
            ", fiatTotal='$fiatTotal'" +
            ", btcSuggestedFee='$btcSuggestedFee'" +
            ", fiatSymbol='$fiatSymbol'" +
            ", isLargeTransaction='$isLargeTransaction'" +
            ", hasConsumedAmounts='$hasConsumedAmounts'" +
            ", warningText='$warningText'" +
            ", warningSubtext='$warningSubtext'" +
            "}"
    }
}