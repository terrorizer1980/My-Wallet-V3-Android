package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.AnnouncementList
import piuk.blockchain.android.ui.dashboard.DashboardPresenter

class DashboardAnnouncements internal constructor(
    coinifyKycModalPopupAnnouncement: CoinifyKycModalPopupAnnouncement,
    stellarModalPopupAnnouncement: StellarModalPopupAnnouncement,
    completeYourProfileCardAnnouncement: CompleteYourProfileCardAnnouncement,
    claimYourFreeCryptoCardAnnouncement: ClaimYourFreeCryptoCardAnnouncement,
    swapAnnouncement: SwapAnnouncement
) {
    val announcementList = AnnouncementList<DashboardPresenter>()
        .add(coinifyKycModalPopupAnnouncement)
        .add(stellarModalPopupAnnouncement)
        .add(completeYourProfileCardAnnouncement)
        .add(claimYourFreeCryptoCardAnnouncement)
        .add(swapAnnouncement)
}
