package piuk.blockchain.android.ui.auth

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.MotionEvent
import com.blockchain.ui.dialog.MaterialProgressDialog
import info.blockchain.wallet.api.data.Settings
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityPasswordRequiredBinding
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.DialogButtonCallback
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.OverlayDetection
import piuk.blockchain.androidcoreui.utils.ViewUtils

/**
 * Created by adambennett on 09/08/2016.
 */

internal class PasswordRequiredActivity : BaseMvpActivity<PasswordRequiredView, PasswordRequiredPresenter>(),
    PasswordRequiredView {

    private val passwordRequiredPresenter: PasswordRequiredPresenter by inject()
    private val overlayDetection: OverlayDetection by inject()
    private var mBinding: ActivityPasswordRequiredBinding? = null
    private var mProgressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_password_required)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.confirm_password)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        mBinding?.buttonContinue?.setOnClickListener { presenter.onContinueClicked() }
        mBinding?.buttonForget?.setOnClickListener { presenter.onForgetWalletClicked() }

        onViewReady()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun restartPage() {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun getPassword(): String {
        return mBinding?.fieldPassword?.text.toString()
    }

    override fun resetPasswordField() {
        if (!isFinishing) mBinding?.fieldPassword?.setText("")
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) {
        if (mProgressDialog != null) {
            mProgressDialog?.setMessage(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)
        }
    }

    override fun showForgetWalletWarning(callback: DialogButtonCallback) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.warning)
            .setMessage(R.string.forget_wallet_warning)
            .setPositiveButton(R.string.forget_wallet) { dialogInterface, i -> callback.onPositiveClicked() }
            .setNegativeButton(android.R.string.cancel) { dialogInterface, i -> callback.onNegativeClicked() }
            .create()
            .show()
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        password: String
    ) {
        ViewUtils.hideKeyboard(this)

        val editText = AppCompatEditText(this)
        editText.setHint(R.string.two_factor_dialog_hint)
        val message: Int
        if (authType == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR) {
            message = R.string.two_factor_dialog_message_authenticator
            editText.inputType = InputType.TYPE_NUMBER_VARIATION_NORMAL
            editText.keyListener = DigitsKeyListener.getInstance("1234567890")
        } else if (authType == Settings.AUTH_TYPE_SMS) {
            message = R.string.two_factor_dialog_message_sms
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        } else {
            throw IllegalArgumentException("Auth Type $authType should not be passed to this function")
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.two_factor_dialog_title)
            .setMessage(message)
            .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.submitTwoFactorCode(responseObject,
                    sessionId,
                    password,
                    editText.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    override fun showProgressDialog(@StringRes messageId: Int, suffix: String?, cancellable: Boolean) {
        dismissProgressDialog()
        mProgressDialog = MaterialProgressDialog(this).apply {
            setCancelable(cancellable)

            setMessage(if (suffix != null) getString(messageId) + "\n\n" + suffix
            else getString(messageId))

            setOnCancelListener { presenter.onProgressCancelled() }
        }
        if (!isFinishing) mProgressDialog?.show()
    }

    override fun dismissProgressDialog() {
        if (mProgressDialog?.isShowing == true) {
            mProgressDialog?.dismiss()
            mProgressDialog = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    override fun enforceFlagSecure(): Boolean {
        return true
    }

    override fun createPresenter(): PasswordRequiredPresenter? {
        return passwordRequiredPresenter
    }

    override fun getView(): PasswordRequiredView {
        return this
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Test for screen overlays before user enters PIN
        return overlayDetection.detectObscuredWindow(this, event) || super.dispatchTouchEvent(event)
    }

    override fun startLogoutTimer() {
        // No-op
    }
}
