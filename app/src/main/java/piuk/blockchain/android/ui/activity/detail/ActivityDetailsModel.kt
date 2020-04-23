package piuk.blockchain.android.ui.activity.detail

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState

data class ActivityDetailState(
    val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem? = null,
    val listOfItems: List<Pair<Int, String>> = emptyList()
) : MviState

class ActivityDetailsModel(
    initialState: ActivityDetailState,
    mainScheduler: Scheduler,
    private val interactor: ActivityDetailsInteractor
) : MviModel<ActivityDetailState, ActivityDetailsIntents>(initialState, mainScheduler) {

    override fun performAction(previousState: ActivityDetailState, intent: ActivityDetailsIntents): Disposable? {
        return when (intent) {
            is LoadActivityDetailsIntent ->
                interactor.loadActivityDetailsData(cryptoCurrency = intent.cryptoCurrency, txHash = intent.txHash)
                    .subscribe({
                        process(ShowActivityDetailsIntent(it))
                    }, {
                        // TODO error case loading from cache
                    })
            is ShowActivityDetailsIntent -> null
        }
    }
}