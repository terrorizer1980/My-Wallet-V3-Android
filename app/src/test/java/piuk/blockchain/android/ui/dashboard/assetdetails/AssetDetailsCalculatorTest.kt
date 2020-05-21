package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.testutils.rxInit
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.androidcore.data.charts.TimeSpan

class AssetDetailsCalculatorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var calculator: AssetDetailsCalculator

    private val token: AssetTokens = mock()
    private val totalGroup: CryptoAccountGroup = mock()
    private val nonCustodialGroup: CryptoAccountGroup = mock()
    private val custodialGroup: CryptoAccountGroup = mock()

    @Before
    fun setUp() {
        calculator = AssetDetailsCalculator()

        whenever(token.accounts(AssetFilter.Total)).thenReturn(Single.just(totalGroup))
        whenever(token.accounts(AssetFilter.Wallet)).thenReturn(Single.just(nonCustodialGroup))
        whenever(token.accounts(AssetFilter.Custodial)).thenReturn(Single.just(custodialGroup))
    }

    @Test
    fun `cryptoBalance and fiatBalance returns the right values`() {

        val price = FiatValue.fromMinor("USD", 5647899)

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto

        val walletFiat = FiatValue.fromMinor("USD", 30985)
        val custodialFiat = FiatValue.fromMinor("USD", 0)
        val totalFiat = walletFiat + custodialFiat

        val expectedResult = mapOf(
            AssetFilter.Total to AssetDisplayInfo(totalCrypto, totalFiat, emptySet()),
            AssetFilter.Wallet to AssetDisplayInfo(walletCrypto, walletFiat, emptySet()),
            AssetFilter.Custodial to AssetDisplayInfo(custodialCrypto, custodialFiat, emptySet())
        )

        whenever(token.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))

        whenever(custodialGroup.isFunded).thenReturn(true)

        calculator.token.accept(token)

        val v = calculator.assetDisplayDetails
            .test()
            .values()

        // Using assertResult(expectedResult) instead of fetching the values and checking them results in
        // an 'AssertionException Not completed' result. I have no clue why; changing the matchers to not use all
        // three possible enum values changes the failure into an expected 'Failed, not equal' result (hence the
        // doAnswer() nonsense instead of eq() etc - I tried many things)
        // All very strange.
        assertEquals(expectedResult, v[0])
    }

    @Test
    fun `custodial not show if unfunded`() {

        val price = FiatValue.fromMinor("USD", 5647899)

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto

        val walletFiat = FiatValue.fromMinor("USD", 30985)
        val custodialFiat = FiatValue.fromMinor("USD", 0)
        val totalFiat = walletFiat + custodialFiat

        val expectedResult = mapOf(
            AssetFilter.Total to AssetDisplayInfo(totalCrypto, totalFiat, emptySet()),
            AssetFilter.Wallet to AssetDisplayInfo(walletCrypto, walletFiat, emptySet())
        )

        whenever(token.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))

        whenever(custodialGroup.isFunded).thenReturn(false)

        calculator.token.accept(token)

        val v = calculator.assetDisplayDetails
            .test()
            .values()

        // Using assertResult(expectedResult) instead of fetching the values and checking them results in
        // an 'AssertionException Not completed' result. I have no clue why; changing the matchers to not use all
        // three possible enum values changes the failure into an expected 'Failed, not equal' result (hence the
        // doAnswer() nonsense instead of eq() etc - I tried many things)
        // All very strange.
        assertEquals(expectedResult, v[0])
    }

    @Test
    fun `cryptoBalance and fiatBalance are never returned if exchange rated fails`() {
        val token: AssetTokens = mock()

        whenever(token.exchangeRate()).thenReturn(Single.error(Throwable()))

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))

        calculator.token.accept(token)

        val testObserver = calculator.assetDisplayDetails.test()
        testObserver.assertNoValues()
    }

    @Test
    fun `cryptoBalance and fiatBalance are never returned if totalBalance fails`() {
        val token: AssetTokens = mock()

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc

        whenever(token.exchangeRate()).thenReturn(Single.just(FiatValue.fromMinor("USD", 5647899)))

        whenever(totalGroup.balance).thenReturn(Single.error(Throwable()))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))

        calculator.token.accept(token)

        val testObserver = calculator.assetDisplayDetails.test()
        testObserver.assertNoValues()
    }

    @Test
    fun `exchange rate is the right one`() {
        val token: AssetTokens = mock()

        whenever(token.exchangeRate()).thenReturn(Single.just(FiatValue.fromMinor("USD", 5647899)))
        calculator.token.accept(token)

        val testObserver = calculator.exchangeRate.test()
        testObserver.assertValue("$56,478.99").assertValueCount(1)
    }

    @Test
    fun `historic prices are returned properly`() {
        val token: AssetTokens = mock()

        whenever(token.historicRateSeries(TimeSpan.MONTH, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.just(listOf(
                PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
            )))

        calculator.token.accept(token)

        val loadingTestObserver = calculator.chartLoading.test()
        val testObserver = calculator.historicPrices.test()

        testObserver.assertValue(listOf(
            PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
        )).assertValueCount(1)

        loadingTestObserver.assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false)
            .assertValueCount(3)
    }

    @Test
    fun `when historic prices api returns error, empty list should be returned`() {
        val token: AssetTokens = mock()
        whenever(token.historicRateSeries(TimeSpan.MONTH, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.error(Throwable()))

        calculator.token.accept(token)

        val loadingTestObserver = calculator.chartLoading.test()
        val testObserver = calculator.historicPrices.test()

        testObserver.assertValue(emptyList()).assertValueCount(1)

        loadingTestObserver.assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false)
            .assertValueCount(3)
    }
}