package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.dialog_activities_tx_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.toFormattedDate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.Date

class SwapActivityItemDelegate<in T>(
    private val currencyPrefs: CurrencyPrefs,
    private val onItemClicked: (CryptoCurrency, String, Boolean) -> Unit // crypto, txID, isCustodial
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is SwapActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SwapActivityItemViewHolder(parent.inflate(R.layout.dialog_activities_tx_item), currencyPrefs)

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
    itemView: View,
    private val currencyPrefs: CurrencyPrefs
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        tx: SwapActivitySummaryItem,
        onAccountClicked: (CryptoCurrency, String, Boolean) -> Unit
    ) {
        with(itemView) {
            icon.setIcon(/*tx.status*/)
            tx_type.setTxLabel(tx.cryptoCurrency, tx.targetValue.currency)

            status_date.setTxStatus(tx)

            asset_balance_fiat.text = tx.fiatValue(currencyPrefs.selectedFiatCurrency).toStringWithSymbol()

            asset_balance_crypto.text = tx.cryptoValue.toStringWithSymbol()
            setOnClickListener { onAccountClicked(tx.cryptoCurrency, tx.txId, true) }
        }
    }
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
    text = Date(tx.timeStampMs).toFormattedDate()
}
