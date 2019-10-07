package piuk.blockchain.android.ui.login

import android.annotation.SuppressLint
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.PairingEvent
import piuk.blockchain.androidcoreui.utils.logging.PairingMethod
import javax.net.ssl.SSLPeerUnverifiedException

class LoginPresenter(
    private val appUtil: AppUtil,
    private val _payloadDataManager: Lazy<PayloadDataManager>,
    private val prefs: PersistentPrefs,
    private val analytics: Analytics
) : BasePresenter<LoginView>() {

    override fun onViewReady() {
        // No-op
    }

    private val payloadDataManager: PayloadDataManager
        get() = _payloadDataManager.value

    @SuppressLint("CheckResult")
    internal fun pairWithQR(raw: String?) {
        appUtil.clearCredentials()

        if (raw == null) view.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)

        val dataManager = payloadDataManager
        dataManager.handleQrCode(raw!!)
            .addToCompositeDisposable(this)
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doOnComplete { appUtil.sharedKey = dataManager.wallet!!.sharedKey }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribe({
                prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, dataManager.wallet!!.guid)
                prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
                prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
                view.startPinEntryActivity()
                analytics.logEvent(AnalyticsEvents.WalletAutoPairing)

                Logging.logCustom(
                    PairingEvent()
                        .putMethod(PairingMethod.QR_CODE)
                        .putSuccess(true)
                )
            }, { throwable ->
                Logging.logCustom(
                    PairingEvent()
                        .putMethod(PairingMethod.QR_CODE)
                        .putSuccess(false)
                )

                if (throwable is SSLPeerUnverifiedException) {
                    // BaseActivity handles message
                    appUtil.clearCredentials()
                } else {
                    view.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                }
            })
    }
}