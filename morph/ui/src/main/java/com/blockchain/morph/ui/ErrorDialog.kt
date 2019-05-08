package com.blockchain.morph.ui

import android.support.v4.app.FragmentManager
import com.blockchain.morph.ui.homebrew.exchange.ExchangeMenuState
import com.blockchain.morph.ui.homebrew.exchange.error.SwapErrorBottomDialog
import info.blockchain.balance.CryptoCurrency

private fun ExchangeMenuState.ErrorType.icon(cryptoCurrency: CryptoCurrency, userTier: Int): Int =
    when (this) {
        ExchangeMenuState.ErrorType.TIER -> if (userTier == 2)
            R.drawable.vector_gold_swap_error else R.drawable.vector_silver_swap_error
        else -> cryptoCurrency.errorIcon()
    }

private fun CryptoCurrency.errorIcon(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_btc_swap_error
        CryptoCurrency.BCH -> R.drawable.vector_bch_swap_error
        CryptoCurrency.ETHER -> R.drawable.vector_eth_swap_error
        CryptoCurrency.XLM -> R.drawable.vector_xlm_swap_error
        CryptoCurrency.PAX -> TODO("ADD PAX ICON WHEN SWAP IS SUPPORTED FOR STABLECOIN")
    }

internal fun showErrorDialog(fragmentManager: FragmentManager, error: ExchangeMenuState.ExchangeMenuError) {
    val bottomSheetDialog = SwapErrorBottomDialog.newInstance(error.toContent())
    bottomSheetDialog.show(fragmentManager, "BottomDialog")
}

private fun ExchangeMenuState.ExchangeMenuError.toContent(): SwapErrorBottomDialog.Content =
    SwapErrorBottomDialog.Content(
        title, message, 0, R.string.ok_cap, errorType.icon(fromCrypto, tier))