package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.services.nabu.TierService
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.utils.PersistentPrefs

class GoForGoldAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val prefs: PersistentPrefs = mock()
    private val tiers: TiersJson = mock()
    private val tierService: TierService = mock()

    private lateinit var subject: GoForGoldAnnouncementRule

    @Before
    fun setUp() {
        whenever(dismissRecorder[GoForGoldAnnouncementRule.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(GoForGoldAnnouncementRule.DISMISS_KEY)

        subject = GoForGoldAnnouncementRule(
            tierService = tierService,
            prefs = prefs,
            dismissRecorder = dismissRecorder
        )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show when tier 1 verified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier1Approved)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show when tier 2 failed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier2Failed)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show when tier 2 verified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier2Approved)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show when tier 2 in review`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier2InReview)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show when pre-IDV check failed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(true)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier1Approved)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
