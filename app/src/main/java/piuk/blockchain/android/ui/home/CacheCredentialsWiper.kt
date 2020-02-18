package piuk.blockchain.android.ui.home

import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import info.blockchain.wallet.payload.PayloadManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.BuyConditions
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState

class CacheCredentialsWiper(

    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val payloadManager: PayloadManager,
    private val buyConditions: BuyConditions,
    private val coinifyDataManager: CoinifyDataManager,
    private val nabuDataManager: NabuDataManager,
    private val walletOptionsState: WalletOptionsState
) {
    fun wipe() {
        ethDataManager.clearEthAccountDetails()
        bchDataManager.clearBchAccountDetails()
        coinifyDataManager.clearAccessToken()
        nabuDataManager.clearAccessToken()
        payloadManager.clearMetadataNodeFactory()
        buyConditions.wipe()
        walletOptionsState.wipe()
    }
}