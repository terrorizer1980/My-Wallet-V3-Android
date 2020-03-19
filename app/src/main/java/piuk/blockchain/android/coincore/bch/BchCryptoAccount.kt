package piuk.blockchain.android.coincore.bch

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class BchCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.BCH
}

internal class BchCryptoAccountNonCustodial(
    override val label: String,
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.BCH

    constructor(jsonAccount: GenericMetadataAccount) : this(jsonAccount.label, jsonAccount.xpub)
}