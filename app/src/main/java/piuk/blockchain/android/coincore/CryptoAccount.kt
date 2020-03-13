package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single

interface CryptoAccount {
    val label: String

    // Not totally happy with this being nullable, but the top level group won't have an associated crypto, so?
    val cryptoCurrency: CryptoCurrency?

    val balance: Single<CryptoValue>

    val activity: Single<ActivitySummaryList>

    val actions: AvailableActions

    val hasTransactions: Single<Boolean>
    val isFunded: Single<Boolean>

    fun findActivityItem(txHash: String): Maybe<ActivitySummaryItem>
}

typealias CryptoAccountsList = List<CryptoAccount>

interface CryptoSingleAccount : CryptoAccount {
    val receiveAddress: Single<String>

//  Later, when we do send:
//    interface PendingTransaction {
//      fun computeFees(priority: FeePriority, pending: PendingTransaction): Single<PendingTransaction>
//      fun validate(pending: PendingTransaction): Boolean
//      fun execute(pending: PendingTransaction)
//    }
}

interface CryptoAccountGroup : CryptoAccount