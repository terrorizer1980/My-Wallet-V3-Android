package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.android.coincore.old.ActivitySummaryList
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.R
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.Erc20Transfer
import piuk.blockchain.androidcore.data.erc20.FeedErc20Transfer
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.math.BigInteger

class PAXTokens(
    rxBus: RxBus,
    private val paxAccount: Erc20Account,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val custodialWalletManager: CustodialWalletManager
) : AssetTokensBase(rxBus) {

    override val asset = CryptoCurrency.PAX

    override fun defaultAccount(): Single<AccountReference> =
        Single.just(getDefaultPaxAccountRef())

    override fun receiveAddress(): Single<String> =
        Single.just(getDefaultPaxAccountRef().receiveAddress)

    private fun getDefaultPaxAccountRef(): AccountReference {
        val paxAddress = paxAccount.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ether wallet found")

        val label = stringUtils.getString(R.string.pax_default_account_label)

        return AccountReference.Pax(label, paxAddress, "")
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
                val feeObservable = ethDataManager.getTransaction(it.transactionHash)
                    .map { transaction -> transaction.gasUsed * transaction.gasPrice }
                FeedErc20Transfer(it, feeObservable)
            }

        return Observables.zip(
            feedTransactions,
            paxAccount.getAccountHash(),
            ethDataManager.getLatestBlockNumber()
        ).map { (transactions, accountHash, latestBlockNumber) ->
            transactions.map { transaction ->
                Erc20ActivitySummaryItem(
                    transaction,
                    accountHash,
                    latestBlockNumber.number
                )
            }
        }
    }
}

class Erc20ActivitySummaryItem(
    private val feedTransfer: FeedErc20Transfer,
    private val accountHash: String,
    private val lastBlockNumber: BigInteger
) : ActivitySummaryItem() {

    override val cryptoCurrency = CryptoCurrency.PAX

    private val transfer: Erc20Transfer
        get() = feedTransfer.transfer

    override val direction: TransactionSummary.Direction
        get() = when {
            transfer.isToAccount(accountHash)
                && transfer.isFromAccount(accountHash) -> TransactionSummary.Direction.TRANSFERRED
            transfer.isFromAccount(accountHash) -> TransactionSummary.Direction.SENT
            else -> TransactionSummary.Direction.RECEIVED
        }

    override val timeStamp: Long
        get() = transfer.timestamp

    override val total: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.BTC, transfer.value)
    }

    override val fee: Observable<BigInteger>
        get() = feedTransfer.feeObservable

    override val hash: String
        get() = transfer.transactionHash

    override val inputsMap: Map<String, BigInteger>
        get() = mapOf(transfer.from to transfer.value)

    override val outputsMap: Map<String, BigInteger>
        get() = mapOf(transfer.to to transfer.value)

    override val confirmations: Int
        get() = (lastBlockNumber - transfer.blockNumber).toInt()
}
