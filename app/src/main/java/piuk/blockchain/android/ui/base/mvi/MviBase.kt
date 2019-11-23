package piuk.blockchain.android.ui.base.mvi

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber

interface MviState

interface MviIntent<S : MviState> {
    fun reduce(oldState: S): S
}

abstract class MviModel<S : MviState, I : MviIntent<S>>(
    initialState: S,
    observeScheduler: Scheduler
) {

    private val _state: BehaviorRelay<S> = BehaviorRelay.createDefault(initialState)
    val state: Observable<S> = _state.observeOn(observeScheduler)

    private val compositeDisposable = CompositeDisposable()
    private val intents = ReplaySubject.create<I>()

    init {
        compositeDisposable +=
            intents.distinctUntilChanged()
                .observeOn(Schedulers.computation())
                .scan(initialState) { previousState, intent ->
                    Timber.d("***> Model: ProcessIntent: ${intent.javaClass.simpleName}")

                    performAction(intent)?.let { compositeDisposable += it }
                    intent.reduce(previousState)
                }.subscribeBy(
                    onNext = { newState ->
                        _state.accept(newState)
                    },
                    onError = { Timber.e("***> Scan loop failed: $it") }
                )
    }

    fun process(intent: I) = intents.onNext(intent)
    fun destroy() = compositeDisposable.clear()

    protected abstract fun performAction(intent: I): Disposable?
}
