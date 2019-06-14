package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.announcement.AnnouncementList
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.dashboard.DashboardPresenter

val dashboardAnnouncementsModule = applicationContext {

    context("Payload") {

        factory {
            DashboardAnnouncements(AnnouncementList<DashboardPresenter>().apply {
                add(get("coinify"))
                add(get("stellar"))
                add(get("profile"))
                add(get("claim"))
                add(get("stablecoin"))
                add(get("swap"))
                add(get("pit"))
            })
        }

        bean("coinify") {
            CoinifyKycModalPopupAnnouncement(get(),
                get(),
                get("ff_notify_coinify_users_to_kyc")) as Announcement<DashboardPresenter>
        }

        factory("stellar") {
            StellarModalPopupAnnouncement(
                tierService = get(),
                dismissRecorder = get(),
                showPopupFeatureFlag = get("ff_get_free_xlm_popup")
            ) as Announcement<DashboardPresenter>
        }

        factory("profile") { CompleteYourProfileCardAnnouncement(get(), get()) as Announcement<DashboardPresenter> }

        factory("pit") { PitAnnouncement(get()) as Announcement<DashboardPresenter> }

        factory("claim") {
            ClaimYourFreeCryptoCardAnnouncement(get(),
                get(),
                get()) as Announcement<DashboardPresenter>
        }

        factory("stablecoin") { StableCoinIntroductionAnnouncement(get(), get()) as Announcement<DashboardPresenter> }

        factory("swap") { SwapAnnouncement(get(), get("merge"), get()) as Announcement<DashboardPresenter> }
    }

    factory { DismissRecorder(get()) }
}
