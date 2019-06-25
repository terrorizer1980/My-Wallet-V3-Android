package piuk.blockchain.android.ui.dashboard.announcements.delegates

import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_announcement_sunriver.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.announcements.SunriverCard
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class SunriverAnnouncementDelegate<in T> : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is SunriverCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SunriverAnnouncementViewHolder(
            parent.inflate(R.layout.item_announcement_sunriver)
        )

    @Suppress("CascadeIf")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        (holder as SunriverAnnouncementViewHolder).bind(items[position] as SunriverCard)
    }

    private class SunriverAnnouncementViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var button: Button = itemView.button_call_to_action
        internal var title: TextView = itemView.text_view_sunriver_announcement_title
        internal var message: TextView = itemView.text_view_sunriver_announcement_message
        internal var close: ImageView = itemView.imageview_close

        fun bind(data: SunriverCard) {
            itemView.setOnClickListener { data.linkFunction() }
            button.setOnClickListener { data.linkFunction() }
            close.setOnClickListener { data.closeFunction() }
            button.setTextOrHide(data.link)
            title.setText(data.title)
            message.setText(data.description)
        }
    }
}

private fun TextView.setTextOrHide(@StringRes text: Int?) {
    text?.let { this.setText(text); visible() } ?: this.gone()
}
