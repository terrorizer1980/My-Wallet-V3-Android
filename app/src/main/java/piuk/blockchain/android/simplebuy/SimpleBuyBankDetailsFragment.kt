package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_simple_buy_bank_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.util.CopyableTextFormItem
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
    }

    override fun render(newState: SimpleBuyState) {
        if (newState.bankAccount != null && bank_details_container.childCount == 0) {
            newState.bankAccount.details.forEach {
                bank_details_container.addView(CopyableTextFormItem(it.title,
                    it.value,
                    it.isCopyable,
                    requireContext()),
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                )
            }
            bank_details_container.addView(CopyableTextFormItem(getString(R.string.amount_to_send),
                newState.order.amount?.toStringWithSymbol() ?: "",
                false,
                requireContext())
            )
        }
        secure_transfer.text = getString(R.string.securely_transfer,
            newState.order.amount?.currencyCode ?: "")
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}