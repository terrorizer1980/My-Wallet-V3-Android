package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import piuk.blockchain.android.R

interface AnnouncementCard {
    val title: Int
    val description: Int
    val link: Int
    val image: Int
    val closeFunction: () -> Unit
    val linkFunction: () -> Unit
    val prefsKey: String
}

// We're moving away from this style in the future
data class ImageLeftAnnouncementCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int,
    @DrawableRes override val image: Int,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,
    override val prefsKey: String
) : AnnouncementCard

data class ImageRightAnnouncementCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int,
    @DrawableRes override val image: Int,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,
    override val prefsKey: String
) : AnnouncementCard

data class SwapAnnouncementCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int = 0,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,

    override val prefsKey: String,
    @DrawableRes override val image: Int = 0
) : AnnouncementCard

data class StableCoinAnnouncementCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int = 0,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,

    override val prefsKey: String,
    @DrawableRes override val image: Int = 0
) : AnnouncementCard

data class SunriverCard(
    @StringRes override val title: Int,
    @StringRes override val description: Int,
    @StringRes override val link: Int = 0,
    @DrawableRes override val image: Int = R.drawable.vector_xlm_colored,
    override val closeFunction: () -> Unit,
    override val linkFunction: () -> Unit,

    override val prefsKey: String
) : AnnouncementCard

// TODO: Ideally we'd have something like:
// data class AnnouncementCard(
//    @StringRes val title: Int,
//    @StringRes val description: Int,
//    val prefsKey: String,
//    val closeFunction: () -> Unit = { },
//    val linkFunction: () -> Unit = { },
//    @StringRes val link: Int = 0,
//    @DrawableRes val image: Int = 0,
//    val style: AnnouncmentStyle = AnnouncmentStyle.ImageLeft
// )
//
// enum class AnnouncmentStyle {
//    ImageLeft,
//    ImageRight,
//    Swap,
//    StableCoin,
//    Sunriver
// }
//
// But first all the data binding in the xml needs to be removed. And the delegates updated.