package piuk.blockchain.android.ui.dashboard.announcement

import com.blockchain.kycui.stablecoin.StableCoinCampaignHelper
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.android.ui.dashboard.adapter.delegates.StableCoinAnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.StableCoinIntroductionAnnouncement
import piuk.blockchain.androidcore.utils.PersistentPrefs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StableCoinIntroductionAnnouncementTest {

    private val stableCoinCampaignHelper: StableCoinCampaignHelper = mock()
    private val prefs: PersistentPrefs = mock()
    private lateinit var dismissRecorder: DismissRecorder
    private val dashboardPresenter: DashboardPresenter = mock()
    private lateinit var stableCoinIntroductionAnnouncement: StableCoinIntroductionAnnouncement

    @Before
    fun setUp() {
        dismissRecorder = DismissRecorder(prefs)
        stableCoinIntroductionAnnouncement =
            StableCoinIntroductionAnnouncement(stableCoinCampaignHelper, dismissRecorder)
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(prefs.getValue("StableCoinIntroductionCard_DISMISSED", false)).thenReturn(true)
        whenever(stableCoinCampaignHelper.isEnabled()).thenReturn(Single.just(true))

        val shouldShowObserver = stableCoinIntroductionAnnouncement.shouldShow(dashboardPresenter).test()

        shouldShowObserver.assertValue {
            !it
        }
        shouldShowObserver.assertValueCount(1)
        shouldShowObserver.assertComplete()
    }

    @Test
    fun `should not show, when not enabled and not already shown`() {
        whenever(prefs.getValue("StableCoinIntroductionCard_DISMISSED", false)).thenReturn(false)
        whenever(stableCoinCampaignHelper.isEnabled()).thenReturn(Single.just(false))

        val shouldShowObserver = stableCoinIntroductionAnnouncement.shouldShow(dashboardPresenter).test()

        shouldShowObserver.assertValue {
            !it
        }
        shouldShowObserver.assertValueCount(1)
        shouldShowObserver.assertComplete()
    }

    @Test
    fun `should show, when enabled and not already shown`() {
        whenever(prefs.getValue("StableCoinIntroductionCard_DISMISSED", false)).thenReturn(false)
        whenever(stableCoinCampaignHelper.isEnabled()).thenReturn(Single.just(true))

        val shouldShowObserver = stableCoinIntroductionAnnouncement.shouldShow(dashboardPresenter).test()

        shouldShowObserver.assertValue {
            it
        }
        shouldShowObserver.assertValueCount(1)
        shouldShowObserver.assertComplete()
    }

    @Test
    fun `should be shown with the correct values`() {
        val captor = argumentCaptor<StableCoinAnnouncementCard>()
        stableCoinIntroductionAnnouncement.show(dashboardPresenter)

        verify(dashboardPresenter).showStableCoinIntroduction(eq(0), captor.capture())

        assertEquals(captor.firstValue.title, R.string.stablecoin_announcement_introducing_title)
        assertEquals(captor.firstValue.description, R.string.stablecoin_announcement_introducing_description)
        assertEquals(captor.firstValue.link, R.string.stablecoin_announcement_introducing_link)
        assertTrue(captor.firstValue.isNew)
    }
}