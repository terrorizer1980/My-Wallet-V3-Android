package piuk.blockchain.android.ui.home

import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.BuyConditions
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState

class CacheCredentialsWiper(

    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val metadataManager: MetadataManager,
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
        metadataManager.reset()
        buyConditions.wipe()
        walletOptionsState.wipe()
    }
}