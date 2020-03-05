package piuk.blockchain.android.ui.account

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Currency

@Parcelize
data class PaymentConfirmationDetails(
    var fromLabel: String = "",
    var toLabel: String = "",
    var fiatUnit: String = "",
    var cryptoUnit: String = "",
    var cryptoAmount: String = "",
    var fiatAmount: String = "",
    var cryptoFee: String = "",
    var fiatFee: String = "",
    var cryptoTotal: String = "",
    var fiatTotal: String = "",
    var btcSuggestedFee: String = "",
    var isLargeTransaction: Boolean = false,
    var hasConsumedAmounts: Boolean = false,
    var showCryptoTotal: Boolean = true,
    var warningText: String = "",
    var warningSubtext: String = ""
) : Parcelable {

    val fiatSymbol: String
        get() = Currency.getInstance(fiatUnit).symbol

    var cryptoFeeUnit: String = ""
        get() = if (field.isEmpty()) cryptoUnit else field

    override fun toString(): String {
        return "PaymentConfirmationDetails{" +
            "fromLabel='$fromLabel'" +
            ", toLabel='$toLabel'" +
            ", cryptoUnit='$cryptoUnit'" +
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