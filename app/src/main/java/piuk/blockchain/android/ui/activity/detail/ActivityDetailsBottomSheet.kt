package piuk.blockchain.android.ui.activity.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.makeBlockExplorerUrl
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import kotlinx.android.synthetic.main.dialog_activity_details_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet

class ActivityDetailsBottomSheet :
    MviBottomSheet<ActivityDetailsModel, ActivityDetailsIntents, ActivityDetailState>() {
    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    override val model: ActivityDetailsModel by inject()
    private val listAdapter: ActivityDetailsAdapter by lazy { ActivityDetailsAdapter() }
    private val listLayoutManager: RecyclerView.LayoutManager by lazy {
        LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    private val Bundle?.txId
        get() = this?.getString(ARG_TRANSACTION_HASH) ?: throw IllegalArgumentException(
            "Transaction id should not be null")

    private val Bundle?.cryptoCurrency
        get() = this?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("Cryptocurrency should not be null")

    override fun render(newState: ActivityDetailState) {
        newState.nonCustodialActivitySummaryItem?.run {
            dialogView.title.text = mapToAction(direction)
            dialogView.amount.text = totalCrypto.toStringWithSymbol()
            dialogView.status.text = if (isPending) "Pending" else "Complete"
        }

        listAdapter.itemList = newState.listOfItems
    }

    override fun initControls(view: View) {
        val explorerUri = makeBlockExplorerUrl(arguments.cryptoCurrency, arguments.txId)
        listAdapter.actionItemClicked = { _ ->
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(explorerUri)
                startActivity(this)
            }
        }

        view.details_list.apply {
            layoutManager = listLayoutManager
            adapter = listAdapter
        }
    }

    private fun loadActivityDetails(cryptoCurrency: CryptoCurrency, txHash: String) {
        model.process(LoadActivityDetailsIntent(cryptoCurrency, txHash))
    }

    private fun mapToAction(direction: TransactionSummary.Direction): String =
        when (direction) {
            TransactionSummary.Direction.TRANSFERRED -> "Transfer"
            TransactionSummary.Direction.RECEIVED -> "Receive"
            TransactionSummary.Direction.SENT -> "Send"
            TransactionSummary.Direction.BUY -> "Buy"
            TransactionSummary.Direction.SELL -> "Sell"
            TransactionSummary.Direction.SWAP -> "Swap"
        }

    companion object {
        private const val ARG_CRYPTO_CURRENCY =
            "crypto_currency"
        private const val ARG_TRANSACTION_HASH =
            "tx_hash"

        fun newInstance(
            cryptoCurrency: CryptoCurrency,
            txHash: String
        ): ActivityDetailsBottomSheet {
            return ActivityDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                    putString(ARG_TRANSACTION_HASH, txHash)
                }

                loadActivityDetails(cryptoCurrency, txHash)
            }
        }
    }
}