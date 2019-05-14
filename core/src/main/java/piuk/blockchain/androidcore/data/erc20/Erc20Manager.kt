package piuk.blockchain.androidcore.data.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.math.BigInteger

class Erc20Manager(
    private val ethDataManager: EthDataManager,
    private val erc20DataStore: Erc20DataStore,
    private val environmentSettings: EnvironmentConfig
) {

    /**
     * Clears the currently stored ERC20 account and [Erc20AddressResponse] from memory.
     */
    fun clearErc20AccountDetails() = erc20DataStore.clearData()

    /**
     * Returns an [Erc20AddressResponse] object for a given ERC20 address as an [Observable]. An
     * [Erc20DataModel] contains a list of transactions associated with the account, as well
     * as a final balance. Calling this function also caches the [Erc20DataModel].
     *
     * @return An [Observable] wrapping an [Erc20DataModel]
     */
    fun fetchErc20Address(): Observable<Erc20DataModel> =
        if (environmentSettings.environment == Environment.TESTNET) {
            Observable.just(Erc20DataModel(Erc20AddressResponse()))
                .doOnNext { erc20DataStore.erc20DataModel = null }
        } else {
            ethDataManager.getErc20Address(CryptoCurrency.PAX).map {
                Erc20DataModel(it)
            }.doOnNext {
                erc20DataStore.erc20DataModel = it
            }.subscribeOn(Schedulers.io())
        }

    fun getTransactions(): Observable<List<Erc20Transfer>> =
        erc20DataStore.erc20DataModel?.let { model ->
            Observable.just(model.transfers)
                .applySchedulers()
        } ?: Observable.empty()

    fun getErc20AccountHash(): Observable<String> =
        erc20DataStore.erc20DataModel?.let { model ->
            Observable.just(model.accountHash)
                .applySchedulers()
        } ?: Observable.empty()

    /**
     * Returns the user's ERC20 account object if previously fetched.
     *
     * @return A nullable [Erc20DataModel] object
     */
    fun getErc20Model(): Erc20DataModel? = erc20DataStore.erc20DataModel

    fun fetchErc20AddressCompletable(): Completable = Completable.fromObservable(fetchErc20Address())

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