package piuk.blockchain.android.ui.dashboard.assetdetails.charts

import com.blockchain.testutils.rxInit
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsViewModel
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.charts.TimeSpan
import java.util.Locale

class AssetDetailsViewModelTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    private val buyDataManager: BuyDataManager = mock()
    private val locale = Locale.US
    private lateinit var viewModel: AssetDetailsViewModel

    @Before
    fun setUp() {
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        viewModel = AssetDetailsViewModel(buyDataManager, locale)
    }

    @Test
    fun `buyDataManager canBuy value should be propagated`() {
        val testObserver = viewModel.userCanBuy.test()
        testObserver.assertValue(true).assertValueCount(1).assertComplete()
    }

    @Test
    fun `when buydatamanager canBuy returns an error then canBy should emit true`() {
        whenever(buyDataManager.canBuy).thenReturn(Observable.error(Throwable()))
        viewModel = AssetDetailsViewModel(buyDataManager, locale)

        val testObserver = viewModel.userCanBuy.test()
        testObserver.assertValue(true).assertValueCount(1).assertComplete()
    }

    @Test
    fun `cryptoBalance and fiatBalance is returned the right values`() {
        val token: AssetTokens = mock()

        whenever(token.exchangeRate()).thenReturn(Single.just(FiatValue.fromMinor("USD", 5647899)))
        whenever(token.totalBalance()).thenReturn(Single.just(CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())))
        viewModel.token.accept(token)

        val testObserver = viewModel.balance.test()
        testObserver.assertValue("0.00548621 BTC" to "$309.85").assertValueCount(1)
    }

    @Test
    fun `cryptoBalance and fiatBalance are never returned if exchange rated fails`() {
        val token: AssetTokens = mock()

        whenever(token.exchangeRate()).thenReturn(Single.error(Throwable()))
        whenever(token.totalBalance()).thenReturn(Single.just(CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())))
        viewModel.token.accept(token)

        val testObserver = viewModel.balance.test()
        testObserver.assertNoValues()
    }

    @Test
    fun `cryptoBalance and fiatBalance are never returned if totalBalance fails`() {
        val token: AssetTokens = mock()

        whenever(token.exchangeRate()).thenReturn(Single.just(FiatValue.fromMinor("USD", 5647899)))
        whenever(token.totalBalance()).thenReturn(Single.error(Throwable()))
        viewModel.token.accept(token)

        val testObserver = viewModel.balance.test()
        testObserver.assertNoValues()
    }

    @Test
    fun `exchange rate is the right one`() {
        val token: AssetTokens = mock()

        whenever(token.exchangeRate()).thenReturn(Single.just(FiatValue.fromMinor("USD", 5647899)))
        viewModel.token.accept(token)

        val testObserver = viewModel.exchangeRate.test()
        testObserver.assertValue("$56,478.99").assertValueCount(1)
    }

    @Test
    fun `historic prices are returned properly`() {
        val token: AssetTokens = mock()

        whenever(token.historicRateSeries(TimeSpan.MONTH, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.just(listOf(
                PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
            )))

        viewModel.token.accept(token)

        val loadingTestObserver = viewModel.chartLoading.test()
        val testObserver = viewModel.historicPrices.test()

        testObserver.assertValue(listOf(
            PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
        )).assertValueCount(1)

        loadingTestObserver.assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false)
            .assertValueCount(3)
    }

    @Test
    fun `when historic prices api returns error, empty list should be returned`() {
        val token: AssetTokens = mock()
        whenever(token.historicRateSeries(TimeSpan.MONTH, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.error(Throwable()))

        viewModel.token.accept(token)

        val loadingTestObserver = viewModel.chartLoading.test()
        val testObserver = viewModel.historicPrices.test()

        testObserver.assertValue(emptyList()).assertValueCount(1)

        loadingTestObserver.assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false)
            .assertValueCount(3)
    }
}