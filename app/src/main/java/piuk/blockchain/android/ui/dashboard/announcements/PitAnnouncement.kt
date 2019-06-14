package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.android.ui.dashboard.adapter.delegates.PitAnnouncementCard

class PitAnnouncement(dismissRecorder: DismissRecorder) : Announcement<DashboardPresenter> {

    private val dismissEntry = dismissRecorder["PitAnnouncement_DISMISSED"]

    override fun shouldShow(context: DashboardPresenter): Single<Boolean> {
        return Single.just(true)
    }

    override fun show(dashboardPresenter: DashboardPresenter) {
        dashboardPresenter.showPitAnnouncement(
            PitAnnouncementCard(
                description = R.string.pit_announcement_description,
                link = R.string.pit_announcement_introducing_link,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    dashboardPresenter.dismissPitAnnouncement()
                },
                linkFunction = {
                    dismissEntry.isDismissed = true
                    // TODO: Add link action
                }
            )
        )
    }
}
