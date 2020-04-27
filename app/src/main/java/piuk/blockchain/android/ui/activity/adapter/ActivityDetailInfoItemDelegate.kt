package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_activity_detail_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsInfoType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ActivityDetailInfoItemDelegate <in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as Pair<ActivityDetailsInfoType, String>
        return item.first != ActivityDetailsInfoType.ACTION && item.first != ActivityDetailsInfoType.DESCRIPTION
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InfoItemViewHolder(parent.inflate(R.layout.item_activity_detail_info))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InfoItemViewHolder).bind(
        items[position] as Pair<ActivityDetailsInfoType, String>
    )
}

private class InfoItemViewHolder(var parent: View) : RecyclerView.ViewHolder(parent), LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: Pair<ActivityDetailsInfoType, String>) {
        itemView.item_activity_detail_title.text = getStringForInfoType(item.first)
        itemView.item_activity_detail_description.text = item.second
    }

    private fun getStringForInfoType(infoType: ActivityDetailsInfoType): String =
        parent.context.getString(
            when (infoType) {
                ActivityDetailsInfoType.CREATED -> R.string.activity_details_created
                ActivityDetailsInfoType.COMPLETED -> R.string.activity_details_completed
                ActivityDetailsInfoType.AMOUNT -> R.string.activity_details_amount
                ActivityDetailsInfoType.FEE -> R.string.activity_details_fee
                ActivityDetailsInfoType.VALUE -> R.string.activity_details_value
                else -> R.string.activity_details_empty
            }
        )
}
