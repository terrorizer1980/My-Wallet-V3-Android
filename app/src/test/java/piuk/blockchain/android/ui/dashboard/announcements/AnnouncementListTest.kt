package piuk.blockchain.android.ui.dashboard.announcements

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`it throws`
import org.junit.Test

class AnnouncementListTest {

    private val host: AnnouncementHost = mock()

    private fun createAnnouncementList(scheduler: Scheduler = Schedulers.trampoline()) =
        AnnouncementList(
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

    private fun announcement(): AnnouncementRule =
        mock {
            on { shouldShow() } `it returns` Single.just(true)
        }

    private fun dontShowAnnouncement(): AnnouncementRule =
        mock {
            on { shouldShow() } `it returns` Single.just(false)
            on { show(host) } `it throws` RuntimeException("Not expected")
        }

    private fun dontCheckAnnouncement(): AnnouncementRule =
        mock {
            on { shouldShow() } `it throws` RuntimeException("Not expected")
            on { show(host) } `it throws` RuntimeException("Not expected")
        }
}