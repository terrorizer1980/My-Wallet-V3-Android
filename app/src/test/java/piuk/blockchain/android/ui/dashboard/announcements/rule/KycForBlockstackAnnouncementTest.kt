package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class KycForBlockstackAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val queries: AnnouncementQueries = mock()

    private lateinit var subject: KycForBlockstackAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[KycForBlockstackAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(KycForBlockstackAnnouncement.DISMISS_KEY)

        subject =
            KycForBlockstackAnnouncement(
                dismissRecorder = dismissRecorder,
                queries = queries
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
    fun `should show, when not already shown, gold kyc isn't started and is in a kyc geo-zone`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.canKyc()).thenReturn(Single.just(true))
        whenever(queries.isKycGoldStartedOrComplete()).thenReturn(Single.just(false))
        whenever(queries.isGoldRejected()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, gold kyc isn't started, is in a kyc geo-zone and is gold-rejected`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.canKyc()).thenReturn(Single.just(true))
        whenever(queries.isKycGoldStartedOrComplete()).thenReturn(Single.just(false))
        whenever(queries.isGoldRejected()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, gold kyc has been started and is in a kyc geo-zone`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.canKyc()).thenReturn(Single.just(true))
        whenever(queries.isKycGoldStartedOrComplete()).thenReturn(Single.just(true))
        whenever(queries.isGoldRejected()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, gold kyc isn't started and is not in a kyc geo-zone`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.canKyc()).thenReturn(Single.just(false))
        whenever(queries.isKycGoldStartedOrComplete()).thenReturn(Single.just(false))
        whenever(queries.isGoldRejected()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, gold kyc has been started started and is not in a kyc geo-zone`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.canKyc()).thenReturn(Single.just(false))
        whenever(queries.isKycGoldStartedOrComplete()).thenReturn(Single.just(true))
        whenever(queries.isGoldRejected()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
