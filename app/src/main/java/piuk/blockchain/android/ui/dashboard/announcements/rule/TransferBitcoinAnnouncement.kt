package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.support.annotation.VisibleForTesting
import com.blockchain.preferences.WalletStatus
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

class TransferBitcoinAnnouncement(
    dismissRecorder: DismissRecorder,
    private val walletStatus: WalletStatus
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return Single.just(!walletStatus.isWalletFunded)
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = AnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.transfer_btc_card_title,
                bodyText = R.string.transfer_btc_card_body,
                ctaText = R.string.transfer_btc_card_cta,
                iconImage = R.drawable.ic_announce_transfer_btc,
                dismissFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                ctaFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    host.startTransferCrypto()
                }
            )
        )
    }

    override val name = "transfer_btc"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "TransferBitcoinAuthAnnouncement_DISMISSED"
    }
}
