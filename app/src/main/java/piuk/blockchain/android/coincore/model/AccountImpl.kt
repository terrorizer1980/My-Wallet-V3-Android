package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import io.reactivex.Single
import piuk.blockchain.android.coincore.AvailableActions

// TEMP while I sketch out the interface - move these to coin specific files/packages later on

abstract class BaseCryptoAccount: CryptoAccount() {

    override val receiveAddress: Single<String>
        get() = Single.error(NotImplementedError("ReceiveAddress not implemented"))

    override val balance: Single<CryptoValue>
        get() = Single.error(NotImplementedError("balance not implemented"))

    override val activity: Single<ActivitySummaryList>
        get() = Single.error(NotImplementedError("activity not implemented"))

    override val actions: AvailableActions
        get() = emptySet()

    override val hasTransactions: Boolean
        get() = false

    override val isFunded: Boolean
        get() = false
}


class BtcCryptoAccount(
    override val label: String,
    private val address: String
) : BaseCryptoAccount() {
    override val cryptoCurrency = CryptoCurrency.BTC

    constructor(jsonAccount: Account)
        : this(jsonAccount.label, jsonAccount.xpub)
    }
}

class BchCryptoAccount(
    override val label: String,
    private val address: String
) : BaseCryptoAccount() {
    override val cryptoCurrency = CryptoCurrency.BCH

    constructor(jsonAccount: GenericMetadataAccount)
        : this(jsonAccount.label, jsonAccount.xpub)
}

class XlmCryptoAccount(
    override val label: String = "",
    private val address: String
) : BaseCryptoAccount() {
    override val cryptoCurrency = CryptoCurrency.XLM
}

class PaxCryptoAccount(
    override val label: String,
    private val address: String
) : BaseCryptoAccount() {
    override val cryptoCurrency = CryptoCurrency.PAX
}

class StxCryptoAccount(
    override val label: String,
    private val address: String
) : BaseCryptoAccount() {
    override val cryptoCurrency = CryptoCurrency.PAX
}