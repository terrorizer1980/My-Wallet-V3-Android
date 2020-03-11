package piuk.blockchain.android.ui.transactions.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_transaction.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.DateUtil
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class ActivityItemDelegate<in T>(
    activity: AppCompatActivity,
    private var showCrypto: Boolean,
    private val listClickListener: TxFeedClickListener,
    private val selectedFiatCurrency: String
) : AdapterDelegate<T> {

    private val dateUtil = DateUtil(activity)

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is ActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        TxViewHolder(
            parent.inflate(
                R.layout.item_transaction
            )
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {

        val viewHolder = holder as TxViewHolder
        val tx = items[position] as ActivitySummaryItem

        viewHolder.timeSince.text = dateUtil.formatted(tx.timeStamp)

        tx.formatting()
            .applyTransactionFormatting(viewHolder)

        tx.note?.let {
            viewHolder.note.text = it
            viewHolder.note.visible()
        } ?: viewHolder.note.gone()

        viewHolder.result.text = if (showCrypto) {
            tx.totalCrypto.toStringWithSymbol()
        } else {
            tx.totalFiat(selectedFiatCurrency)
                .toStringWithSymbol()
        }

        viewHolder.watchOnly.goneIf(!tx.watchOnly)
        viewHolder.doubleSpend.goneIf(!tx.doubleSpend)

        // TODO: Move this click listener to the ViewHolder to avoid unnecessary object instantiation during binding
        viewHolder.result.setOnClickListener {
            showCrypto = !showCrypto
            listClickListener.onValueClicked(showCrypto)
        }

        // TODO: Move this click listener to the ViewHolder to avoid unnecessary object instantiation during binding
        viewHolder.itemView.setOnClickListener {
            listClickListener.onTransactionClicked(tx.cryptoCurrency, tx.hash)
        }
    }

    fun onViewFormatUpdated(showCrypto: Boolean) {
        this.showCrypto = showCrypto
    }

    private class TxViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var direction: TextView = itemView.direction
        internal var watchOnly: TextView = itemView.watch_only
        internal var doubleSpend: ImageView = itemView.double_spend_warning
        internal var note: TextView = itemView.tx_note
    }

    private fun ActivitySummaryFormatting.applyTransactionFormatting(viewHolder: TxViewHolder) {
        viewHolder.direction.setText(text)
        viewHolder.result.setBackgroundResource(valueBackground)
        viewHolder.direction.setTextColor(
            viewHolder.context.getResolvedColor(
                directionColour
            )
        )
    }
}
