package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.dialog_activities_tx_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.toFormattedDate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.Date

class SwapActivityItemDelegate<in T>(
    private val onItemClicked: (CryptoCurrency, String, Boolean) -> Unit // crypto, txID, isCustodial
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is SwapActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SwapActivityItemViewHolder(parent.inflate(R.layout.dialog_activities_tx_item))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as SwapActivityItemViewHolder).bind(
        items[position] as SwapActivitySummaryItem,
        onItemClicked
    )
}

private class SwapActivityItemViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        tx: SwapActivitySummaryItem,
        onAccountClicked: (CryptoCurrency, String, Boolean) -> Unit
    ) {
        with(itemView) {
            icon.setIcon(/*tx.status*/)
            tx_type.setTxLabel(tx.cryptoCurrency, tx.targetValue.currency)

            status_date.setTxStatus(tx)
//            setTextColours(tx.status)

//            asset_balance_fiat.text = tx.fundedFiat.toStringWithSymbol()

            asset_balance_crypto.text = tx.totalCrypto.toStringWithSymbol()
            setOnClickListener { onAccountClicked(tx.cryptoCurrency, tx.txId, true) }
        }
    }

//    private fun setTextColours(txStatus: OrderState) {
//        with(itemView) {
//            if (txStatus == OrderState.FINISHED) {
//                tx_type.setTextColor(ContextCompat.getColor(context, R.color.black))
//                status_date.setTextColor(ContextCompat.getColor(context, R.color.black))
//                asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.black))
//                asset_balance_crypto.setTextColor(ContextCompat.getColor(context, R.color.black))
//            } else {
//                tx_type.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
//                status_date.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
//                asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
//                asset_balance_crypto.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
//            }
//        }
//    }
}

private fun ImageView.setIcon(/*status: OrderState*/) =
    setImageDrawable(
        AppCompatResources.getDrawable(
            context,
            R.drawable.ic_tx_swap
        )
    )

private fun TextView.setTxLabel(fromCrypto: CryptoCurrency, toCrypto: CryptoCurrency) {
    text = context.resources.getString(R.string.tx_title_swap, fromCrypto.displayTicker, toCrypto.displayTicker)
}

private fun TextView.setTxStatus(tx: SwapActivitySummaryItem) {
    text =Date(tx.timeStampMs).toFormattedDate()
//
//        OrderState.FINISHED -> Date(tx.timeStampMs).toFormattedDate()
//        OrderState.UNINITIALISED -> context.getString(R.string.activity_state_uninitialised)
//        OrderState.INITIALISED -> context.getString(R.string.activity_state_initialised)
//        OrderState.AWAITING_FUNDS -> context.getString(R.string.activity_state_awaiting_funds)
//        OrderState.PENDING_EXECUTION -> context.getString(R.string.activity_state_pending)
//        OrderState.PENDING_CONFIRMATION -> context.getString(R.string.activity_state_pending)
//        OrderState.UNKNOWN -> context.getString(R.string.activity_state_unknown)
//        OrderState.CANCELED -> context.getString(R.string.activity_state_canceled)
//        OrderState.FAILED -> context.getString(R.string.activity_state_failed)
//    }
}

//UNKNOWN("unknown"),
//NO_DEPOSITS("no_deposits"),
//RECEIVED("received"),
//COMPLETE("complete"),
//FAILED("failed"),
//RESOLVED("resolved"),
//REFUNDED("refunded"),
//REFUND_IN_PROGRESS("refund_in_progress"),
//EXPIRED("expired"),
//IN_PROGRESS("in_progress");