package piuk.blockchain.android.ui.swap.homebrew.exchange.history

import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface TradeHistoryView : View {

    val locale: Locale

    fun renderUi(uiState: ExchangeUiState)
}