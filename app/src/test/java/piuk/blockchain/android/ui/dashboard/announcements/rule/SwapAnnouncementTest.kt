package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class SwapAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private val queries: AnnouncementQueries = mock()
    private val tradeData: MorphTradeDataHistoryList = mock()
    private val morphTrade: MorphTrade = mock()

    private val sampleTrades by lazy { listOf(morphTrade) }

    private lateinit var subject: SwapAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[SwapAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(SwapAnnouncement.DISMISS_KEY)

        subject = SwapAnnouncement(
            queries = queries,
            dataManager = tradeData,
            dismissRecorder = dismissRecorder
        )
    }

    @Test
    fun `should not show when is already dismissed`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()

        verify(queries, never()).isTier1Or2Verified()
        verify(tradeData, never()).getTrades()
    }

    @Test
    fun `should show for kyc verified and no swaps if not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.isTier1Or2Verified()).thenReturn(Single.just(true))
        whenever(tradeData.getTrades()).thenReturn(Single.just(emptyList()))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()

        verify(queries).isTier1Or2Verified()
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
        verify(queries, never()).isTier1Or2Verified()
    }
}