package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

class MiniAnnouncementCard(
    override val name: String,
    val dismissRule: DismissRule,
    val dismissEntry: DismissRecorder.DismissEntry,
    @StringRes val titleText: Int = 0,
    @StringRes val bodyText: Int = 0,
    @DrawableRes val iconImage: Int = 0,
    private val ctaFunction: () -> Unit = { },
    val hasCta: Boolean
) : AnnouncementCard {
    fun ctaClicked() {
        ctaFunction.invoke()
    }

    override val dismissKey: String
        get() = ""
}