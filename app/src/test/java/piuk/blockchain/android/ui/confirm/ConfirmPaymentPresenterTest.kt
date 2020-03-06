package piuk.blockchain.android.ui.confirm

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.androidcoreui.ui.base.UiState
import java.util.Locale

class ConfirmPaymentPresenterTest {

    private lateinit var subject: ConfirmPaymentPresenter
    private val mockActivity: ConfirmPaymentView = mock()

    @Before
    fun setUp() {
        subject = ConfirmPaymentPresenter()
        subject.initView(mockActivity)

        Locale.setDefault(Locale.US)
    }

    @Test
    fun `onViewReady payment details null`() {
        // Arrange

        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).closeDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewReady() {
        // Arrange
        val fromLabel = "FROM_LABEL"
        val toLabel = "TO_LABEL"
        val btcAmount = "BTC_AMOUNT"
        val btcUnit = "BTC_UNIT"
        val fiatAmount = "FIAT_AMOUNT"
        val fiatUnit = "USD"
        val btcFee = "BTC_FEE"
        val fiatFee = "FIAT_FEE"
        val btcTotal = "BTC_TOTAL"
        val fiatTotal = "FIAT_TOTAL"
        val confirmationDetails = PaymentConfirmationDetails().apply {
            this.fiatUnit = fiatUnit
            this.fromLabel = fromLabel
            this.toLabel = toLabel
            this.cryptoAmount = btcAmount
            this.cryptoUnit = btcUnit
            this.fiatAmount = fiatAmount
            this.cryptoFee = btcFee
            this.fiatFee = fiatFee
            this.cryptoTotal = btcTotal
            this.fiatTotal = fiatTotal
        }
        val contactNote = "CONTACT_NOTE"
        val contactNoteDescription = "CONTACT_NOTE_DESCRIPTION"
        whenever(mockActivity.paymentDetails).thenReturn(confirmationDetails)
        whenever(mockActivity.contactNote).thenReturn(contactNote)
        whenever(mockActivity.contactNoteDescription).thenReturn(contactNoteDescription)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).contactNote
        verify(mockActivity).contactNoteDescription
        verify(mockActivity).setFromLabel(fromLabel)
        verify(mockActivity).setToLabel(toLabel)
        verify(mockActivity).setAmount("$btcAmount $btcUnit ($$fiatAmount)")
        verify(mockActivity).setFee("$btcFee $btcUnit ($$fiatFee)")
        verify(mockActivity).setTotals("$btcTotal $btcUnit", "$$fiatTotal")
        verify(mockActivity).contactNote = contactNote
        verify(mockActivity).contactNoteDescription = contactNoteDescription
        verify(mockActivity).setUiState(UiState.CONTENT)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewReady_erc20() {
        // Arrange
        val fromLabel = "FROM_LABEL"
        val toLabel = "TO_LABEL"
        val erc20Amount = "PAX_AMOUNT"
        val erc20Unit = "PAX_UNIT"
        val fiatAmount = "FIAT_AMOUNT"
        val fiatUnit = "USD"
        val ethFee = "ETH_FEE"
        val ethUnit = "ETH_UNIT"
        val fiatFee = "FIAT_FEE"
        val fiatTotal = "FIAT_TOTAL"
        val confirmationDetails = PaymentConfirmationDetails().apply {
            this.fromLabel = fromLabel
            this.toLabel = toLabel
            this.cryptoAmount = erc20Amount
            this.cryptoUnit = erc20Unit
            this.fiatAmount = fiatAmount
            this.fiatUnit = fiatUnit
            this.cryptoFee = ethFee
            this.cryptoFeeUnit = ethUnit
            this.fiatFee = fiatFee
            this.fiatTotal = fiatTotal
            this.showCryptoTotal = false
        }
        val contactNote = "CONTACT_NOTE"
        val contactNoteDescription = "CONTACT_NOTE_DESCRIPTION"
        whenever(mockActivity.paymentDetails).thenReturn(confirmationDetails)
        whenever(mockActivity.contactNote).thenReturn(contactNote)
        whenever(mockActivity.contactNoteDescription).thenReturn(contactNoteDescription)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).contactNote
        verify(mockActivity).contactNoteDescription
        verify(mockActivity).setFromLabel(fromLabel)
        verify(mockActivity).setToLabel(toLabel)
        verify(mockActivity).setAmount("$erc20Amount $erc20Unit ($$fiatAmount)")
        verify(mockActivity).setFee("$ethFee $ethUnit ($$fiatFee)")
        verify(mockActivity).setFiatTotalOnly("$$fiatTotal")
        verify(mockActivity).contactNote = contactNote
        verify(mockActivity).contactNoteDescription = contactNoteDescription
        verify(mockActivity).setUiState(UiState.CONTENT)
        verifyNoMoreInteractions(mockActivity)
    }
}