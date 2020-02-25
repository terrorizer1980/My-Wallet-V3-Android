package piuk.blockchain.android.ui.transactions.adapter

import androidx.appcompat.app.AppCompatActivity
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.androidcoreui.utils.extensions.autoNotify
import kotlin.properties.Delegates

class TxFeedAdapter(
    activity: AppCompatActivity,
    showCrypto: Boolean,
    listClickListener: TxFeedClickListener
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val summaryDelegate =
        ActivityItemDelegate<Any>(
            activity,
            showCrypto,
            listClickListener
        )

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(summaryDelegate)
        setHasStableIds(true)
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data
     */
    override var items: List<Any> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    /**
     * Notifies the adapter that the View format (ie, whether or not to show BTC) has been changed.
     * Will rebuild the entire adapter.
     */
    fun onViewFormatUpdated(showCrypto: Boolean) {
        summaryDelegate.onViewFormatUpdated(showCrypto)
        notifyDataSetChanged()
    }
}

interface TxFeedClickListener {
    fun onTransactionClicked(crypto: CryptoCurrency, txHash: String)
    fun onValueClicked(isCrypto: Boolean)
}