package piuk.blockchain.android.ui.backup.completed

import com.blockchain.preferences.WalletStatus
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class BackupWalletCompletedPresenter(
    private val transferFundsDataManager: TransferFundsDataManager,
    private val walletStatus: WalletStatus
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = walletStatus.lastBackupTime
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }

    internal fun checkTransferableFunds() {
        compositeDisposable += transferFundsDataManager.transferableFundTransactionListForDefaultAccount
            .subscribe({ triple ->
                if (triple.left.isNotEmpty()) {
                    view.showTransferFundsPrompt()
                }
            }, { Timber.e(it) })
    }
}
