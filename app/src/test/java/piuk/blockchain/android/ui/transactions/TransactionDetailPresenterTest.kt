package piuk.blockchain.android.ui.transactions

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.TestActivitySummaryItem
import piuk.blockchain.android.ui.transactions.mapping.TransactionDetailModel
import piuk.blockchain.android.ui.transactions.mapping.TransactionInOutDetails
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.android.ui.transactions.mapping.TransactionInOutMapper
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.util.Locale

class TransactionDetailPresenterTest {

    private val assetSelect: Coincore = mock()
    private val assetTokens: AssetTokensBase = mock()
    private val inputOutMapper: TransactionInOutMapper = mock()
    private val prefsUtil: PersistentPrefs = mock()
    private val stringUtils: StringUtils = mock()
    private val view: TransactionDetailView = mock()
    private val exchangeRates: ExchangeRateDataManager = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)

    private lateinit var subject: TransactionDetailPresenter

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        whenever(prefsUtil.selectedFiatCurrency).thenReturn("USD")
        Locale.setDefault(Locale("EN", "US"))

        whenever(assetSelect[any()]).thenReturn(assetTokens)
        whenever(assetTokens.findCachedActivityItem(INVALID_TX_HASH)).thenReturn(null)

        subject = TransactionDetailPresenter(
            assetLookup = assetSelect,
            inputOutputMapper = inputOutMapper,
            prefs = prefsUtil,
            stringUtils = stringUtils,
            exchangeRateDataManager = exchangeRates
        )

        setupStringUtils()

        subject.initView(view)
        subject.onViewReady()
    }

    private fun setupStringUtils() {
        whenever(stringUtils.getString(R.string.transaction_detail_pending))
            .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
        whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred))
            .thenReturn("Value when moved: ")
        whenever(stringUtils.getString(R.string.transaction_detail_confirmed))
            .thenReturn("Confirmed")
        whenever(stringUtils.getString(R.string.pax_default_account_label_1))
            .thenReturn("My Usd pax Wallet")
        whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
            .thenReturn("Value when sent: ")
    }

    @Test
    fun onViewReadyIntentHashNotFound() {
        //  Arrange

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, INVALID_TX_HASH)

        //  Assert
        verify(view).pageFinish()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `simple activity item updates ui correctly`() {
        //  Arrange
        val item = TestActivitySummaryItem(
            exchangeRates = exchangeRates,
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.TRANSFERRED,
            timeStamp = 0,
            totalCrypto = CryptoValue.bitcoinFromMajor(10),
            fee = Observable.just(CryptoValue.bitcoinFromSatoshis(1L)),
            hash = VALID_TX_HASH,
            inputsMap = mapOf("addr1" to CryptoValue.bitcoinFromSatoshis(1000L)),
            outputsMap = mapOf("addr2" to CryptoValue.bitcoinFromSatoshis(2000L)),
            description = null
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(
                TransactionInOutDetails(
                    inputs = listOf(TransactionDetailModel(address = "address_1")),
                    outputs = listOf(TransactionDetailModel(address = "address_2"))
                )
            )
        )

        whenever(exchangeRates.getHistoricPrice(any<CryptoValue>(), any(), any()))
            .thenReturn(Single.just(FiatValue.fromMajor("USD", 10.toBigDecimal())))

        whenever(exchangeRates.getLastPrice(any(), any()))
            .thenReturn(10.0)

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        //  Assert
        verify(view).setStatus(
            CryptoCurrency.BTC,
            "Pending (0/3 Confirmations)",
            VALID_TX_HASH
        )
        verify(view).setTransactionType(TransactionSummary.Direction.TRANSFERRED, false)
        verify(view).updateFeeFieldVisibility(any())
        verify(view).setTransactionColour(R.color.product_grey_transferred_50)
        verify(view).setTransactionValue(any())
        verify(view).setDescription(null)
        verify(view).setDate(any())
        verify(view).setFee("0.00000001 BTC")
        verify(view).setToAddresses(any())
        verify(view).setFromAddress(any())
        verify(view).setTransactionValueFiat(any())
        verify(view).setIsDoubleSpend(any())
        verify(view).onDataLoaded()

        verifyNoMoreInteractions(view)
    }

    @Test
    fun setTransactionStatusNoConfirmations() {
        // Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.ETHER,
            direction = TransactionSummary.Direction.SENT,
            confirmations = 0,
            hash = VALID_TX_HASH
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        // Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        // Assert
        verify(view).setStatus(CryptoCurrency.ETHER, "Pending (0/12 Confirmations)", VALID_TX_HASH)
    }

    @Test
    fun setTransactionStatusConfirmed() {
        // Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.SENT,
            confirmations = 3,
            hash = VALID_TX_HASH
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        // Act
        subject.showDetailsForTransaction(CryptoCurrency.BTC, VALID_TX_HASH)

        // Assert
        verify(view).setStatus(CryptoCurrency.BTC, "Confirmed", VALID_TX_HASH)
    }

    @Test
    fun setTransactionColorMove() {
        //  Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.TRANSFERRED,
            confirmations = 0
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.BTC, VALID_TX_HASH)

        //  Assert
        verify(view).setTransactionColour(R.color.product_grey_transferred_50)
    }

    @Test
    fun setTransactionColorMoveConfirmed() {
        //  Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.TRANSFERRED,
            confirmations = 3
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.BTC, VALID_TX_HASH)

        //  Assert
        verify(view).setTransactionColour(R.color.product_grey_transferred)
    }

    @Test
    fun setTransactionColorSent() {
        //  Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.SENT,
            confirmations = 2
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.BTC, VALID_TX_HASH)

        //  Assert
        verify(view).setTransactionColour(R.color.product_red_sent_50)
    }

    @Test
    fun setTransactionColorSentConfirmed() {
        // Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.SENT,
            confirmations = 3
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        // Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        // Assert
        verify(view).setTransactionColour(R.color.product_red_sent)
    }

    @Test
    fun setTransactionColorReceived() {
        // Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.ETHER,
            direction = TransactionSummary.Direction.RECEIVED,
            confirmations = 7
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        // Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        // Assert
        verify(view).setTransactionColour(R.color.product_green_received_50)
    }

    @Test
    fun setTransactionColorReceivedConfirmed() {
        //  Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.BTC,
            direction = TransactionSummary.Direction.RECEIVED,
            confirmations = 3
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        //  Assert
        verify(view).setTransactionColour(R.color.product_green_received)
    }

    @Test
    fun `fee should be hidden if transaction is a fee one`() {
        // Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.ETHER,
            direction = TransactionSummary.Direction.SENT,
            confirmations = 7,
            isFeeTransaction = true
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)

        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        // Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        //  Assert
        verify(view).updateFeeFieldVisibility(false)
    }

    @Test
    fun `fee should be hidden if transaction is a receive one`() {
        //  Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.ETHER,
            direction = TransactionSummary.Direction.RECEIVED,
            confirmations = 7
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)

        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        //  Assert
        verify(view).updateFeeFieldVisibility(false)
    }

    @Test
    fun `fee should not be hidden if transaction is a sent one`() {
        //  Arrange
        val item = TestActivitySummaryItem(
            cryptoCurrency = CryptoCurrency.ETHER,
            direction = TransactionSummary.Direction.SENT,
            confirmations = 7
        )

        whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)

        whenever(inputOutMapper.transformInputAndOutputs(item)).thenReturn(
            Single.just(TransactionInOutDetails(emptyList(), emptyList()))
        )

        //  Act
        subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)

        //  Assert
        verify(view).updateFeeFieldVisibility(true)
    }

    companion object {
        private const val VALID_TX_HASH = "valid_hash"
        private const val INVALID_TX_HASH = "nope!"
    }
}
