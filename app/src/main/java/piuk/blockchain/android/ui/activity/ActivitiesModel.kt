package piuk.blockchain.android.ui.activity

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber

enum class ActivitiesSheet {
    ACCOUNT_SELECTOR
}

data class ActivitiesState(
    val account: CryptoAccount? = null, // CoinCore.getMasterAccountGroup(),
    val activityList: ActivitySummaryList = emptyList(),
    val isLoading: Boolean = false,
    val bottomSheet: ActivitiesSheet? = null
) : MviState

class ActivitiesModel(
    initialState: ActivitiesState,
    mainScheduler: Scheduler,
    private val interactor: ActivitiesInteractor
) : MviModel<ActivitiesState, ActivitiesIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(previousState: ActivitiesState, intent: ActivitiesIntent): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is AccountSelectedIntent ->
                interactor.getActivityForAccount(intent.account)
                    .subscribeBy(
                        onSuccess = { process(ActivityListUpdatedIntent(it)) },
                        onError = { }
                    )
            is SelectDefaultAccountIntent ->
                interactor.getDefaultAccount()
                    .subscribeBy(
                        onSuccess = { process(AccountSelectedIntent(it)) },
                        onError = { }
                    )
            is ShowActivityDetailsIntent,
            is ClearBottomSheetIntent,
            is ActivityListUpdatedIntent,
            is ShowAccountSelectionIntent -> null
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.e("***> Scan loop failed: $t")
    }
}
