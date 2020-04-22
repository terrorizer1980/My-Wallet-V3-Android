package piuk.blockchain.android.ui.activity

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class ActivitiesIntent : MviIntent<ActivitiesState>

class AccountSelectedIntent(
    val account: CryptoAccount
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        val activitiesList = if (oldState.account == account) {
                oldState.activityList // Is a refresh, keep the list
            } else {
                emptyList()
            }
            return oldState.copy(
                account = account,
                isLoading = true,
                activityList = activitiesList
            )
    }
}

object SelectDefaultAccountIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            account = null,
            isLoading = true,
            activityList = emptyList()
        )
    }
}

class ActivityListUpdatedIntent(
    val activityList: ActivitySummaryList
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
            isError = activityList.isEmpty(),
            isLoading = false,
            activityList = activityList
        )
    }
}

class ActivityListUpdatedErrorIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(
                isLoading = false,
                activityList = emptyList(),
                isError = true
        )
    }
}

object ShowAccountSelectionIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState.copy(bottomSheet = ActivitiesSheet.ACCOUNT_SELECTOR)
    }
}

class ShowActivityDetailsIntent(
    cryptoCurrency: CryptoCurrency,
    txHash: String
) : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState {
        return oldState
    }
}

object ClearBottomSheetIntent : ActivitiesIntent() {
    override fun reduce(oldState: ActivitiesState): ActivitiesState =
        oldState.copy(bottomSheet = null)
}
