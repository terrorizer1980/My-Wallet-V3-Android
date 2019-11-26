package piuk.blockchain.android.ui.dashboard.announcements

import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.blockchain.notifications.analytics.Analytics
import kotlinx.android.synthetic.main.item_announcement_mini.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class MiniAnnouncementDelegate<in T>(private val analytics: Analytics) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position]
        return item is MiniAnnouncementCard
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(
            parent.inflate(R.layout.item_announcement_mini)
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder, payloads: List<*>) {
        val announcement = items[position] as MiniAnnouncementCard
        (holder as AnnouncementViewHolder).apply {
            if (announcement.titleText != 0) {
                title.setText(announcement.titleText)
                title.visible()
            } else {
                title.gone()
            }
            if (announcement.bodyText != 0) {
                body.setText(announcement.bodyText)
                body.visible()
            } else {
                body.gone()
            }
            if (announcement.iconImage != 0) {
                icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, announcement.iconImage))
                icon.visible()
            } else {
                icon.gone()
            }

            if (announcement.hasCta) {
                cardContainer.setOnClickListener {
                    analytics.logEvent(AnnouncementAnalyticsEvent.CardActioned(announcement.name))
                    announcement.ctaClicked()
                }
                actionIcon.visible()
            } else {
                actionIcon.gone()
            }
        }
        analytics.logEvent(AnnouncementAnalyticsEvent.CardShown(announcement.name))
    }

    private class AnnouncementViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        internal val icon: ImageView = itemView.icon
        internal val title: TextView = itemView.msg_title
        internal val body: TextView = itemView.msg_body
        internal val cardContainer: ConstraintLayout = itemView.card_container
        internal val actionIcon: ImageView = itemView.action_icon
    }
}