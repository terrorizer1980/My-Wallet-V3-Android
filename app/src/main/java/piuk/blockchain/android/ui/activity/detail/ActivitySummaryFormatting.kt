package piuk.blockchain.android.ui.activity.detail

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem

internal fun NonCustodialActivitySummaryItem.formatting() =
    when (direction) {
        TransactionSummary.Direction.TRANSFERRED -> transferredFormatting(this)
        TransactionSummary.Direction.RECEIVED -> receivedFormatting(this)
        TransactionSummary.Direction.SENT -> if (!isFeeTransaction) sendFormatting(this) else paxFeeFormatting(this)
        TransactionSummary.Direction.BUY -> receivedFormatting(this)
        TransactionSummary.Direction.SELL -> sendFormatting(this)
        TransactionSummary.Direction.SWAP -> transferredFormatting(this)
    }

internal class ActivitySummaryFormatting(
    @StringRes
    val text: Int,

    @ColorRes
    val directionColour: Int,

    @DrawableRes
    val valueBackground: Int
)

private fun transferredFormatting(tx: NonCustodialActivitySummaryItem) =
    ActivitySummaryFormatting(
        text = R.string.tx_moved,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_transferred_50,
            R.drawable.rounded_view_transferred
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_grey_transferred_50,
            R.color.product_grey_transferred
        )
    )

private fun receivedFormatting(tx: NonCustodialActivitySummaryItem) =
    ActivitySummaryFormatting(
        text = R.string.tx_received,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_green_50,
            R.drawable.rounded_view_green
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_green_received_50,
            R.color.product_green_received
        )
    )

private fun sendFormatting(tx: NonCustodialActivitySummaryItem) =
    ActivitySummaryFormatting(
        text = R.string.tx_sent,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_red_50,
            R.drawable.rounded_view_red
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_red_sent_50,
            R.color.product_red_sent
        )
    )

private fun paxFeeFormatting(tx: NonCustodialActivitySummaryItem) =
    ActivitySummaryFormatting(
        text = R.string.pax_fee_1,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_red_50,
            R.drawable.rounded_view_red
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_red_sent_50,
            R.color.product_red_sent
        )
    )

private fun getColorForConfirmations(
    tx: NonCustodialActivitySummaryItem,
    @DrawableRes colorLight: Int,
    @DrawableRes colorDark: Int
) = if (tx.confirmations < tx.cryptoCurrency.requiredConfirmations) colorLight else colorDark