package com.blockchain.morph.ui.homebrew.exchange.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.detail.HomebrewTradeDetailActivity
import com.blockchain.morph.ui.homebrew.exchange.history.adapter.TradeHistoryAdapter
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewNavHostActivity
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import com.blockchain.preferences.FiatCurrencyPreference
import kotlinx.android.synthetic.main.activity_homebrew_trade_history.*
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.util.Locale

class TradeHistoryActivity :
    BaseMvpActivity<TradeHistoryView, TradeHistoryPresenter>(),
    TradeHistoryView {

    override val locale: Locale = Locale.getDefault()
    private val fiat: FiatCurrencyPreference by inject()

    private val tradeHistoryAdapter = TradeHistoryAdapter(this::tradeClicked)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_history)
        get<Analytics>().logEvent(AnalyticsEvents.ExchangeHistory)

        button_new_exchange.setOnClickListener {
            HomebrewNavHostActivity.start(this, fiat.fiatCurrencyPreference)
        }

        setupToolbar(R.id.toolbar_constraint, R.string.swap)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TradeHistoryActivity)
            adapter = tradeHistoryAdapter
        }

        swipe_layout.setOnRefreshListener { onViewReady() }

        presenter.onViewReady()
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun renderUi(uiState: ExchangeUiState) {
        when (uiState) {
            is ExchangeUiState.Data -> renderData(uiState)
            ExchangeUiState.Error -> renderError()
            ExchangeUiState.Empty -> renderError()
            ExchangeUiState.Loading -> swipe_layout.isRefreshing = true
        }
    }

    private fun renderData(uiState: ExchangeUiState.Data) {
        tradeHistoryAdapter.items = uiState.trades
        recyclerView.visible()
        swipe_layout.isRefreshing = false
    }

    private fun renderError() {
        swipe_layout.isRefreshing = false
        emptyState.visible()
        recyclerView.gone()
    }

    private fun tradeClicked(trade: Trade) {
        Intent(this, HomebrewTradeDetailActivity::class.java).apply {
            putExtra("EXTRA_TRADE", trade)
        }.run { startActivity(this) }
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): TradeHistoryPresenter = get()

    override fun getView(): TradeHistoryView = this

    companion object {

        fun start(context: Context) {
            Intent(context, TradeHistoryActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}
