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
        val list = activityDetailsComposite?.run {
            when (nonCustodialActivitySummaryItem?.direction) {
                TransactionSummary.Direction.TRANSFERRED -> TODO()
                TransactionSummary.Direction.RECEIVED -> TODO()
                TransactionSummary.Direction.SENT -> addSentItems(this)
                TransactionSummary.Direction.BUY -> TODO()
                TransactionSummary.Direction.SELL -> TODO()
                TransactionSummary.Direction.SWAP -> TODO()
                else -> TODO()
            }
        } ?: emptyList()
        return oldState.copy(
            nonCustodialActivitySummaryItem = activityDetailsComposite?.nonCustodialActivitySummaryItem,
            listOfItems = list
        )
    }

    private fun addSentItems(
        activityDetailsComposite: ActivityDetailsComposite
    ): List<ActivityDetailsListItem> {
        val itemList = mutableListOf<ActivityDetailsListItem>()
        activityDetailsComposite.nonCustodialActivitySummaryItem?.run {
            itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.CREATED,
                Date(timeStampMs).toFormattedDate()))
            if (isConfirmed) {
                itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.COMPLETED, "TODO"))
                itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.AMOUNT,
                    totalCrypto.toStringWithSymbol()))
                itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.FEE,
                    activityDetailsComposite.fee?.toStringWithSymbol() ?: ""))
                itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.VALUE,
                    activityDetailsComposite.fiatAtExecution?.toStringWithSymbol() ?: ""))
                itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.DESCRIPTION, ""))
                itemList.add(ActivityDetailsListItem(ActivityDetailsInfoType.ACTION, ""))
                itemList.forEach {
                    Timber.e("---- value: ${it.activityDetailsType} - ${it.itemValue}")
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
        return itemList
    }

    private fun addReceivedItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem,
        itemList: MutableList<Pair<String, String>>
    ) {
    }
}