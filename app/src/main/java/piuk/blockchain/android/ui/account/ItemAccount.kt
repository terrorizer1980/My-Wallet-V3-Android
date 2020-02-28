package piuk.blockchain.android.ui.account

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoValue
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiat

class ItemAccount {

    enum class TYPE {
        ALL_ACCOUNTS_AND_LEGACY, ALL_LEGACY, SINGLE_ACCOUNT
    }

    var label: String? = null
    var displayBalance: CryptoValue? = null
    var tag: String? = null
//    var absoluteBalance: Long? = null

    // Ultimately this is used to sign txs
    var accountObject: JsonSerializableAccount? = null

    // Address/Xpub to fetch balance/tx list
    var address: String? = null
    var type: TYPE = TYPE.SINGLE_ACCOUNT

    constructor() {
        // Empty constructor for serialization
    }

    @JvmOverloads
    constructor(
        label: String?,
        displayBalance: CryptoValue?,
        tag: String? = null,
        absoluteBalance: Long?,
        accountObject: JsonSerializableAccount? = null,
        address: String? = null,
        type: TYPE = TYPE.SINGLE_ACCOUNT
    ) {
        this.label = label
        this.displayBalance = displayBalance
        this.tag = tag
        this.absoluteBalance = absoluteBalance
        this.address = address
        this.accountObject = accountObject
        this.type = type
    }

    constructor(label: String?) {
        this.label = label
    }
}

fun ItemAccount.formatDisplayBalance(currencyState: CurrencyState, exchangeRates: ExchangeRateDataManager) =
    if(currencyState.displayMode == CurrencyState.DisplayMode.Fiat) {
        displayBalance?.toFiat(exchangeRates, currencyState.fiatUnit)
    } else {
        displayBalance
    }?.toStringWithSymbol() ?: ""