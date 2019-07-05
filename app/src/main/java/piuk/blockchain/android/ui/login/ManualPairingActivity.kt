package piuk.blockchain.android.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import info.blockchain.wallet.api.data.Settings
import kotlinx.android.synthetic.main.activity_manual_pairing.*
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.OverlayDetection
import piuk.blockchain.androidcoreui.utils.ViewUtils

class ManualPairingActivity : BaseMvpActivity<ManualPairingView, ManualPairingPresenter>(),
    ManualPairingView {

    private val presenter: ManualPairingPresenter by inject()
    private val overlayDetection: OverlayDetection by inject()

    private var progressDialog: MaterialProgressDialog? = null

    override val guid: String = wallet_id.text.toString()
    override val password: String = wallet_pass.text.toString()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_pairing)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.manual_pairing)

        command_next.setOnClickListener { presenter.onContinueClicked() }

        wallet_pass.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_GO) {
                presenter.onContinueClicked()
            }
            true
        }
        onViewReady()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) {
        progressDialog?.setMessage(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {
        ViewUtils.hideKeyboard(this)

        val editText = AppCompatEditText(this)
        editText.setHint(R.string.two_factor_dialog_hint)
        val message: Int

        when (authType) {
            Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR -> {
                message = R.string.two_factor_dialog_message_authenticator
                editText.inputType = InputType.TYPE_NUMBER_VARIATION_NORMAL
                editText.keyListener = DigitsKeyListener.getInstance("1234567890")
            }
            Settings.AUTH_TYPE_SMS -> {
                message = R.string.two_factor_dialog_message_sms
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }
            else -> throw IllegalArgumentException("Auth Type $authType should not be passed to this function")
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.two_factor_dialog_title)
            .setMessage(message)
            .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.submitTwoFactorCode(
                    responseObject,
                    sessionId,
                    guid,
                    password,
                    editText.text.toString()
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    override fun showProgressDialog(
        @StringRes messageId: Int,
        suffix: String?,
        cancellable: Boolean
    ) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(cancellable)
            val msg = getString(messageId) + if (suffix != null) "\n\n" + suffix else ""
            setMessage(msg)
            setOnCancelListener { presenter.onProgressCancelled() }

            if (!isFinishing)
                show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        progressDialog = null
    }

    override fun resetPasswordField() {
        if (!isFinishing)
            wallet_pass.setText("")
    }

    public override fun onDestroy() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        dismissProgressDialog()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Test for screen overlays before user enters PIN
        return overlayDetection.detectObscuredWindow(this, event) ||
                super.dispatchTouchEvent(event)
    }

    override fun startLogoutTimer() { /* No-op */
    }

    override fun enforceFlagSecure(): Boolean = true

    override fun createPresenter() = presenter

    override fun getView() = this
}
