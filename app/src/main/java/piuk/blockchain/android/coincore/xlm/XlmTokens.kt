package piuk.blockchain.android.coincore.xlm

import androidx.annotation.VisibleForTesting
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.compareTo
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.impl.mapList
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalArgumentException

internal class XlmTokens(
    private val xlmDataManager: XlmDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokensBase(labels, crashLogger, rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.XLM

    override fun initToken(): Completable =
        Completable.complete()

    override fun initActivities(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccount(labels: DefaultLabels): List<CryptoSingleAccount> =
        emptyList()

    override fun loadCustodialAccount(labels: DefaultLabels): List<CryptoSingleAccount> =
        emptyList()

    override fun defaultAccountRef(): Single<AccountReference> =
        xlmDataManager.defaultAccountReference()

    override fun defaultAccount(): Single<CryptoSingleAccount> {
        TODO("not implemented")
    }

    override fun receiveAddress(): Single<String> =
        defaultAccountRef().map {
            it.receiveAddress
        }

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.XLM)

    override fun noncustodialBalance(): Single<CryptoValue> =
        xlmDataManager.getBalance()

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Xlm
            ?: throw IllegalArgumentException("Not an XLM Account Ref")
        return xlmDataManager.getBalance(ref)
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.XLM, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.XLM, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.XLM, currencyPrefs.selectedFiatCurrency, period)

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        getTransactions()
            .singleOrError()

    private fun getTransactions(): Observable<ActivitySummaryList> =
        xlmDataManager.getTransactionList()
            .toObservable()
            .mapList {
                XlmActivitySummaryItem(
                    it,
                    exchangeRates
                )
            }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class XlmActivitySummaryItem(
    private val xlmTransaction: XlmTransaction,
    exchangeRates: ExchangeRateDataManager
) : ActivitySummaryItem(exchangeRates) {
    override val cryptoCurrency = CryptoCurrency.XLM

    override val direction: TransactionSummary.Direction
        get() = if (xlmTransaction.value > CryptoValue.ZeroXlm) {
            TransactionSummary.Direction.RECEIVED
        } else {
            TransactionSummary.Direction.SENT
        }

    override val timeStamp: Long
        get() = xlmTransaction.timeStamp.fromIso8601ToUtc()!!.toLocalTime().time.div(1000)

    override val totalCrypto: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.XLM, xlmTransaction.accountDelta.amount.abs())
    }

    override val description: String? = null

    override val fee: Observable<CryptoValue>
        get() = Observable.just(
            CryptoValue.fromMinor(CryptoCurrency.XLM, xlmTransaction.fee.amount)
        )

    override val hash: String
        get() = xlmTransaction.hash

    override val inputsMap: HashMap<String, CryptoValue>
        get() = hashMapOf(xlmTransaction.from.accountId to CryptoValue.ZeroXlm)

    override val outputsMap: HashMap<String, CryptoValue>
        get() = hashMapOf(xlmTransaction.to.accountId to CryptoValue.fromMinor(CryptoCurrency.XLM, totalCrypto.amount))

    override val confirmations: Int
        get() = CryptoCurrency.XLM.requiredConfirmations
}
