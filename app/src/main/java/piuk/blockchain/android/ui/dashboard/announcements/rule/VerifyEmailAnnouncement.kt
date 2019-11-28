package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class VerifyEmailAnnouncement(
    dismissRecorder: DismissRecorder,
    private val walletSettings: SettingsDataManager
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        return walletSettings.getSettings()
            .map { !it.isEmailVerified && it.email.isNotEmpty() }
            .singleOrError()
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = AnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPersistent,
                dismissEntry = dismissEntry,
                titleText = R.string.verify_email_card_title,
                bodyText = R.string.verify_email_card_body,
                ctaText = R.string.verify_email_card_cta,
                iconImage = R.drawable.ic_announce_verify_email,
                dismissFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                ctaFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    host.startVerifyEmail()
                }
            )
        )
    }

    override val name = "verify_email"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "VerifyEmailAnnouncement_DISMISSED"
    }
}
