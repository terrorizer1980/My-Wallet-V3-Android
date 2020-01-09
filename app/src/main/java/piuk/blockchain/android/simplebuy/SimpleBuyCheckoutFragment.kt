package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_simple_buy_checkout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SimpleBuyCheckoutFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(), SimpleBuyScreen {

    override val model: SimpleBuyModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_checkout)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_buy.setOnClickListener {
            model.process(SimpleBuyIntent.ConfirmOrder)
        }
        button_cancel.setOnClickListener {
            model.process(SimpleBuyIntent.CancelOrder)
        }
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun render(newState: SimpleBuyState) {
        total_cost.text = newState.enteredFiat?.formatOrSymbolForZero()
        button_buy.text = resources.getString(R.string.buy_crypto, newState.selectedCryptoCurrency?.symbol)
        when (newState.orderState) {
            OrderState.CANCELLED -> navigator().exitSimpleBuyFlow()
            OrderState.CONFIRMED -> {
                /*MOVE TO NEXT SCREEN*/
            }
            else -> {
            }
        }
    }
}