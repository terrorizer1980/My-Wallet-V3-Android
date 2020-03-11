package piuk.blockchain.android.coincore.btc

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.Account
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class BtcCryptoAccount(
    override val label: String,
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.BTC

    constructor(jsonAccount: Account)
        : this(jsonAccount.label, jsonAccount.xpub)
}