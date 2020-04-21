package piuk.blockchain.android.coincore.xlm

import com.blockchain.sunriver.XlmDataManager
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.TxCache
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList

internal class XlmCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager,
    override val txCache: TxCache
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.XLM)
}

internal class XlmCryptoAccountNonCustodial(
    override val label: String = "",
    private val address: String,
    private val xlmManager: XlmDataManager,
    override val exchangeRates: ExchangeRateDataManager,
    override val txCache: TxCache
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.XLM)

    override val isDefault: Boolean = true // Only one account ever, so always default

    override val balance: Single<CryptoValue>
        get() = xlmManager.getBalance()

    override val activity: Single<ActivitySummaryList>
        get() = xlmManager.getTransactionList()
            .mapList {
                XlmActivitySummaryItem(
                    it,
                    exchangeRates
                ) as ActivitySummaryItem
            }
            .doOnSuccess { txCache.addToCache(it) }

    constructor(
        account: AccountReference.Xlm,
        xlmManager: XlmDataManager,
        exchangeRates: ExchangeRateDataManager,
        txCache: TxCache
    ) : this(account.label, account.accountId, xlmManager, exchangeRates, txCache)
}