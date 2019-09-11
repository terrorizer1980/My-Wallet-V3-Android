package piuk.blockchain.android.ui.dashboard.announcements.delegates

import android.annotation.SuppressLint
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_announcement_standard.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.isVisible
import android.graphics.drawable.GradientDrawable

class AnnouncementDelegate<in T> : AdapterDelegate<T> {

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        val announcement = items[position] as AnnouncementCard

        (holder as AnnouncementViewHolder).apply {
            if (announcement.titleText != 0) {
                title?.setText(announcement.titleText)
            } else {
                title?.gone()
            }

            if (announcement.bodyText != 0) {
                body?.setText(announcement.bodyText)
            } else {
                body?.gone()
            }

            if (announcement.iconImage != 0) {
                icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, announcement.iconImage))
            } else {
                icon.gone()
            }

            if (announcement.ctaText != 0) {
                ctaBtn.setText(announcement.ctaText)
                ctaBtn.setOnClickListener { announcement.ctaClicked() }
            } else {
                ctaBtn.gone()
            }

            if (announcement.dismissText != 0) {
                dismissBtn?.setText(announcement.dismissText)
                dismissBtn?.setOnClickListener { announcement.dismissClicked() }
                closeBtn.gone()
            } else {
                dismissBtn.gone()
            }

            if (announcement.dismissRule != DismissRule.CardPersistent) {
                closeBtn.setOnClickListener { announcement.dismissClicked() }
            } else {
                closeBtn.gone()
                dismissBtn.gone()
            }

            paintButtons(announcement.buttonColor)
        }
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position]
        return (item is AnnouncementCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AnnouncementViewHolder(parent.inflate(R.layout.item_announcement_standard))

    private class AnnouncementViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal val icon: ImageView = itemView.icon
        internal val title: TextView? = itemView.msg_title
        internal val body: TextView? = itemView.msg_body
        internal val closeBtn: ImageView = itemView.btn_close
        internal val ctaBtn: TextView = itemView.btn_cta1
        internal val dismissBtn: TextView? = itemView.btn_dismiss

        fun paintButtons(@ColorRes btnColour: Int) {
            val colour = ContextCompat.getColor(ctaBtn.context, btnColour)
            if (ctaBtn.isVisible()) {
                ctaBtn.setBackgroundColor(colour)
            }

            if (dismissBtn.isVisible()) {
                val bgColour = ContextCompat.getColor(ctaBtn.context, R.color.announce_background)
                val gd = GradientDrawable()
                gd.setColor(bgColour)
                gd.setStroke(2, colour)
                dismissBtn?.background = gd
                dismissBtn?.setTextColor(colour)
            }
        }
    }
}
