package piuk.blockchain.android.ui.transactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.Toolbar
import android.text.InputFilter
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.TransactionsAnalyticsEvents
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction
import kotlinx.android.synthetic.main.activity_transaction_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.util.ArrayList
import java.util.Locale

class TransactionDetailActivity : BaseMvpActivity<TransactionDetailView, TransactionDetailPresenter>(),
    TransactionDetailView {

    private val transactionDetailPresenter: TransactionDetailPresenter by inject()
    private val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.transaction_detail_title)

        edit_icon.setOnClickListener { description_field.performClick() }

        description_field.setOnClickListener { descriptionFieldClickListener() }

        onViewReady()
    }

    private fun descriptionFieldClickListener() {
        val editText = AppCompatEditText(this).apply {
            inputType = INPUT_FIELD_FLAGS

            val maxLength = 256
            val fArray = arrayOfNulls<InputFilter>(1)
            fArray[0] = InputFilter.LengthFilter(maxLength)
            filters = fArray
            setText(presenter.transactionNote)
            setSelection(text.length)
            contentDescription = resources.getString(R.string.edit_transaction_hint)
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.updateTransactionNote(editText.text.toString())
                setDescription(editText.text.toString())
            }
            .show()
    }

    override fun setTransactionType(type: Direction, isFeeTransaction: Boolean) {
        transaction_type.text =
            when (type) {
                Direction.TRANSFERRED -> resources.getString(R.string.MOVED)
                Direction.RECEIVED -> resources.getString(R.string.RECEIVED)
                Direction.SENT -> if (isFeeTransaction)
                        resources.getString(R.string.pax_fee)
                    else
                        resources.getString(R.string.SENT)
            }
    }

    override fun showTransactionAsPaid() {
        transaction_type.setText(R.string.paid)
    }

    override fun onDataLoaded() {
        main_layout.visible()
        loading_layout.gone()
    }

    override fun setTransactionColour(@ColorRes colour: Int) {
        transaction_amount.setTextColor(ResourcesCompat.getColor(resources, colour, theme))
        transaction_type.setTextColor(ResourcesCompat.getColor(resources, colour, theme))
    }

    override fun setTransactionNote(note: String?) {
        if (!note.isNullOrEmpty()) {
            transaction_note.text = note
            transaction_note_layout.visible()
        }
    }

    override fun updateFeeFieldVisibility(isVisible: Boolean) {
        transaction_fee.goneIf(!isVisible)
    }

    override fun hideDescriptionField() {
        description_layout.gone()
        description_layout_divider.gone()
    }

    override fun setTransactionValue(value: String?) {
        transaction_amount.text = value
    }

    override fun setTransactionValueFiat(fiat: String?) {
        transaction_value.text = fiat
    }

    override fun setToAddresses(addresses: List<TransactionDetailModel>) {
        if (addresses.size == 1) {
            to_address.text = addresses[0].address

            if (addresses[0].hasAddressDecodeError()) {
                to_address.setTextColor(ResourcesCompat.getColor(resources, R.color.product_red_medium, theme))
            }
        } else {
            to_address.text = String.format(Locale.getDefault(), "%1s Recipients", addresses.size)
            to_address.setOnClickListener { to_spinner.performClick() }
            to_spinner.visible()
            val adapter = TransactionDetailAdapter(ArrayList(addresses))
            to_spinner.adapter = adapter
            to_spinner.onItemSelectedListener = null
        }
    }

    override fun setDate(date: String?) {
        txt_date.text = date
    }

    override fun setDescription(description: String?) {
        description_field.text = description
    }

    override fun setFromAddress(addresses: List<TransactionDetailModel>) {
        if (addresses.size == 1) {
            from_address.text = addresses[0].address

            if (addresses[0].hasAddressDecodeError()) {
                from_address.setTextColor(ResourcesCompat.getColor(resources, R.color.product_red_medium, theme))
            }
        } else {
            from_spinner.visible()
            val adapter = TransactionDetailAdapter(ArrayList(addresses))
            from_address.text = String.format(Locale.getDefault(), "%1s Senders", addresses.size)
            from_address.setOnClickListener { from_spinner.performClick() }
            from_spinner.adapter = adapter
            from_spinner.onItemSelectedListener = null
        }
    }

    override fun setStatus(
        cryptoCurrency: CryptoCurrency,
        status: String?,
        hash: String?
    ) {
        txt_status.text = status

        when (cryptoCurrency) {
            CryptoCurrency.BTC, CryptoCurrency.ETHER -> button_verify.setText(R.string.transaction_detail_verify)
            CryptoCurrency.BCH -> {
                button_verify.setText(R.string.transaction_detail_verify_bch)
                transaction_note_layout.gone()
            }
            CryptoCurrency.XLM -> {
                button_verify.setText(R.string.transaction_detail_verify_stellar_chain)
                transaction_note_layout.gone()
            }
            else -> {
            }
        }

        button_verify.setOnClickListener {
            val viewIntent = Intent(Intent.ACTION_VIEW)
            viewIntent.data = Uri.parse(presenter.transactionHash.explorerUrl)
            startActivity(viewIntent)
            analytics.logEvent(TransactionsAnalyticsEvents.ViewOnWeb(cryptoCurrency.symbol))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_transaction_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, presenter.transactionHash.explorerUrl)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.transaction_detail_share_chooser)))
                analytics.logEvent(TransactionsAnalyticsEvents.ItemShare(presenter.displayable.cryptoCurrency.symbol))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setIsDoubleSpend(isDoubleSpend: Boolean) {
        if (isDoubleSpend) {
            double_spend_warning.visible()
        }
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun setFee(fee: String?) {
        transaction_fee.text =
            String.format(Locale.getDefault(), getString(R.string.transaction_detail_fee), fee)
    }

    override fun pageFinish() {
        finish()
    }

    override fun getPageIntent(): Intent? {
        return intent
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun createPresenter() = transactionDetailPresenter

    override fun getView() = this

    companion object {
        private const val INPUT_FIELD_FLAGS: Int = (
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or
                InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
            )

        fun start(context: Context, args: Bundle) {
            context.startActivity(
                Intent(context, TransactionDetailActivity::class.java).apply {
                    putExtras(args)
                }
            )
        }
    }
}
