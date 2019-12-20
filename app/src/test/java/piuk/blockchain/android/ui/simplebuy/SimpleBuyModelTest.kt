package piuk.blockchain.android.ui.simplebuy

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.simplebuy.ExchangePriceState
import piuk.blockchain.android.simplebuy.SimpleBuyIntent
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import java.math.BigDecimal

class SimpleBuyModelTest {

    private lateinit var model: SimpleBuyModel
    private val interactor: SimpleBuyInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = SimpleBuyModel(SimpleBuyState(), Schedulers.io(), interactor)
    }

    @Test
    fun `when currency changes, price state is updating normally`() {

        whenever(interactor.updateExchangePriceForCurrency(CryptoCurrency.BTC))
            .thenReturn(Single.just(SimpleBuyIntent.PriceUpdate(
                FiatValue.fromMajor("USD", BigDecimal.ZERO))))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(CryptoCurrency.BTC))

        testObserver.assertValueCount(4)
        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC))
        testObserver.assertValueAt(2,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(isLoading = true)))
        testObserver.assertValueAt(3,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(price = FiatValue.fromMajor("USD", BigDecimal.ZERO))))
    }

    @Test
    fun `when currency changes, price state has error when exchange rate fails`() {

        whenever(interactor.updateExchangePriceForCurrency(CryptoCurrency.BTC)).thenReturn(Single.error(Throwable("")))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(CryptoCurrency.BTC))

        testObserver.assertValueCount(4)
        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC))
        testObserver.assertValueAt(2,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(isLoading = true)))
        testObserver.assertValueAt(3,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(hasError = true)))
    }

    @Test
    fun `interactor fetched limits should be applied to state`() {
        whenever(interactor.fetchBuyLimits("USD"))
            .thenReturn(Single.just(SimpleBuyIntent.BuyLimits(FiatValue.zero("USD"),
                FiatValue.fromMinor("USD", 23400))))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchBuyLimits("USD"))

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(FiatValue.zero("USD"), FiatValue.fromMinor("USD", 23400)))
    }
}