package piuk.blockchain.android.coincore.eth

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class EthCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.ETHER
}

internal class EthCryptoAccountNonCustodial(
    override val label: String,
    private val address: String,
    private val token: EthTokens
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = token.asset

    constructor(asset: EthTokens, jsonAccount: EthereumAccount)
        : this(jsonAccount.label, jsonAccount.address, asset)

    override val balance: Single<CryptoValue>
        get() = token.noncustodialBalance()

    override val receiveAddress: Single<String>
        get() = Single.just(address)

    override val activity: Single<ActivitySummaryList>
        get() = token.getTransactions().singleOrError()
}