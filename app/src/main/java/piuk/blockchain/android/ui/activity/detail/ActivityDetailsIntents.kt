package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviIntent
import timber.log.Timber

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
        itemList: MutableList<Pair<Int, String>>
    ) {
        itemList.clear()
        activityDetailsComposite.nonCustodialActivitySummaryItem?.run {
            itemList.add(Pair(R.string.activity_details_created,
                timeStampMs.toString()))
            if (isConfirmed) {
                itemList.add(Pair(R.string.activity_details_completed, "TODO"))
                itemList.add(Pair(R.string.activity_details_amount,
                    totalCrypto.toStringWithSymbol()))
                itemList.add(Pair(R.string.activity_details_fee, activityDetailsComposite.fee))
                itemList.add(Pair(R.string.activity_details_amount,
                    activityDetailsComposite.fiatAtExecution))
                itemList.forEach {
                    Timber.e("---- value: ${it.second}")
                }
                itemList.add(Pair(DESCRIPTION_ITEM, "Add a description"))
                itemList.add(Pair(ACTION_ITEM, ""))
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