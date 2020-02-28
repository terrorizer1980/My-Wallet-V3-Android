package piuk.blockchain.android.ui.transactions.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemBalanceAccountDropdownBinding
import piuk.blockchain.android.databinding.SpinnerBalanceHeaderBinding
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.formatDisplayBalance
import piuk.blockchain.androidcore.data.currency.CurrencyState
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
        convertView: View,
        parent: ViewGroup
    ): View {
        return getCustomView(position, parent, true)
    }

    override fun getView(
        position: Int,
        convertView: View,
        parent: ViewGroup
    ): View {
        return getCustomView(position, parent, false)
    }

    private fun getCustomView(
        position: Int,
        parent: ViewGroup,
        isDropdownView: Boolean
    ): View {
        return if (isDropdownView) {
            val binding =
                DataBindingUtil.inflate<ItemBalanceAccountDropdownBinding>(
                    LayoutInflater.from(context),
                    R.layout.item_balance_account_dropdown,
                    parent,
                    false
                )
            val item = getItem(position)
            binding.accountName.text = item!!.label
            binding.balance.text = item.formatDisplayBalance(currencyState, exchangeRates)
            binding.root
        } else {
            val binding =
                DataBindingUtil.inflate<SpinnerBalanceHeaderBinding>(
                    LayoutInflater.from(context),
                    R.layout.spinner_balance_header,
                    parent,
                    false
                )
            val item = getItem(position)
            binding.text.text = item!!.label
            binding.root
        }
    }

    fun updateAccountList(accountList: List<ItemAccount>) {
        clear()
        addAll(accountList)
        notifyDataSetChanged()
    }

    val isNotEmpty: Boolean
        get() = accountList.isNotEmpty()

    fun showSpinner(): Boolean {
        return accountList.size > 1
    }
}