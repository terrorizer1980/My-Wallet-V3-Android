package piuk.blockchain.android.ui.simplebuy

import info.blockchain.balance.FiatValue
import org.junit.Test
import piuk.blockchain.android.simplebuy.SimpleBuyState
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleBuyStateTest {

    @Test
    fun `amount is valid when entered amount is number and between limits`() {
        val state = SimpleBuyState(minAmount = FiatValue.zero("USD"),
            maxAmount = FiatValue.fromMinor("USD", 10000), enteredAmount = "99.32")
        assertTrue(state.isAmountValid())
    }

    @Test
    fun `amount is not valid when entered amount is not between limits`() {
        val state = SimpleBuyState(minAmount = FiatValue.zero("USD"),
            maxAmount = FiatValue.fromMinor("USD", 10000), enteredAmount = "101")
        assertFalse(state.isAmountValid())
    }

    @Test
    fun `amount is not valid when entered amount is not numeric`() {
        val state = SimpleBuyState(minAmount = FiatValue.zero("USD"),
            maxAmount = FiatValue.fromMinor("USD", 10000), enteredAmount = "1021f1")
        assertFalse(state.isAmountValid())
    }

    @Test
    fun `amount is not valid when entered amount is empty`() {
        val state = SimpleBuyState(minAmount = FiatValue.zero("USD"),
            maxAmount = FiatValue.fromMinor("USD", 10000), enteredAmount = "")
        assertFalse(state.isAmountValid())
    }
}