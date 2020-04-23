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
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.base.mvi.MviFragment

class ActivityDetailsBottomSheet : MviBottomSheet<ActivityDetailsModel, ActivityDetailsIntents, ActivityDetailState>() {
    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    override val model: ActivityDetailsModel by inject()
    lateinit var parent: View


    override fun render(newState: ActivityDetailState) {
        parent.title.text = newState.nonCustodialActivitySummaryItem?.cryptoCurrency?.networkTicker
    }

    override fun initControls(view: View) {
       parent = view
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
        private const val ARG_CRYPTO_CURRENCY = "crypto_currency"
        private const val ARG_TRANSACTION_HASH = "tx_hash"

        fun newInstance(cryptoCurrency: CryptoCurrency, txHash: String): ActivityDetailsBottomSheet {
            return ActivityDetailsBottomSheet().apply {
                loadActivityDetails(cryptoCurrency, txHash)
                /*arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                    putString(ARG_TRANSACTION_HASH, txHash)
                }*/
            }
        }
    }

}