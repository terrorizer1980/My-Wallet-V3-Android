package piuk.blockchain.android.ui.onboarding

import android.support.annotation.VisibleForTesting
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy

import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper

internal class OnboardingPresenter constructor(
    private val fingerprintHelper: FingerprintHelper,
    private val accessState: AccessState,
    private val settingsDataManager: SettingsDataManager
) : BasePresenter<OnboardingView>() {

    private val showEmailOnly: Boolean by lazy { view.isEmailOnly }

    @VisibleForTesting
    internal var email: String? = null

    override fun onViewReady() {
        compositeDisposable += settingsDataManager.getSettings()
            .doAfterTerminate { this.checkAppState() }
            .subscribeBy(
                onNext = { settings -> email = settings.email },
                onError = { it.printStackTrace() }
            )
    }

    /**
     * Checks status of fingerprint hardware and either prompts the user to verify their fingerprint
     * or enroll one if the fingerprint sensor has never been set up.
     */
    internal fun onEnableFingerprintClicked() {
        if (fingerprintHelper.isFingerprintAvailable()) {
            val pin = accessState.pin

            if (pin.isNotEmpty()) {
                view.showFingerprintDialog(pin)
            } else {
                throw IllegalStateException("PIN not found")
            }
        } else if (fingerprintHelper.isHardwareDetected()) {
            // Hardware available but user has never set up fingerprints
            view.showEnrollFingerprintsDialog()
        } else {
            throw IllegalStateException("Fingerprint hardware not available, yet functions requiring hardware called.")
        }
    }

    /**
     * Sets fingerprint unlock enabled and clears the encrypted PIN if {@param enabled} is false
     *
     * @param enabled Whether or not the fingerprint unlock feature is set up
     */
    internal fun setFingerprintUnlockEnabled(enabled: Boolean) {
        fingerprintHelper.setFingerprintUnlockEnabled(enabled)
        if (!enabled) {
            fingerprintHelper.clearEncryptedData(PersistentPrefs.KEY_ENCRYPTED_PIN_CODE)
        }
    }

    private fun checkAppState() {
        when {
            showEmailOnly -> view.showEmailPrompt()
            fingerprintHelper.isHardwareDetected() -> view.showFingerprintPrompt()
            else -> view.showEmailPrompt()
        }
    }

    internal fun disableAutoLogout() {
        accessState.canAutoLogout = false
    }

    internal fun enableAutoLogout() {
        accessState.canAutoLogout = true
    }
}
