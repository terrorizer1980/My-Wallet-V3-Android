package piuk.blockchain.android.ui.recover

import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.widget.Toolbar
import android.view.inputmethod.EditorInfo
import com.blockchain.annotations.CommonCode
import kotlinx.android.synthetic.main.activity_recover_funds.*
import org.koin.android.ext.android.get

import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

import java.util.Locale

internal class RecoverFundsActivity
    : BaseMvpActivity<RecoverFundsView, RecoverFundsPresenter>(),
    RecoverFundsView {

    private var progressDialog: MaterialProgressDialog? = null

    private val recoveryPhrase: String
        get() = field_passphrase.text.toString().toLowerCase(Locale.US).trim()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recover_funds)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.recover_funds)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        button_continue.setOnClickListener { presenter.onContinueClicked(recoveryPhrase) }
        field_passphrase.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_GO) {
                presenter.onContinueClicked(recoveryPhrase)
            }
            true
        }
        onViewReady()
    }

    override fun gotoCredentialsActivity(recoveryPhrase: String) {
        val intent = Intent(this, CreateWalletActivity::class.java)
        intent.putExtra(RECOVERY_PHRASE, recoveryPhrase)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun startLogoutTimer() { /* No-op */ }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    @CommonCode("Move to base")
    override fun showProgressDialog(@StringRes messageId: Int) {
        dismissProgressDialog()

        if (isFinishing) return

        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(getString(messageId))
            show()
        }
    }

    @CommonCode("Move to base")
    override fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    override fun enforceFlagSecure(): Boolean = true

    override fun createPresenter(): RecoverFundsPresenter = get()
    override fun getView(): RecoverFundsView = this

    companion object {
        const val RECOVERY_PHRASE = "RECOVERY_PHRASE"
    }
}
