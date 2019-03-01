package piuk.blockchain.android.ui.dashboard.announcements

import org.koin.dsl.module.applicationContext

val dashboardAnnouncementsModule = applicationContext {

    context("Payload") {

        factory { DashboardAnnouncements(get(), get(), get()) }

        factory { StellarModalPopupAnnouncement(get(), get()) }

        factory { CompleteYourProfileCardAnnouncement(get(), get()) }

        factory { ClaimYourFreeCryptoCardAnnouncement(get(), get(), get()) }
    }

    factory { DismissRecorder(get()) }
}
