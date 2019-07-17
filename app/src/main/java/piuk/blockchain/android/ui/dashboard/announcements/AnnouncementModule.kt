@file:Suppress("USELESS_CAST")

package piuk.blockchain.android.ui.dashboard.announcements

import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.applicationContext

val dashboardAnnouncementsModule = applicationContext {

    context("Payload") {

        bean {
            AnnouncementList(
                mainScheduler = AndroidSchedulers.mainThread()
            ).apply {
                add(get("kycresubmission")) // Always first
                add(get("pit"))
                add(get("stablecoin"))
                add(get("coinify"))
                add(get("profile"))
                add(get("claim"))
                add(get("swap"))
                add(get("kycincomplete")) // Always last
            }
        }

        factory("kycresubmission") {
            KycResubmissionAnnouncementRule(
                kycTiersQueries = get(),
                dismissRecorder = get()
            ) as AnnouncementRule
        }

        factory("kycincomplete") {
            KycIncompleteAnnouncementRule(
                kycTiersQueries = get(),
                sunriverCampaignHelper = get(),
                dismissRecorder = get(),
                mainScheduler = AndroidSchedulers.mainThread()
            ) as AnnouncementRule
        }

        factory("coinify") {
            CoinifyKycModalPopupAnnouncementRule(
                tierService = get(),
                coinifyWalletService = get(),
                showPopupFeatureFlag = get("ff_notify_coinify_users_to_kyc"),
                dismissRecorder = get()
            ) as AnnouncementRule
        }

        factory("stellar") {
            StellarModalPopupAnnouncementRule(
                tierService = get(),
                dismissRecorder = get(),
                showPopupFeatureFlag = get("ff_get_free_xlm_popup")
            ) as AnnouncementRule
        }

        factory("profile") {
            GoForGoldAnnouncementRule(
                tierService = get(),
                prefs = get(),
                dismissRecorder = get()
            ) as AnnouncementRule
        }

        factory("pit") {
            PitAnnouncementRule(
                pitLink = get(),
                dismissRecorder = get()
            ) as AnnouncementRule
        }

        factory("claim") {
            ClaimYourFreeCryptoAnnouncementRule(
                tierService = get(),
                sunriverCampaignHelper = get(),
                dismissRecorder = get()
            ) as AnnouncementRule
        }

        factory("stablecoin") {
            StableCoinIntroAnnouncementRule(
                featureEnabled = get("ff_stablecoin"),
                config = get(),
                analytics = get(),
                dismissRecorder = get()
            ) as AnnouncementRule
        }

        factory("swap") {
            SwapAnnouncementRule(
                tierService = get(),
                dataManager = get("merge"),
                dismissRecorder = get()
            ) as AnnouncementRule
        }
    }

    bean { DismissRecorder(prefs = get()) }
}
