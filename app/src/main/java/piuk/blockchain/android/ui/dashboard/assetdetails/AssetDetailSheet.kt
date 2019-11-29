package piuk.blockchain.android.ui.dashboard.assetdetails

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.AppCompatTextView
import android.view.View
import com.blockchain.balance.currencyName
import com.blockchain.balance.setCoinIcon
import com.blockchain.preferences.CurrencyPrefs
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_dashboared_asset_details.view.*
import kotlinx.android.synthetic.main.layout_buy_swap_tabs.view.*
import kotlinx.android.synthetic.main.layout_send_request_tabs.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.dashboard.setDeltaColour
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.setOnTabSelectedListener
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssetDetailSheet : SlidingModalBottomDialog() {

    val compositeDisposable = CompositeDisposable()

    private val currencyPrefs: CurrencyPrefs by inject()
    private val currencyFormatManager: CurrencyFormatManager by inject()
    private val assetDetailsViewModel: AssetDetailsViewModel by inject()
    private val locale: Locale by inject()

    interface Host {
        fun gotoSendFor(cryptoCurrency: CryptoCurrency)
        fun goToReceiveFor(cryptoCurrency: CryptoCurrency)
        fun onSheetClosed()
        fun goToBuy()
        fun gotoSwapWithCurrencies(fromCryptoCurrency: CryptoCurrency, toCryptoCurrency: CryptoCurrency)
    }

    private val host: Host by lazy {
        parentFragment as? Host ?: throw IllegalStateException("Host fragment is not a AssetDetailSheet.Host")
    }

    private val cryptoCurrency: CryptoCurrency by lazy {
        arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("No cryptoCurrency specified")
    }

    private val assetSelect: AssetTokenLookup by inject()
    private val token: AssetTokens by lazy {
        assetSelect[cryptoCurrency]
    }

    override val layoutResource: Int
        get() = R.layout.dialog_dashboared_asset_details

    private lateinit var dialogView: View

    override fun initControls(view: View) {
        with(view) {
            asset_name.text = getString(cryptoCurrency.currencyName())
            balance_for_asset.text = getString(R.string.dashboard_balance_for_asset, cryptoCurrency.symbol)
            asset_icon.setCoinIcon(cryptoCurrency)
            buy_swap_tabs.buy_button.text = getString(R.string.dashboard_buy_for_asset, cryptoCurrency.symbol)
            send_request_tabs.request_btn.setOnClickListener {
                host.goToReceiveFor(cryptoCurrency)
            }

            send_request_tabs.send_btn.setOnClickListener {
                host.gotoSendFor(cryptoCurrency)
            }

            buy_swap_tabs.swap_btn.setOnClickListener {
                host.gotoSwapWithCurrencies(fromCryptoCurrency = cryptoCurrency,
                    toCryptoCurrency = cryptoCurrency.defaultSwapTo)
            }

            buy_swap_tabs.buy_button.setOnClickListener {
                host.goToBuy()
            }

            configureChart(chart,
                currencyFormatManager.getFiatSymbol(currencyPrefs.selectedFiatCurrency),
                token.asset.getDecimalPlaces())

            configureTabs(view.chart_price_periods)

            assetDetailsViewModel.token.accept(token)

            compositeDisposable += assetDetailsViewModel.balance
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (cryptoBalance, fiatBalance) ->
                    asset_balance_crypto.text = cryptoBalance
                    asset_balance_fiat.text = fiatBalance
                }

            compositeDisposable += assetDetailsViewModel.exchangeRate
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy({
                }) {
                    current_price.text = it
                }

            compositeDisposable += assetDetailsViewModel.timeSpan.subscribeBy {
                configureUiForSelection(view, it)
            }

            compositeDisposable += assetDetailsViewModel.userCanBuy.doOnSubscribe {
                view.buy_swap_tabs.buy_button.gone()
                view.buy_swap_tabs.separator.gone()
            }.subscribeBy { canBuy ->
                view.buy_swap_tabs.buy_button.goneIf(!canBuy)
                view.buy_swap_tabs.separator.goneIf(!canBuy)
            }

            compositeDisposable += assetDetailsViewModel.chartLoading.observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { isLoading ->
                    if (isLoading) {
                        chartToLoadingState()
                    } else {
                        chartToDataState()
                    }
                }

            compositeDisposable += assetDetailsViewModel.historicPrices.observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { data ->
                    chart.apply {
                        updateChart(chart, data)
                    }
                    updatePriceChange(view.price_change, data)
                }

            dialogView = view
        }
    }

    private fun updateChart(chart: LineChart, data: List<PriceDatum>) {
        chart.apply {
            visible()
            clear()
            if (data.isEmpty()) {
                dialogView.price_change?.text = "--"
                return
            }
            val entries = data.map { Entry(it.timestamp.toFloat(), it.price.toFloat()) }
            this.data = LineData(LineDataSet(entries, null).apply {
                color = ContextCompat.getColor(context, getDataRepresentationColor(data))
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                setDrawCircles(false)
                isHighlightEnabled = true
                setDrawHighlightIndicators(false)
                marker = ValueMarker(
                    context,
                    R.layout.price_chart_marker,
                    currencyFormatManager.getFiatSymbol(currencyPrefs.selectedFiatCurrency),
                    cryptoCurrency.getDecimalPlaces()
                )
            })
            animateX(500)
        }
    }

    private fun chartToLoadingState() {
        dialogView.prices_loading?.visible()
        dialogView.chart?.invisible()
        dialogView.price_change?.apply {
            text = "--"
            setTextColor(resources.getColor(R.color.dashboard_chart_unknown))
        }
    }

    private fun chartToDataState() {
        dialogView.prices_loading?.gone()
        dialogView.chart?.visible()
    }

    private fun configureTabs(chartPricePeriods: TabLayout) {
        TimeSpan.values().forEachIndexed { index, timeSpan ->
            chartPricePeriods.getTabAt(index)?.text = timeSpan.tabName()
        }
        chartPricePeriods.setOnTabSelectedListener {
            assetDetailsViewModel.timeSpan.accept(TimeSpan.values()[it])
        }
    }

    private fun TimeSpan.tabName() =
        when (this) {
            TimeSpan.ALL_TIME -> "ALL"
            TimeSpan.YEAR -> "1Y"
            TimeSpan.MONTH -> "1M"
            TimeSpan.WEEK -> "1W"
            TimeSpan.DAY -> "1D"
        }

    private fun getDataRepresentationColor(data: PriceSeries): Int {
        val diff = data.last().price - data.first().price
        return if (diff < 0) R.color.dashboard_chart_negative else R.color.dashboard_chart_positive
    }

    @SuppressLint("SetTextI18n")
    private fun updatePriceChange(percentageView: AppCompatTextView, data: PriceSeries) {
        val firstPrice = data.first().price
        val lastPrice = data.last().price
        val difference = lastPrice - firstPrice

        val percentChange = (difference / firstPrice) * 100

        percentageView.text =
            FiatValue.fromMajor(currencyPrefs.selectedFiatCurrency,
                difference.toBigDecimal()).formatOrSymbolForZero() +
                    " (${String.format("%.1f",
                        percentChange)}%)"

        percentageView.setDeltaColour(
            delta = difference,
            negativeColor = R.color.dashboard_chart_negative,
            positiveColor = R.color.dashboard_chart_positive
        )
    }

    private fun configureChart(chart: LineChart, fiatSymbol: String, decimalPlaces: Int) {
        chart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)

            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return fiatSymbol + NumberFormat.getNumberInstance(Locale.getDefault())
                        .apply {
                            maximumFractionDigits = decimalPlaces
                            minimumFractionDigits = decimalPlaces
                            roundingMode = RoundingMode.HALF_UP
                        }.format(value)
                }
            }

            axisLeft.granularity = 0.005f
            axisLeft.isGranularityEnabled = true
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_grey_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_grey_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.isGranularityEnabled = true
            setExtraOffsets(8f, 0f, 0f, 10f)
            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_grey_medium))
            loadFont(
                context,
                CustomFont.MONTSERRAT_LIGHT
            ) {
                xAxis.typeface = it
                axisLeft.typeface = it
            }
        }
    }

    private fun configureUiForSelection(view: View, selection: TimeSpan) {
        val dateFormat = when (selection) {
            TimeSpan.ALL_TIME -> SimpleDateFormat("yyyy", locale)
            TimeSpan.YEAR -> SimpleDateFormat("MMM ''yy", locale)
            TimeSpan.MONTH, TimeSpan.WEEK -> SimpleDateFormat("dd. MMM", locale)
            TimeSpan.DAY -> SimpleDateFormat("H:00", locale)
        }

        val granularity = when (selection) {
            TimeSpan.ALL_TIME -> 60 * 60 * 24 * 365F
            TimeSpan.YEAR -> 60 * 60 * 24 * 30F
            TimeSpan.MONTH, TimeSpan.WEEK -> 60 * 60 * 24 * 2F
            TimeSpan.DAY -> 60 * 60 * 4F
        }

        view.chart.chart.xAxis.apply {
            this.granularity = granularity
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateFormat.format(Date(value.toLong() * 1000))
                }
            }
        }

        view.price_change_period.text = resources.getString(when (selection) {
            TimeSpan.YEAR -> R.string.dashboard_time_span_last_year
            TimeSpan.MONTH -> R.string.dashboard_time_span_last_month
            TimeSpan.WEEK -> R.string.dashboard_time_span_last_week
            TimeSpan.DAY -> R.string.dashboard_time_span_last_day
            TimeSpan.ALL_TIME -> R.string.dashboard_time_span_all_time
        })

        view.chart_price_periods.getTabAt(selection.ordinal)?.select()
    }

    private fun CryptoCurrency.getDecimalPlaces(): Int =
        when (this) {
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.BCH -> 2
            CryptoCurrency.XLM -> 4
            CryptoCurrency.PAX -> 2
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    override fun onSheetHidden() {
        host.onSheetClosed()
        super.onSheetHidden()
    }

    override fun onCancel(dialog: DialogInterface) {
        host.onSheetClosed()
        super.onCancel(dialog)
    }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto"

        fun newInstance(cryptoCurrency: CryptoCurrency): AssetDetailSheet {
            return AssetDetailSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                }
            }
        }
    }
}