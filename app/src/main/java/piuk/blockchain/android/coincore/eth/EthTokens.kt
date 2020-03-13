package piuk.blockchain.android.coincore.eth

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
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.lang.IllegalArgumentException

internal class EthTokens(
    private val ethDataManager: EthDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val crashLogger: CrashLogger,
    private val custodialWalletManager: CustodialWalletManager,
    private val labels: DefaultLabels,
    rxBus: RxBus
) : AssetTokensBase(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun init(): Completable =
        ethDataManager.initEthereumWallet(
            stringUtils.getString(R.string.eth_default_account_label),
            stringUtils.getString(R.string.pax_default_account_label_1)
        ).doOnError { throwable ->
            crashLogger.logException(throwable, "Coincore: Failed to load ETH wallet")
        }
        .andThen(Completable.defer { loadAccounts() })
        .andThen(Completable.defer { initActivities() })
        .doOnComplete { Timber.d("Coincore: Init ETH Complete") }
        .doOnError { Timber.d("Coincore: Init ETH Failed") }

    private fun loadAccounts(): Completable =
        Completable.complete()

    private fun initActivities(): Completable =
        Completable.complete()

    override fun defaultAccountRef(): Single<AccountReference> =
        Single.just(getDefaultEthAccountRef())

    override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.fromCallable {
            EthCryptoAccountNonCustodial(
                this,
                ethDataManager.getEthWallet()?.account ?: throw Exception("No ether wallet found")
            )
        }

    override fun accounts(filter: Set<AssetFilter>): Single<CryptoAccountGroup> {
        TODO("not implemented")
    }

    override fun receiveAddress(): Single<String> =
        Single.just(getDefaultEthAccountRef().receiveAddress)

    private fun getDefaultEthAccountRef(): AccountReference =
        ethDataManager.getEthWallet()?.account?.toAccountReference()
            ?: throw Exception("No ether wallet found")

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.ETHER)

    override fun noncustodialBalance(): Single<CryptoValue> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Ethereum
            ?: throw IllegalArgumentException("Not an XLM Account Ref")

        return ethDataManager.getBalance(ref.address)
            .map { CryptoValue.etherFromWei(it) }
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(
            CryptoCurrency.ETHER,
            currencyPrefs.selectedFiatCurrency,
            epochWhen
        )

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(
            CryptoCurrency.ETHER,
            currencyPrefs.selectedFiatCurrency,
            period
        )

    override fun onLogoutSignal(event: AuthEvent) {
        if (event != AuthEvent.LOGIN) {
            ethDataManager.clearEthAccountDetails()
        }
    }

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        getTransactions()
            .singleOrError()

    internal fun getTransactions(): Observable<ActivitySummaryList> =
        ethDataManager.getLatestBlock()
            .flatMapSingle { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map {
                        val ethFeeForPaxTransaction = it.to.equals(
                            ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                            ignoreCase = true
                        )
                        EthActivitySummaryItem(
                            ethDataManager,
                            it,
                            ethFeeForPaxTransaction,
                            latestBlock.blockHeight,
                            exchangeRates
                        )
                    }.toList()
                }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class EthActivitySummaryItem(
    private val ethDataManager: EthDataManager,
    private val ethTransaction: EthTransaction,
    override val isFeeTransaction: Boolean,
    private val blockHeight: Long,
    exchangeRates: ExchangeRateDataManager
) : ActivitySummaryItem(exchangeRates) {

    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.ETHER

    override val direction: TransactionSummary.Direction by unsafeLazy {
        val combinedEthModel = ethDataManager.getEthResponseModel()!!
        combinedEthModel.getAccounts().let {
            when {
                it[0] == ethTransaction.to && it[0] == ethTransaction.from ->
                    TransactionSummary.Direction.TRANSFERRED
                it.contains(ethTransaction.from) ->
                    TransactionSummary.Direction.SENT
                else ->
                    TransactionSummary.Direction.RECEIVED
            }
        }
    }

    override val timeStamp: Long
        get() = ethTransaction.timeStamp

    override val totalCrypto: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.ETHER,
            when (direction) {
                TransactionSummary.Direction.RECEIVED -> ethTransaction.value
                else -> ethTransaction.value.plus(ethTransaction.gasUsed.multiply(ethTransaction.gasPrice))
            }
        )
    }

    override val description: String?
        get() = ethDataManager.getTransactionNotes(hash)

    override val fee: Observable<CryptoValue>
        get() = Observable.just(
            CryptoValue.fromMinor(
                CryptoCurrency.ETHER,
                ethTransaction.gasUsed.multiply(ethTransaction.gasPrice)
            )
        )

    override val hash: String
        get() = ethTransaction.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = mapOf(ethTransaction.from to CryptoValue.fromMinor(CryptoCurrency.ETHER, ethTransaction.value))

    override val outputsMap: Map<String, CryptoValue>
        get() = mapOf(ethTransaction.to to CryptoValue.fromMinor(CryptoCurrency.ETHER, ethTransaction.value))

    override val confirmations: Int
        get() {
            val blockNumber = ethTransaction.blockNumber ?: return 0
            val blockHash = ethTransaction.blockHash ?: return 0

            return if (blockNumber == 0L || blockHash == "0x") 0 else (blockHeight - blockNumber).toInt()
        }

    override fun updateDescription(description: String): Completable =
        ethDataManager.updateTransactionNotes(hash, description)
}
