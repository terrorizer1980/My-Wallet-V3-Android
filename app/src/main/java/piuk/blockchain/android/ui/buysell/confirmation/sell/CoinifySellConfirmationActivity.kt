package piuk.blockchain.android.ui.buysell.confirmation.sell

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.blockchain.ui.password.SecondPasswordHandler
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.payment.card.PaymentState
import piuk.blockchain.android.ui.buysell.payment.complete.CoinifyPaymentCompleteActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import com.blockchain.ui.dialog.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.button_confirm_sell as buttonConfirm
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.check_box_sell_rate_disclaimer as checkBoxDisclaimer
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.text_view_btc_total_to_send_description as textViewBtcToSend
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.text_view_sell_amount_detail as textViewSellAmount
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.text_view_sell_fiat_to_be_received_detail as textViewFiatTotal
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.text_view_sell_payment_fee_detail as textViewPaymentFee
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.text_view_sell_time_remaining as textViewTime
import kotlinx.android.synthetic.main.activity_coinify_sell_confirmation.text_view_sell_transaction_fee_detail as textViewTransactionFee

class CoinifySellConfirmationActivity :
    BaseMvpActivity<CoinifySellConfirmationView, CoinifySellConfirmationPresenter>(),
    CoinifySellConfirmationView {

    private val presenter: CoinifySellConfirmationPresenter by inject()
    override val locale: Locale = Locale.getDefault()
    private val compositeDisposable = CompositeDisposable()

    override val displayableQuote by unsafeLazy {
        intent.getParcelableExtra(EXTRA_DISPLAY_MODEL) as SellConfirmationDisplayModel
    }
    override val bankAccountId: Int by unsafeLazy { intent.getIntExtra(EXTRA_BANK_ID, -1) }
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinify_sell_confirmation)
        setupToolbar(toolbar_general, R.string.buy_sell_confirmation_title_sell)

        compositeDisposable += RxView.clicks(buttonConfirm)
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribeBy(onNext = { presenter.onConfirmClicked() })

        renderUi()

        onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    @SuppressLint("SetTextI18n")
    private fun renderUi() {
        with(displayableQuote) {
            val currencyOut = currencyToSend.toUpperCase()
            textViewSellAmount.text = "$amountToSend $currencyOut"
            textViewTransactionFee.text = "-$networkFee $currencyOut"
            textViewFiatTotal.text = totalAmountToReceiveFormatted
            textViewPaymentFee.text = "-$paymentFee"
            textViewBtcToSend.text =
                getString(
                    R.string.buy_sell_confirmation_btc_to_receive_description,
                    totalCostFormatted
                )
        }

        checkBoxDisclaimer.setOnCheckedChangeListener { _, isChecked ->
            buttonConfirm.isEnabled = isChecked
        }
    }

    override fun displaySecondPasswordDialog() {
        secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                throw IllegalStateException("SecondPasswordHandler instantiated despite no second password set")
            }

            override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                presenter.validatedSecondPassword = validatedSecondPassword
                presenter.onConfirmClicked()
            }
        })
    }

    override fun updateCounter(timeRemaining: String) {
        textViewTime.text = timeRemaining
    }

    override fun showTimeExpiring() {
        textViewTime.setTextColor(getResolvedColor(R.color.product_red_medium))
    }

    override fun showQuoteExpiredDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.buy_sell_confirmation_order_expired)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                setResult(AppCompatActivity.RESULT_CANCELED)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun showErrorDialog(errorMessage: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(errorMessage)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showTransactionComplete() {
        CoinifyPaymentCompleteActivity.start(this, PaymentState.SUCCESS)
        setResult(AppCompatActivity.RESULT_OK)
        finish()
    }

    override fun displayProgressDialog() {
        if (!isFinishing) {
            progressDialog = MaterialProgressDialog(this).apply {
                setMessage(getString(R.string.please_wait))
                setCancelable(false)
                show()
            }
        }
    }

    override fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        setResult(AppCompatActivity.RESULT_CANCELED)
        finish()
    }

    override fun createPresenter(): CoinifySellConfirmationPresenter = presenter

    override fun getView(): CoinifySellConfirmationView = this

    companion object {

        private const val EXTRA_DISPLAY_MODEL =
            "piuk.blockchain.android.ui.buysell.confirmation.sell.EXTRA_DISPLAY_MODEL"
        private const val EXTRA_BANK_ID =
            "piuk.blockchain.android.ui.buysell.confirmation.sell.EXTRA_BANK_ID"

        const val REQUEST_CODE_CONFIRM_MAKE_SELL_PAYMENT = 807

        fun start(
            activity: AppCompatActivity,
            displayModel: SellConfirmationDisplayModel,
            bankAccountId: Int
        ) {
            Intent(activity, CoinifySellConfirmationActivity::class.java)
                .apply {
                    putExtra(EXTRA_DISPLAY_MODEL, displayModel)
                    putExtra(EXTRA_BANK_ID, bankAccountId)
                    addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                }
                .run { activity.startActivity(this) }
        }
    }
}
