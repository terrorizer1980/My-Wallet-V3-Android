package piuk.blockchain.android.ui.activity.detail

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.datamanagers.OrderState
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
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.util.Date

class ActivityDetailsModelTest {

    private lateinit var model: ActivityDetailsModel
    private var state = ActivityDetailState()
    private val scheduler = Schedulers.io()
    private val interactor: ActivityDetailsInteractor = mock()

    private data class NonCustodialTestClass(
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
    ) : NonCustodialActivitySummaryItem()

    private val custodialItem = CustodialActivitySummaryItem(
        exchangeRates = mock(),
        cryptoCurrency = mock(),
        txId = "123",
        timeStampMs = 1L,
        totalCrypto = mock(),
        fundedFiat = mock(),
        status = OrderState.FINISHED,
        fee = mock()
    )

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

    @Test
    fun initial_state_loads_non_custodial_details() {
        val item = NonCustodialTestClass()
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(item)

        model.process(LoadActivityDetailsIntent(crypto, txId, false))

        verify(interactor, times(1)).getNonCustodialActivityDetails(crypto, txId)
    }

    @Test
    fun initial_state_loads_custodial_details() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getCustodialActivityDetails(crypto, txId)).thenReturn(custodialItem)
        whenever(interactor.loadCustodialItems(custodialItem)).thenReturn(
            Single.just(listOf())
        )

        model.process(LoadActivityDetailsIntent(crypto, txId, true))

        verify(interactor, times(1)).getCustodialActivityDetails(crypto, txId)
    }

    @Test
    fun load_non_custodial_header_data_success() {
        val item = NonCustodialTestClass()

        val testObserver = model.state.test()
        model.process(LoadNonCustodialHeaderDataIntent(item))

        // need to wait for next intent to fire
        Thread.sleep(200)

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
    fun load_custodial_header_data_success() {
        val testObserver = model.state.test()
        model.process(LoadCustodialHeaderDataIntent(custodialItem))

        // need to wait for next intent to fire
        Thread.sleep(200)

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(
            direction = TransactionSummary.Direction.BUY,
            amount = custodialItem.totalCrypto,
            isPending = false,
            isFeeTransaction = false,
            confirmations = 0,
            totalConfirmations = 0
        ))
    }

    @Test
    fun load_creation_date_success() {
        val item = NonCustodialTestClass()
        val returnDate = Date()
        whenever(interactor.loadCreationDate(item)).thenReturn(returnDate)
        whenever(interactor.loadConfirmedSentItems(item)).thenReturn(Single.just(listOf()))

        val testObserver = model.state.test()
        model.process(LoadNonCustodialCreationDateIntent(item))

        verify(interactor, times(1)).loadCreationDate(item)

        // need to wait for next intent to fire
        Thread.sleep(200)

        val list = state.listOfItems.toMutableSet()
        list.add(Created(returnDate))
        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(listOfItems = list))
    }

    @Test
    fun non_custodial_activity_details_load_fail() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(null)

        val testObserver = model.state.test()
        model.process(LoadActivityDetailsIntent(crypto, txId, false))

        // need to wait for next intent to fire
        Thread.sleep(200)

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(isError = true))
    }

    @Test
    fun custodial_activity_details_load_fail() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getCustodialActivityDetails(crypto, txId)).thenReturn(null)

        val testObserver = model.state.test()
        model.process(LoadActivityDetailsIntent(crypto, txId, true))

        // need to wait for next intent to fire
        Thread.sleep(200)

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(isError = true))
    }

    @Test
    fun load_activity_items_success() {
        val list = listOf(Fee(mock()), Amount(mock()), To(""), From(""))

        val currentList = state.listOfItems.toMutableSet()
        currentList.addAll(list.toSet())

        val testObserver = model.state.test()
        model.process(ListItemsLoadedIntent(list))

        // need to wait for next intent to fire
        Thread.sleep(200)

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(
            listOfItems = currentList
        ))
    }
}