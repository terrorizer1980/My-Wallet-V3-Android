package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.kycui.sunriver.SunriverCardType
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import timber.log.Timber

internal class KycIncompleteAnnouncementRule(
    private val kycTiersQueries: KycTiersQueries,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    private val mainScheduler: Scheduler,
    dismissRecorder: DismissRecorder
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return kycTiersQueries.isKycInProgress()
    }

    override fun show(host: AnnouncementHost) {
        host.disposables += sunriverCampaignHelper.getCampaignCardType()
            .observeOn(mainScheduler)
            .subscribeBy(
                onSuccess = { campaignCard ->
                    val card = createCard(host, campaignCard)
                    host.showAnnouncementCard(card)
                },
                onError = { t -> Timber.e(t) }
            )
    }

    private fun createCard(host: AnnouncementHost, cardType: SunriverCardType) =
        AnnouncementCard(
            style = AnnouncementStyle.ImageRight,
            title = R.string.buy_sell_verify_your_identity,
            description = R.string.kyc_drop_off_card_description,
            link = R.string.kyc_drop_off_card_button,
            image = R.drawable.vector_kyc_onboarding,
            closeFunction = {
                dismissEntry.dismiss(DismissRule.DismissForSession)
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
            },
            linkFunction = {
                val campaignType = if (cardType == SunriverCardType.FinishSignUp) {
                    CampaignType.Sunriver
                } else {
                    CampaignType.Swap
                }
                host.startKyc(campaignType)
            },
            prefsKey = dismissEntry.prefsKey
        )

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KYC_INCOMPLETE_DISMISSED"
    }
}
