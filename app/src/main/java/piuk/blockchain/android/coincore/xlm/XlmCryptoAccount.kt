package piuk.blockchain.android.coincore.xlm

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class XlmCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.XLM
}

internal class XlmCryptoAccountNonCustodial(
    override val label: String = "",
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.XLM
}