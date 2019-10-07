package piuk.blockchain.android.ui.charts

import android.annotation.SuppressLint
import piuk.blockchain.android.ui.charts.models.ArbitraryPrecisionFiatValue
import piuk.blockchain.android.ui.charts.models.toStringWithSymbol
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.charts.models.ChartDatumDto
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import kotlin.properties.Delegates

class ChartsPresenter(
    private val chartsDataManager: ChartsDataManager,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val prefs: PersistentPrefs,
    private val currencyFormatManager: CurrencyFormatManager
) : BasePresenter<ChartsView>() {

    internal var selectedTimeSpan by Delegates.observable(TimeSpan.MONTH) { _, _, new ->
        updateChartsData(new)
    }

    override fun onViewReady() {
        selectedTimeSpan = TimeSpan.MONTH
    }

    @SuppressLint("CheckResult")
    private fun updateChartsData(timeSpan: TimeSpan) {
        compositeDisposable.clear()
        getCurrentPrice()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))

        when (timeSpan) {
            TimeSpan.ALL_TIME -> chartsDataManager.getAllTimePrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.YEAR -> chartsDataManager.getYearPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
            TimeSpan.DAY -> chartsDataManager.getDayPrice(
                view.cryptoCurrency,
                getFiatCurrency()
            )
        }.addToCompositeDisposable(this)
            .toList()
            .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
            .doOnSubscribe { view.updateSelectedCurrency(view.cryptoCurrency) }
            .doOnSuccess { view.updateChartState(getChartsData(it)) }
            .doOnError { view.updateChartState(ChartsState.Error) }
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    it.printStackTrace()
                }
            )
    }

    private fun getChartsData(list: List<ChartDatumDto>) =
        ChartsState.Data(list, getCurrencySymbol())

    private fun getCurrentPrice() {
        val fiatCurrency = getFiatCurrency()
        val price = exchangeRateFactory.getLastPrice(view.cryptoCurrency, fiatCurrency)
        view.updateCurrentPrice(
            ArbitraryPrecisionFiatValue.fromMajor(
                fiatCurrency,
                price.toBigDecimal()
            ).toStringWithSymbol()
        )
    }

    private fun getFiatCurrency() = prefs.selectedFiatCurrency

    private fun getCurrencySymbol() =
        currencyFormatManager.getFiatSymbol(getFiatCurrency(), view.locale)
}

sealed class ChartsState {

    data class Data(
        val data: List<ChartDatumDto>,
        val fiatSymbol: String
    ) : ChartsState()

    data class TimeSpanUpdated(val timeSpan: TimeSpan) : ChartsState()
    object Loading : ChartsState()
    object Error : ChartsState()
}