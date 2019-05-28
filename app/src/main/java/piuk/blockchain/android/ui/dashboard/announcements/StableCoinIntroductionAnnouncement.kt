package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.kycui.stablecoin.StableCoinCampaignHelper
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.android.ui.dashboard.adapter.delegates.StableCoinAnnouncementCard

class StableCoinIntroductionAnnouncement(
    private val stableCoinCampaignHelper: StableCoinCampaignHelper,
    dismissRecorder: DismissRecorder
) : Announcement<DashboardPresenter> {

    private val dismissEntry = dismissRecorder["StableCoinIntroductionCard_DISMISSED"]

    override fun shouldShow(dashboardPresenter: DashboardPresenter): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return stableCoinCampaignHelper.isEnabled()
    }

    override fun show(dashboardPresenter: DashboardPresenter) {
        dashboardPresenter.showStableCoinIntroduction(
            0, StableCoinAnnouncementCard(
                title = R.string.stablecoin_announcement_introducing_title,
                description = R.string.stablecoin_announcement_introducing_description,
                link = R.string.stablecoin_announcement_introducing_link,
                isNew = true,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    dashboardPresenter.dismissStableCoinIntroduction()
                },
                linkFunction = {
                    dashboardPresenter.exchangeRequested(CryptoCurrency.PAX)
                }
            )
        )
    }
}