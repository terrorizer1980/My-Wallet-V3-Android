package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

class AssetActivityRepo(
    private val coincore: Coincore,
    private val swapHistory: MorphTradeDataManager,
    private val exchangeRates: ExchangeRateDataManager
) {
    private val transactionCache = mutableMapOf<CryptoAccount, List<ActivitySummaryItem>>()

    fun fetch(account: CryptoAccount): Observable<ActivitySummaryList> {
        val emitter = ObservableOnSubscribe<ActivitySummaryList> { emitter ->
            if (transactionCache.isNotEmpty()) {
                when (account) {
                    is AllWalletsAccount -> {
                        Timber.e(
                            "----- returning from cache for all wallets :${transactionCache.size}")
                        emitter.onNext(transactionCache.values.flatten())
                    }
                    else -> {
                        Timber.e(
                            "----- returning from cache for account ${account.label} - ${transactionCache[account]}")
                        emitter.onNext((transactionCache[account]) as ActivitySummaryList)
                    }
                }
            } else {
                Timber.e("----- cache empty, requesting account for : ${account.label}")
            }

            if (account is AllWalletsAccount) {
                Timber.e("----- getting all accounts")

                account.allAccounts().doOnSuccess { accountList ->
                    Timber.e("---- do on success , mapping all accounts")
                    accountList.map { cryptoAccount ->
                        (cryptoAccount as CryptoAccountCompoundGroup).accounts.map { ca ->
                            ca.activity.subscribe({ activityList ->
                                Timber.e(
                                    "----- adding all accounts to cache: ${cryptoAccount.label} - list: ${activityList.size}")
                                transactionCache.clear()
                                transactionCache[cryptoAccount] = activityList
                                Timber.e("----- saved to cache  :${transactionCache.size}")
                                emitter.onNext(activityList)
                            }, {
                                Timber.e("----- error parsing activity for all accounts: ${it.message}")
                                emitter.onError(it)
                            })
                        }
                    }
                }.subscribe({
                    Timber.e("---- on success getting all accounts")

                }, {
                    Timber.e("----- error getting all accounts: ${it.message}")
                    emitter.onError(it)
                })
            } else {
                Singles.zip(
                    fetchSwapHistory(),
                    account.activity
                ) { swapList, accountList ->
                    //val swaps = swapList.filter { account.cryptoCurrencies.contains(it.cryptoCurrency) }
                    //val txSet = buildTxLookupSet(swaps as List<SwapActivitySummaryItem>)

                    //val sortedList = removeSendWhereSwapped(txSet, accountList).plus(swaps).sorted()
                    transactionCache[account] = emptyList()
                    transactionCache[account] = accountList

                    Timber.e("----- got details for account: ${account.label} - $accountList")
                    Timber.e("----- saved to cache ${transactionCache[account]}")

                    accountList
                }.subscribe({
                    Timber.e("----- returning after getting net data :${it.size}")
                    emitter.onNext(it as ActivitySummaryList)
                    emitter.onComplete()
                }, {
                    Timber.e("----- error with data")
                    emitter.onError(it)
                })
            }
        }
        return Observable.create(emitter)


        /* return Singles.zip(
             fetchSwapHistory(),
             account.activity
         ) { swapList, accountList ->
             val swaps = swapList.filter { account.cryptoCurrencies.contains(it.cryptoCurrency) }
             val txSet = buildTxLookupSet(swaps as List<SwapActivitySummaryItem>)

             val sortedList = removeSendWhereSwapped(txSet, accountList).plus(swaps).sorted()
             transactionCache[account]?.addAll(sortedList)
             sortedList
         }*/
    }

    fun getActivityForAccountWithCurrencyAndId(txId: String, account: CryptoAccount,
                                               cryptoCurrency: CryptoCurrency) =
        transactionCache[account]?.find { it.txId == txId && it.cryptoCurrency == cryptoCurrency }

    fun getActivityForAccount(cryptoAccount: CryptoAccount) =
        transactionCache[cryptoAccount]

    private fun buildTxLookupSet(swapList: List<SwapActivitySummaryItem>): Set<String> =
        swapList
            .map { it.targetAddress }
            .toSet()

    private fun removeSendWhereSwapped(swapTx: Set<String>, activityList: ActivitySummaryList) =
        activityList.filter { !it.isSwapTransaction(swapTx) }

    private fun fetchSwapHistory(): Single<ActivitySummaryList> {
        return swapHistory.getTrades()
            .flattenAsObservable { it }
            .map { it.map(exchangeRates) }
            .toList()
    }
}

private fun ActivitySummaryItem.isSwapTransaction(swapTx: Set<String>): Boolean {
//    val i = this as? NonCustodialActivitySummaryItem
//    if(i != null) {
//        if (i.direction == TransactionSummary.Direction.SENT) {
//            i.outputsMap.keys.forEach {
//                if (it in swapTx) {
//                    return true
//                }
//            }
//        }
//    }
    return false
}

private fun MorphTrade.map(exchangeRates: ExchangeRateDataManager): ActivitySummaryItem =
    SwapActivitySummaryItem(
        exchangeRates = exchangeRates,
        cryptoCurrency = quote.pair.from,
        txId = this.hashOut ?: "",
        timeStampMs = timestamp * 1000,
        cryptoValue = quote.depositAmount,
        targetValue = quote.withdrawalAmount,
        fee = quote.minerFee,
        transactionHash = hashOut ?: "",
        sourceAddress = withdrawlAddress,
        targetAddress = depositAddress
    )