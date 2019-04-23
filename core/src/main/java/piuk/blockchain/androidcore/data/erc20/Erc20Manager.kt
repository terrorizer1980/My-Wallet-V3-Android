package piuk.blockchain.androidcore.data.erc20

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import java.math.BigInteger

class Erc20Manager(private val ethDataManager: EthDataManager) {

    fun getBalance(currency: CryptoCurrency): Single<BigInteger> = ethDataManager.getErc20Address(currency).map {
        it.balance
    }.singleOrError().onErrorReturn {
        0.toBigInteger()
    }
}