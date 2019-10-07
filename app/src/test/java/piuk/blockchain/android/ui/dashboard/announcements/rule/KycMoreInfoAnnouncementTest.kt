package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.androidbuysell.api.CoinifyWalletService

class KycMoreInfoAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private val tierService: TierService = mock()
    private val walletService: CoinifyWalletService = mock()
    private val featureFlag: FeatureFlag = mock()

    private lateinit var subject: KycMoreInfoAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[KycMoreInfoAnnouncement.DISMISS_KEY])
            .thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey)
            .thenReturn(KycMoreInfoAnnouncement.DISMISS_KEY)

        subject = KycMoreInfoAnnouncement(
            tierService = tierService,
            coinifyWalletService = walletService,
            showPopupFeatureFlag = featureFlag,
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
}
