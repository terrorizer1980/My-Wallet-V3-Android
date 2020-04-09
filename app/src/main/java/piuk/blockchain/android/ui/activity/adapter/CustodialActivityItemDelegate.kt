package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.OrderState
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_activitiex_tx_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.toFormattedDate
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import java.util.Date

class CustodialActivityItemDelegate<in T>(
    private val disposables: CompositeDisposable,
    private val currencyPrefs: CurrencyPrefs,
    private val onItemClicked: (CryptoCurrency, String, Boolean) -> Unit // crypto, txID, isCustodial
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CustodialActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialActivityItemViewHolder(parent.inflate(R.layout.dialog_activitiex_tx_item))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialActivityItemViewHolder).bind(
        items[position] as CustodialActivitySummaryItem,
        disposables,
        currencyPrefs.selectedFiatCurrency,
        onItemClicked
    )
}

private class CustodialActivityItemViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        tx: CustodialActivitySummaryItem,
        disposables: CompositeDisposable,
        fiatCurrency: String,
        onAccountClicked: (CryptoCurrency, String, Boolean) -> Unit
    ) {
        with(itemView) {
            icon.setIcon(tx.status)
            tx_type.setTxLabel(tx.cryptoCurrency)

            status_date.setTxStatus(tx)
            setTextColours(tx.status)

            asset_balance_fiat.gone()
            asset_balance_crypto.text = tx.totalCrypto.toStringWithSymbol()
            disposables += tx.totalFiatWhenExecuted(fiatCurrency)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        asset_balance_fiat.text = it.toStringWithSymbol()
                        asset_balance_fiat.visible()
                    },
                    onError = {
                        Timber.e("Cannot convert to fiat")
                    }
                )

            setOnClickListener { onAccountClicked(tx.cryptoCurrency, tx.txId, true) }
        }
    }

    private fun setTextColours(txStatus: OrderState) {
        with(itemView) {
            if (txStatus == OrderState.FINISHED) {
                tx_type.setTextColor(ContextCompat.getColor(context, R.color.black))
                status_date.setTextColor(ContextCompat.getColor(context, R.color.black))
                asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.black))
                asset_balance_crypto.setTextColor(ContextCompat.getColor(context, R.color.black))
            } else {
                tx_type.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                status_date.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                asset_balance_crypto.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
            }
        }
    }
}

private fun ImageView.setIcon(status: OrderState) =
    setImageDrawable(
        AppCompatResources.getDrawable(
            context,
            when (status) {
                OrderState.FINISHED -> R.drawable.ic_tx_buy
                OrderState.AWAITING_FUNDS,
                OrderState.PENDING_EXECUTION -> R.drawable.ic_tx_confirming
                OrderState.UNINITIALISED, // should not see these next ones ATM
                OrderState.INITIALISED,
                OrderState.CANCELED,
                OrderState.FAILED -> R.drawable.ic_tx_buy
            }
        )
    )

private fun TextView.setTxLabel(cryptoCurrency: CryptoCurrency) {
    text = context.resources.getString(R.string.tx_title_buy, cryptoCurrency.displayTicker)
}

private fun TextView.setTxStatus(tx: CustodialActivitySummaryItem) {
    text = when (tx.status) {
        OrderState.FINISHED -> Date(tx.timeStampMs).toFormattedDate()
        OrderState.UNINITIALISED -> context.getString(R.string.activity_state_uninitialised)
        OrderState.INITIALISED -> context.getString(R.string.activity_state_initialised)
        OrderState.AWAITING_FUNDS -> context.getString(R.string.activity_state_awaiting_funds)
        OrderState.PENDING_EXECUTION -> context.getString(R.string.activity_state_pending)
        OrderState.CANCELED -> context.getString(R.string.activity_state_canceled)
        OrderState.FAILED -> context.getString(R.string.activity_state_failed)
    }
}
