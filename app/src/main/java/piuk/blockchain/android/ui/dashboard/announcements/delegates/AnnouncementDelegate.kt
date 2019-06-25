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
import piuk.blockchain.android.ui.dashboard.announcements.ImageLeftAnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.ImageRightAnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.StableCoinAnnouncementCard
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate

abstract class AnnouncementDelegate<in T> : AdapterDelegate<T> {

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        val announcement = items[position] as AnnouncementCard

        (holder as AnnouncementViewHolder).apply {
            title.setText(announcement.title)
            description.setText(announcement.description)

            if (announcement.image > 0) {
                image?.setImageDrawable(ContextCompat.getDrawable(holder.title.context, announcement.image))
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

    protected class AnnouncementViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView = itemView.textview_title
        internal val description: TextView = itemView.textview_content
        internal val close: ImageView = itemView.imageview_close
        internal val link: TextView = itemView.textview_link
        internal val image: ImageView? = itemView.imageview_icon
    }
}

class ImageRightAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {

    override fun isForViewType(items: List<T>, position: Int) = items[position] is ImageRightAnnouncementCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_right_icon))
}

class ImageLeftAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {

    override fun isForViewType(items: List<T>, position: Int) = items[position] is ImageLeftAnnouncementCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_left_icon))
}

class StableCoinAnnouncementDelegate<in T> : AnnouncementDelegate<T>() {
    override fun isForViewType(items: List<T>, position: Int) = items[position] is StableCoinAnnouncementCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_stablecoin))
}