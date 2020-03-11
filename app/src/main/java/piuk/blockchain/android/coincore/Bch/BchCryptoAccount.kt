package piuk.blockchain.android.coincore.bch

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class BchCryptoAccount(
    override val label: String,
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.BCH

    constructor(jsonAccount: GenericMetadataAccount)
        : this(jsonAccount.label, jsonAccount.xpub)
}