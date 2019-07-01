package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class ClaimYourFreeCryptoAnnouncementRuleTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val campaignHelper: SunriverCampaignHelper = mock()
    private val tierService: TierService = mock()

    private lateinit var subject: ClaimYourFreeCryptoAnnouncementRule

    @Before
    fun setUp() {
        whenever(dismissRecorder[ClaimYourFreeCryptoAnnouncementRule.DISMISS_KEY])
            .thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey)
            .thenReturn(ClaimYourFreeCryptoAnnouncementRule.DISMISS_KEY)

        subject = ClaimYourFreeCryptoAnnouncementRule(
            tierService = tierService,
            sunriverCampaignHelper = campaignHelper,
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
    fun `should not show when campaign already active`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(campaignHelper.userIsInSunRiverCampaign()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}