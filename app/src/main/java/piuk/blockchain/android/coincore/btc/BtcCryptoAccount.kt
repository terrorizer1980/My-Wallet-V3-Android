package piuk.blockchain.android.coincore.btc

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.Account
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class BtcCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.BTC
}

internal class BtcCryptoAccountNonCustodial(
    override val label: String,
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.BTC

    constructor(jsonAccount: Account) : this(jsonAccount.label, jsonAccount.xpub)
}