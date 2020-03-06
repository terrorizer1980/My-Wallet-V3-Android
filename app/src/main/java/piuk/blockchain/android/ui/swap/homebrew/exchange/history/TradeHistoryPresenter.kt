package piuk.blockchain.android.ui.swap.homebrew.exchange.history

import androidx.annotation.VisibleForTesting
import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.Trade
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.DateUtil
import timber.log.Timber

class TradeHistoryPresenter(
    private val dataManager: MorphTradeDataHistoryList,
    private val dateUtil: DateUtil
) : BasePresenter<TradeHistoryView>() {

    override fun onViewReady() { }

    override fun onViewResumed() {
        super.onViewResumed()
        getTradeHistory()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getTradeHistory() {
        compositeDisposable +=
            dataManager.getTrades()
                .subscribeOn(Schedulers.io())
                .flattenAsObservable { it }
                .map { it.map() }
                .toList()
                .toObservable()
                .map<ExchangeUiState> {
                    if (it.isNotEmpty()) {
                        ExchangeUiState.Data(it)
                    } else {
                        ExchangeUiState.Empty
                    }
                }
                .doOnError { Timber.e(it) }
                .onErrorReturn { ExchangeUiState.Error }
                .startWith(ExchangeUiState.Loading)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = { view.renderUi(it) })
    }

    private fun MorphTrade.map(): Trade = Trade(
        id = this.quote.orderId,
        state = this.status,
        currency = this.quote.pair.to.symbol,
        price = this.quote.fiatValue?.toStringWithSymbol(view.locale) ?: "",
        pair = this.quote.pair.pairCode,
        quantity = this.quote.withdrawalAmount.toStringWithSymbol(view.locale),
        createdAt = dateUtil.formatted(this.timestamp),
        depositQuantity = this.quote.depositAmount.toStringWithSymbol(view.locale),
        fee = this.quote.minerFee.toStringWithSymbol(view.locale)
    )
}

sealed class ExchangeUiState {
    data class Data(val trades: List<Trade>) : ExchangeUiState()
    object Error : ExchangeUiState()
    object Loading : ExchangeUiState()
    object Empty : ExchangeUiState()
}