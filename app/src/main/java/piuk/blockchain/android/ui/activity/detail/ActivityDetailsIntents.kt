package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.util.extensions.toFormattedDate
import timber.log.Timber
import java.util.Date

sealed class ActivityDetailsIntents : MviIntent<ActivityDetailState>

class LoadActivityDetailsIntent(
    val cryptoCurrency: CryptoCurrency,
    val txHash: String
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState
    }
}

class ShowActivityDetailsIntent(
    private val activityDetailsComposite: ActivityDetailsComposite?
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        val list =
            oldState.listOfItems.toMutableList()
        activityDetailsComposite?.run {
            when (nonCustodialActivitySummaryItem?.direction) {
                TransactionSummary.Direction.TRANSFERRED -> TODO()
                TransactionSummary.Direction.RECEIVED -> TODO()
                TransactionSummary.Direction.SENT -> addSentItems(this, list)
                TransactionSummary.Direction.BUY -> TODO()
                TransactionSummary.Direction.SELL -> TODO()
                TransactionSummary.Direction.SWAP -> TODO()
            }
        }
        return oldState.copy(
            nonCustodialActivitySummaryItem = activityDetailsComposite?.nonCustodialActivitySummaryItem,
            listOfItems = list
        )
    }

    private fun addSentItems(
        activityDetailsComposite: ActivityDetailsComposite,
        itemList: MutableList<Pair<ActivityDetailsInfoType, String>>
    ) {
        itemList.clear()
        activityDetailsComposite.nonCustodialActivitySummaryItem?.run {
            itemList.add(Pair(ActivityDetailsInfoType.CREATED,
                Date(timeStampMs).toFormattedDate()))
            if (isConfirmed) {
                itemList.add(Pair(ActivityDetailsInfoType.COMPLETED, "TODO"))
                itemList.add(Pair(ActivityDetailsInfoType.AMOUNT,
                    totalCrypto.toStringWithSymbol()))
                itemList.add(Pair(ActivityDetailsInfoType.FEE,
                    activityDetailsComposite.fee?.toStringWithSymbol() ?: ""))
                itemList.add(Pair(ActivityDetailsInfoType.VALUE,
                    activityDetailsComposite.fiatAtExecution?.toStringWithSymbol() ?: ""))
                itemList.add(Pair(ActivityDetailsInfoType.DESCRIPTION, "Add a description"))
                itemList.add(Pair(ActivityDetailsInfoType.ACTION, ""))
                itemList.forEach {
                    Timber.e("---- value: ${it.first} - ${it.second}")
                }
            } else {
            }
            // created
            // if confirmed
            // completed date
            // amount
            // fee
            // value
            // from
            // to
            // else
            // from
            // to
            // fee
        }
    }

    private fun addReceivedItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem,
        itemList: MutableList<Pair<String, String>>
    ) {
    }
}