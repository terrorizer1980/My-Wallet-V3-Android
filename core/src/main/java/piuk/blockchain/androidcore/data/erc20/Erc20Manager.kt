package piuk.blockchain.androidcore.data.erc20

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import java.math.BigInteger

class Erc20Manager(private val ethDataManager: EthDataManager) {

    fun getBalance(currency: CryptoCurrency): Single<BigInteger> =
        ethDataManager.getErc20Address(currency).map {
            it.balance
        }.singleOrError().onErrorReturn {
            0.toBigInteger()
        }

    fun createErc20Transaction(
        nonce: BigInteger,
        to: String,
        contractAddress: String,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        amount: BigInteger
    ): RawTransaction? =
        RawTransaction.createTransaction(
            nonce,
            gasPriceWei,
            gasLimitGwei,
            contractAddress,
            0.toBigInteger(),
            data(to, amount))

    private fun data(to: String, amount: BigInteger): String {
        val transferMethodHex = "0xa9059cbb"

        return transferMethodHex + TypeEncoder.encode(Address(to)) +
                TypeEncoder.encode(org.web3j.abi.datatypes.generated.Uint256(
                    amount))
    }
}