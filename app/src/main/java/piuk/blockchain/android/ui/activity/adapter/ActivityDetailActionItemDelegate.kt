package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_activity_detail_action.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsInfoType
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsListItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ActivityDetailActionItemDelegate <in T>(
    private val onActionItemClicked: (View) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ActivityDetailsListItem
        return item.activityDetailsType == ActivityDetailsInfoType.ACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ActionItemViewHolder(parent.inflate(R.layout.item_activity_detail_action))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ActionItemViewHolder).bind(
        items[position] as ActivityDetailsListItem,
        onActionItemClicked
    )
}

private class ActionItemViewHolder(var parent: View) : RecyclerView.ViewHolder(parent),
    LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: ActivityDetailsListItem, actionItemClicked: (View) -> Unit) {
        itemView.activity_details_action.setOnClickListener(actionItemClicked)
    }
}