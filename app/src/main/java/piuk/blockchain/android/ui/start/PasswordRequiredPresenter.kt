package piuk.blockchain.android.ui.start

import piuk.blockchain.android.R
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil

interface PasswordRequiredView : PasswordAuthView {
    fun restartPage()
    fun showForgetWalletWarning(onForgetConfirmed: () -> Unit)
}

class PasswordRequiredPresenter(
    override val appUtil: AppUtil,
    override val prefs: PersistentPrefs,
    override val authDataManager: AuthDataManager,
    override val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val coinifyDataManager: CoinifyDataManager
) : PasswordAuthPresenter<PasswordRequiredView>() {

    fun onContinueClicked(password: String) {
        if (password.length > 1) {
            val guid = prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")
            verifyPassword(guid, password)
        } else {
            view?.apply {
                showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
                restartPage()
            }
        }
    }

    fun onForgetWalletClicked() {
        view?.showForgetWalletWarning {
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
    }

    override fun onAuthFailed() {
        showErrorToastAndRestartApp(R.string.auth_failed)
    }

    override fun onAuthComplete() { /* No op */ }
}
