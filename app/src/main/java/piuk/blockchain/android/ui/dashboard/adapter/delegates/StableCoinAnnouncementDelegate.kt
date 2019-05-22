package piuk.blockchain.android.ui.dashboard.adapter.delegates

import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.databinding.ItemAnnouncementStablecoinBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class StableCoinAnnouncementDelegate<in T> : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is StableCoinAnnouncementCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemAnnouncementStablecoinBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return StableCoinAnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        (holder as StableCoinAnnouncementViewHolder).bind(items[position] as StableCoinAnnouncementCard)
    }

    private class StableCoinAnnouncementViewHolder internal constructor(
        private val binding: ItemAnnouncementStablecoinBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: StableCoinAnnouncementCard) {
            binding.stableCoinCard = card
            val click = View.OnClickListener {
                card.linkFunction()
            }
            binding.textViewStablecoinAnnouncementLink.setOnClickListener(click)
            binding.linkArrow.setOnClickListener(click)
            binding.executePendingBindings()
        }
    }
}

data class StableCoinAnnouncementCard(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val link: Int? = null,
    val isNew: Boolean,
    val closeFunction: () -> Unit,
    val linkFunction: () -> Unit
)