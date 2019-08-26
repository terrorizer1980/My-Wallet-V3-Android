package piuk.blockchain.android.ui.recover

import com.nhaarman.mockito_kotlin.mock
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class RecoverFundsPresenterTest {

    private var subject: RecoverFundsPresenter = RecoverFundsPresenter()
    private val view: RecoverFundsView = mock()

    @Before
    fun setUp() {
        subject.initView(view)
    }

    /**
     * Recovery phrase missing, should inform user.
     */
    @Test
    fun onContinueClickedNoRecoveryPhrase() {
        // Arrange

        // Act
        subject.onContinueClicked("")

        // Assert
        verify(view).showToast(anyInt(), anyString())
        verifyNoMoreInteractions(view)
    }

    /**
     * Recovery phrase is too short to be valid, should inform user.
     */
    @Test
    fun onContinueClickedInvalidRecoveryPhraseLength() {
        // Arrange

        // Act
        subject.onContinueClicked("one two three four")

        // Assert
        verify(view).showToast(anyInt(), anyString())
        verifyNoMoreInteractions(view)
    }

    /**
     * Successful restore. Should take the user to the PIN entry page.
     */
    @Test
    fun onContinueClickedSuccessful() {
        // Arrange
        val mnemonic = "all all all all all all all all all all all all"

        // Act
        subject.onContinueClicked(mnemonic)

        // Assert
        verify(view).gotoCredentialsActivity(mnemonic)
        verifyNoMoreInteractions(view)
    }

    /**
     * Restore failed, inform the user.
     */
    @Test
    fun onContinueClickedFailed() {
        // Arrange
        // TODO: 13/07/2017 isValidMnemonic not testable
        // Act

        // Assert
    }
}