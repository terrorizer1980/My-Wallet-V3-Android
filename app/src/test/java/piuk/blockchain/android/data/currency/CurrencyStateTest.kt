package piuk.blockchain.android.data.currency

import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockito_kotlin.mock
import org.amshove.kluent.`should be`
import org.junit.Test
import piuk.blockchain.android.data.currency.CurrencyState

class CurrencyStateTest {

    private val mockPrefs: CurrencyPrefs = mock()
    private val subject: CurrencyState = CurrencyState(mockPrefs)

    @Test
    fun isDisplayingCryptoDefault() {
        // Arrange

        // Act

        // Assert
        subject.isDisplayingCryptoCurrency `should be` true
        subject.displayMode `should be` CurrencyState.DisplayMode.Crypto
    }

    @Test
    fun isDisplayingCryptoFalse() {
        // Arrange

        // Act
        subject.isDisplayingCryptoCurrency = false
        // Assert
        subject.isDisplayingCryptoCurrency `should be` false
        subject.displayMode `should be` CurrencyState.DisplayMode.Fiat
    }

    @Test
    fun `fiat display mode`() {
        // Arrange

        // Act
        subject.displayMode = CurrencyState.DisplayMode.Fiat
        // Assert
        subject.isDisplayingCryptoCurrency `should be` false
        subject.displayMode `should be` CurrencyState.DisplayMode.Fiat
    }

    @Test
    fun isDisplayingCryptoTrue() {
        // Arrange

        // Act
        subject.isDisplayingCryptoCurrency = true
        // Assert
        subject.isDisplayingCryptoCurrency `should be` true
        subject.displayMode `should be` CurrencyState.DisplayMode.Crypto
    }

    @Test
    fun `crypto display mode`() {
        // Arrange

        // Act
        subject.displayMode = CurrencyState.DisplayMode.Crypto
        // Assert
        subject.isDisplayingCryptoCurrency `should be` true
        subject.displayMode `should be` CurrencyState.DisplayMode.Crypto
    }
}
