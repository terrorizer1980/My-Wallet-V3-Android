package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviIntent
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

class LoadCreationDateIntent(val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem) :
    ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState
    }
}

class ActivityDetailsLoadFailedIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            isError = true
        )
    }
}

class LoadHeaderDataIntent(
    private val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
) :
    ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            direction = nonCustodialActivitySummaryItem.direction,
            amount = nonCustodialActivitySummaryItem.totalCrypto,
            isPending = nonCustodialActivitySummaryItem.isPending,
            confirmations = nonCustodialActivitySummaryItem.confirmations,
            totalConfirmations = nonCustodialActivitySummaryItem.cryptoCurrency.requiredConfirmations
        )
    }
}

class ListItemsLoadedIntent(private val list: List<ActivityDetailsType>) :
    ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        val currentList = oldState.listOfItems.toMutableSet()
        currentList.addAll(list.toSet())
        return oldState.copy(
            listOfItems = currentList
        )
    }
}

object CreationDateLoadFailedIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState
    }
}

class CreationDateLoadedIntent(private val createdDate: Date) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        val list = oldState.listOfItems.toMutableSet()
        list.add(Created(createdDate))
        return oldState.copy(
            listOfItems = list
        )
    }
}
