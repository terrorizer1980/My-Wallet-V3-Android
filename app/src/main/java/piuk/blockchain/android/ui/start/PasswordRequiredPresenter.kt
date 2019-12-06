package piuk.blockchain.android.ui.start

import androidx.annotation.StringRes

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.rxkotlin.plusAssign
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.android.util.DialogButtonCallback
import retrofit2.Response
import timber.log.Timber

interface PasswordRequiredView : MvpView {

    val password: String
    fun resetPasswordField()
    fun goToPinPage()
    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
    fun restartPage()
    fun updateWaitingForAuthDialog(secondsRemaining: Int)
    fun showForgetWalletWarning(callback: DialogButtonCallback)
    fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        password: String
    )
}

class PasswordRequiredPresenter(
    private val appUtil: AppUtil,
    private val prefs: PersistentPrefs,
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val coinifyDataManager: CoinifyDataManager
) : MvpPresenter<PasswordRequiredView>() {

    override fun onViewAttached() { /* no-op */ }
    override fun onViewDetached() { /* no-op */ }

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    private var sessionId: String? = null
    private var waitingForAuth = false

    val isWaitingForAuth: Boolean
        get() = waitingForAuth

    fun onContinueClicked() {
        // Seems that on low memory devices it's quite possible that the view is null here
        view?.apply {
            if (password.length > 1) {
                verifyPassword(password)
            } else {
                showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
                restartPage()
            }
        }
    }

    fun onForgetWalletClicked() {
        view?.showForgetWalletWarning(
            object : DialogButtonCallback {
                override fun onPositiveClicked() {
                    // TODO: 14/06/2018 This doesn't wipe anything
                    /**
                     * Most data will be overwritten when the user logs in again, however we should
                     * really be clearing OR broadcasting via RxBus a logout message and having
                     * Data Managers clear up after themselves. See LogoutActivity for details.
                     *
                     * Here we're clearing BuyDataManager and CoinifyDataManager as we know for sure
                     * that they aren't overwritten on re-login due to caching strategies.
                     */
                    buyDataManager.wipe()
                    coinifyDataManager.clearAccessToken()
                    appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                }

                override fun onNegativeClicked() { /* No-op */ }
            }
        )
    }

    fun submitTwoFactorCode(
        responseObject: JSONObject,
        sessionId: String,
        password: String,
        code: String?
    ) {
        if (code.isNullOrEmpty()) {
            view?.showToast(R.string.two_factor_null_error, ToastCustom.TYPE_ERROR)
        } else {
            val guid = prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")
            compositeDisposable += authDataManager.submitTwoFactorCode(sessionId, guid, code)
                .doOnSubscribe {
                    view?.showProgressDialog(R.string.please_wait)
                }
                .doAfterTerminate { view?.dismissProgressDialog() }
                .subscribe(
                    { response ->
                        // This is slightly hacky, but if the user requires 2FA login,
                        // the payload comes in two parts. Here we combine them and
                        // parse/decrypt normally.
                        responseObject.put("payload", response.string())
                        val responseBody = responseObject.toString()
                            .toResponseBody("application/json".toMediaTypeOrNull())

                        val payload = Response.success(responseBody)
                        handleResponse(password, guid, payload)
                    },
                    {
                        showErrorToast(R.string.two_factor_incorrect_error)
                    }
                )
        }
    }

    private fun verifyPassword(password: String) {
        val guid = prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")
        waitingForAuth = true

        compositeDisposable += authDataManager.getSessionId(guid)
            .doOnSubscribe {
                view?.showProgressDialog(R.string.validating_password)
            }
            .doOnNext { s -> sessionId = s }
            .flatMap { sessionId -> authDataManager.getEncryptedPayload(guid, sessionId) }
            .subscribe(
                { response -> handleResponse(password, guid, response) },
                { throwable ->
                    Timber.e(throwable)
                    showErrorToastAndRestartApp(R.string.auth_failed)
                }
            )
    }

    private fun handleResponse(password: String, guid: String, response: Response<ResponseBody>) {
        val errorBody = if (response.errorBody() != null) response.errorBody()!!.string() else ""

        if (errorBody.contains(KEY_AUTH_REQUIRED)) {
            showCheckEmailDialog()

            compositeDisposable += authDataManager.startPollingAuthStatus(guid, sessionId!!)
                .subscribe(
                    { payloadResponse ->
                        waitingForAuth = false

                        if (payloadResponse == null || payloadResponse.contains(KEY_AUTH_REQUIRED)) {
                            showErrorToastAndRestartApp(R.string.auth_failed)
                        } else {
                            val responseBody = payloadResponse
                                .toResponseBody("application/json".toMediaTypeOrNull())
                            checkTwoFactor(password, Response.success(responseBody))
                        }
                    },
                    { throwable ->
                        Timber.e(throwable)
                        waitingForAuth = false
                        showErrorToastAndRestartApp(R.string.auth_failed)
                    }
                )
        } else {
            waitingForAuth = false
            checkTwoFactor(password, response)
        }
    }

    private fun checkTwoFactor(password: String, response: Response<ResponseBody>) {

        val responseBody = response.body()!!.string()
        val jsonObject = JSONObject(responseBody)
        // Check if the response has a 2FA Auth Type but is also missing the payload,
        // as it comes in two parts if 2FA enabled.
        if (jsonObject.isAuth() && (jsonObject.isGoogleAuth() || jsonObject.isSMSAuth())) {
            view?.dismissProgressDialog()
            view?.showTwoFactorCodeNeededDialog(
                jsonObject,
                sessionId!!,
                jsonObject.getInt("auth_type"),
                password
            )
        } else {
            attemptDecryptPayload(password, responseBody)
        }
    }

    private fun attemptDecryptPayload(password: String, payload: String) {
        compositeDisposable += payloadDataManager.initializeFromPayload(payload, password)
            .doOnComplete {
                appUtil.sharedKey = payloadDataManager.wallet!!.sharedKey

                prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, payloadDataManager.wallet!!.guid)
                prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
                prefs.removeValue(PersistentPrefs.KEY_PIN_IDENTIFIER)
            }
            .subscribe(
                { view?.goToPinPage() },
                { throwable ->
                    when (throwable) {
                        is HDWalletException -> showErrorToast(R.string.pairing_failed)
                        is DecryptionException -> showErrorToast(R.string.auth_failed)
                        else -> showErrorToastAndRestartApp(R.string.auth_failed)
                    }
                }
            )
    }

    private fun showCheckEmailDialog() {
        view?.showProgressDialog(R.string.check_email_to_auth_login, ::onProgressCancelled)

        compositeDisposable += authDataManager.createCheckEmailTimer()
            .takeUntil { !waitingForAuth }
            .subscribe(
                { integer ->
                    if (integer <= 0) {
                    // Only called if timer has run out
                        showErrorToastAndRestartApp(R.string.pairing_failed)
                    } else {
                        view?.updateWaitingForAuthDialog(integer!!)
                    }
            },
            {
                showErrorToast(R.string.auth_failed)
                waitingForAuth = false
            })
    }

    private fun onProgressCancelled() {
        waitingForAuth = false
        compositeDisposable.clear()
    }

    private fun showErrorToast(@StringRes message: Int) {
        view?.apply {
            dismissProgressDialog()
            resetPasswordField()
            showToast(message, ToastCustom.TYPE_ERROR)
        }
    }

    private fun showErrorToastAndRestartApp(@StringRes message: Int) {
        view?.apply {
            resetPasswordField()
            dismissProgressDialog()
            showToast(message, ToastCustom.TYPE_ERROR)
            appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
        }
    }

    companion object {
        const val KEY_AUTH_REQUIRED = "authorization_required"
    }
}

private fun JSONObject.isAuth(): Boolean =
    has("auth_type") && !has("payload")

private fun JSONObject.isGoogleAuth(): Boolean =
    getInt("auth_type") == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR

private fun JSONObject.isSMSAuth(): Boolean =
    getInt("auth_type") == Settings.AUTH_TYPE_SMS