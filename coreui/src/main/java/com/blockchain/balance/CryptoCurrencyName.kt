package com.blockchain.balance

import androidx.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.R

@StringRes
fun CryptoCurrency.currencyName() =
    when (this) {
        CryptoCurrency.BTC -> R.string.bitcoin
        CryptoCurrency.ETHER -> R.string.ethereum
        CryptoCurrency.BCH -> R.string.bitcoin_cash
        CryptoCurrency.XLM -> R.string.lumens
        CryptoCurrency.PAX -> R.string.usd_pax
        CryptoCurrency.STX -> R.string.stacks
    }
