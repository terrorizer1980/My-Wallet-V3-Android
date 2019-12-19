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
            is SimpleBuyIntent.FetchBuyLimits -> interactor.fetchBuyLimits().subscribeBy(
                onSuccess = { process(it) },
                onError = { }
            )

            is SimpleBuyIntent.BuyLimits -> null
            is SimpleBuyIntent.PriceLoading -> null
            is SimpleBuyIntent.PriceUpdate -> null
            is SimpleBuyIntent.PriceError -> null
            is SimpleBuyIntent.EnteredAmount -> null
        }
}