package piuk.blockchain.android.coincore.pax

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
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
import io.reactivex.rxkotlin.Observables
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.impl.mapList
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.Erc20Transfer
import piuk.blockchain.androidcore.data.erc20.FeedErc20Transfer
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigInteger

internal class PaxTokens(
    private val paxAccount: Erc20Account,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val custodialWalletManager: CustodialWalletManager,
    private val labels: DefaultLabels,
    rxBus: RxBus
) : AssetTokensBase(rxBus) {

    override val asset = CryptoCurrency.PAX

    override fun init(): Completable =
        Completable.complete()
            .andThen(Completable.defer { loadAccounts() })
            .andThen(Completable.defer { initActivities() })
            .doOnComplete { Timber.d("Coincore: Init PAX Complete") }
            .doOnError { Timber.d("Coincore: Init PAX Failed") }

    private fun loadAccounts(): Completable =
        Completable.complete()

    private fun initActivities(): Completable =
        Completable.complete()

    override fun defaultAccountRef(): Single<AccountReference> =
        Single.just(getDefaultPaxAccountRef())

    override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.just(getNonCustodialPaxAccount())

    override fun accounts(filter: AssetFilter): Single<CryptoAccountGroup> {
        TODO("not implemented")
    }

    override fun receiveAddress(): Single<String> =
        Single.just(getDefaultPaxAccountRef().receiveAddress)

    private fun getDefaultPaxAccountRef(): AccountReference {
        val paxAddress = paxAccount.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ether wallet found")

        val label = stringUtils.getString(R.string.pax_default_account_label_1)

        return AccountReference.Pax(label, paxAddress, "")
    }

    private fun getNonCustodialPaxAccount(): CryptoSingleAccount {
        val paxAddress = paxAccount.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ether wallet found")

        val label = stringUtils.getString(R.string.pax_default_account_label_1)

        return PaxCryptoAccount(label, paxAddress)
    }

    override fun noncustodialBalance(): Single<CryptoValue> =
        paxAccount.getBalance()
            .map { CryptoValue.usdPaxFromMinor(it) }

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.PAX)

    override fun balance(account: AccountReference): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.PAX, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.PAX,
            currencyPrefs.selectedFiatCurrency,
            epochWhen)

    // PAX does not support historic prices, so return an empty list
    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        getPaxTransactions()
            .singleOrError()

    private fun getPaxTransactions(): Observable<ActivitySummaryList> {
        val ethDataManager = paxAccount.ethDataManager

        val feedTransactions =
            paxAccount.getTransactions().mapList {
                val feeObservable = ethDataManager
                    .getTransaction(it.transactionHash)
                    .map { transaction ->
                        transaction.gasUsed * transaction.gasPrice
                    }
                FeedErc20Transfer(it, feeObservable)
            }

        return Observables.zip(
            feedTransactions,
            paxAccount.getAccountHash(),
            ethDataManager.getLatestBlockNumber()
        ).map { (transactions, accountHash, latestBlockNumber) ->
            transactions.map { transaction ->
                PaxActivitySummaryItem(
                    transaction,
                    accountHash,
                    ethDataManager,
                    latestBlockNumber.number,
                    exchangeRates
                )
            }
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class PaxActivitySummaryItem(
    private val feedTransfer: FeedErc20Transfer,
    private val accountHash: String,
    private val ethDataManager: EthDataManager,
    lastBlockNumber: BigInteger,
    exchangeRates: ExchangeRateDataManager
) : ActivitySummaryItem(exchangeRates) {

    override val cryptoCurrency = CryptoCurrency.PAX

    private val transfer: Erc20Transfer = feedTransfer.transfer

    override val direction: TransactionSummary.Direction by unsafeLazy {
        when {
            transfer.isToAccount(accountHash)
                && transfer.isFromAccount(accountHash) -> TransactionSummary.Direction.TRANSFERRED
            transfer.isFromAccount(accountHash) -> TransactionSummary.Direction.SENT
            else -> TransactionSummary.Direction.RECEIVED
        }
    }

    override val timeStamp: Long = transfer.timestamp

    override val totalCrypto: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.PAX, transfer.value)
    }

    override val description: String?
        get() = ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes[hash]

    override val fee: Observable<CryptoValue>
        get() = feedTransfer.feeObservable
            .map { CryptoValue.etherFromWei(it) }

    override val hash: String = transfer.transactionHash

    override val inputsMap: Map<String, CryptoValue> =
        mapOf(transfer.from to CryptoValue.fromMinor(CryptoCurrency.PAX, transfer.value))

    override val outputsMap: Map<String, CryptoValue> =
        mapOf(transfer.to to CryptoValue.fromMinor(CryptoCurrency.PAX, transfer.value))

    override val confirmations: Int = (lastBlockNumber - transfer.blockNumber).toInt()

    override fun updateDescription(description: String): Completable =
        ethDataManager.updateErc20TransactionNotes(hash, description)
}
