package piuk.blockchain.android.ui.dashboard.announcements.delegates

import android.annotation.SuppressLint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_announcement_left_icon.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementStyle
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate

sealed class AnnouncementDelegate<in T> : AdapterDelegate<T> {

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        val announcement = items[position] as AnnouncementCard

        (holder as AnnouncementViewHolder).apply {
            if (announcement.title > 0) {
                title?.setText(announcement.title)
            } else {
                title?.gone()
            }

            if (announcement.description > 0) {
                description?.setText(announcement.description)
            } else {
                description?.gone()
            }

            if (announcement.image > 0) {
                image?.setImageDrawable(ContextCompat.getDrawable(itemView.context, announcement.image))
            } else {
                image?.gone()
            }

            if (announcement.link > 0) {
                link.setText(announcement.link)
            } else {
                link.gone()
            }

            close.setOnClickListener { announcement.closeFunction() }
            link.setOnClickListener { announcement.linkFunction() }
        }
    }

    final override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position]
        return if (item is AnnouncementCard) {
            isForAnnouncementStyle(item)
        } else {
            false
        }
    }

    protected abstract fun isForAnnouncementStyle(card: AnnouncementCard): Boolean

    protected class AnnouncementViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView? = itemView.textview_title
        internal val description: TextView? = itemView.textview_content
        internal val close: ImageView = itemView.imageview_close
        internal val link: TextView = itemView.textview_link
        internal val image: ImageView? = itemView.imageview_icon
    }
}

class ImageRightAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForAnnouncementStyle(card: AnnouncementCard) = card.style == AnnouncementStyle.ImageRight

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_right_icon))
}

class ImageLeftAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForAnnouncementStyle(card: AnnouncementCard) = card.style == AnnouncementStyle.ImageLeft

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_left_icon))
}

class StableCoinAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForAnnouncementStyle(card: AnnouncementCard) = card.style == AnnouncementStyle.StableCoin

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_stablecoin))
}

class PitAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForAnnouncementStyle(card: AnnouncementCard) = card.style == AnnouncementStyle.ThePit

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_pit))
}

class SwapAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForAnnouncementStyle(card: AnnouncementCard) = card.style == AnnouncementStyle.Swap

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_swap))
}

class SunriverAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForAnnouncementStyle(card: AnnouncementCard) = card.style == AnnouncementStyle.Sunriver

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_sunriver))
}
