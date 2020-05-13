package piuk.blockchain.androidcore.data.charts

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.rxjava.RxBus

class ChartsDataManagerTest : RxTest() {

    private lateinit var subject: ChartsDataManager
    private val historicPriceApi: PriceApi = mock()
    private val rxBus = RxBus()

    @Before
    fun setUp() {
        subject = ChartsDataManager(historicPriceApi, rxBus)
    }

    @Test
    fun `getAllTimePrice BTC`() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                btc.networkTicker,
                fiat,
                ChartsDataManager.FIRST_BTC_ENTRY_TIME,
                TimeInterval.FIVE_DAYS.intervalSeconds
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.ALL_TIME).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            btc.networkTicker,
            fiat,
            ChartsDataManager.FIRST_BTC_ENTRY_TIME,
            TimeInterval.FIVE_DAYS.intervalSeconds
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun `getAllTimePrice ETH`() {
        // Arrange
        val eth = CryptoCurrency.ETHER
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eth.networkTicker,
                fiat,
                ChartsDataManager.FIRST_ETH_ENTRY_TIME,
                TimeInterval.FIVE_DAYS.intervalSeconds
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(eth, fiat, TimeSpan.ALL_TIME).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eth.networkTicker,
            fiat,
            ChartsDataManager.FIRST_ETH_ENTRY_TIME,
            TimeInterval.FIVE_DAYS.intervalSeconds
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getYearPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.networkTicker),
                eq(fiat),
                any(),
                eq(TimeInterval.ONE_DAY.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.YEAR).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.networkTicker),
            eq(fiat),
            any(),
            eq(TimeInterval.ONE_DAY.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getMonthPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.networkTicker),
                eq(fiat),
                any(),
                eq(TimeInterval.TWO_HOURS.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.MONTH).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.networkTicker),
            eq(fiat),
            any(),
            eq(TimeInterval.TWO_HOURS.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getWeekPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.networkTicker),
                eq(fiat),
                any(),
                eq(TimeInterval.ONE_HOUR.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.WEEK).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.networkTicker),
            eq(fiat),
            any(),
            eq(TimeInterval.ONE_HOUR.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    fun getDayPrice() {
        // Arrange
        val btc = CryptoCurrency.BTC
        val fiat = "USD"
        whenever(
            historicPriceApi.getHistoricPriceSeries(
                eq(btc.networkTicker),
                eq(fiat),
                any(),
                eq(TimeInterval.FIFTEEN_MINUTES.intervalSeconds)
            )
        ).thenReturn(Single.just(listOf(PriceDatum())))

        // Act
        val testObserver = subject.getHistoricPriceSeries(btc, fiat, TimeSpan.DAY).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
            eq(btc.networkTicker),
            eq(fiat),
            any(),
            eq(TimeInterval.FIFTEEN_MINUTES.intervalSeconds)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }
}