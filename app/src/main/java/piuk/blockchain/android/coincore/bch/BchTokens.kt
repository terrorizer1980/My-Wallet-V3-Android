package piuk.blockchain.android.coincore.bch

import androidx.annotation.VisibleForTesting
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.bitcoinj.core.Address
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.impl.BitcoinLikeTokens
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.impl.mapList
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.impl.toCryptoSingle
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber

internal class BchTokens(
    private val bchDataManager: BchDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val custodialWalletManager: CustodialWalletManager,
    private val environmentSettings: EnvironmentConfig,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : BitcoinLikeTokens(labels, crashLogger, rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun initToken(): Completable =
        bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
            .andThen(Completable.defer { updater() })

   override fun initActivities(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccount(labels: DefaultLabels): List<CryptoSingleAccount> =
        emptyList()

    override fun loadCustodialAccount(labels: DefaultLabels): List<CryptoSingleAccount> =
        emptyList()

    override fun defaultAccountRef(): Single<AccountReference> =
        with(bchDataManager) {
            val a = getAccountMetadataList()[getDefaultAccountPosition()]
            Single.just(a.toAccountReference())
        }

    override fun defaultAccount(): Single<CryptoSingleAccount> =
        with(bchDataManager) {
            val a = getAccountMetadataList()[getDefaultAccountPosition()]
            Single.just(BchCryptoAccount(a))
        }

    override fun receiveAddress(): Single<String> =
        bchDataManager.getNextReceiveAddress(
            bchDataManager.getAccountMetadataList().indexOfFirst {
                it.xpub == bchDataManager.getDefaultGenericMetadataAccount()!!.xpub
            }
        ).map {
            val address = Address.fromBase58(environmentSettings.bitcoinCashNetworkParameters, it)
            address.toCashAddress()
        }.singleOrError()

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.BCH)

    override fun noncustodialBalance(): Single<CryptoValue> =
        updater().toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getWalletBalance() }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = accountReference(account)
        return updater().toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getAddressBalance(ref.xpub) }
    }

    override fun doUpdateBalances(): Completable =
        bchDataManager.updateAllBalances()
            .doOnComplete { Timber.d("Got btc balance") }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, period)

    override fun onLogoutSignal(event: AuthEvent) {
        if (event != AuthEvent.LOGIN) {
            bchDataManager.clearBchAccountDetails()
        }
        super.onLogoutSignal(event)
    }

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        when (itemAccount.type) {
            ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY -> getAllTransactions()
            ItemAccount.TYPE.ALL_LEGACY -> getLegacyTransactions()
            ItemAccount.TYPE.SINGLE_ACCOUNT -> getAccountTransactions(itemAccount.address!!)
        }.singleOrError()

    private fun getAllTransactions(): Observable<ActivitySummaryList> =
        bchDataManager.getWalletTransactions(transactionFetchCount, transactionFetchOffset)
            .mapList {
                BchActivitySummaryItem(
                    it,
                    exchangeRates
                )
            }

    private fun getLegacyTransactions(): Observable<ActivitySummaryList> =
        bchDataManager.getImportedAddressTransactions(transactionFetchCount, transactionFetchOffset)
            .mapList {
                BchActivitySummaryItem(
                    it,
                    exchangeRates
                )
            }

    private fun getAccountTransactions(address: String): Observable<List<ActivitySummaryItem>> =
        bchDataManager.getAddressTransactions(address, transactionFetchCount, transactionFetchOffset)
            .mapList {
                BchActivitySummaryItem(
                    it,
                    exchangeRates
                )
            }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class BchActivitySummaryItem(
    private val transactionSummary: TransactionSummary,
    exchangeRates: ExchangeRateDataManager
) : ActivitySummaryItem(exchangeRates) {

    override val cryptoCurrency = CryptoCurrency.BCH
    override val direction: TransactionSummary.Direction
        get() = transactionSummary.direction
    override val timeStamp: Long
        get() = transactionSummary.time

    override val totalCrypto: CryptoValue =
        CryptoValue.fromMinor(CryptoCurrency.BCH, transactionSummary.total)

    override val description: String? = null

    override val fee: Observable<CryptoValue>
        get() = Observable.just(CryptoValue.fromMinor(CryptoCurrency.BCH, transactionSummary.fee))

    override val hash: String =
        transactionSummary.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = transactionSummary.inputsMap.mapValues { CryptoValue.fromMinor(CryptoCurrency.BCH, it.value) }

    override val outputsMap: Map<String, CryptoValue>
        get() = transactionSummary.outputsMap.mapValues { CryptoValue.fromMinor(CryptoCurrency.BCH, it.value) }

    override val confirmations: Int
        get() = transactionSummary.confirmations

    override val watchOnly: Boolean
        get() = transactionSummary.isWatchOnly

    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend

    override val isPending: Boolean
        get() = transactionSummary.isPending
}
