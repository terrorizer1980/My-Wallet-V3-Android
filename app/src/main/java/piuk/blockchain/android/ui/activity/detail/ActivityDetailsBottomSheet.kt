package piuk.blockchain.android.ui.activity.detail

import android.os.Bundle
import android.view.View
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import kotlinx.android.synthetic.main.dialog_activity_details_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class ActivityDetailsBottomSheet : SlidingModalBottomDialog() {
    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    private val coincore: Coincore by inject()

    override fun initControls(view: View) {

        val crypto = arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as CryptoCurrency
        val txHash = arguments?.getString(ARG_TRANSACTION_HASH) ?: ""
        val nonCustodialActivitySummaryItem = coincore[crypto].findCachedActivityItem(txHash) as? NonCustodialActivitySummaryItem
        nonCustodialActivitySummaryItem?.let {
            view.title.text = "${mapToAction(it.direction)}"
        }

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
        private const val ARG_CRYPTO_CURRENCY = "crypto_currency"
        private const val ARG_TRANSACTION_HASH = "tx_hash"

        fun newInstance(cryptoCurrency: CryptoCurrency, txHash: String): ActivityDetailsBottomSheet {
            return ActivityDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                    putString(ARG_TRANSACTION_HASH, txHash)
                }
            }
        }
    }
}