package piuk.blockchain.android.ui.dashboard.announcement

import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.LimitsJson
import com.blockchain.kyc.models.nabu.TierJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.SwapAnnouncement
import piuk.blockchain.androidcore.utils.PersistentPrefs

class SwapAnnouncementTest {

    @Mock
    private lateinit var tierService: TierService
    @Mock
    private lateinit var tradeData: MorphTradeDataHistoryList
    @Mock
    private lateinit var prefs: PersistentPrefs
    @Mock
    private lateinit var dashboardPresenter: DashboardPresenter
    @Mock
    private lateinit var morphTrade: MorphTrade

    private lateinit var dismissRecorder: DismissRecorder
    private lateinit var swapAnnouncement: SwapAnnouncement
    private val sampleLimits = LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())

    private val sampleTrades by lazy {
        listOf(morphTrade)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        dismissRecorder = DismissRecorder(prefs)
        swapAnnouncement = SwapAnnouncement(tierService, tradeData, dismissRecorder)
    }

    @Test
    fun `should not show when is already dismissed`() {
        mockDismissRecorder(true)

        swapAnnouncement.shouldShow(dashboardPresenter).test().apply {
            assertNoErrors()
            values() `should equal` listOf(false)
        }

        verify(tierService, never()).tiers()
        verify(tradeData, never()).getTrades()
    }

    @Test
    fun `should show for tier1Verified and no swaps if not already shown`() {
        mockDismissRecorder(false)
        whenever(tierService.tiers()).thenReturn(Single.just(TiersJson(listOf(
            TierJson(0,
                "",
                KycTierState.None,
                sampleLimits),
            TierJson(0,
                "",
                KycTierState.Verified,
                sampleLimits),
            TierJson(0,
                "",
                KycTierState.None,
                sampleLimits)
        ))))
        whenever(tradeData.getTrades()).thenReturn(Single.just(emptyList()))

        swapAnnouncement.shouldShow(dashboardPresenter).test().apply {
            assertNoErrors()
            values() `should equal` listOf(true)
        }

        verify(tierService).tiers()
        verify(tradeData).getTrades()
    }

    @Test
    fun `should show for tier2Verified and no swaps if not already shown`() {
        mockDismissRecorder(false)
        whenever(tierService.tiers()).thenReturn(Single.just(TiersJson(listOf(
            TierJson(0,
                "",
                KycTierState.None,
                sampleLimits),
            TierJson(0,
                "",
                KycTierState.Verified,
                sampleLimits),
            TierJson(0,
                "",
                KycTierState.Verified,
                sampleLimits)
        ))))
        whenever(tradeData.getTrades()).thenReturn(Single.just(emptyList()))

        swapAnnouncement.shouldShow(dashboardPresenter).test().apply {
            assertNoErrors()
            values() `should equal` listOf(true)
        }

        verify(tierService).tiers()
        verify(tradeData).getTrades()
    }

    @Test
    fun `should not show if there are swaps`() {
        mockDismissRecorder(false)

        whenever(tradeData.getTrades()).thenReturn(Single.just(sampleTrades))

        swapAnnouncement.shouldShow(dashboardPresenter).test().apply {
            assertNoErrors()
            values() `should equal` listOf(false)
        }

        verify(tradeData).getTrades()
        verify(tierService, never()).tiers()
    }

    private fun mockDismissRecorder(alreadyShown: Boolean) {
        whenever(prefs.getValue(any(), any<Boolean>())).thenReturn(alreadyShown)
    }
}