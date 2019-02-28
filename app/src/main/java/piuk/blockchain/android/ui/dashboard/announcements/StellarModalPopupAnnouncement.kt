package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.sunriver.ui.AirdropBottomDialog
import io.reactivex.Single
import piuk.blockchain.android.ui.dashboard.DashboardPresenter

internal class StellarModalPopupAnnouncement(
    private val tierService: TierService,
    dismissRecorder: DismissRecorder
) : Announcement<DashboardPresenter> {

    private val dismissEntry = dismissRecorder["AirdropBottomDialog"]

    override fun shouldShow(context: DashboardPresenter): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return tierService.tiers().map {
            it.combinedState !in listOf(
                Kyc2TierState.Tier2InReview,
                Kyc2TierState.Tier2Approved,
                Kyc2TierState.Tier2Failed
            )
        }
    }

    override fun show(context: DashboardPresenter) {
        context.view.showBottomSheetDialog(AirdropBottomDialog())
        dismissEntry.isDismissed = true
    }
}
