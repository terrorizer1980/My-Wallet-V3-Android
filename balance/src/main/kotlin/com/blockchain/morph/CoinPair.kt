package com.blockchain.morph

import info.blockchain.balance.CryptoCurrency

enum class CoinPair(
    val pairCode: String,
    val from: CryptoCurrency,
    val to: CryptoCurrency,
    val pairCodeUpper: String = pairCode.toUpperCase().replace("_", "-")
) {

    BTC_TO_BTC("btc_btc", CryptoCurrency.BTC, CryptoCurrency.BTC),
    BTC_TO_ETH("btc_eth", CryptoCurrency.BTC, CryptoCurrency.ETHER),
    BTC_TO_BCH("btc_bch", CryptoCurrency.BTC, CryptoCurrency.BCH),
    BTC_TO_XLM("btc_xlm", CryptoCurrency.BTC, CryptoCurrency.XLM),
    BTC_TO_PAX("btc_pax", CryptoCurrency.BTC, CryptoCurrency.PAX),
    BTC_TO_ALG("btc_alg", CryptoCurrency.BTC, CryptoCurrency.ALG),

    ETH_TO_ETH("eth_eth", CryptoCurrency.ETHER, CryptoCurrency.ETHER),
    ETH_TO_BTC("eth_btc", CryptoCurrency.ETHER, CryptoCurrency.BTC),
    ETH_TO_BCH("eth_bch", CryptoCurrency.ETHER, CryptoCurrency.BCH),
    ETH_TO_XLM("eth_xlm", CryptoCurrency.ETHER, CryptoCurrency.XLM),
    ETH_TO_PAX("eth_pax", CryptoCurrency.ETHER, CryptoCurrency.PAX),
    ETH_TO_ALG("eth_alg", CryptoCurrency.ETHER, CryptoCurrency.ALG),

    BCH_TO_BCH("bch_bch", CryptoCurrency.BCH, CryptoCurrency.BCH),
    BCH_TO_BTC("bch_btc", CryptoCurrency.BCH, CryptoCurrency.BTC),
    BCH_TO_ETH("bch_eth", CryptoCurrency.BCH, CryptoCurrency.ETHER),
    BCH_TO_XLM("bch_xlm", CryptoCurrency.BCH, CryptoCurrency.XLM),
    BCH_TO_PAX("bch_pax", CryptoCurrency.BCH, CryptoCurrency.PAX),
    BCH_TO_ALG("bch_alg", CryptoCurrency.BCH, CryptoCurrency.ALG),

    XLM_TO_XLM("xlm_xlm", CryptoCurrency.XLM, CryptoCurrency.XLM),
    XLM_TO_BTC("xlm_btc", CryptoCurrency.XLM, CryptoCurrency.BTC),
    XLM_TO_ETH("xlm_eth", CryptoCurrency.XLM, CryptoCurrency.ETHER),
    XLM_TO_BCH("xlm_bch", CryptoCurrency.XLM, CryptoCurrency.BCH),
    XLM_TO_PAX("xlm_pax", CryptoCurrency.XLM, CryptoCurrency.PAX),
    XLM_TO_ALG("xlm_alg", CryptoCurrency.XLM, CryptoCurrency.ALG),

    PAX_TO_PAX("pax_pax", CryptoCurrency.PAX, CryptoCurrency.PAX),
    PAX_TO_BTC("pax_btc", CryptoCurrency.PAX, CryptoCurrency.BTC),
    PAX_TO_ETH("pax_eth", CryptoCurrency.PAX, CryptoCurrency.ETHER),
    PAX_TO_BCH("pax_bch", CryptoCurrency.PAX, CryptoCurrency.BCH),
    PAX_TO_XLM("pax_xlm", CryptoCurrency.PAX, CryptoCurrency.XLM),
    PAX_TO_ALG("pax_alg", CryptoCurrency.PAX, CryptoCurrency.ALG),

    ALG_TO_ALG("alg_alg", CryptoCurrency.ALG, CryptoCurrency.ALG),
    ALG_TO_BTC("alg_btc", CryptoCurrency.ALG, CryptoCurrency.BTC),
    ALG_TO_ETH("alg_eth", CryptoCurrency.ALG, CryptoCurrency.ETHER),
    ALG_TO_BCH("alg_bch", CryptoCurrency.ALG, CryptoCurrency.BCH),
    ALG_TO_XLM("alg_xlm", CryptoCurrency.ALG, CryptoCurrency.XLM),
    ALG_TO_PAX("alg_pax", CryptoCurrency.ALG, CryptoCurrency.PAX) ;

    val sameInputOutput = from == to

    fun inverse() = to to from

    companion object {

        fun fromPairCode(pairCode: String): CoinPair {
            return fromPairCodeOrNull(pairCode) ?: throw IllegalStateException("Attempt to get invalid pair $pairCode")
        }

        fun fromPairCodeOrNull(pairCode: String?): CoinPair? {
            pairCode?.split('_')?.let {
                if (it.size == 2) {
                    val from = CryptoCurrency.fromNetworkTicker(it.first())
                    val to = CryptoCurrency.fromNetworkTicker(it.last())
                    if (from != null && to != null) {
                        return from to to
                    }
                }
            }
            return null
        }
    }
}

