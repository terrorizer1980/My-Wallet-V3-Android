package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.kycui.sunriver.SunriverCardType
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`it throws`
import org.junit.Test
import piuk.blockchain.androidcore.utils.PersistentPrefs

class AnnouncementListTest {

    private val host: AnnouncementHost = mock()
    private val prefs: PersistentPrefs = mock()
    private val sunriverCampaignHelper: SunriverCampaignHelper = mock()
    private val kycTiersQueries: KycTiersQueries = mock()

    private fun createAnnouncementList(scheduler: Scheduler = Schedulers.trampoline()) =
        AnnouncementList(
            dismissRecorder = DismissRecorder(prefs = prefs),
            sunriverCampaignHelper = sunriverCampaignHelper,
            kycTiersQueries = kycTiersQueries,
            mainScheduler = scheduler
        )

    @Test
    fun `calls no announcements until subscribed`() {
        createAnnouncementList()
            .add(dontCheckAnnouncement())
            .add(dontShowAnnouncement())
            .showNextAnnouncement(host)
    }

    @Test
    fun `calls first announcement that says it should show`() {
        val announcement = announcement()
        createAnnouncementList()
            .add(dontShowAnnouncement())
            .add(announcement)
            .showNextAnnouncement(host)
            .test()
            .assertValue(announcement)
            .assertComplete()
            .assertNoErrors()
        verify(announcement).show(host)
    }

    @Test
    fun `nothing available`() {
        createAnnouncementList()
            .add(dontShowAnnouncement())
            .add(dontShowAnnouncement())
            .add(dontShowAnnouncement())
            .showNextAnnouncement(host)
            .test()
            .assertValues()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `does not check announcements beyond one that says it should show`() {
        val announcement = announcement()

        createAnnouncementList()
            .add(announcement)
            .add(dontCheckAnnouncement())
            .showNextAnnouncement(host)
            .test()
            .assertValue(announcement)
            .assertComplete()
            .assertNoErrors()
        verify(announcement).show(host)
    }

    @Test
    fun `calls first announcement that says it should show - alternative scheduler`() {
        val announcement = announcement()
        val scheduler = TestScheduler()

        val test = createAnnouncementList(scheduler)
            .add(dontShowAnnouncement())
            .add(announcement)
            .showNextAnnouncement(host)
            .test()
            .assertValues()
            .assertNoErrors()
            .assertNotComplete()
        verify(announcement, never()).show(host)
        scheduler.triggerActions()
        verify(announcement).show(host)
        test
            .assertValue(announcement)
            .assertComplete()
            .assertNoErrors()
    }

    private fun announcement(): Announcement =
        mock {
            on { shouldShow() } `it returns` Single.just(true)
        }

    private fun dontShowAnnouncement(): Announcement =
        mock {
            on { shouldShow() } `it returns` Single.just(false)
            on { show(host) } `it throws` RuntimeException("Not expected")
        }

    private fun dontCheckAnnouncement(): Announcement =
        mock {
            on { shouldShow() } `it throws` RuntimeException("Not expected")
            on { show(host) } `it throws` RuntimeException("Not expected")
        }

    @Test
    fun `addSunriverPrompts type none`() {
        // Arrange
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.just(SunriverCardType.None))

        // Act
        createAnnouncementList()
            .addSunriverPrompts(host).test()

        // Assert
        verifyZeroInteractions(host)
    }

    @Test
    fun `addSunriverPrompts type FinishSignUp ignored as already dismissed`() {
        // Arrange
        whenever(sunriverCampaignHelper.getCampaignCardType())
            .thenReturn(Single.just(SunriverCardType.FinishSignUp))
        whenever(prefs.getValue(SunriverCardType.FinishSignUp.javaClass.simpleName, false))
            .thenReturn(true)

        // Act
        createAnnouncementList()
            .addSunriverPrompts(host).test()

        // Assert
        verifyZeroInteractions(host)
    }
}