package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState

enum class ActivityDetailsInfoType {
    CREATED,
    COMPLETED,
    AMOUNT,
    FEE,
    VALUE,
    DESCRIPTION,
    ACTION
}

data class ActivityDetailsListItem(val activityDetailsType: ActivityDetailsInfoType, val itemValue: String)

data class ActivityDetailState(
    val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem? = null,
    val listOfItems: List<ActivityDetailsListItem> = emptyList()
) : MviState

data class ActivityDetailsComposite(
    val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem? = null,
    val fee: CryptoValue? = null,
    val fiatAtExecution: FiatValue? = null
)

class ActivityDetailsModel(
    initialState: ActivityDetailState,
    mainScheduler: Scheduler,
    private val interactor: ActivityDetailsInteractor
) : MviModel<ActivityDetailState, ActivityDetailsIntents>(initialState, mainScheduler) {

    override fun performAction(
        previousState: ActivityDetailState,
        intent: ActivityDetailsIntents
    ): Disposable? {
        return when (intent) {
            is LoadActivityDetailsIntent ->
                interactor.getCompositeActivityDetails(cryptoCurrency = intent.cryptoCurrency,
                    txHash = intent.txHash)
                    .subscribe({
                        process(ShowActivityDetailsIntent(it))
                    },
                    {
                        // TODO error case loading from cache
                    })

            is ShowActivityDetailsIntent -> null
        }
    }
}