package piuk.blockchain.android.ui.dashboard.adapter.delegates

import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.databinding.ItemAnnouncementPitBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class PitAnnouncementDelegate<in T> : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is PitAnnouncementCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemAnnouncementPitBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return PitAnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        (holder as PitAnnouncementViewHolder).bind(items[position] as PitAnnouncementCard)
    }

    private class PitAnnouncementViewHolder internal constructor(
        private val binding: ItemAnnouncementPitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: PitAnnouncementCard) {
            binding.pitCard = card
            val click = View.OnClickListener {
                card.linkFunction()
            }
            binding.textViewPitAnnouncementLink.setOnClickListener(click)
            binding.linkArrow.setOnClickListener(click)
            binding.executePendingBindings()
        }
    }
}

data class PitAnnouncementCard(
    @StringRes val description: Int,
    @StringRes val link: Int = 0,
    val closeFunction: () -> Unit,
    val linkFunction: () -> Unit
)