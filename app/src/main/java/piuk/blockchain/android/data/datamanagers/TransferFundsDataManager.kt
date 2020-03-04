package piuk.blockchain.android.data.datamanagers

import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue.Companion.bitcoinFromSatoshis
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.apache.commons.lang3.tuple.Triple
import org.bitcoinj.core.ECKey
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver
import java.math.BigInteger
import java.util.ArrayList

//public data class TransferableFundTransactionList(
//        val pendingTransactions: PendingTransaction
//        val totalToSend: Long,
//        val totalFee Long
//        )
class TransferFundsDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val coinSelectionRemoteConfig: CoinSelectionRemoteConfig
) {
    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of [PendingTransaction] objects with outputs set to an account defined by it's
     * index in the list of HD accounts.
     *
     * @param addressToReceiveIndex The index of the account to which you want to send the funds
     * @return Returns a Map which bundles together the List of [PendingTransaction] objects,
     * as well as a Pair which contains the total to send and the total fees, in that order.
     */
    fun getTransferableFundTransactionList(addressToReceiveIndex: Int): Observable<Triple<List<PendingTransaction>, Long, Long>> {
        return Observable.fromCallable {
            val suggestedFeePerKb =
                BigInteger.valueOf(dynamicFeeCache.btcFeeOptions!!.regularFee * 1000)
            val pendingTransactionList: MutableList<PendingTransaction> =
                ArrayList()
            val legacyAddresses =
                payloadDataManager.wallet!!.legacyAddressList
            var totalToSend = 0L
            var totalFee = 0L
            for (legacyAddress in legacyAddresses) {
                if (!legacyAddress.isWatchOnly
                    && payloadDataManager.getAddressBalance(legacyAddress.address)
                        .compareTo(BigInteger.ZERO) > 0
                ) {
                    val unspentOutputs =
                        sendDataManager.getUnspentBtcOutputs(legacyAddress.address)
                            .blockingFirst()
                    val newCoinSelectionEnabled =
                        coinSelectionRemoteConfig.enabled.toObservable()
                            .blockingFirst()
                    val sweepableCoins =
                        sendDataManager.getMaximumAvailable(
                            CryptoCurrency.BTC,
                            unspentOutputs,
                            suggestedFeePerKb,
                            newCoinSelectionEnabled
                        )
                    val sweepAmount = sweepableCoins.left
                    // Don't sweep if there are still unconfirmed funds in address
                    if (unspentOutputs.notice == null && sweepAmount.compareTo(Payment.DUST) > 0) {
                        val pendingSpend = PendingTransaction()
                        pendingSpend.unspentOutputBundle =
                            sendDataManager.getSpendableCoins(
                                unspentOutputs,
                                bitcoinFromSatoshis(sweepAmount),
                                suggestedFeePerKb,
                                newCoinSelectionEnabled
                            )
                        var label = legacyAddress.label
                        if (label == null) {
                            label = ""
                        }
                        pendingSpend.sendingObject = ItemAccount(
                            label,
                            null,
                            "",
                            legacyAddress,
                            legacyAddress.address
                        )
                        pendingSpend.bigIntFee =
                            pendingSpend.unspentOutputBundle!!.absoluteFee
                        pendingSpend.bigIntAmount = sweepAmount
                        pendingSpend.addressToReceiveIndex = addressToReceiveIndex
                        totalToSend += pendingSpend.bigIntAmount.longValue()
                        totalFee += pendingSpend.bigIntFee.longValue()
                        pendingTransactionList.add(pendingSpend)
                    }
                }
            }
            Triple.of<List<PendingTransaction>, Long, Long>(
                pendingTransactionList,
                totalToSend,
                totalFee
            )
        }
            .compose(
                RxUtil.applySchedulersToObservable()
            )
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of [PendingTransaction] objects with outputs set to the default HD account.
     *
     * @return Returns a Triple object which bundles together the List of [PendingTransaction]
     * objects, as well as the total to send and the total fees, in that order.
     */
    val transferableFundTransactionListForDefaultAccount: Observable<Triple<List<PendingTransaction>, Long, Long>>
        get() = getTransferableFundTransactionList(payloadDataManager.defaultAccountIndex)

    /**
     * Takes a list of [PendingTransaction] objects and transfers them all. Emits a String
     * which is the Tx hash for each successful payment, and calls onCompleted when all
     * PendingTransactions have been finished successfully.
     *
     * @param pendingTransactions A list of [PendingTransaction] objects
     * @param secondPassword      The double encryption password if necessary
     * @return An [<]
     */
    fun sendPayment(
        pendingTransactions: List<PendingTransaction>,
        secondPassword: String?
    ): Observable<String> {
        return getPaymentObservable(pendingTransactions, secondPassword)
            .compose(RxUtil.applySchedulersToObservable())
    }

    private fun getPaymentObservable(
        pendingTransactions: List<PendingTransaction>,
        secondPassword: String?
    ): Observable<String> {
        return Observable.create { subscriber: ObservableEmitter<String> ->
            for (i in pendingTransactions.indices) {
                val pendingTransaction = pendingTransactions[i]
                val legacyAddress =
                    pendingTransaction.sendingObject!!.accountObject as LegacyAddress?
                val changeAddress = legacyAddress!!.address
                val receivingAddress =
                    payloadDataManager.getNextReceiveAddress(pendingTransaction.addressToReceiveIndex)
                        .blockingFirst()
                val keys: MutableList<ECKey?> =
                    ArrayList()
                keys.add(payloadDataManager.getAddressECKey(legacyAddress, secondPassword))
                sendDataManager.submitBtcPayment(
                    pendingTransaction.unspentOutputBundle!!,
                    keys,
                    receivingAddress,
                    changeAddress,
                    pendingTransaction.bigIntFee,
                    pendingTransaction.bigIntAmount
                )
                    .blockingSubscribe(
                        { s: String ->
                            if (!subscriber.isDisposed) {
                                subscriber.onNext(s)
                            }
                            // Increment index on receive chain
                            val account =
                                payloadDataManager.wallet
                                    .getHdWallets()[0]
                                    .accounts[pendingTransaction.addressToReceiveIndex]
                            payloadDataManager.incrementReceiveAddress(account)
                            // Update Balances temporarily rather than wait for sync
                            val spentAmount: Long =
                                pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue()
                            payloadDataManager.subtractAmountFromAddressBalance(
                                legacyAddress.address,
                                spentAmount
                            )
                            if (i == pendingTransactions.size - 1) { // Sync once transactions are completed
                                payloadDataManager.syncPayloadWithServer()
                                    .subscribe(IgnorableDefaultObserver<Any>())
                                if (!subscriber.isDisposed) {
                                    subscriber.onComplete()
                                }
                            }
                        }
                    ) { throwable: Throwable? ->
                        if (!subscriber.isDisposed) {
                            subscriber.onError(Throwable(throwable))
                        }
                    }
            }
        }
    }
}