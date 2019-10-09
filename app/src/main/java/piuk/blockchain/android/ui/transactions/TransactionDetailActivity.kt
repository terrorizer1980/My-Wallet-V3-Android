package piuk.blockchain.android.ui.transactions

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
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
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityTransactionDetailsBinding
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

    private lateinit var binding: ActivityTransactionDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.transaction_detail_title)

        binding.editIcon.setOnClickListener { v -> binding.descriptionField.performClick() }
        binding.descriptionField.setOnClickListener {
            val editText = AppCompatEditText(this)
            editText.inputType = (InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
                    or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE)
            editText.setHint(R.string.transaction_detail_description_hint)

            val maxLength = 256
            val fArray = arrayOfNulls<InputFilter>(1)
            fArray[0] = InputFilter.LengthFilter(maxLength)
            editText.filters = fArray
            editText.setText(presenter.transactionNote)
            editText.setSelection(editText.text?.length ?: 0)

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

        onViewReady()
    }

    override fun setTransactionType(type: Direction, isFeeTransaction: Boolean) {
        binding.transactionType.text =
            when (type) {
                Direction.TRANSFERRED -> resources.getString(R.string.MOVED)
                Direction.RECEIVED -> resources.getString(R.string.RECEIVED)
                Direction.SENT -> if (isFeeTransaction) resources.getString(R.string.pax_fee) else
                    resources.getString(R.string.SENT)
            }
    }

    override fun showTransactionAsPaid() {
        binding.transactionType.setText(R.string.paid)
    }

    override fun onDataLoaded() {
        binding.mainLayout.visible()
        binding.loadingLayout.gone()
    }

    override fun setTransactionColour(@ColorRes colour: Int) {
        binding.transactionAmount.setTextColor(ResourcesCompat.getColor(resources, colour, theme))
        binding.transactionType.setTextColor(ResourcesCompat.getColor(resources, colour, theme))
    }

    override fun setTransactionNote(note: String?) {
        if (note != null && !note.isEmpty()) {
            binding.transactionNote.text = note
            binding.transactionNoteLayout.visible()
        }
    }

    override fun updateFeeFieldVisibility(isVisible: Boolean) {
        binding.transactionFee.goneIf(!isVisible)
    }

    override fun hideDescriptionField() {
        binding.descriptionLayout.gone()
        binding.descriptionLayoutDivider.gone()
    }

    override fun setTransactionValue(value: String?) {
        binding.transactionAmount.text = value
    }

    override fun setTransactionValueFiat(fiat: String?) {
        binding.transactionValue.text = fiat
    }

    override fun setToAddresses(addresses: List<TransactionDetailModel>) {
        if (addresses.size == 1) {
            binding.toAddress.text = addresses[0].address

            if (addresses[0].hasAddressDecodeError()) {
                binding.toAddress.setTextColor(ResourcesCompat.getColor(resources, R.color.product_red_medium, theme))
            }
        } else {
            binding.toSpinner.visible()
            val adapter = TransactionDetailAdapter(ArrayList(addresses))
            binding.toAddress.text = String.format(Locale.getDefault(), "%1s Recipients", addresses.size)
            binding.toAddress.setOnClickListener { binding.toSpinner.performClick() }
            binding.toSpinner.adapter = adapter
            binding.toSpinner.onItemSelectedListener = null
        }
    }

    override fun setDate(date: String?) {
        binding.date.text = date
    }

    override fun setDescription(description: String?) {
        binding.descriptionField.text = description
    }

    override fun setFromAddress(addresses: List<TransactionDetailModel>) {
        if (addresses.size == 1) {
            binding.fromAddress.text = addresses[0].address

            if (addresses[0].hasAddressDecodeError()) {
                binding.fromAddress.setTextColor(ResourcesCompat.getColor(resources,
                    R.color.product_red_medium,
                    theme))
            }
        } else {
            binding.fromSpinner.visible()
            val adapter = TransactionDetailAdapter(ArrayList(addresses))
            binding.fromAddress.text = String.format(Locale.getDefault(), "%1s Senders", addresses.size)
            binding.fromAddress.setOnClickListener { binding.fromSpinner.performClick() }
            binding.fromSpinner.adapter = adapter
            binding.fromSpinner.onItemSelectedListener = null
        }
    }

    override fun setStatus(
        cryptoCurrency: CryptoCurrency,
        status: String?,
        hash: String?
    ) {

        binding.status.text = status

        when (cryptoCurrency) {
            CryptoCurrency.BTC, CryptoCurrency.ETHER -> binding.buttonVerify.setText(R.string.transaction_detail_verify)
            CryptoCurrency.BCH -> {
                binding.buttonVerify.setText(R.string.transaction_detail_verify_bch)
                binding.transactionNoteLayout.gone()
            }
            CryptoCurrency.XLM -> {
                binding.buttonVerify.setText(R.string.transaction_detail_verify_stellar_chain)
                binding.transactionNoteLayout.gone()
            }
            else -> {
            }
        }

        binding.buttonVerify.setOnClickListener {
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
        when (item.itemId) {
            R.id.action_share -> {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_TEXT, presenter.transactionHash.explorerUrl)
                shareIntent.type = "text/plain"
                startActivity(Intent.createChooser(shareIntent, getString(R.string.transaction_detail_share_chooser)))
                analytics.logEvent(TransactionsAnalyticsEvents.ItemShare(presenter.displayable.cryptoCurrency.symbol))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun setIsDoubleSpend(isDoubleSpend: Boolean) {
        if (isDoubleSpend) {
            binding.doubleSpendWarning.visible()
        }
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun setFee(fee: String?) {
        binding.transactionFee.text =
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

    override fun getView(): TransactionDetailView {
        return this
    }

    companion object {

        fun start(context: Context, args: Bundle) {
            val starter = Intent(context, TransactionDetailActivity::class.java)
            starter.putExtras(args)
            context.startActivity(starter)
        }
    }
}
