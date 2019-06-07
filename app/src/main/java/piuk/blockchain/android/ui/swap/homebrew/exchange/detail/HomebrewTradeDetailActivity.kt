package piuk.blockchain.android.ui.swap.homebrew.exchange.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.view.View
import com.blockchain.morph.trade.MorphTrade
import piuk.blockchain.android.ui.swap.homebrew.exchange.extensions.toDrawable
import piuk.blockchain.android.ui.swap.homebrew.exchange.extensions.toStatusString
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.Trade
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import kotlinx.android.synthetic.main.activity_homebrew_trade_detail.*
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible

class HomebrewTradeDetailActivity : BaseAuthActivity() {

    private var showSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_detail)
        get<Analytics>().logEvent(AnalyticsEvents.ExchangeDetailOverview)

        val trade = intent.extras.get(EXTRA_TRADE) as Trade
        showSuccess = intent.extras.get(EXTRA_SHOW_SUCCESS) as Boolean

        setupToolbar(R.id.toolbar_general, R.string.order_detail)

        status.text = trade.state.toStatusString(this)
        status.setCompoundDrawablesWithIntrinsicBounds(
            trade.state.toDrawable(this),
            null,
            null,
            null
        )
        value.text = if (trade.approximateValue()) trade.price.displayAsApproximate() else trade.price
        receive.text = trade.quantity
        fees.text = trade.fee

        if (trade.shouldStrike()) {
            receive.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }

        if (trade.refunding()) {
            receive.text = trade.quantity.displayAsApproximate()
            fees.text = trade.fee.displayAsApproximate()
            receive_title.setText(R.string.morph_status_refund_in_progress)
        }

        if (trade.refunded()) {
            receive.text = trade.quantity
            fees.text = trade.fee.displayAsNegative()
            receive_title.setText(R.string.morph_exchange)
        }

        trade_id.text = trade.id
        exchange.text = trade.depositQuantity
        status_detail_textView.text = trade.toMessage()

        if (trade.expired()) {
            button_request_refund.visible()
            button_request_refund.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REFUND_LINK)))
            }
        }

        var idClicked = false
        val transition = AutoTransition().apply { duration = 300 }

        trade_id.setOnClickListener {
            if (!idClicked) {
                idClicked = true
                copyToClipboard(trade)
                trade_id_title.invisible()
                TransitionManager.beginDelayedTransition(constraint_layout_morph_detail, transition)

                ConstraintSet().apply {
                    clone(constraint_layout_morph_detail)
                    connect(
                        trade_id.id,
                        ConstraintSet.START,
                        constraint_layout_morph_detail.id,
                        ConstraintSet.START
                    )
                    applyTo(constraint_layout_morph_detail)
                }
            } else {
                idClicked = false
                TransitionManager.beginDelayedTransition(constraint_layout_morph_detail, transition)

                ConstraintSet().apply {
                    clone(constraint_layout_morph_detail)
                    connect(
                        trade_id.id,
                        ConstraintSet.START,
                        trade_id_title.id,
                        ConstraintSet.END
                    )
                    setVisibility(trade_id_title.id, View.VISIBLE)
                    applyTo(constraint_layout_morph_detail)
                }
            }
        }

        hidePriceIfShapeShiftTrade(trade)
    }

    override fun onStart() {
        super.onStart()
        if (showSuccess) {
            showSuccessDialog(intent.extras.getBoolean(EXTRA_IS_FIRST_PAX, false))
        }
    }

    private fun hidePriceIfShapeShiftTrade(trade: Trade) {
        if (trade.price.isEmpty()) {
            value.gone()
            value_title.gone()
            view_value.gone()

            ConstraintSet().apply {
                clone(constraint_layout_morph_detail)
                connect(
                    exchange_title.id,
                    ConstraintSet.TOP,
                    status_title_textView.id,
                    ConstraintSet.BOTTOM
                )
                connect(
                    exchange.id,
                    ConstraintSet.TOP,
                    status.id,
                    ConstraintSet.BOTTOM
                )
                applyTo(constraint_layout_morph_detail)
            }
        }
    }

    private fun copyToClipboard(trade: Trade) {
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Trade ID", trade.id)
        clipboard.primaryClip = clip
        toast(R.string.copied_to_clipboard, ToastCustom.TYPE_OK)
    }

    private fun showSuccessDialog(firstGoldPaxTrade: Boolean) {
        val title = if (!firstGoldPaxTrade) getString(R.string.morph_success_dlg_text) else
            getString(R.string.morph_success_for_first_gold_pax_trade_title)

        val description = if (!firstGoldPaxTrade) "" else
            getString(R.string.morph_success_for_first_gold_pax_trade_description)

        val exchangeStartedDialog = ErrorBottomDialog.newInstance(
            ErrorBottomDialog.Content(
                title = title,
                description = description,
                ctaButtonText = R.string.morph_success_dlg_button,
                dismissText = 0,
                icon = R.drawable.ic_swap_in_progress_check
            )
        )
        exchangeStartedDialog.isCancelable = false
        exchangeStartedDialog.onCtaClick = { showSuccess = false }
        exchangeStartedDialog.show(supportFragmentManager, "BottomDialog")
    }

    private fun Trade.toMessage(): String? =
        when (this.state) {
            MorphTrade.Status.FAILED,
            MorphTrade.Status.REFUNDED -> getString(R.string.status_failed_detail)
            MorphTrade.Status.IN_PROGRESS -> getString(R.string.status_inprogress_detail)
            MorphTrade.Status.REFUND_IN_PROGRESS -> getString(R.string.status_refund_in_progress_detail)
            MorphTrade.Status.EXPIRED -> getString(R.string.status_expired_detail)
            else -> null
        }

    private fun Trade.shouldStrike(): Boolean =
        when (this.state) {
            MorphTrade.Status.EXPIRED,
            MorphTrade.Status.REFUNDED,
            MorphTrade.Status.REFUND_IN_PROGRESS,
            MorphTrade.Status.FAILED -> true
            else -> false
        }

    private fun Trade.approximateValue(): Boolean =
        this.state == MorphTrade.Status.IN_PROGRESS

    private fun Trade.refunding(): Boolean =
        this.state == MorphTrade.Status.REFUND_IN_PROGRESS

    private fun Trade.refunded(): Boolean =
        this.state == MorphTrade.Status.REFUNDED

    private fun Trade.expired(): Boolean =
        this.state == MorphTrade.Status.EXPIRED

    private fun String.displayAsApproximate() = "~$this"

    private fun String.displayAsNegative() = "-$this"

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    companion object {
        private const val REFUND_LINK =
            "https://support.blockchain.com/hc/en-us/requests/new?ticket_form_id=360000014686"

        private const val EXTRA_TRADE = "EXTRA_TRADE"
        private const val EXTRA_SHOW_SUCCESS = "SHOW_SUCCESS"
        private const val EXTRA_IS_FIRST_PAX = "IS_FIRST_PAX"

        fun start(ctx: Context, trade: Trade, showSuccess: Boolean = false, isFirstPax: Boolean = false) {
            Intent(ctx, HomebrewTradeDetailActivity::class.java).apply {
                putExtra(EXTRA_TRADE, trade)
                putExtra(EXTRA_SHOW_SUCCESS, showSuccess)
                putExtra(EXTRA_IS_FIRST_PAX, isFirstPax)
                ctx.startActivity(this)
            }
        }
    }
}
