package piuk.blockchain.android.ui.dashboard.announcements

import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.thepit.PitLinking

class PitAnnouncementRuleTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val pitLinking: PitLinking = mock()

    private lateinit var subject: PitAnnouncementRule

    @Before
    fun setUp() {
        whenever(dismissRecorder[PitAnnouncementRule.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(PitAnnouncementRule.DISMISS_KEY)

        subject = PitAnnouncementRule(
            pitLink = pitLinking,
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
    fun `should not show, if the pit is already linked`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(pitLinking.isPitLinked()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, if the pit is not linked`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(pitLinking.isPitLinked()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}
