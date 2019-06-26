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
    private val tiers: TiersJson = mock()
    private val prefs: PersistentPrefs = mock()
    private val tierService: TierService = mock()
    private lateinit var dismissRecorder: DismissRecorder
    private lateinit var subject: GoForGoldAnnouncement

    @Before
    fun setUp() {
        dismissRecorder = DismissRecorder(prefs)
        subject = GoForGoldAnnouncement(
            tierService = tierService,
            prefs = prefs,
            dismissRecorder = dismissRecorder
        )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(prefs.getValue(GoForGoldAnnouncement.DISMISS_KEY, false)).thenReturn(true)

        val shouldShowObserver = subject.shouldShow().test()

        shouldShowObserver.assertValue { !it }
        shouldShowObserver.assertValueCount(1)
        shouldShowObserver.assertComplete()
    }

    @Test
    fun `should show when tier 1 verified`() {
        whenever(prefs.getValue(GoForGoldAnnouncement.DISMISS_KEY, false)).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier1Approved)

        val result = subject.shouldShow().test()

        result.assertValue { it }
        result.assertValueCount(1)
        result.assertComplete()
    }

    @Test
    fun `should not show when tier 2 failed`() {
        whenever(prefs.getValue(GoForGoldAnnouncement.DISMISS_KEY, false)).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier2Failed)

        val result = subject.shouldShow().test()

        result.assertValue { !it }
        result.assertValueCount(1)
        result.assertComplete()
    }

    @Test
    fun `should not show when tier 2 verified`() {
        whenever(prefs.getValue(GoForGoldAnnouncement.DISMISS_KEY, false)).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier2Approved)

        val result = subject.shouldShow().test()

        result.assertValue { !it }
        result.assertValueCount(1)
        result.assertComplete()
    }

    @Test
    fun `should not show when tier 2 in review`() {
        whenever(prefs.getValue(GoForGoldAnnouncement.DISMISS_KEY, false)).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(false)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier2InReview)

        val result = subject.shouldShow().test()

        result.assertValue { !it }
        result.assertValueCount(1)
        result.assertComplete()
    }

    @Test
    fun `should not show when pre-IDV check failed`() {
        whenever(prefs.getValue(GoForGoldAnnouncement.DISMISS_KEY, false)).thenReturn(false)
        whenever(prefs.devicePreIDVCheckFailed).thenReturn(true)
        whenever(tierService.tiers()).thenReturn(Single.just(tiers))
        whenever(tiers.combinedState).thenReturn(Kyc2TierState.Tier1Approved)

        val result = subject.shouldShow().test()

        result.assertValue { !it }
        result.assertValueCount(1)
        result.assertComplete()
    }
}
