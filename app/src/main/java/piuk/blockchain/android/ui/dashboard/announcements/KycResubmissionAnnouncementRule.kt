package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kycui.navhost.models.CampaignType
import io.reactivex.Single
import piuk.blockchain.android.R

internal class KycResubmissionAnnouncementRule(
    private val kycTiersQueries: KycTiersQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return kycTiersQueries.isKycResubmissionRequired()
    }

    override fun show(host: AnnouncementHost) {

        val card = AnnouncementCard(
            style = AnnouncementStyle.ImageRight,
            title = R.string.kyc_resubmission_card_title,
            description = R.string.kyc_resubmission_card_description,
            link = R.string.kyc_resubmission_card_button,
            image = R.drawable.vector_kyc_onboarding,
            closeFunction = {
                dismissEntry.dismiss(DismissRule.DismissForSession)
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
            },
            linkFunction = {
                host.startKyc(CampaignType.Resubmission)
            },
            prefsKey = dismissEntry.prefsKey
        )
        host.showAnnouncementCard(card)
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KYC_RESUBMISSION_DISMISSED"
    }
}
