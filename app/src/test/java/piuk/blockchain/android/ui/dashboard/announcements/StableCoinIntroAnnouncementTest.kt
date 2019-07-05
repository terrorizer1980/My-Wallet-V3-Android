package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R
import kotlin.test.assertEquals

class StableCoinIntroAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private val host: AnnouncementHost = mock()
    private lateinit var subject: StableCoinIntroAnnouncementRule

    private val config: RemoteConfig = mock()
    private val analytics: Analytics = mock()
    private val featureEnabled: FeatureFlag = mock()

    @Before
    fun setUp() {
        whenever(dismissRecorder[StableCoinIntroAnnouncementRule.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(StableCoinIntroAnnouncementRule.DISMISS_KEY)

        subject = StableCoinIntroAnnouncementRule(
            featureEnabled = featureEnabled,
            config = config,
            analytics = analytics,
            dismissRecorder = dismissRecorder
        )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)
        whenever(featureEnabled.enabled).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not enabled and not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(featureEnabled.enabled).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when enabled and not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(featureEnabled.enabled).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show card with the correct values when in AB test control group`() {
        whenever(config.getABVariant(RemoteConfig.AB_PAX_POPUP)).thenReturn(Single.just(false))
        val captor = argumentCaptor<AnnouncementCard>()
        subject.show(host)

        verify(host).showAnnouncementCard(captor.capture())

        assertEquals(captor.firstValue.title, R.string.stablecoin_announcement_introducing_title)
        assertEquals(captor.firstValue.description, R.string.stablecoin_announcement_introducing_description)
        assertEquals(captor.firstValue.link, R.string.stablecoin_announcement_introducing_link)

        verifyNoMoreInteractions(host)
    }

    @Test
    fun `should show popup if in AB test variant group`() {
        whenever(config.getABVariant(RemoteConfig.AB_PAX_POPUP)).thenReturn(Single.just(false))

        subject.show(host)

        verify(host).showAnnouncementCard(any())
        verifyNoMoreInteractions(host)
    }
}