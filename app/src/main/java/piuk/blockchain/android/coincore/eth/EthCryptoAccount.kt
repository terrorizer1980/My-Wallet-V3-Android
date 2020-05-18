package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class EthCryptoAccountNonCustodial(
    override val label: String,
    private val address: String,
    private val ethDataManager: EthDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.ETHER)

    constructor(
        ethDataManager: EthDataManager,
        jsonAccount: EthereumAccount,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        jsonAccount.label,
        jsonAccount.address,
        ethDataManager,
        exchangeRates
    )

    override val balance: Single<CryptoValue>
        get() = ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }

    override val receiveAddress: Single<String>
        get() = Single.just(address)

    override val activity: Single<ActivitySummaryList>
        get() = ethDataManager.getLatestBlock()
            .singleOrError()
            .flatMap { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map {
                        val ethFeeForPaxTransaction = it.to.equals(
                            ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                            ignoreCase = true
                        )
                        EthActivitySummaryItem(
                            ethDataManager,
                            it,
                            ethFeeForPaxTransaction,
                            latestBlock.blockHeight,
                            exchangeRates,
                            account = this
                        ) as ActivitySummaryItem
                    }.toList()
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override val isDefault: Boolean = true // Only one ETH account, so always default
}
