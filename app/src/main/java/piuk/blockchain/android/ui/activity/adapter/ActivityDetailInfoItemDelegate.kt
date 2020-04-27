package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_activity_detail_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsInfoType
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsListItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ActivityDetailInfoItemDelegate <in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ActivityDetailsListItem
        return item.activityDetailsType != ActivityDetailsInfoType.ACTION && item.activityDetailsType != ActivityDetailsInfoType.DESCRIPTION
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InfoItemViewHolder(parent.inflate(R.layout.item_activity_detail_info))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InfoItemViewHolder).bind(
        items[position] as ActivityDetailsListItem
    )
}

private class InfoItemViewHolder(var parent: View) : RecyclerView.ViewHolder(parent), LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: ActivityDetailsListItem) {
        itemView.item_activity_detail_title.text = getStringForInfoType(item.activityDetailsType)
        itemView.item_activity_detail_description.text = item.itemValue
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
