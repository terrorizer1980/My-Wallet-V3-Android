package piuk.blockchain.androidcore.data.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.math.BigInteger

class UsdtAccount(
    override val ethDataManager: EthDataManager,
    override val dataStore: Erc20DataStore,
    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.USDT,
    environmentSettings: EnvironmentConfig
) : Erc20BaseAccount(environmentSettings)