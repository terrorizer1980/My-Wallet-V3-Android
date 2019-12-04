package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class AssetMapTest {

    private val subject = AssetMap(
        map = mapOfAssets(
            CryptoCurrency.BTC to initialBtcState,
            CryptoCurrency.ETHER to initialEthState,
            CryptoCurrency.XLM to initialXlmState
        )
    )

    @Test(expected = IllegalArgumentException::class)
    fun `Exception thrown if unknown asset requested from get()`() {
        subject[CryptoCurrency.PAX]
    }

    @Test
    fun `copy with patchAsset works as expected`() {
        val newAsset = AssetModel(
            currency = CryptoCurrency.BTC,
            cryptoBalance = CryptoValue.bitcoinFromMajor(20),
            price = FiatValue.fromMajor(FIAT_CURRENCY, 300.toBigDecimal()),
            price24h = FiatValue.fromMajor(FIAT_CURRENCY, 400.toBigDecimal()),
            priceTrend = emptyList()
        )

        val copy = subject.copy(patchAsset = newAsset)

        assertNotEquals(copy[CryptoCurrency.BTC], subject[CryptoCurrency.BTC])
        assertEquals(copy[CryptoCurrency.BTC], newAsset)
        assertEquals(copy[CryptoCurrency.ETHER], subject[CryptoCurrency.ETHER])
        assertEquals(copy[CryptoCurrency.XLM], subject[CryptoCurrency.XLM])
    }

    @Test
    fun `copy with patchBalance works as expected`() {
        val newBalance = CryptoValue.etherFromMajor(20)

        val copy = subject.copy(patchBalance = newBalance)

        assertEquals(copy[CryptoCurrency.BTC], subject[CryptoCurrency.BTC])
        assertNotEquals(copy[CryptoCurrency.ETHER], subject[CryptoCurrency.ETHER])
        assertEquals(copy[CryptoCurrency.ETHER].cryptoBalance, newBalance)
        assertEquals(copy[CryptoCurrency.XLM], subject[CryptoCurrency.XLM])
    }

    @Test
    fun `reset() replaces all assets`() {
        val result = subject.reset()

        assertEquals(result.size, subject.size)
        subject.keys.forEach {
            assertNotSame(result[it], subject[it])
        }
    }
}
