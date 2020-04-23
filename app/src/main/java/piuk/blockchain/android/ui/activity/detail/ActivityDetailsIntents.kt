package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.toFiat
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
    private val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem?
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        val list =
            oldState.listOfItems.toMutableList()
        nonCustodialActivitySummaryItem?.run {
            when (direction) {
                TransactionSummary.Direction.TRANSFERRED -> TODO()
                TransactionSummary.Direction.RECEIVED -> TODO()
                TransactionSummary.Direction.SENT -> addSentItems(this, list)
                TransactionSummary.Direction.BUY -> TODO()
                TransactionSummary.Direction.SELL -> TODO()
                TransactionSummary.Direction.SWAP -> TODO()
            }
        }
        return oldState.copy(
            nonCustodialActivitySummaryItem = nonCustodialActivitySummaryItem
        )
    }

    private fun addSentItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem,
        itemList: MutableList<Pair<Int, String>>
    ) {
        itemList.clear()
        itemList.add(Pair(R.string.activity_details_created,
            nonCustodialActivitySummaryItem.timeStampMs.toString()))
        if (nonCustodialActivitySummaryItem.isConfirmed) {
            itemList.add(Pair(R.string.activity_details_completed,
                "TODO"))
            itemList.add(Pair(R.string.activity_details_amount,
                nonCustodialActivitySummaryItem.totalCrypto.toStringWithSymbol()))
            // fixme composite disposable here?
            nonCustodialActivitySummaryItem.fee.subscribe(
                {
                    itemList.add(Pair(R.string.activity_details_fee,
                        it.toStringWithSymbol()))
                },
                { Timber.e("Adding fee to details error") }
            )
            itemList.add(Pair(R.string.activity_details_amount,
                nonCustodialActivitySummaryItem.totalCrypto.toFiat(
                    FiatValue.fromMajor(nonCustodialActivitySummaryItem.totalCrypto.currencyCode,
                        nonCustodialActivitySummaryItem.totalCrypto.toBigDecimal(),
                        true))
                    .toString()))
            itemList.forEach {
                Timber.e("---- value: ${it.second}")
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

    private fun addReceivedItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem,
        itemList: MutableList<Pair<String, String>>
    ) {
    }
}