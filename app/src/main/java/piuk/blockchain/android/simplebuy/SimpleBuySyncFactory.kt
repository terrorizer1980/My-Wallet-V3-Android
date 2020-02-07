package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean

// Ensure that the local and remote SimpleBuy state is the same.
// Resolution stratagy is:
//  - check simple buy is enabled
//  - inflate the local state, if any
//  - fetch the remote state, if any
//      - if the remote state is the same as the local state, then do nothing
//      - if the remote state exists and the local state is in an earlier stage, use the remote state
//      - if the remote state and the local state refer to the same order (id) and the remote state
//        is completed/error/cancel, then wipe the local state
//

class SimpleBuySyncFactory(
    private val custodialWallet: CustodialWalletManager,
    private val prefs: SimpleBuyPrefs,
    private val gson: Gson,
    private val simpleBuyFlag: FeatureFlag
) {
    private var isEnabled = AtomicBoolean(false)

    fun performSync(): Completable =
        checkEnabled()
            .doOnSuccess { isEnabled.set(it) }
            .flatMapCompletable {
                if (!it) {
                    clearLocalState()
                    Completable.complete()
                } else {
                    syncStates()
                        .doOnSuccess { v ->
                            updateLocalState(v)
                        }
                        .doOnComplete {
                            clearLocalState()
                        }
                        .ignoreElement()
                }
            }
            .observeOn(Schedulers.computation())

    fun currentState(): SimpleBuyState? {
        return inflateLocalState()
    }

    fun isEnabled(): Boolean {
        return isEnabled.get()
    }

    private fun checkEnabled(): Single<Boolean> =
        Singles.zip(
            custodialWallet.isEligibleForSimpleBuy(),
            simpleBuyFlag.enabled
        ) { enabled, simpleBuyEnabled ->
            enabled && simpleBuyEnabled
        }

    private fun syncStates(): Maybe<SimpleBuyState> {
        return maybeInflateLocalState()
            .flatMap(
                /* onSuccessMapper = */ { Maybe.defer { updateWithRemote(it) } },
                /* onErrorMapper = */ { Maybe.error<SimpleBuyState>(it) },
                /* onCompleteSupplier = */ { Maybe.defer { getPendingBuy() } }
            )
    }

    private fun getPendingBuy(): Maybe<SimpleBuyState> {
        return custodialWallet.getOutstandingBuyOrders()
            .flatMapMaybe { list ->
                list.firstOrNull {
                    setOf(
                        OrderState.AWAITING_FUNDS,
                        OrderState.PENDING
                    ).contains(it.state)
                }?.let { Maybe.just(it.toSimpleBuyState()) } ?: Maybe.empty()
            }
    }

    private fun updateWithRemote(localState: SimpleBuyState): Maybe<SimpleBuyState> =
        getRemoteForLocal(localState.id)
            .defaultIfEmpty(localState)
            .map { remoteState ->
                if (localState.orderState < remoteState.orderState) {
                    remoteState
                } else {
                    localState
                }
            }
            .flatMap { state ->
                when (state.orderState) {
                    OrderState.UNINITIALISED,
                    OrderState.INITIALISED,
                    OrderState.AWAITING_FUNDS,
                    OrderState.PENDING -> Maybe.just(state)
                    OrderState.FINISHED,
                    OrderState.CANCELED,
                    OrderState.FAILED -> Maybe.empty()
                }
            }

    private fun getRemoteForLocal(id: String?): Maybe<SimpleBuyState> =
        id?.let {
            custodialWallet.getBuyOrder(it)
                .map { order -> order.toSimpleBuyState() }
        } ?: Maybe.empty()

    private fun maybeInflateLocalState(): Maybe<SimpleBuyState> =
        inflateLocalState()?.let { Maybe.just(it) } ?: Maybe.empty()

    private fun inflateLocalState(): SimpleBuyState? =
        prefs.simpleBuyState()?.let {
            gson.fromJson(it, SimpleBuyState::class.java)
        }

    private fun updateLocalState(newState: SimpleBuyState) =
        prefs.updateSimpleBuyState(gson.toJson(newState))

    private fun clearLocalState() {
        prefs.clearState()
    }
}

private fun BuyOrder.toSimpleBuyState(): SimpleBuyState =
    SimpleBuyState(
        id = id,
        enteredAmount = fiatInput.formatOrSymbolForZero().replace(",", ""),
        currency = fiatInput.currencyCode,
        selectedCryptoCurrency = outputCrypto.currency,
        orderState = state,
        expirationDate = expires,
        kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE, // This MUST be so if we have an order in process.
        currentScreen = FlowScreen.BANK_DETAILS
    )
