package piuk.blockchain.android.ui.auth

import android.annotation.SuppressLint
import android.support.annotation.StringRes
import android.support.annotation.UiThread
import android.support.annotation.VisibleForTesting
import android.view.View
import com.crashlytics.android.answers.LoginEvent
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.api.data.UpdateType
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.PayloadException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import org.spongycastle.crypto.InvalidCipherTextException
import java.net.SocketTimeoutException
import java.util.Arrays
import javax.inject.Inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.home.SecurityPromptDialog
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.DialogButtonCallback
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.annotations.Thunk
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber

class PinEntryPresenter @Inject constructor(
    private val mAuthDataManager: AuthDataManager,
    val appUtil: AppUtil,
    private val prefs: PersistentPrefs,
    private val mPayloadDataManager: PayloadDataManager,
    private val mStringUtils: StringUtils,
    private val mFingerprintHelper: FingerprintHelper,
    private val mAccessState: AccessState,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val prngFixer: PrngFixer
) :
    BasePresenter<PinEntryView>() {

    @VisibleForTesting
    var mCanShowFingerprintDialog = true
    @VisibleForTesting
    var isForValidatingPinForResult = false
    @VisibleForTesting
    var mUserEnteredPin = ""
    @VisibleForTesting
    var mUserEnteredConfirmationPin: String? = null
    @VisibleForTesting
    internal var bAllowExit = true

    internal val ifShouldShowFingerprintLogin: Boolean
        get() = (!(isForValidatingPinForResult || isCreatingNewPin) &&
                mFingerprintHelper.isFingerprintUnlockEnabled() &&
                mFingerprintHelper.getEncryptedData(PersistentPrefs.KEY_ENCRYPTED_PIN_CODE) != null)

    val isCreatingNewPin: Boolean
        get() = prefs.getValue(PersistentPrefs.KEY_PIN_IDENTIFIER, "").isEmpty()

    private val isChangingPin: Boolean
        get() = isCreatingNewPin && mAccessState.pin?.isNotEmpty() ?: false

    override fun onViewReady() {
        prngFixer.applyPRNGFixes()

        if (view.pageIntent != null) {
            val extras = view.pageIntent?.extras
            if (extras != null) {
                if (extras.containsKey(KEY_VALIDATING_PIN_FOR_RESULT)) {
                    isForValidatingPinForResult = extras.getBoolean(KEY_VALIDATING_PIN_FOR_RESULT)
                }
            }
        }

        checkPinFails()
        checkFingerprintStatus()
        doTestnetCheck()
    }

    private fun doTestnetCheck() {
        if (environmentSettings.environment == Environment.TESTNET) {
            view.showTestnetWarning()
        }
    }

    fun checkFingerprintStatus() {
        if (ifShouldShowFingerprintLogin) {
            view.showFingerprintDialog(
                mFingerprintHelper.getEncryptedData(PersistentPrefs.KEY_ENCRYPTED_PIN_CODE)!!)
        } else {
            view.showKeyboard()
        }
    }

    fun canShowFingerprintDialog(): Boolean {
        return mCanShowFingerprintDialog
    }

    fun loginWithDecryptedPin(pincode: String) {
        mCanShowFingerprintDialog = false
        for (view in view.pinBoxList) {
            view.setImageResource(R.drawable.rounded_view_dark_blue)
        }
        validatePIN(pincode)
    }

    fun onDeleteClicked() {
        if (mUserEnteredPin.isNotEmpty()) {
            // Remove last char from pin string
            mUserEnteredPin = mUserEnteredPin.substring(0, mUserEnteredPin.length - 1)

            // Clear last box
            view.pinBoxList[mUserEnteredPin.length].setImageResource(R.drawable.rounded_view_blue_white_border)
        }
    }

    fun onPadClicked(string: String?) {
        if (string == null || mUserEnteredPin.length == PIN_LENGTH) {
            return
        }

        // Append tapped #
        mUserEnteredPin += string

        for (i in 0 until mUserEnteredPin.length) {
            // Ensures that all necessary dots are filled
            view.pinBoxList[i].setImageResource(R.drawable.rounded_view_dark_blue)
        }

        // Perform appropriate action if PIN_LENGTH has been reached
        if (mUserEnteredPin.length == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (mUserEnteredPin == "0000") {
                showErrorToast(R.string.zero_pin)
                clearPinViewAndReset()
                if (isCreatingNewPin) {
                    view.setTitleString(R.string.create_pin)
                }
                return
            }

            // Only show warning on first entry and if user is creating a new PIN
            if (isCreatingNewPin && isPinCommon(mUserEnteredPin) && mUserEnteredConfirmationPin == null) {
                view.showCommonPinWarning(object : DialogButtonCallback {
                    override fun onPositiveClicked() {
                        clearPinViewAndReset()
                    }

                    override fun onNegativeClicked() {
                        validateAndConfirmPin()
                    }
                })

                // If user is changing their PIN and it matches their old one, disallow it
            } else if (isChangingPin &&
                mUserEnteredConfirmationPin == null &&
                mAccessState.pin == mUserEnteredPin
            ) {
                showErrorToast(R.string.change_pin_new_matches_current)
                clearPinViewAndReset()
            } else {
                validateAndConfirmPin()
            }
        }
    }

    @Thunk
    internal fun validateAndConfirmPin() {
        // Validate
        if (!prefs.getValue(PersistentPrefs.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            view.setTitleVisibility(View.INVISIBLE)
            validatePIN(mUserEnteredPin)
        } else if (mUserEnteredConfirmationPin == null) {
            // End of Create -  Change to Confirm
            mUserEnteredConfirmationPin = mUserEnteredPin
            mUserEnteredPin = ""
            view.setTitleString(R.string.confirm_pin)
            clearPinBoxes()
        } else if (mUserEnteredConfirmationPin == mUserEnteredPin) {
            // End of Confirm - Pin is confirmed
            createNewPin(mUserEnteredPin)
        } else {
            // End of Confirm - Pin Mismatch
            showErrorToast(R.string.pin_mismatch_error)
            view.setTitleString(R.string.create_pin)
            clearPinViewAndReset()
        }
    }

    /**
     * Resets the view without restarting the page
     */
    @Thunk
    internal fun clearPinViewAndReset() {
        clearPinBoxes()
        mUserEnteredConfirmationPin = null
        checkFingerprintStatus()
    }

    fun clearPinBoxes() {
        mUserEnteredPin = ""
        view?.clearPinBoxes()
    }

    @VisibleForTesting
    fun updatePayload(password: String) {
        view.showProgressDialog(R.string.decrypting_wallet, null)

        compositeDisposable.add(
            mPayloadDataManager.initializeAndDecrypt(
                prefs.getValue(PersistentPrefs.KEY_SHARED_KEY, ""),
                prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, ""),
                password)
                .doAfterTerminate {
                    view.dismissProgressDialog()
                    mCanShowFingerprintDialog = true
                }
                .subscribe({
                    appUtil.sharedKey = mPayloadDataManager.wallet!!.sharedKey

                    setAccountLabelIfNecessary()

                    Logging.logLogin(LoginEvent().putSuccess(true))

                    if (!mPayloadDataManager.wallet!!.isUpgraded) {
                        view.goToUpgradeWalletActivity()
                    } else {
                        appUtil.restartAppWithVerifiedPin(LauncherActivity::class.java)
                    }
                }, { throwable ->
                    Logging.logLogin(LoginEvent().putSuccess(false))
                    if (throwable is InvalidCredentialsException) {
                        view.goToPasswordRequiredActivity()
                    } else if (throwable is ServerConnectionException || throwable is SocketTimeoutException) {
                        view.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR)
                        appUtil.restartApp(LauncherActivity::class.java)
                    } else if (throwable is UnsupportedVersionException) {
                        view.showWalletVersionNotSupportedDialog(throwable.message)
                    } else if (throwable is DecryptionException) {
                        view.goToPasswordRequiredActivity()
                    } else if (throwable is PayloadException) {
                        // This shouldn't happen - Payload retrieved from server couldn't be parsed
                        view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                        appUtil.restartApp(LauncherActivity::class.java)
                    } else if (throwable is HDWalletException) {
                        // This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                        view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                        appUtil.restartApp(LauncherActivity::class.java)
                    } else if (throwable is InvalidCipherTextException) {
                        // Password changed on web, needs re-pairing
                        view.showToast(R.string.password_changed_explanation, ToastCustom.TYPE_ERROR)
                        mAccessState.pin = null
                        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                    } else if (throwable is AccountLockedException) {
                        view.showAccountLockedDialog()
                    } else {
                        Logging.logException(throwable)
                        view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                        appUtil.restartApp(LauncherActivity::class.java)
                    }
                }))
    }

    fun validatePassword(password: String) {
        view.showProgressDialog(R.string.validating_password, null)

        compositeDisposable.add(
            mPayloadDataManager.initializeAndDecrypt(
                prefs.getValue(PersistentPrefs.KEY_SHARED_KEY, ""),
                prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, ""),
                password)
                .doAfterTerminate { view.dismissProgressDialog() }
                .subscribe({
                    view.showToast(R.string.pin_4_strikes_password_accepted, ToastCustom.TYPE_OK)
                    prefs.removeValue(PersistentPrefs.KEY_PIN_FAILS)
                    prefs.removeValue(PersistentPrefs.KEY_PIN_IDENTIFIER)
                    mAccessState.pin = null
                    view.restartPageAndClearTop()
                }, { throwable ->

                    if (throwable is ServerConnectionException || throwable is SocketTimeoutException) {
                        view.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR)
                    } else if (throwable is PayloadException) {
                        // This shouldn't happen - Payload retrieved from server couldn't be parsed
                        view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                        appUtil.restartApp(LauncherActivity::class.java)
                    } else if (throwable is HDWalletException) {
                        // This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                        view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                        appUtil.restartApp(LauncherActivity::class.java)
                    } else if (throwable is AccountLockedException) {
                        view.showAccountLockedDialog()
                    } else {
                        Logging.logException(throwable)
                        showErrorToast(R.string.invalid_password)
                        view.showValidationDialog()
                    }
                }))
    }

    private fun createNewPin(pin: String) {
        val tempPassword = mPayloadDataManager.tempPassword
        if (tempPassword == null) {
            showErrorToast(R.string.create_pin_failed)
            prefs.clear()
            appUtil.restartApp(LauncherActivity::class.java)
            return
        }

        compositeDisposable.add(
            mAuthDataManager.createPin(tempPassword, pin)
                .doOnSubscribe { disposable -> view.showProgressDialog(R.string.creating_pin, null) }
                .subscribe({
                    view.dismissProgressDialog()
                    mFingerprintHelper.clearEncryptedData(PersistentPrefs.KEY_ENCRYPTED_PIN_CODE)
                    mFingerprintHelper.setFingerprintUnlockEnabled(false)
                    prefs.setValue(PersistentPrefs.KEY_PIN_FAILS, 0)
                    updatePayload(tempPassword)
                }, { throwable ->
                    showErrorToast(R.string.create_pin_failed)
                    prefs.clear()
                    appUtil.restartApp(LauncherActivity::class.java)
                }))
    }

    @SuppressLint("CheckResult")
    private fun validatePIN(pin: String) {
        view.showProgressDialog(R.string.validating_pin, null)

        mAuthDataManager.validatePin(pin)
            .subscribe({ password ->
                view.dismissProgressDialog()
                if (password != null) {
                    if (isForValidatingPinForResult) {
                        view.finishWithResultOk(pin)
                    } else {
                        updatePayload(password)
                    }
                    prefs.setValue(PersistentPrefs.KEY_PIN_FAILS, 0)
                } else {
                    handleValidateFailure()
                }
            }, { throwable ->
                Timber.e(throwable)
                if (throwable is InvalidCredentialsException) {
                    handleValidateFailure()
                } else {
                    showErrorToast(R.string.api_fail)
                    view.restartPageAndClearTop()
                }
            })
    }

    private fun handleValidateFailure() {
        if (isForValidatingPinForResult) {
            incrementFailureCount()
        } else {
            incrementFailureCountAndRestart()
        }
    }

    private fun incrementFailureCount() {
        var fails = prefs.getValue(PersistentPrefs.KEY_PIN_FAILS, 0)
        prefs.setValue(PersistentPrefs.KEY_PIN_FAILS, ++fails)
        showErrorToast(R.string.invalid_pin)
        mUserEnteredPin = ""
        for (textView in view.pinBoxList) {
            textView.setImageResource(R.drawable.rounded_view_blue_white_border)
        }
        view.setTitleVisibility(View.VISIBLE)
        view.setTitleString(R.string.pin_entry)
    }

    fun incrementFailureCountAndRestart() {
        var fails = prefs.getValue(PersistentPrefs.KEY_PIN_FAILS, 0)
        prefs.setValue(PersistentPrefs.KEY_PIN_FAILS, ++fails)
        showErrorToast(R.string.invalid_pin)
        view.restartPageAndClearTop()
    }

    // Check user's password if PIN fails >= 4
    private fun checkPinFails() {
        val fails = prefs.getValue(PersistentPrefs.KEY_PIN_FAILS, 0)
        if (fails >= MAX_ATTEMPTS) {
            showErrorToast(R.string.pin_4_strikes)
            view.showMaxAttemptsDialog()
        }
    }

    private fun setAccountLabelIfNecessary() {
        if (mAccessState.isNewlyCreated &&
            !mPayloadDataManager.accounts.isEmpty() &&
            mPayloadDataManager.getAccount(0) != null &&
            (mPayloadDataManager.getAccount(0).label == null ||
                    mPayloadDataManager.getAccount(0).label.isEmpty())
        ) {

            mPayloadDataManager.getAccount(0).label = mStringUtils.getString(R.string.default_wallet_name)
        }
    }

    private fun isPinCommon(pin: String): Boolean {
        val commonPins = Arrays.asList("1234", "1111", "1212", "7777", "1004")
        return commonPins.contains(pin)
    }

    fun resetApp() {
        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    fun allowExit(): Boolean {
        return bAllowExit
    }

    @UiThread
    private fun showErrorToast(@StringRes message: Int) {
        view.dismissProgressDialog()
        view.showToast(message, ToastCustom.TYPE_ERROR)
    }

    internal fun clearLoginState() {
        mAccessState.logout()
    }

    @SuppressLint("CheckResult")
    fun fetchInfoMessage() {
        walletOptionsDataManager.fetchInfoMessage(view.locale)
            .compose(RxUtil.addObservableToCompositeDisposable(this))
            .subscribe({ message ->
                if (!message.isEmpty())
                    view.showCustomPrompt(getWarningPrompt(message))
            }) { Timber.e(it) }
    }

    @SuppressLint("CheckResult")
    fun checkForceUpgradeStatus(versionName: String) {
        walletOptionsDataManager.checkForceUpgrade(versionName)
            .compose(RxUtil.addObservableToCompositeDisposable(this))
            .subscribe(
                { updateType ->
                    if (updateType !== UpdateType.NONE)
                        view.appNeedsUpgrade(updateType === UpdateType.FORCE)
                }) { Timber.e(it) }
    }

    private fun getWarningPrompt(message: String): SecurityPromptDialog {
        val prompt = SecurityPromptDialog.newInstance(
            R.string.information,
            message,
            R.drawable.vector_help,
            R.string.ok_cap,
            false,
            false)
        prompt.positiveButtonListener = {
            prompt.dismiss()
            Unit
        }
        return prompt
    }

    companion object {
        private val PIN_LENGTH = 4
        private val MAX_ATTEMPTS = 4
    }
}
