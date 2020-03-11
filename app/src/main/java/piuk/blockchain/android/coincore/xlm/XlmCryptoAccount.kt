package piuk.blockchain.android.coincore.xlm

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class XlmCryptoAccount(
    override val label: String = "",
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.XLM
}