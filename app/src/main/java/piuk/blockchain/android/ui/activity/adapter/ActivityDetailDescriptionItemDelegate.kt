package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsInfoType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ActivityDetailDescriptionItemDelegate<in T>(
    private val onDescriptionItemClicked: () -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as Pair<ActivityDetailsInfoType, String>
        return item.first == ActivityDetailsInfoType.DESCRIPTION
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        DescriptionItemViewHolder(parent.inflate(R.layout.item_activity_detail_description))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as DescriptionItemViewHolder).bind(
        items[position] as Pair<ActivityDetailsInfoType, String>,
        onDescriptionItemClicked
    )
}

private class DescriptionItemViewHolder(var parent: View) : RecyclerView.ViewHolder(parent),
    LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: Pair<ActivityDetailsInfoType, String>, onDescriptionClicked: () -> Unit) {
        // TODO click for description
    }
}