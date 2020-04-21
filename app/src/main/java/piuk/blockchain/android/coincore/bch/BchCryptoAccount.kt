package piuk.blockchain.android.coincore.bch

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.TxCache
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList

internal class BchCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager,
    override val txCache: TxCache
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.BCH)
}

internal class BchCryptoAccountNonCustodial(
    override val label: String,
    private val address: String,
    private val bchManager: BchDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    override val txCache: TxCache
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.BCH)

    override val balance: Single<CryptoValue>
        get() = bchManager.getBalance(address)
            .map { CryptoValue.fromMinor(CryptoCurrency.BCH, it) }

    override val activity: Single<ActivitySummaryList>
        get() = bchManager.getAddressTransactions(address, transactionFetchCount, transactionFetchOffset)
            .mapList {
                BchActivitySummaryItem(
                    it,
                    exchangeRates
                ) as ActivitySummaryItem
            }.doOnSuccess { txCache.addToCache(it) }

    constructor(
        jsonAccount: GenericMetadataAccount,
        bchManager: BchDataManager,
        isDefault: Boolean,
        exchangeRates: ExchangeRateDataManager,
        txCache: TxCache
    ) : this(
        jsonAccount.label,
        jsonAccount.xpub,
        bchManager,
        isDefault,
        exchangeRates,
        txCache
    )
}