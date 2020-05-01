package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Quote
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.fragment_simple_buy_checkout.*
import kotlinx.android.synthetic.main.fragment_simple_buy_checkout.progress
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import java.text.SimpleDateFormat
import java.util.Locale

class SimpleBuyCheckoutFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    SimpleBuyCancelOrderBottomSheet.Host {

    override val model: SimpleBuyModel by inject()
    private var lastState: SimpleBuyState? = null
    private val adapter = CheckoutAdapter()

    private val isForPendingPayment: Boolean by unsafeLazy {
        arguments?.getBoolean(PENDING_PAYMENT_ORDER_KEY, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_checkout)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null && !isForPendingPayment) {
            model.process(SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne)
        }

        button_buy.setOnClickListener {
            model.process(SimpleBuyIntent.ConfirmOrder)
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CONFIRMED)
        }

        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = adapter

        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.CHECKOUT))
        activity.setupToolbar(if (isForPendingPayment) R.string.order_details else R.string.checkout,
            !isForPendingPayment)
        analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_SHOWN)
        model.process(SimpleBuyIntent.FetchQuote)
        model.process(SimpleBuyIntent.FetchBankAccount)
    }

    override fun backPressedHandled(): Boolean =
        isForPendingPayment

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun render(newState: SimpleBuyState) {
        lastState = newState
        progress.visibleIf { newState.isLoading }
        purchase_note.text = if (newState.selectedPaymentMethod?.isBank() == true)
            getString(R.string.purchase_bank_note,
                newState.selectedCryptoCurrency?.displayTicker) else getString(R.string.purchase_card_note)

        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }

        val list = (if (newState.selectedPaymentMethod?.isBank() == true) getFieldsForBank(newState)
        else getFieldsForCard(newState)).toMutableList()

        if (isPendingOrAwaitingFunds(newState.orderState))
            list.add(CheckoutItem(getString(R.string.status), getString(R.string.order_pending)))

        adapter.items = list.toList()

        btn_ok.text = if (newState.orderState == OrderState.AWAITING_FUNDS && !isForPendingPayment) {
            getString(R.string.complete_payment)
        } else getString(R.string.ok_cap)

        configureButtons(newState.orderState == OrderState.AWAITING_FUNDS)

        button_buy.text = resources.getString(R.string.buy_crypto,
            newState.selectedCryptoCurrency?.displayTicker)

        checkout_subtitle.text =
            resources.getString(R.string.checkout_subtitle, newState.selectedCryptoCurrency?.displayTicker)

        button_buy.isEnabled = !newState.isLoading

        button_cancel.setOnClickListenerDebounced {
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_PRESS_CANCEL)
            showBottomSheet(SimpleBuyCancelOrderBottomSheet.newInstance())
        }

        when (newState.order.orderState) {
            OrderState.AWAITING_FUNDS -> {
                if (newState.confirmationActionRequested) {
                    if (newState.selectedPaymentMethod?.isBank() == true) navigator().goToBankDetailsScreen()
                    else navigator().goToCardPaymentScreen()
                }
            }
            OrderState.CANCELED -> {
                model.process(SimpleBuyIntent.ClearState)
                navigator().exitSimpleBuyFlow()
            }
            else -> {
            }
        }
    }

    private fun getFieldsForCard(state: SimpleBuyState): List<CheckoutItem> =
        listOf(
            CheckoutItem(getString(R.string.date), state.order.quote?.formatDate() ?: ""),

            CheckoutItem(getString(R.string.total_cost), calculateFeedAmount(state.fee, state.order.amount)),

            CheckoutItem(getString(R.string.amount),
                state.price?.toStringWithSymbol() ?: ""),

            CheckoutItem(getString(R.string.fees),
                state.fee?.toStringWithSymbol() ?: ""),

            CheckoutItem(getString(R.string.total),
                state.order.amount?.toStringWithSymbol() ?: ""),

            CheckoutItem(getString(R.string.payment_method),
                state.selectedPaymentMethod?.let {
                    paymentMethodLabel(it)
                } ?: ""
            )
        )

    private fun calculateFeedAmount(fee: FiatValue?, amount: FiatValue?): String = fee?.let {
        amount?.minus(it)?.toStringWithSymbol() ?: ""
    } ?: amount?.toStringWithSymbol() ?: ""

    private fun getFieldsForBank(state: SimpleBuyState): List<CheckoutItem> =
        listOf(
            CheckoutItem(getString(R.string.date), state.order.quote?.formatDate() ?: ""),

            CheckoutItem(getString(R.string.total_cost), calculateFeedAmount(state.fee, state.order.amount)),

            CheckoutItem(getString(R.string.fees),
                state.fee?.toStringWithSymbol() ?: FiatValue.zero(state.fiatCurrency).toStringWithSymbol()),

            CheckoutItem(getString(R.string.total),
                state.order.amount?.toStringWithSymbol() ?: ""),

            CheckoutItem(getString(R.string.payment_method),
                state.selectedPaymentMethod?.let {
                    paymentMethodLabel(it)
                } ?: ""
            )
        )

    private fun isPendingOrAwaitingFunds(orderState: OrderState) =
        isForPendingPayment || orderState == OrderState.AWAITING_FUNDS

    private fun configureButtons(isOrderAwaitingFunds: Boolean) {
        button_buy.visibleIf { !isForPendingPayment && !isOrderAwaitingFunds }
        button_cancel.visibleIf { !isForPendingPayment && !isOrderAwaitingFunds }
        btn_ok.visibleIf { isForPendingPayment || isOrderAwaitingFunds }

        btn_ok.setOnClickListener {
            if (!isOrderAwaitingFunds)
                navigator().exitSimpleBuyFlow()
            else
                navigator().goToCardPaymentScreen()
        }
    }

    private fun paymentMethodLabel(selectedPaymentMethod: SelectedPaymentMethod): String =
        when (selectedPaymentMethod.id) {
            PaymentMethod.BANK_PAYMENT_ID -> getString(R.string.checkout_bank_transfer_label)
            else -> selectedPaymentMethod.label ?: ""
        }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    override fun cancelOrderConfirmAction(cancelOrder: Boolean, orderId: String?) {
        if (cancelOrder) {
            model.process(SimpleBuyIntent.CancelOrder)
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CANCELLATION_CONFIRMED)
        } else {
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CANCELLATION_GO_BACK)
        }
    }

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.ConfirmationHandled)
    }

    override fun onSheetClosed() {
        if (lastState?.errorState != null) {
            model.process(SimpleBuyIntent.ClearError)
            model.process(SimpleBuyIntent.ClearState)
            navigator().exitSimpleBuyFlow()
        }
    }

    private fun Quote.estAmount(): String? =
        getString(R.string.approximately_symbol, estimatedAmount.toStringWithSymbol())

    companion object {
        private const val PENDING_PAYMENT_ORDER_KEY = "PENDING_PAYMENT_KEY"

        fun newInstance(isForPending: Boolean = false): SimpleBuyCheckoutFragment {
            val fragment = SimpleBuyCheckoutFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(PENDING_PAYMENT_ORDER_KEY, isForPending)
            }
            return fragment
        }
    }
}

private fun Quote.formatDate(): String {
    val format = SimpleDateFormat("MMMM d, yyyy @ hh:mm aa", Locale.getDefault())
    return format.format(this.date)
}
