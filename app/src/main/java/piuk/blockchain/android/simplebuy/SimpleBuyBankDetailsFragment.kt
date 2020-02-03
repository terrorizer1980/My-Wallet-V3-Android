package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_simple_buy_bank_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SimpleBuyBankDetailsFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(), SimpleBuyScreen {
    override val model: SimpleBuyModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_bank_details)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(SimpleBuyIntent.FetchBankAccount)
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.BANK_DETAILS))
        activity.setupToolbar(R.string.transfer, false)
    }

    override fun render(newState: SimpleBuyState) {
        if (newState.bankAccount != null && newState.order.amount != null) {
            bank_details_container.initWithBankDetailsAndAmount(newState.bankAccount.details,
                newState.order.amount!!)
            secure_transfer.text = getString(R.string.securely_transfer,
                newState.order.amount?.currencyCode ?: "")
        }
    }

    override fun backPressedHandled(): Boolean {
        return true
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}