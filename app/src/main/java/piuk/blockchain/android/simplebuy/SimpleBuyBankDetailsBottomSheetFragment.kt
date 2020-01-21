package piuk.blockchain.android.simplebuy

import android.content.DialogInterface
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_simple_buy_bank_details_bottom_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class SimpleBuyBankDetailsBottomSheetFragment : SlidingModalBottomDialog() {

    private val model: SimpleBuyModel by inject()
    private val compositeDisposable = CompositeDisposable()

    override val layoutResource: Int
        get() = R.layout.fragment_simple_buy_bank_details_bottom_sheet

    override fun initControls(view: View) {
        compositeDisposable += model.state.subscribe {
            renderState(view, it)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        compositeDisposable.clear()
    }

    private fun renderState(view: View, newState: SimpleBuyState) {
        if (newState.bankAccount != null && newState.order.amount != null) {
            view.bank_details_container?.initWithBankDetailsAndAmount(newState.bankAccount.details,
                newState.order.amount!!)
            view.secure_transfer?.text = getString(R.string.securely_transfer,
                newState.order.amount?.currencyCode ?: "")
        }
    }
}