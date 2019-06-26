@file:Suppress("USELESS_CAST")

package piuk.blockchain.android.ui.dashboard.announcements

import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.applicationContext

val dashboardAnnouncementsModule = applicationContext {

    context("Payload") {

        bean {
            AnnouncementList(
                dismissRecorder = get(),
                sunriverCampaignHelper = get(),
                kycTiersQueries = get(),
                mainScheduler = AndroidSchedulers.mainThread()
            ).apply {
                add(get("stablecoin"))
                add(get("coinify"))
                add(get("stellar"))
                add(get("profile"))
                add(get("claim"))
                add(get("swap"))
            }
        }

        bean("coinify") {
            CoinifyKycModalPopupAnnouncement(get(),
                get(),
                get("ff_notify_coinify_users_to_kyc")) as Announcement
        }

        factory("stellar") {
            StellarModalPopupAnnouncement(
                tierService = get(),
                dismissRecorder = get(),
                showPopupFeatureFlag = get("ff_get_free_xlm_popup")
            ) as Announcement
        }

        factory("profile") {
            GoForGoldAnnouncement(
                tierService = get(),
                prefs = get(),
                dismissRecorder = get()
            ) as Announcement
        }

        factory("claim") {
            ClaimYourFreeCryptoCardAnnouncement(get(),
                get(),
                get()) as Announcement
        }

        factory("stablecoin") {
            StableCoinIntroductionAnnouncement(
                featureEnabled = get("ff_stablecoin"),
                config = get(),
                analytics = get(),
                dismissRecorder = get()
            ) as Announcement
        }

        factory("swap") { SwapAnnouncement(get(), get("merge"), get()) as Announcement }
    }

    factory { DismissRecorder(get()) }
}
