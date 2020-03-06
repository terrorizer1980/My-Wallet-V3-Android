package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.extensions.exhaustive
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.CustodialBalanceClicked
import com.blockchain.notifications.analytics.CustodialBalanceSendClicked
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.dialog_dashboard_asset_detail_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.util.currencyName
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced

data class AssetDetailItem(
    val assetFilter: AssetFilter,
    val tokens: AssetTokens,
    val crypto: CryptoValue,
    val fiat: FiatValue
)

class AssetDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        item: AssetDetailItem,
        onActionSelected: AssetActionHandler,
        analytics: Analytics
    ) {
        with(itemView) {
            asset_icon.setCoinIcon(item.tokens.asset)
            asset_name.text = resources.getString(item.tokens.asset.currencyName())
            asset_filter_label.setText(
                when (item.assetFilter) {
                    AssetFilter.Total -> R.string.dashboard_asset_balance_total
                    AssetFilter.Wallet -> R.string.dashboard_asset_balance_wallet
                    AssetFilter.Custodial -> R.string.dashboard_asset_balance_custodial
                }
            )
            setOnClickListenerDebounced { doShowMenu(item, onActionSelected, analytics) }

            asset_spend_locked.goneIf { item.assetFilter == AssetFilter.Wallet }

            asset_balance_crypto.text = item.crypto.toStringWithSymbol()
            asset_balance_fiat.text = item.fiat.toStringWithSymbol()
        }
    }

    private fun doShowMenu(
        detailItem: AssetDetailItem,
        onActionSelected: AssetActionHandler,
        analytics: Analytics
    ) {
        val crypto = detailItem.tokens.asset

        if (detailItem.assetFilter == AssetFilter.Custodial) {
            analytics.logEvent(CustodialBalanceClicked(crypto))
        }

        PopupMenu(itemView.context, itemView.action_menu).apply {
            menuInflater.inflate(R.menu.menu_asset_actions, menu)

            // enable available actions
            detailItem.tokens.actions(detailItem.assetFilter).forEach {
                menu.findItem(mapActionToMenuItem(it))?.isVisible = true
            }

            MenuCompat.setGroupDividerEnabled(menu, true)

            setOnMenuItemClickListener {
                val filter = detailItem.assetFilter
                val action = mapMenuItemToAction(it.itemId)

                if (filter == AssetFilter.Custodial && action == AssetAction.Send) {
                    analytics.logEvent(CustodialBalanceSendClicked(crypto))
                }

                onActionSelected(action, filter)
                true
            }

            setOnDismissListener {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            gravity = Gravity.END
            show()
        }

        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.grey_000))
    }

    private fun mapMenuItemToAction(menuId: Int): AssetAction =
        when (menuId) {
            R.id.action_activity -> AssetAction.ViewActivity
            R.id.action_send -> AssetAction.Send
            R.id.action_receive -> AssetAction.Receive
            R.id.action_swap -> AssetAction.Swap
            else -> throw IllegalArgumentException("id maps to unknown action")
        }

    private fun mapActionToMenuItem(action: AssetAction): Int =
        when (action) {
            AssetAction.ViewActivity -> R.id.action_activity
            AssetAction.Send -> R.id.action_send
            AssetAction.Receive -> R.id.action_receive
            AssetAction.Swap -> R.id.action_swap
        }.exhaustive
}

typealias AssetActionHandler = (action: AssetAction, assetFilter: AssetFilter) -> Unit

internal class AssetDetailAdapter(
    private val itemList: List<AssetDetailItem>,
    private val onActionSelected: AssetActionHandler,
    private val analytics: Analytics
) : RecyclerView.Adapter<AssetDetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetDetailViewHolder =
        AssetDetailViewHolder(parent.inflate(R.layout.dialog_dashboard_asset_detail_item))

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: AssetDetailViewHolder, position: Int) {
        holder.bind(itemList[position], onActionSelected, analytics)
    }
}
