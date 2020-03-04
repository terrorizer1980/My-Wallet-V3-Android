package piuk.blockchain.android.ui.transactions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import android.text.InputFilter
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.TransactionsAnalyticsEvents
import com.blockchain.ui.urllinks.makeBlockExplorerUrl
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction
import kotlinx.android.synthetic.main.activity_transaction_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transactions.mapping.TransactionDetailModel
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

class TransactionDetailActivity : BaseMvpActivity<TransactionDetailView, TransactionDetailPresenter>(),
    TransactionDetailView {

    private val transactionDetailPresenter: TransactionDetailPresenter by inject()
    private val analytics: Analytics by inject()

    private var shareIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.transaction_detail_title)

        edit_icon.setOnClickListener { description_field.performClick() }

        onViewReady()
    }

    override fun onStart() {
        super.onStart()
        presenter.showDetailsForTransaction(intent.cryptoCurrency, intent.txHash)
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

    override fun setTransactionNote(note: String?) {
        if (!note.isNullOrEmpty()) {
            transaction_note.text = note
            transaction_note_layout.visible()

            description_field.setOnClickListener {
                descriptionFieldClickListener(note)
            }
        }
    }

    private fun descriptionFieldClickListener(note: String) {
        val editText = AppCompatEditText(this).apply {
            inputType = INPUT_FIELD_FLAGS
            filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))
            setText(note)
            setSelection(note.length)
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

    override fun updateFeeFieldVisibility(isVisible: Boolean) {
        transaction_fee.goneIf(!isVisible)
    }

    override fun setTransactionColour(@ColorRes colour: Int) {
        transaction_amount.setTextColor(ResourcesCompat.getColor(resources, colour, theme))
        transaction_type.setTextColor(ResourcesCompat.getColor(resources, colour, theme))
    }

    override fun setTransactionValue(value: CryptoValue) {
        transaction_amount.text = value.toStringWithSymbol()
    }

    override fun setTransactionValueFiat(fiat: String?) {
        transaction_value.text = fiat
    }

    override fun setToAddresses(addresses: List<TransactionDetailModel>) {
        if (addresses.size == 1) {
            to_address.text = addresses[0].address

            if (addresses[0].addressDecodeError) {
                to_address.setTextColor(ResourcesCompat.getColor(resources, R.color.product_red_medium, theme))
            }
        } else {
            to_address.text = String.format(Locale.getDefault(), "%1s Recipients", addresses.size)
            to_address.setOnClickListener { to_spinner.performClick() }
            to_spinner.visible()
            to_spinner.adapter = TransactionDetailAdapter(addresses)
            to_spinner.onItemSelectedListener = null
        }
    }

    override fun setDescription(description: String?) {
        if (description != null) {
            description_layout.visible()
            description_layout_divider.visible()
            description_field.text = description
        } else {
            description_layout.gone()
            description_layout_divider.gone()
        }
    }

    override fun setFromAddress(addresses: List<TransactionDetailModel>) {
        if (addresses.size == 1) {
            from_address.text = addresses[0].address

            if (addresses[0].addressDecodeError) {
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
        hash: String
    ) {
        txt_status.text = status

        when (cryptoCurrency) {
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER -> button_verify.setText(R.string.transaction_detail_verify)
            CryptoCurrency.BCH -> {
                button_verify.setText(R.string.transaction_detail_verify_bch)
                transaction_note_layout.gone()
            }
            CryptoCurrency.XLM -> {
                button_verify.setText(R.string.transaction_detail_verify_stellar_chain)
                transaction_note_layout.gone()
            }
            else -> { }
        }

        val explorerUri = makeBlockExplorerUrl(cryptoCurrency, hash)
        button_verify.setOnClickListener {
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(explorerUri)
                startActivity(this)
            }
            analytics.logEvent(TransactionsAnalyticsEvents.ViewOnWeb(cryptoCurrency.symbol))
        }

        shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, explorerUri)
            type = "text/plain"
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
                shareIntent?.let {
                    startActivity(
                        Intent.createChooser(shareIntent, getString(R.string.transaction_detail_share_chooser))
                    )
                    analytics.logEvent(TransactionsAnalyticsEvents.ItemShare(intent.cryptoCurrency))
                }
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

    @SuppressLint("SetTextI18n")
    override fun setDate(datetimeMillis: Long) {
        val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateText = dateFormat.format(datetimeMillis)
        val timeText = timeFormat.format(datetimeMillis)

        txt_date.text = "$dateText @ $timeText"
    }

    override fun pageFinish() {
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun createPresenter() = transactionDetailPresenter

    override fun getView() = this

    companion object {
        private const val MAX_NOTE_LENGTH = 255

        private const val INPUT_FIELD_FLAGS: Int = (
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or
                        InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
                )

        private const val KEY_CRYPTO = "crypto_currency"
        private const val KEY_TRANSACTION_HASH = "tx_hash"

        fun start(ctx: Context, crypto: CryptoCurrency, txHash: String) {
            ctx.startActivity(
                Intent(ctx, TransactionDetailActivity::class.java).apply {
                    putExtras(
                        Bundle().also {
                            it.putString(KEY_CRYPTO, crypto.symbol)
                            it.putString(KEY_TRANSACTION_HASH, txHash)
                        }
                    )
                }
            )
        }
    }

    private val Intent?.cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.fromSymbol(this?.getStringExtra(KEY_CRYPTO) ?: "BTC")
            ?: CryptoCurrency.BTC

    private val Intent?.txHash: String
        get() = this?.getStringExtra(KEY_TRANSACTION_HASH) ?: ""
}
