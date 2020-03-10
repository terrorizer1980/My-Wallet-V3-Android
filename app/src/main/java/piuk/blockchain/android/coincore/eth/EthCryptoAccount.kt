package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.ETHTokens
import piuk.blockchain.android.coincore.model.ActivitySummaryList
import piuk.blockchain.android.coincore.model.BaseCryptoAccount

internal class EthCryptoAccountCustodial {

}

internal class EthCryptoAccountNonCustodial(
    override val label: String,
    private val address: String,
    private val token: ETHTokens
) : BaseCryptoAccount() {
    override val cryptoCurrency = token.asset

    constructor(asset: ETHTokens, jsonAccount: EthereumAccount)
        : this(jsonAccount.label, jsonAccount.address, asset)

    override val balance: Single<CryptoValue>
        get() = token.noncustodialBalance()

    override val receiveAddress: Single<String>
        get() = Single.just(address)

    override val activity: Single<ActivitySummaryList>
        get() = token.getTransactions().singleOrError()

    override val actions: AvailableActions
        get() = availableActions


    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send,
        AssetAction.Receive,
        AssetAction.Swap
    )
}