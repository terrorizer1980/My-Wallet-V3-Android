package piuk.blockchain.android.ui.activity.detail

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.times
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.util.Date

class ActivityDetailsModelTests {

    private lateinit var model: ActivityDetailsModel
    private val state = ActivityDetailState()
    private val scheduler = Schedulers.io()
    private val interactor: ActivityDetailsInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        model = spy(ActivityDetailsModel(state, scheduler, interactor))
    }

    data class DummyTestClass(override val exchangeRates: ExchangeRateDataManager = mock(),
                              override val cryptoCurrency: CryptoCurrency = mock(),
                              override val txId: String = "123",
                              override val timeStampMs: Long = 1L,
                              override val totalCrypto: CryptoValue = mock(),
                              override val direction: TransactionSummary.Direction = TransactionSummary.Direction.SENT,
                              override val fee: Observable<CryptoValue> = mock(),
                              override val inputsMap: Map<String, CryptoValue> = mock(),
                              override val outputsMap: Map<String, CryptoValue> = mock(),
                              override val description: String? = "desc") :
        NonCustodialActivitySummaryItem()

    @Test
    fun initial_state_loads_details() {
        val item = DummyTestClass()
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getActivityDetails(crypto, txId)).thenReturn(Single.just(item))

        model.process(LoadActivityDetailsIntent(crypto, txId))

        verify(interactor, times(1)).getActivityDetails(crypto, txId)
    }

    @Test
    fun load_header_data_success() {
        val item = DummyTestClass()

        val testObserver = model.state.test()
        model.process(LoadHeaderDataIntent(item))

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

        val testObserver = model.state.test()
        model.process(LoadCreationDateIntent(item))

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
        whenever(interactor.getActivityDetails(crypto, txId)).thenReturn(Single.error(Exception()))

        val testObserver = model.state.test()
        model.process(LoadActivityDetailsIntent(crypto, txId))

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(isError = true))
    }
}