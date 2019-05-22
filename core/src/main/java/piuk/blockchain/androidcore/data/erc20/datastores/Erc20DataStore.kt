package piuk.blockchain.androidcore.data.erc20.datastores

import piuk.blockchain.androidcore.data.datastores.SimpleDataStore
import piuk.blockchain.androidcore.data.erc20.Erc20DataModel

class Erc20DataStore : SimpleDataStore {

    var erc20DataModel: Erc20DataModel? = null

    override fun clearData() {
        erc20DataModel = null
    }
}