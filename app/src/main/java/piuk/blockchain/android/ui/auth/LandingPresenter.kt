package piuk.blockchain.android.ui.auth

import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class LandingPresenter(
    private val environmentSettings: EnvironmentConfig,
    private val prefs: PersistentPrefs,
    private val rootUtil: RootUtil
) : BasePresenter<LandingView>() {

    override fun onViewReady() {
        if (environmentSettings.shouldShowDebugMenu()) {
            with(view) {
                showToast(
                    "Current environment: ${environmentSettings.environment.getName()}",
                    ToastCustom.TYPE_GENERAL
                )
                showDebugMenu()
            }
        }
    }

    internal fun checkForRooted() {
        if (rootUtil.isDeviceRooted && !prefs.disableRootedWarning) {
            view.showIsRootedWarning()
        }
    }
}
