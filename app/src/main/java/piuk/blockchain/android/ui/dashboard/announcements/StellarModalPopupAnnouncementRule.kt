package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.remoteconfig.FeatureFlag
import piuk.blockchain.android.ui.dashboard.announcements.popups.StellarModelPopup
import io.reactivex.Single

internal class StellarModalPopupAnnouncementRule(
    private val tierService: TierService,
    dismissRecorder: DismissRecorder,
    private val showPopupFeatureFlag: FeatureFlag
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

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
        StellarModelPopup.show(host, DISMISS_KEY)
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "StellarModalPopupAnnouncement_DISMISSED"
    }
}
