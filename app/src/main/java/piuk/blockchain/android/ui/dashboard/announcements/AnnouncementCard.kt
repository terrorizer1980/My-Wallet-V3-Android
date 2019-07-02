package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

enum class AnnouncementStyle {
    ImageLeft,
    ImageRight,
    Swap,
    StableCoin,
    Sunriver,
    ThePit
}

data class AnnouncementCard(
    val style: AnnouncementStyle,
    @StringRes val title: Int = 0,
    @StringRes val description: Int = 0,
    @StringRes val link: Int = 0,
    @DrawableRes val image: Int = 0,
    val closeFunction: () -> Unit,
    val linkFunction: () -> Unit = { },
    val prefsKey: String
)