infix fun CryptoCurrency.to(other: CryptoCurrency) =
    when (this) {
        CryptoCurrency.BTC -> when (other) {
            CryptoCurrency.BTC -> CoinPair.BTC_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.BTC_TO_ETH
            CryptoCurrency.BCH -> CoinPair.BTC_TO_BCH
            CryptoCurrency.XLM -> CoinPair.BTC_TO_XLM
            CryptoCurrency.PAX -> CoinPair.BTC_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALG -> CoinPair.BTC_TO_ALG
        }
        CryptoCurrency.ETHER -> when (other) {
            CryptoCurrency.ETHER -> CoinPair.ETH_TO_ETH
            CryptoCurrency.BTC -> CoinPair.ETH_TO_BTC
            CryptoCurrency.BCH -> CoinPair.ETH_TO_BCH
            CryptoCurrency.XLM -> CoinPair.ETH_TO_XLM
            CryptoCurrency.PAX -> CoinPair.ETH_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALG -> CoinPair.ETH_TO_ALG
        }
        CryptoCurrency.BCH -> when (other) {
            CryptoCurrency.BCH -> CoinPair.BCH_TO_BCH
            CryptoCurrency.BTC -> CoinPair.BCH_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.BCH_TO_ETH
            CryptoCurrency.XLM -> CoinPair.BCH_TO_XLM
            CryptoCurrency.PAX -> CoinPair.BCH_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALG -> CoinPair.BCH_TO_ALG
        }
        CryptoCurrency.XLM -> when (other) {
            CryptoCurrency.XLM -> CoinPair.XLM_TO_XLM
            CryptoCurrency.BTC -> CoinPair.XLM_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.XLM_TO_ETH
            CryptoCurrency.BCH -> CoinPair.XLM_TO_BCH
            CryptoCurrency.PAX -> CoinPair.XLM_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALG -> CoinPair.XLM_TO_ALG
        }
        CryptoCurrency.PAX -> when (other) {
            CryptoCurrency.PAX -> CoinPair.PAX_TO_PAX
            CryptoCurrency.BTC -> CoinPair.PAX_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.PAX_TO_ETH
            CryptoCurrency.BCH -> CoinPair.PAX_TO_BCH
            CryptoCurrency.XLM -> CoinPair.PAX_TO_XLM
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALG -> CoinPair.PAX_TO_ALG
        }
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        CryptoCurrency.ALG -> when (other) {
            CryptoCurrency.ALG -> CoinPair.ALG_TO_ALG
            CryptoCurrency.BTC -> CoinPair.ALG_TO_BTC
            CryptoCurrency.ETHER -> CoinPair.ALG_TO_ETH
            CryptoCurrency.BCH -> CoinPair.ALG_TO_BCH
            CryptoCurrency.XLM -> CoinPair.ALG_TO_XLM
            CryptoCurrency.PAX -> CoinPair.ALG_TO_PAX
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }
    }
