package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_checkout_item.view.*
import piuk.blockchain.android.R
import kotlin.properties.Delegates

class CheckoutAdapter : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    var items: List<CheckoutItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val key: TextView = itemView.key
        val value: TextView = itemView.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            R.layout.layout_checkout_item,
            parent,
            false
        )
        return ViewHolder(layout)
    }

    override fun getItemCount(): Int =
        items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            val item = items[position]
            key.text = items[position].key
            value.text = items[position].value
        }
    }
}

data class CheckoutItem(val key: String, val value: String)