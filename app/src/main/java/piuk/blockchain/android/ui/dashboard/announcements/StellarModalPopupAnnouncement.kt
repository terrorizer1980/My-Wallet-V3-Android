package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.remoteconfig.FeatureFlag
import piuk.blockchain.android.ui.dashboard.announcements.popups.AirdropPopup
import io.reactivex.Single

internal class StellarModalPopupAnnouncement(
    private val tierService: TierService,
    dismissRecorder: DismissRecorder,
    private val showPopupFeatureFlag: FeatureFlag
) : Announcement {

    private val dismissEntry = dismissRecorder["StellarModalPopupAnnouncement_DISMISSED"]

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        val hasNotTriedTier2 = tierService.tiers().map {
            it.combinedState !in listOf(
                Kyc2TierState.Tier2InReview,
                Kyc2TierState.Tier2Approved,
                Kyc2TierState.Tier2Failed
            )
        }
        return Single.merge(hasNotTriedTier2, showPopupFeatureFlag.enabled).all { it }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncmentPopup(AirdropPopup())
        dismissEntry.isDismissed = true
    }
}
