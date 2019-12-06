package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

class AssetTokenLookup(
    private val btcTokens: BTCTokens,
    private val bchTokens: BCHTokens,
    private val ethTokens: ETHTokens,
    private val xlmTokens: XLMTokens,
    private val paxTokens: PAXTokens,
    private val stxTokens: STXTokens
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
}
