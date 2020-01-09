package piuk.blockchain.android.simplebuy

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel

class SimpleBuyModel(state: SimpleBuyState, scheduler: Scheduler, private val interactor: SimpleBuyInteractor) :
    MviModel<SimpleBuyState, SimpleBuyIntent>(state, scheduler) {

    override fun performAction(intent: SimpleBuyIntent): Disposable? =
        when (intent) {
            is SimpleBuyIntent.NewCryptoCurrencySelected ->
                interactor.updateExchangePriceForCurrency(intent.currency).doOnSubscribe {
                    process(SimpleBuyIntent.PriceLoading)
                }.subscribeBy(
                    onSuccess = { process(it) },
                    onError = {
                        process(SimpleBuyIntent.PriceError)
                    }

                )
            is SimpleBuyIntent.FetchBuyLimits -> interactor.fetchBuyLimits(intent.currency).subscribeBy(
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
            is SimpleBuyIntent.UpdatedPredefinedAmounts -> null
            is SimpleBuyIntent.BuyLimits -> null
            is SimpleBuyIntent.PriceLoading -> null
            is SimpleBuyIntent.PriceUpdate -> null
            is SimpleBuyIntent.PriceError -> null
            is SimpleBuyIntent.EnteredAmount -> null
            is SimpleBuyIntent.OrderConfirmed -> null
            is SimpleBuyIntent.OrderCanceled -> null
        }
}