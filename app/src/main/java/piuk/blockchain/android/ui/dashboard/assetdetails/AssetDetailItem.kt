package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.extensions.exhaustive
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.format
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

    fun bind(item: AssetDetailItem, onActionSelected: AssetActionHandler) {
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
            setOnClickListenerDebounced { doShowMenu(item, onActionSelected) }

            asset_spend_locked.goneIf { item.assetFilter == AssetFilter.Wallet }

            asset_balance_crypto.text = item.crypto.format()
            asset_balance_fiat.text = item.fiat.toStringWithSymbol()
        }
    }

    // Can't use a standard PopupMenu here - because we don't get icon in popup menus until API29
    // However, the menubuilder approach with icons leads to a scrappy looking menu. What we'll
    // probably have to do to get this to look like the designs is to build a custom popup window, which is
    // a bit (lot!) annoying.
    // TODO:
    //      I'll come back to this later; for now it just needs to be working... and who knows
    //      in a day or three I may have had a better idea, or found a library or something :fingerscrossed:

    private fun doShowMenu(detailItem: AssetDetailItem, onActionSelected: AssetActionHandler) {

        val menuBuilder = MenuBuilder(itemView.context)
        val inflater = MenuInflater(itemView.context)
        inflater.inflate(R.menu.menu_asset_actions, menuBuilder)

        // enable available actions
        detailItem.tokens.actions(detailItem.assetFilter).forEach {
            menuBuilder.findItem(mapActionToMenuItem(it))?.isVisible = true
        }

        menuBuilder.isGroupDividerEnabled = true

        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuModeChange(menu: MenuBuilder?) {}

            override fun onMenuItemSelected(menu: MenuBuilder?, item: MenuItem?): Boolean =
                item?.run {
                    onActionSelected(
                        mapMenuItemToAction(item.itemId),
                        detailItem.assetFilter
                    )
                    true
                } ?: false
            }
        )

        val actionMenu = MenuPopupHelper(itemView.context, menuBuilder, itemView.action_menu)
        actionMenu.setOnDismissListener {
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
        }
        actionMenu.setForceShowIcon(true)
        actionMenu.show()

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
    private val onActionSelected: AssetActionHandler
) : RecyclerView.Adapter<AssetDetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetDetailViewHolder =
        AssetDetailViewHolder(parent.inflate(R.layout.dialog_dashboard_asset_detail_item))

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: AssetDetailViewHolder, position: Int) {
        holder.bind(itemList[position], onActionSelected)
    }
}
