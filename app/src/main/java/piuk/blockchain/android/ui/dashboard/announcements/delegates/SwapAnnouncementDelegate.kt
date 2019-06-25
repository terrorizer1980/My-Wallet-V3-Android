package piuk.blockchain.android.ui.dashboard.announcements.delegates

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.ItemAnnouncementSwapBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.announcements.SwapAnnouncementCard

class SwapAnnouncementDelegate<in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is SwapAnnouncementCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemAnnouncementSwapBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return SwapAnnouncementViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        (holder as SwapAnnouncementViewHolder).bind(items[position] as SwapAnnouncementCard)
    }

    private class SwapAnnouncementViewHolder internal constructor(
        private val binding: ItemAnnouncementSwapBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(swapAnnouncementCard: SwapAnnouncementCard) {
            binding.swapCard = swapAnnouncementCard
            binding.textViewSwapAnnouncementLink.setOnClickListener {
                swapAnnouncementCard.linkFunction()
            }
            binding.executePendingBindings()
        }
    }
}
