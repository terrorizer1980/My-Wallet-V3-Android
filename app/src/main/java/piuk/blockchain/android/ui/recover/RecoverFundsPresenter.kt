package piuk.blockchain.android.ui.recover

import info.blockchain.wallet.bip44.HDWalletFactory

import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException

import java.util.Locale

import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber

class RecoverFundsPresenter : BasePresenter<RecoverFundsView>() {

    override fun onViewReady() {
        // No-op
    }

    fun onContinueClicked(recoveryPhrase: String) {
        if (recoveryPhrase.isEmpty()) {
            view.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR)
            return
        }

        try {
            if (isValidMnemonic(recoveryPhrase)) {
                view.gotoCredentialsActivity(recoveryPhrase)
            } else {
                view.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR)
            }
        } catch (e: Exception) {
            // This should never happen
            Timber.wtf(e)
            view.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR)
        }
    }

    /**
     * We only support US english mnemonics atm
     */
    private fun isValidMnemonic(recoveryPhrase: String): Boolean {
        val words = recoveryPhrase.trim().split("\\s+".toRegex())

        val wis = HDWalletFactory::class.java.classLoader
            .getResourceAsStream("wordlist/" + Locale("en", "US") + ".txt")
            ?: throw MnemonicException.MnemonicWordException("cannot read BIP39 word list")

        val mc = MnemonicCode(wis, null)

        return try {
            mc.check(words)
            true
        } catch (e: MnemonicException) {
            false
        }
    }
}
