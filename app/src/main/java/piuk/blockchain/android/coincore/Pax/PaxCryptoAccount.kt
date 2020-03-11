package piuk.blockchain.android.coincore.pax

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase

internal class PaxCryptoAccount(
    override val label: String,
    private val address: String
) : CryptoSingleAccountNonCustodialBase() {
    override val cryptoCurrency = CryptoCurrency.PAX
}
