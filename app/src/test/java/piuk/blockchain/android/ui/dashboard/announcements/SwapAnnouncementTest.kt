package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.LimitsJson
import com.blockchain.kyc.models.nabu.TierJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class SwapAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private val tierService: TierService = mock()
    private val tradeData: MorphTradeDataHistoryList = mock()
    private val morphTrade: MorphTrade = mock()

    private val sampleLimits = LimitsJson("", 0.toBigDecimal(), 0.toBigDecimal())
    private val sampleTrades by lazy { listOf(morphTrade) }

    private lateinit var subject: SwapAnnouncementRule

    @Before
    fun setUp() {
        whenever(dismissRecorder[SwapAnnouncementRule.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(SwapAnnouncementRule.DISMISS_KEY)

        subject = SwapAnnouncementRule(tierService, tradeData, dismissRecorder)
    }

    @Test
    fun `should not show when is already dismissed`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()

        verify(tierService, never()).tiers()
        verify(tradeData, never()).getTrades()
    }

    @Test
    fun `should show for tier1Verified and no swaps if not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(tierService.tiers()).thenReturn(
            Single.just(
                TiersJson(
                    listOf(
                        TierJson(0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        ),
                        TierJson(0,
                            "",
                            KycTierState.Verified,
                            sampleLimits
                        ),
                        TierJson(0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        )
                    )
                )
            )
        )
        whenever(tradeData.getTrades()).thenReturn(Single.just(emptyList()))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()

        verify(tierService).tiers()
        verify(tradeData).getTrades()
    }

    @Test
    fun `should show for tier2Verified and no swaps if not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(tierService.tiers()).thenReturn(
            Single.just(
                TiersJson(
                    listOf(
                        TierJson(0,
                            "",
                            KycTierState.None,
                            sampleLimits
                        ),
                        TierJson(0,
                            "",
                            KycTierState.Verified,
                            sampleLimits
                        ),
                        TierJson(0,
                            "",
                            KycTierState.Verified,
                            sampleLimits
                        )
                    )
                )
            )
        )
        whenever(tradeData.getTrades()).thenReturn(Single.just(emptyList()))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()

        verify(tierService).tiers()
        verify(tradeData).getTrades()
    }

    @Test
    fun `should not show if there are swaps`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(tradeData.getTrades()).thenReturn(Single.just(sampleTrades))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()

        verify(tradeData).getTrades()
        verify(tierService, never()).tiers()
    }
}