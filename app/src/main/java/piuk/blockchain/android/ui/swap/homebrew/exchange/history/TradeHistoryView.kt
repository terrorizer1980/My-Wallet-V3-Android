package piuk.blockchain.android.ui.swap.homebrew.exchange.history

import piuk.blockchain.androidcoreui.ui.base.View

interface TradeHistoryView : View {

    fun renderUi(uiState: ExchangeUiState)
}