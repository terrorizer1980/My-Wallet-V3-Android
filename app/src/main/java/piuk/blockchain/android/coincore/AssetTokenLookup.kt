package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.bch.BchTokens
import piuk.blockchain.android.coincore.pax.PaxTokens
import piuk.blockchain.android.coincore.btc.BtcTokens
import piuk.blockchain.android.coincore.eth.EthTokens
import piuk.blockchain.android.coincore.stx.StxTokens
import piuk.blockchain.android.coincore.xlm.XlmTokens

class AssetTokenLookup internal constructor(
    private val btcTokens: BtcTokens,
    private val bchTokens: BchTokens,
    private val ethTokens: EthTokens,
    private val xlmTokens: XlmTokens,
    private val paxTokens: PaxTokens,
    private val stxTokens: StxTokens
) {
    operator fun get(cryptoCurrency: CryptoCurrency): AssetTokens =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> btcTokens
            CryptoCurrency.ETHER -> ethTokens
            CryptoCurrency.BCH -> bchTokens
            CryptoCurrency.XLM -> xlmTokens
            CryptoCurrency.PAX -> paxTokens
            CryptoCurrency.STX -> stxTokens
        }

    fun init() { /* TODO: When second password is removed, init the coin metadata here */ }
}
