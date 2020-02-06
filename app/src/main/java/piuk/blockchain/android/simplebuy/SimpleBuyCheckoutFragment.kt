package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import kotlinx.android.synthetic.main.fragment_simple_buy_checkout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.text.SimpleDateFormat
import java.util.Locale

class SimpleBuyCheckoutFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    CancelOrderConfirmationListener,
    SlidingModalBottomDialog.Host {

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
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.CHECKOUT))
        activity.setupToolbar(R.string.checkout)

        model.process(SimpleBuyIntent.FetchQuote)
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun render(newState: SimpleBuyState) {
        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }
        total_cost.text = newState.order.amount?.formatOrSymbolForZero()
        button_buy.text = resources.getString(R.string.buy_crypto, newState.selectedCryptoCurrency?.symbol)
        checkout_subtitle.text =
            resources.getString(R.string.checkout_subtitle, newState.selectedCryptoCurrency?.symbol)

        date.text = newState.order.quote?.formatDate()

        button_cancel.setOnClickListener {
            showBottomSheet(SimpleBuyCancelOrderBottomSheet.newInstance(newState.selectedCryptoCurrency
                ?: return@setOnClickListener))
        }

        when (newState.order.orderState) {
            OrderState.CANCELED -> {
                model.process(SimpleBuyIntent.ClearState)
                navigator().exitSimpleBuyFlow()
            }
            OrderState.AWAITING_FUNDS -> {
                if (newState.confirmationActionRequested)
                    navigator().goToBankDetailsScreen()
            }
            else -> {
            }
        }
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(ErrorDialogData(
            getString(R.string.ops),
            getString(R.string.something_went_wrong_try_again),
            getString(R.string.ok_cap)
        )))
    }

    override fun onErrorCtaActionOrDismiss() {
        super.onErrorCtaActionOrDismiss()
        model.process(SimpleBuyIntent.ClearError)
    }

    override fun onOrderCancelationConfirmed() {
        model.process(SimpleBuyIntent.CancelOrder)
    }

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.ConfirmationHandled)
    }

    override fun onSheetClosed() {
        // TODO - clear state if required?
    }
}

private fun Quote.formatDate(): String {
    val format = SimpleDateFormat("MMMM d, yyyy @ hh:mm aa", Locale.getDefault())
    return format.format(this.date)
}

interface CancelOrderConfirmationListener {
    fun onOrderCancelationConfirmed()
}