package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.layout_crypto_currency_chooser_item.view.*
import kotlinx.android.synthetic.main.simple_buy_ctypto_currency_chooser.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.drawableResFilled

class CryptoCurrencyChooserBottomSheet : SlidingModalBottomDialog() {

    override val layoutResource: Int
        get() = R.layout.simple_buy_ctypto_currency_chooser

    override fun initControls(view: View) {
        view.recycler.adapter =
            BottomSheetCryptoCurrenciesAdapter(
                CryptoCurrency.activeCurrencies()
                    .map {
                        BottomSheetAdapterItem(it) {
                            (parentFragment as? CurrencyChangeListener)?.onCurrencyChanged(it)
                            dismiss()
                        }
                    })
        view.recycler.layoutManager = LinearLayoutManager(context)
    }
}

private class BottomSheetCryptoCurrenciesAdapter(private val adapterItems: List<BottomSheetAdapterItem>) :
    RecyclerView.Adapter<BottomSheetCryptoCurrenciesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.layout_crypto_currency_chooser_item,
                parent,
                false
            )
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int =
        adapterItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = adapterItems[position]
        holder.iconView.setImageResource(item.cryptoCurrency.drawableResFilled())
        holder.textView.text = item.cryptoCurrency.unit
        holder.container.setOnClickListener { item.clickAction() }
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: AppCompatImageView = itemView.icon
        val textView: AppCompatTextView = itemView.text
        val container: View = itemView.container
    }
}

private data class BottomSheetAdapterItem(val cryptoCurrency: CryptoCurrency, val clickAction: () -> Unit)
