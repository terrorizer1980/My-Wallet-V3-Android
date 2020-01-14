package piuk.blockchain.android.ui.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.simplebuy.ExchangePriceState
import piuk.blockchain.android.simplebuy.KycState
import piuk.blockchain.android.simplebuy.OrderState
import piuk.blockchain.android.simplebuy.SimpleBuyIntent
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import java.math.BigDecimal

class SimpleBuyModelTest {

    private lateinit var model: SimpleBuyModel
    private val defaultState = SimpleBuyState()
    private val gson = Gson()
    private val interactor: SimpleBuyInteractor = mock()
    private val prefs: SimpleBuyPrefs = mock {
        on { simpleBuyState() } `it returns` gson.toJson(defaultState)
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model =
            SimpleBuyModel(
                prefs = prefs,
                initialState = defaultState,
                gson = gson,
                scheduler = Schedulers.io(),
                interactor = interactor
            )
    }

    @Test
    fun `when currency changes, price state is updating normally`() {

        whenever(interactor.updateExchangePriceForCurrency(CryptoCurrency.BTC))
            .thenReturn(Single.just(SimpleBuyIntent.PriceUpdate(
                FiatValue.fromMajor("USD", BigDecimal.ZERO))))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(CryptoCurrency.BTC))

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(0, SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC))
        testObserver.assertValueAt(1,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(isLoading = true)))
        testObserver.assertValueAt(2,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(price = FiatValue.fromMajor("USD", BigDecimal.ZERO))))
    }

    @Test
    fun `when currency changes, price state has error when exchange rate fails`() {

        whenever(interactor.updateExchangePriceForCurrency(CryptoCurrency.BTC)).thenReturn(Single.error(Throwable("")))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(CryptoCurrency.BTC))

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(0, SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC))
        testObserver.assertValueAt(1,
            SimpleBuyState(selectedCryptoCurrency = CryptoCurrency.BTC,
                exchangePriceState = ExchangePriceState(isLoading = true)))
        testObserver.assertValueAt(2,
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

    @Test
    fun `cancel order should make the order to cancel if interactor doesnt return an error`() {
        whenever(interactor.cancelOrder())
            .thenReturn(Single.just(SimpleBuyIntent.OrderCanceled))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.CancelOrder)

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(orderState = OrderState.CANCELLED))
    }

    @Test
    fun `confirm order should make the order to confirm if interactor doesnt return an error`() {
        whenever(interactor.confirmOrder())
            .thenReturn(Single.just(SimpleBuyIntent.OrderConfirmed))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.ConfirmOrder)

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(orderState = OrderState.CONFIRMED))
    }

    @Test
    fun `update kyc state shall make interactor poll for kyc state and update the state accordingly`() {
        whenever(interactor.pollForKycState())
            .thenReturn(Single.just(SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED)))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchKycState)

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(kycVerificationState = KycState.VERIFIED))
    }
}