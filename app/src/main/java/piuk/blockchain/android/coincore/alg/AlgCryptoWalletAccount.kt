package piuk.blockchain.android.coincore.alg

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class AlgCryptoWalletAccount(
    override val label: String,
    private val address: String,
    private val payloadDataManager: PayloadDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.ALG)

    override val balance: Single<CryptoValue>
        get() = payloadDataManager.getAddressBalanceRefresh(address)

    override val receiveAddress: Single<String>
        get() = payloadDataManager.getNextReceiveAddress(
            payloadDataManager.getAccount(payloadDataManager.defaultAccountIndex)
        ).singleOrError()

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions
        get() = TODO("Not yet implemented")

    override val isFunded: Boolean
        get() = TODO("Not yet implemented")

    constructor(
        jsonAccount: Account,
        payloadDataManager: PayloadDataManager,
        isDefault: Boolean = false,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        jsonAccount.label,
        jsonAccount.xpub,
        payloadDataManager,
        isDefault,
        exchangeRates
    )

    constructor(
        legacyAccount: LegacyAddress,
        payloadDataManager: PayloadDataManager,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        legacyAccount.label ?: legacyAccount.address,
        legacyAccount.address,
        payloadDataManager,
        false,
        exchangeRates
    )
}