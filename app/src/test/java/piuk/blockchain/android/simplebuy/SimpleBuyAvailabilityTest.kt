package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.KycTierState
import com.blockchain.swap.nabu.models.nabu.LimitsJson
import com.blockchain.swap.nabu.models.nabu.TierJson
import com.blockchain.swap.nabu.models.nabu.TiersJson
import com.blockchain.swap.nabu.service.TierService
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class SimpleBuyAvailabilityTest {

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val tierService: TierService = mock()
    private val simpleBuyFlag: FeatureFlag = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {
        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyPrefs, custodialWalletManager, currencyPrefs, tierService, simpleBuyFlag
        )

        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        whenever(tierService.tiers()).thenReturn(Single.just(
            TiersJson(
                listOf(
                    TierJson(0,
                        "",
                        KycTierState.None,
                        LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
                    ),
                    TierJson(1,
                        "",
                        KycTierState.Verified,
                        LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
                    ),
                    TierJson(2,
                        "",
                        KycTierState.Verified,
                        LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
                    )
                )
            )
        ))
        whenever(custodialWalletManager.isEligibleForSimpleBuy())
            .thenReturn(Single.just(true))
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(true))
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
    fun `should  be available when is not gold but local state exists`() {
        whenever(tierService.tiers())
            .thenReturn(Single.just(
                TiersJson(
                    listOf(
                        TierJson(0,
                            "",
                            KycTierState.None,
                            LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
                        ),
                        TierJson(1,
                            "",
                            KycTierState.Verified,
                            LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
                        ),
                        TierJson(2,
                            "",
                            KycTierState.None,
                            LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
                        )
                    )
                )
            ))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, true)
    }

    @Test
    fun `should not  be available when is gold but not eligible`() {
        whenever(custodialWalletManager.isEligibleForSimpleBuy()).thenReturn(Single.just(false))

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when tiers failed`() {
        whenever(tierService.tiers()).thenReturn(Single.error(Throwable()))

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when eligibility fails and no local state exists`() {
        whenever(custodialWalletManager.isEligibleForSimpleBuy()).thenReturn(Single.error(Throwable()))
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