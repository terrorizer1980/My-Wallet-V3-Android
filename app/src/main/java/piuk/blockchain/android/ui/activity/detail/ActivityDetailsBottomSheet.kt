package piuk.blockchain.android.ui.activity.detail

import android.view.View
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class ActivityDetailsBottomSheet : SlidingModalBottomDialog() {
    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    override fun initControls(view: View) {

    }



    companion object {
        fun newInstance(crypto: CryptoCurrency, txHash: String): ActivityDetailsBottomSheet {
            return ActivityDetailsBottomSheet()
        }
    }
}