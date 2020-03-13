package piuk.blockchain.android.coincore.btc

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.impl.BitcoinLikeTokens
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.impl.toCryptoSingle
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

internal class BtcTokens(
    private val payloadDataManager: PayloadDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val payloadManager: PayloadManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val labels: DefaultLabels,
    rxBus: RxBus
) : BitcoinLikeTokens(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun init(): Completable =
        updater()
            .andThen(Completable.defer { loadAccounts() })
            .andThen(Completable.defer { initActivities() })
            .doOnComplete { Timber.d("Coincore: Init BTC Complete") }
            .doOnError { Timber.d("Coincore: Init BTC Failed") }

    private fun loadAccounts(): Completable =
        Completable.complete()

    private fun initActivities(): Completable =
        Completable.complete()

    override fun defaultAccountRef(): Single<AccountReference> =
        Single.just(payloadDataManager.defaultAccount.toAccountReference())

    override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.just(BtcCryptoAccount(payloadDataManager.defaultAccount))

    override fun accounts(filter: Set<AssetFilter>): Single<CryptoAccountGroup> {
        TODO("not implemented")
    }

    override fun receiveAddress(): Single<String> =
        payloadDataManager.getNextReceiveAddress(payloadDataManager.getAccount(payloadDataManager.defaultAccountIndex))
            .singleOrError()

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.BTC)

    override fun noncustodialBalance(): Single<CryptoValue> =
        updater().toCryptoSingle(CryptoCurrency.BTC) { payloadManager.walletBalance }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = accountReference(account)
        return updater()
            .toCryptoSingle(CryptoCurrency.BTC) { payloadManager.getAddressBalance(ref.xpub) }
    }

    override fun doUpdateBalances(): Completable =
        payloadDataManager.updateAllBalances()

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.BTC, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.BTC, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.BTC, currencyPrefs.selectedFiatCurrency, period)

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        when (itemAccount.type) {
            ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY -> getAllTransactions()
            ItemAccount.TYPE.ALL_LEGACY -> getLegacyTransactions()
            ItemAccount.TYPE.SINGLE_ACCOUNT -> getAccountTransactions(itemAccount.address)
        }

    private fun getAllTransactions(): Single<ActivitySummaryList> =
        Single.fromCallable {
            payloadManager.getAllTransactions(transactionFetchCount, transactionFetchOffset)
                .map {
                    BtcActivitySummaryItem(
                        it,
                        payloadDataManager,
                        exchangeRates
                    )
                }
        }

    private fun getLegacyTransactions(): Single<ActivitySummaryList> =
        Single.fromCallable {
            payloadManager.getImportedAddressesTransactions(transactionFetchCount, transactionFetchOffset)
                .map {
                    BtcActivitySummaryItem(
                        it,
                        payloadDataManager,
                        exchangeRates
                    )
                }
        }

    private fun getAccountTransactions(address: String): Single<ActivitySummaryList> =
        Single.fromCallable {
            payloadManager.getAccountTransactions(address, transactionFetchCount, transactionFetchOffset)
                .map {
                    BtcActivitySummaryItem(
                        it,
                        payloadDataManager,
                        exchangeRates
                    )
                }
        }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class BtcActivitySummaryItem(
    private val transactionSummary: TransactionSummary,
    private val payloadDataManager: PayloadDataManager,
    exchangeRates: ExchangeRateDataManager
) : ActivitySummaryItem(exchangeRates) {

    override val cryptoCurrency = CryptoCurrency.BTC

    override val direction: TransactionSummary.Direction
        get() = transactionSummary.direction

    override val timeStamp: Long
        get() = transactionSummary.time

    override val totalCrypto: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.BTC, transactionSummary.total)
    }

    override val description: String?
        get() = payloadDataManager.getTransactionNotes(hash)

    override val fee: Observable<CryptoValue>
        get() = Observable.just(CryptoValue.fromMinor(CryptoCurrency.BTC, transactionSummary.fee))

    override val hash: String
        get() = transactionSummary.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = transactionSummary.inputsMap
            .mapValues {
                CryptoValue.fromMinor(CryptoCurrency.BTC, it.value)
            }

    override val outputsMap: Map<String, CryptoValue>
        get() = transactionSummary.outputsMap
            .mapValues {
                CryptoValue.fromMinor(CryptoCurrency.BTC, it.value)
            }

    override val confirmations: Int
        get() = transactionSummary.confirmations

    override val watchOnly: Boolean
        get() = transactionSummary.isWatchOnly

    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend

    override val isPending: Boolean
        get() = transactionSummary.isPending

    override fun updateDescription(description: String): Completable =
        payloadDataManager.updateTransactionNotes(hash, description)
}
