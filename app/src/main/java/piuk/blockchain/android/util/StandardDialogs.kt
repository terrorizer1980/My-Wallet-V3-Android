package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AlertDialog
import piuk.blockchain.android.R

const val URL_BLOCKCHAIN_SUPPORT_PORTAL = "https://support.blockchain.com/"
const val URL_BLOCKCHAIN_PAX_FAQ = "https://support.blockchain.com/hc/en-us/sections/360004368351-USD-Pax-FAQ"
const val URL_BLOCKCHAIN_PAX_NEEDS_ETH_FAQ =
    "https://support.blockchain.com/hc/en-us/articles/360027492092-Why-do-I-need-ETH-to-send-my-PAX-"

fun calloutToExternalSupportLinkDlg(ctx: Context, supportUrl: String) {

    AlertDialog.Builder(ctx, R.style.AlertDialogStyle)
        .setTitle(R.string.app_name)
        .setMessage(R.string.support_leaving_app_warning)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            ctx.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(supportUrl)
                )
            )
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}