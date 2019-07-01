package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class StellarModalPopupAnnouncementRuleTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private lateinit var subject: StellarModalPopupAnnouncementRule

    private val tierService: TierService = mock()
    private val featureFlag: FeatureFlag = mock()

    @Before
    fun setUp() {
        whenever(dismissRecorder[StellarModalPopupAnnouncementRule.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(StellarModalPopupAnnouncementRule.DISMISS_KEY)

        subject = StellarModalPopupAnnouncementRule(
            tierService = tierService,
            dismissRecorder = dismissRecorder,
            showPopupFeatureFlag = featureFlag
        )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)
        whenever(featureFlag.enabled).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}