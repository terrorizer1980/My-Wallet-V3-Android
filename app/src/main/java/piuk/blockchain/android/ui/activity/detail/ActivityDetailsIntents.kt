package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class ActivityDetailsIntents: MviIntent<ActivityDetailState>

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
        return oldState.copy(
            nonCustodialActivitySummaryItem = nonCustodialActivitySummaryItem
        )
    }
}