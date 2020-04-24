package piuk.blockchain.android.ui.activity.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_activity_detail_action.view.*
import piuk.blockchain.android.R

const val DESCRIPTION_ITEM = -54321
const val ACTION_ITEM = -654321

const val INFO_TYPE = 1
const val DESCRIPTION_TYPE = 2
const val ACTION_TYPE = 3

class ActivityDetailsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var itemList: List<Pair<Int, String>> =
        emptyList()
        set(value) {
            field =
                value
            notifyDataSetChanged()
        }

    lateinit var actionItemClicked: (View) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            INFO_TYPE -> {
                InfoItemViewHolder(LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_activity_detail_info,
                        parent,
                        false
                    ))
            }
            DESCRIPTION_TYPE -> DescriptionItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_activity_detail_description,
                    parent,
                    false
                ))
            ACTION_TYPE -> ActionItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_activity_detail_action,
                    parent,
                    false
                ))
            else -> InfoItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_activity_detail_info,
                    parent,
                    false
                )) // TODO empty state?
        }
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is InfoItemViewHolder -> holder.bind(itemList[position])
            is DescriptionItemViewHolder -> holder.bind("Description TODO")
            is ActionItemViewHolder -> holder.bind(actionItemClicked)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position].first) {
            DESCRIPTION_ITEM -> DESCRIPTION_TYPE
            ACTION_ITEM -> ACTION_TYPE
            else -> INFO_TYPE
        }
    }

    class InfoItemViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        fun bind(item: Pair<Int, String>) {
        }
    }

    class DescriptionItemViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        fun bind(s: String) {
        }
    }

    class ActionItemViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        fun bind(actionItemClicked: (View) -> Unit) {
            itemView.activity_details_action.setOnClickListener(actionItemClicked)
        }
    }
}