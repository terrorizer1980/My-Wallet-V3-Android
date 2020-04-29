package piuk.blockchain.android.ui.activity.detail

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.times
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.util.Date
import java.util.MissingResourceException

class ActivityDetailsModelTest {

    private lateinit var model: ActivityDetailsModel
    private var state = ActivityDetailState()
    private val scheduler = Schedulers.io()
    private val interactor: ActivityDetailsInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        model = ActivityDetailsModel(state, scheduler, interactor)
    }

    @After
    fun teardown() {
        state = ActivityDetailState()
    }

    data class DummyTestClass(
        override val exchangeRates: ExchangeRateDataManager = mock(),
        override val cryptoCurrency: CryptoCurrency = mock(),
        override val txId: String = "123",
        override val timeStampMs: Long = 1L,
        override val totalCrypto: CryptoValue = mock(),
        override val direction: TransactionSummary.Direction = TransactionSummary.Direction.SENT,
        override val fee: Observable<CryptoValue> = mock(),
        override val inputsMap: Map<String, CryptoValue> = mock(),
        override val outputsMap: Map<String, CryptoValue> = mock(),
        override val description: String? = "desc"
    ) :
        NonCustodialActivitySummaryItem()

    @Test
    fun initial_state_loads_non_custodial_details() {
        val item = DummyTestClass()
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(Single.just(item))

        model.process(LoadActivityDetailsIntent(crypto, txId, false))

        verify(interactor, times(1)).getNonCustodialActivityDetails(crypto, txId)
    }

    @Test
    fun initial_state_loads_custodial_details() {
        val item = DummyTestClass()
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(Single.just(item))

        model.process(LoadActivityDetailsIntent(crypto, txId, true))

        verify(interactor, times(1)).getCustodialActivityDetails(crypto, txId)
    }


    @Test
    fun load_header_data_success() {
        val item = DummyTestClass()

        val testObserver = model.state.test()
        model.process(LoadNonCustodialHeaderDataIntent(item))

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(
            direction = item.direction,
            amount = item.totalCrypto,
            isPending = item.isPending,
            isFeeTransaction = item.isFeeTransaction,
            confirmations = item.confirmations,
            totalConfirmations = item.cryptoCurrency.requiredConfirmations
        ))
    }

    @Test
    fun load_creation_date_success() {
        val item = DummyTestClass()
        val returnDate = Date()
        whenever(interactor.loadCreationDate(item)).thenReturn(Single.just(returnDate))
        whenever(interactor.loadConfirmedSentItems(item)).thenReturn(Single.just(listOf()))

        val testObserver = model.state.test()
        model.process(LoadNonCustodialCreationDateIntent(item))

        verify(interactor, times(1)).loadCreationDate(item)

        val list = state.listOfItems.toMutableSet()
        list.add(Created(returnDate))
        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(listOfItems = list))
    }

    @Test
    fun activity_details_load_fail() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        val issue = MissingResourceException("Could not find the activity item",
            NonCustodialActivitySummaryItem::class.simpleName, "")
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(Single.error(issue))

        val testObserver = model.state.test()
        model.process(LoadActivityDetailsIntent(crypto, txId, false))

        // need to wait for next intent to fire
        Thread.sleep(200)

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(isError = true))
    }
}