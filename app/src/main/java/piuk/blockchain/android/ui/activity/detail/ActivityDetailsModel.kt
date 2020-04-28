package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import java.util.Date

sealed class ActivityDetailsType
data class Created(val date: Date) : ActivityDetailsType()
data class Amount(val cryptoValue: CryptoValue) : ActivityDetailsType()
data class Fee(val feeValue: CryptoValue) : ActivityDetailsType()
data class Value(val fiatAtExecution: FiatValue) : ActivityDetailsType()
data class From(val fromAddress: String) : ActivityDetailsType()
data class To(val toAddress: String) : ActivityDetailsType()
data class Description(val description: String = "") : ActivityDetailsType()
data class Action(val action: String = "") : ActivityDetailsType()

data class ActivityDetailState(
    val direction: TransactionSummary.Direction? = null,
    val amount: CryptoValue? = null,
    val isPending: Boolean = false,
    val isFeeTransaction: Boolean = false,
    val confirmations: Int = 0,
    val totalConfirmations: Int = 0,
    val listOfItems: Set<ActivityDetailsType> = emptySet(),
    val isError: Boolean = false
) : MviState

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
                interactor.getActivityDetails(cryptoCurrency = intent.cryptoCurrency,
                    txHash = intent.txHash).subscribeBy(
                    onSuccess = {
                        process(LoadCreationDateIntent(it))
                        process(LoadHeaderDataIntent(it))
                    },
                    onError = { process(ActivityDetailsLoadFailedIntent()) }
                )
            is LoadCreationDateIntent ->
                interactor.loadCreationDate(intent.nonCustodialActivitySummaryItem).subscribeBy(
                    onSuccess = {
                        process(CreationDateLoadedIntent(it))
                        when (intent.nonCustodialActivitySummaryItem.direction) {
                            TransactionSummary.Direction.TRANSFERRED -> TODO()
                            TransactionSummary.Direction.RECEIVED -> TODO()
                            TransactionSummary.Direction.SENT -> {
                                if (intent.nonCustodialActivitySummaryItem.isConfirmed) {
                                    interactor.loadConfirmedSentItems(
                                        intent.nonCustodialActivitySummaryItem
                                    ).subscribeBy(
                                        onSuccess = { activityItemsList ->
                                            process(ListItemsLoadedIntent(activityItemsList))
                                        },
                                        onError = {
                                            process(ListItemsFailedToLoadIntent)
                                        }
                                    )
                                } else {
                                    interactor.loadUnconfirmedSentItems(
                                        intent.nonCustodialActivitySummaryItem
                                    ).subscribeBy(onSuccess = { activityItemsList ->
                                        process(ListItemsLoadedIntent(activityItemsList))
                                    },
                                    onError = {
                                        process(ListItemsFailedToLoadIntent)
                                    })
                                }
                            }
                            TransactionSummary.Direction.BUY -> TODO()
                            TransactionSummary.Direction.SELL -> TODO()
                            TransactionSummary.Direction.SWAP -> TODO()
                        }
                    },
                    onError = {
                        process(CreationDateLoadFailedIntent)
                    })
            is ListItemsFailedToLoadIntent,
            is ListItemsLoadedIntent,
            is CreationDateLoadedIntent,
            is CreationDateLoadFailedIntent,
            is ActivityDetailsLoadFailedIntent,
            is LoadHeaderDataIntent -> null
        }
    }
}