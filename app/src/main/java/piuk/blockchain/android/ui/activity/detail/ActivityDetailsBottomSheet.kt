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
import piuk.blockchain.android.ui.activity.adapter.ActivityDetailsDelegateAdapter
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet

class ActivityDetailsBottomSheet :
    MviBottomSheet<ActivityDetailsModel, ActivityDetailsIntents, ActivityDetailState>() {
    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    override val model: ActivityDetailsModel by inject()

    private val listAdapter: ActivityDetailsDelegateAdapter by lazy {
        ActivityDetailsDelegateAdapter(
            onActionItemClicked = { onActionItemClicked() },
            onDescriptionItemClicked = { onDescriptionItemClicked() }
        )
    }

    private val Bundle?.txId
        get() = this?.getString(ARG_TRANSACTION_HASH) ?: throw IllegalArgumentException(
            "Transaction id should not be null")

    private val Bundle?.cryptoCurrency
        get() = this?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("Cryptocurrency should not be null")

    override fun render(newState: ActivityDetailState) {
            dialogView.title.text = mapToAction(newState.direction)
            dialogView.amount.text = newState.amount?.toStringWithSymbol()
            dialogView.status.text =
                if (newState.isPending) getString(R.string.activity_details_label_pending) else getString(
                    R.string.activity_details_label_complete)

        if (listAdapter.items != newState.listOfItems) {
            listAdapter.items = newState.listOfItems.toList()
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun initControls(view: View) {
        view.details_list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = listAdapter
        }
    }

    private fun onDescriptionItemClicked() {
        // TODO
    }

    private fun onActionItemClicked() {
        val explorerUri = makeBlockExplorerUrl(arguments.cryptoCurrency, arguments.txId)

        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(explorerUri)
            startActivity(this)
        }
    }

    private fun loadActivityDetails(cryptoCurrency: CryptoCurrency, txHash: String) {
        model.process(LoadActivityDetailsIntent(cryptoCurrency, txHash))
    }

    private fun mapToAction(direction: TransactionSummary.Direction?): String =
        when (direction) {
            TransactionSummary.Direction.TRANSFERRED -> getString(
                R.string.activity_details_title_transferred)
            TransactionSummary.Direction.RECEIVED -> getString(
                R.string.activity_details_title_received)
            TransactionSummary.Direction.SENT -> getString(R.string.activity_details_title_sent)
            TransactionSummary.Direction.BUY -> getString(R.string.activity_details_title_buy)
            TransactionSummary.Direction.SELL -> getString(R.string.activity_details_title_sell)
            TransactionSummary.Direction.SWAP -> getString(R.string.activity_details_title_swap)
            else -> ""
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