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
import kotlinx.android.synthetic.main.dialog_activity_details_sheet.*
import kotlinx.android.synthetic.main.dialog_activity_details_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet

class ActivityDetailsBottomSheet :
    MviBottomSheet<ActivityDetailsModel, ActivityDetailsIntents, ActivityDetailState>() {
    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    override val model: ActivityDetailsModel by inject()
    private lateinit var parent: View
    private val listAdapter: ActivityDetailsAdapter by lazy { ActivityDetailsAdapter() }
    private val listLayoutManager: RecyclerView.LayoutManager by lazy {
        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    override fun render(newState: ActivityDetailState) {
        newState.nonCustodialActivitySummaryItem?.run {
            parent.title.text = mapToAction(direction)
            parent.amount.text = totalCrypto.toStringWithSymbol()
            parent.status.text = if (isPending) "Pending" else "Complete"
        }
    }

    override fun initControls(view: View) {
        val crypto = arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as CryptoCurrency
        val txHash = arguments?.getString(ARG_TRANSACTION_HASH) ?: ""

        parent = view

        val explorerUri = makeBlockExplorerUrl(crypto, txHash)
        listAdapter.actionItemClicked = { _ ->
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(explorerUri)
                startActivity(this)
            }
        }

        details_list.apply {
            adapter = listAdapter
            layoutManager = listLayoutManager
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