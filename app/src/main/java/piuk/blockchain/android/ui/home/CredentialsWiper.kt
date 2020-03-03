package piuk.blockchain.android.ui.home

import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import info.blockchain.wallet.payload.PayloadManagerWiper
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.erc20.Erc20Account

class CredentialsWiper(
    private val payloadManagerWiper: PayloadManagerWiper,
    private val paxAccount: Erc20Account,
    private val buyDataManager: BuyDataManager,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val accessState: AccessState,
    private val appUtil: AppUtil

) {
    fun unload() {
        payloadManagerWiper.wipe()
        accessState.logout()
        accessState.unpairWallet()
        appUtil.restartApp(LauncherActivity::class.java)
        accessState.clearPin()
        buyDataManager.wipe()
        paxAccount.clear()
        buyDataManager.wipe()
        shapeShiftDataManager.clearShapeShiftData()
        paxAccount.clear()
    }
}