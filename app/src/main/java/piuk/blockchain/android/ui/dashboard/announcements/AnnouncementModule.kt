package piuk.blockchain.android.ui.dashboard.announcements

import org.koin.dsl.module.applicationContext

val dashboardAnnouncementsModule = applicationContext {

    context("Payload") {

        factory { DashboardAnnouncements(get()) }

        factory { StellarModalPopupAnnouncement(get(), get()) }
    }

    factory { DismissRecorder(get()) }
}
