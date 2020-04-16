package piuk.blockchain.android.ui.transactions.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.item_balance_account_dropdown.view.*
import kotlinx.android.synthetic.main.spinner_balance_header.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.formatDisplayBalance
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class AccountsAdapter(
    context: Context,
    textViewResourceId: Int,
    private val accountList: List<ItemAccount>,
    private val currencyState: CurrencyState,
    private val exchangeRates: ExchangeRateDataManager
) : ArrayAdapter<ItemAccount>(context, textViewResourceId, accountList.toMutableList()) {

    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ) = getCustomView(position, parent, true)

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ) = getCustomView(position, parent, false)

    private fun getCustomView(
        position: Int,
        parent: ViewGroup,
        isDropdownView: Boolean
    ): View {
        val item = getItem(position)!!
        val inflater = LayoutInflater.from(context)

        return if (isDropdownView) {
            inflater.inflate(R.layout.item_balance_account_dropdown, parent, false).apply {
                account_name.text = item.label
                balance.text = item.formatDisplayBalance(currencyState, exchangeRates)
            }
        } else {
            inflater.inflate(R.layout.spinner_balance_header, parent, false).apply {
                text.text = item.label
            }
        }
    }

    fun updateAccountList(accountList: List<ItemAccount>) {
        clear()
        addAll(accountList)
        notifyDataSetChanged()
    }

    val isNotEmpty: Boolean
        get() = accountList.isNotEmpty()
}