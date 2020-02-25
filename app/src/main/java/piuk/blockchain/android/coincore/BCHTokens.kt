package piuk.blockchain.android.coincore

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.toAccountReference
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
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.android.coincore.model.ActivitySummaryList
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber
import java.math.BigInteger

class BCHTokens(
    private val bchDataManager: BchDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val crashLogger: CrashLogger,
    private val custodialWalletManager: CustodialWalletManager,
    private val environmentSettings: EnvironmentConfig,
    rxBus: RxBus
) : BitcoinLikeTokens(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun defaultAccount(): Single<CryptoAccount> =
        with(bchDataManager) {
            val a = getAccountMetadataList()[getDefaultAccountPosition()]
            Single.just(a.toAccountReference())
        }

    override fun receiveAddress(): Single<String> =
        bchDataManager.getNextReceiveAddress(
            bchDataManager.getAccountMetadataList().indexOfFirst {
                it.xpub == bchDataManager.getDefaultGenericMetadataAccount()!!.xpub
            }).map {
            val address = Address.fromBase58(environmentSettings.bitcoinCashNetworkParameters, it)
            address.toCashAddress()
        }.singleOrError()

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.BCH)

    override fun noncustodialBalance(): Single<CryptoValue> =
        walletInitialiser()
            .andThen(Completable.defer { updater() })
            .toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getWalletBalance() }

    override fun balance(account: CryptoAccount): Single<CryptoValue> {
        val ref = accountReference(account)

        return walletInitialiser()
            .andThen(Completable.defer { updater() })
            .toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getAddressBalance(ref.xpub) }
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

    private var isWalletUninitialised = true

    private fun walletInitialiser() =
        if (isWalletUninitialised) {
            bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
                .doOnError { throwable ->
                    crashLogger.logException(throwable, "Failed to load bch wallet")
                }.doOnComplete {
                    isWalletUninitialised = false
                }
        } else {
            Completable.complete()
        }

    override fun onLogoutSignal(event: AuthEvent) {
        isWalletUninitialised = true
        bchDataManager.clearBchAccountDetails()
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
                BchActivitySummaryItem(it, exchangeRates, currencyPrefs.selectedFiatCurrency)
            }

    private fun getLegacyTransactions(): Observable<ActivitySummaryList> =
        bchDataManager.getImportedAddressTransactions(transactionFetchCount, transactionFetchOffset)
            .mapList {
                BchActivitySummaryItem(it, exchangeRates, currencyPrefs.selectedFiatCurrency)
            }

    private fun getAccountTransactions(address: String): Observable<List<ActivitySummaryItem>> =
        bchDataManager.getAddressTransactions(address, transactionFetchCount, transactionFetchOffset)
            .mapList {
                BchActivitySummaryItem(it, exchangeRates, currencyPrefs.selectedFiatCurrency)
            }
}

class BchActivitySummaryItem(
    private val transactionSummary: TransactionSummary,
    exchangeRates: ExchangeRateDataManager,
    selectedFiat: String
) : ActivitySummaryItem() {

    override val cryptoCurrency = CryptoCurrency.BCH
    override val direction: TransactionSummary.Direction
        get() = transactionSummary.direction
    override val timeStamp: Long
        get() = transactionSummary.time

    override val totalCrypto: CryptoValue =
        CryptoValue.fromMinor(CryptoCurrency.BCH, transactionSummary.total)

    override val totalFiat: FiatValue =
        totalCrypto.toFiat(exchangeRates, selectedFiat)

    override val fee: Observable<BigInteger>
        get() = Observable.just(transactionSummary.fee)

    override val hash: String =
        transactionSummary.hash

    override val inputsMap: Map<String, BigInteger>
        get() = transactionSummary.inputsMap

    override val outputsMap: Map<String, BigInteger>
        get() = transactionSummary.outputsMap

    override val confirmations: Int
        get() = transactionSummary.confirmations

    override val watchOnly: Boolean
        get() = transactionSummary.isWatchOnly

    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend

    override val isPending: Boolean
        get() = transactionSummary.isPending
}
