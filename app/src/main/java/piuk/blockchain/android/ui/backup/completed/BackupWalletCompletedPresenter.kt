package piuk.blockchain.android.ui.backup.completed

import android.annotation.SuppressLint
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class BackupWalletCompletedPresenter(
    private val transferFundsDataManager: TransferFundsDataManager,
    private val prefs: PersistentPrefs
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = prefs.getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0L)
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }

    @SuppressLint("CheckResult")
    internal fun checkTransferableFunds() {
        transferFundsDataManager.transferableFundTransactionListForDefaultAccount
            .addToCompositeDisposable(this)
            .subscribe({ triple ->
                if (!triple.left.isEmpty()) {
                    view.showTransferFundsPrompt()
                }
            }, { Timber.e(it) })
    }
}
