package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel

class SimpleBuyModel(
    private val prefs: SimpleBuyPrefs,
    initialState: SimpleBuyState,
    scheduler: Scheduler,
    private val gson: Gson,
    private val interactor: SimpleBuyInteractor
) : MviModel<SimpleBuyState, SimpleBuyIntent>(
    gson.fromJson(prefs.simpleBuyState(), SimpleBuyState::class.java) ?: initialState,
    scheduler) {

    override fun performAction(intent: SimpleBuyIntent): Disposable? =
        when (intent) {
            is SimpleBuyIntent.FetchBuyLimits -> interactor.fetchBuyLimitsAndSupportedCryptoCurrencies(intent.currency)
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { /*handle case when limits weren't received*/ }
                )
            is SimpleBuyIntent.FetchPredefinedAmounts -> interactor.fetchPredefinedAmounts(intent.currency).subscribeBy(
                onSuccess = { process(it) },
                onError = { /*do nothing, show no amounts*/ }
            )
            is SimpleBuyIntent.CancelOrder -> interactor.cancelOrder().subscribeBy(
                onSuccess = { process(it) },
                onError = { /*do nothing, show no amounts*/ }
            )
            is SimpleBuyIntent.ConfirmOrder -> interactor.confirmOrder().subscribeBy(
                onSuccess = { process(it) },
                onError = { /*do nothing, show no amounts*/ }
            )
            is SimpleBuyIntent.FetchBankAccount -> interactor.fetchBankAccount().subscribeBy(
                onSuccess = { process(it) },
                onError = { /*do nothing, show no amounts*/ }
            )
            is SimpleBuyIntent.FetchKycState -> interactor.pollForKycState().subscribeBy(
                onSuccess = { process(it) },
                onError = { /*never fails. will return SimpleBuyIntent.KycStateUpdated(KycState.FAILED)*/ }
            )
            is SimpleBuyIntent.KycStateUpdated -> null
            is SimpleBuyIntent.UpdatedPredefinedAmounts -> null
            is SimpleBuyIntent.BankAccountUpdated -> null
            is SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies -> null
            is SimpleBuyIntent.NewCryptoCurrencySelected -> null
            is SimpleBuyIntent.EnteredAmount -> null
            is SimpleBuyIntent.OrderConfirmed -> null
            is SimpleBuyIntent.OrderCanceled -> null
        }

    override fun onStateUpdate(s: SimpleBuyState) {
        prefs.updateSimpleBuyState(gson.toJson(s))
    }
}