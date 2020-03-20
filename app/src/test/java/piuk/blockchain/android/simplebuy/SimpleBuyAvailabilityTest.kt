package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class SimpleBuyAvailabilityTest {

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val simpleBuyFlag: FeatureFlag = mock()
    private val buyDataManager: BuyDataManager = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {

        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        whenever(buyDataManager.hasCoinifyAccount)
            .thenReturn(Single.just(false))

        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(true))

        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyPrefs, custodialWalletManager, currencyPrefs, simpleBuyFlag, buyDataManager
        )
    }

    @Test
    fun `should work in the happy scenario`() {
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, true)
    }

    @Test
    fun `should not be available when currency is not supported`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(false))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not be available when feature flag is off`() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(false))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not be available when is not eligible and no local state exists`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(false))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when is coinify tagged and no local state`() {
        whenever(buyDataManager.hasCoinifyAccount).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when user request fails and no local state exists`() {
        whenever(buyDataManager.hasCoinifyAccount)
            .thenReturn(Single.error(Throwable()))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when currency call fails`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.error(Throwable()))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }
}