package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class KycForBlockstackMiniAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val queries: AnnouncementQueries = mock()
    private val kycForBlockstackAnnouncement: AnnouncementRule = mock()

    private lateinit var subject: KycForBlockstackMiniAnnouncement

    @Before
    fun setUp() {
        subject =
            KycForBlockstackMiniAnnouncement(
                dismissRecorder = dismissRecorder,
                queries = queries,
                kycForBlockstackAnnouncement = kycForBlockstackAnnouncement
            )
    }

    @Test
    fun `Dont show when  Gold kyc process hasn't been completed but standard announcement hasn't been dismissed`() {
        whenever(kycForBlockstackAnnouncement.isDismissed()).thenReturn(false)
        whenever(queries.isGoldComplete()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `Dont show when  Gold kyc process hasn been completed and standard announcement hasn't been dismissed`() {
        whenever(kycForBlockstackAnnouncement.isDismissed()).thenReturn(false)
        whenever(queries.isGoldComplete()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `Dont show when the Gold kyc process has been completed`() {
        whenever(queries.isGoldComplete()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()

        Mockito.verifyZeroInteractions(kycForBlockstackAnnouncement)
    }

    @Test
    fun `Show when the Gold kyc process hasn't been completed and standard announcement has been dismissed`() {
        whenever(queries.isGoldComplete()).thenReturn(Single.just(false))
        whenever(kycForBlockstackAnnouncement.isDismissed()).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}