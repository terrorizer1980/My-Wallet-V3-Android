package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.KycState
import com.blockchain.swap.nabu.models.nabu.NabuUser
import com.blockchain.swap.nabu.models.nabu.UserState
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class SimpleBuyAvailabilityTest {

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val simpleBuyFlag: FeatureFlag = mock()
    private val nabuToken: NabuToken = mock()
    private val nabuDataManager: NabuDataManager = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {

        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        whenever(nabuToken.fetchNabuToken())
            .thenReturn(Single.just(NabuOfflineTokenResponse("", "")))

        whenever(nabuDataManager.getUser(any()))
            .thenReturn(Single.just(nabuUser(false)))

        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(true))

        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyPrefs, custodialWalletManager, currencyPrefs, simpleBuyFlag, nabuToken, nabuDataManager
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
        whenever(nabuDataManager.getUser(any())).thenReturn(Single.just(nabuUser(true)))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when user request fails and no local state exists`() {
        whenever(nabuDataManager.getUser(any())).thenReturn(Single.error(Throwable()))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when currency call fails`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.error(Throwable()))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    private fun nabuUser(isCoinifyTagged: Boolean) =
        NabuUser(
            firstName = "",
            lastName = "",
            email = "",
            emailVerified = false,
            dob = null,
            mobile = "",
            mobileVerified = false,
            address = null,
            state = UserState.None,
            kycState = KycState.None,
            insertedAt = "",
            updatedAt = ""
        ).copy(tags = if (isCoinifyTagged) mapOf("COINIFY" to mapOf("" to "")) else emptyMap())
}