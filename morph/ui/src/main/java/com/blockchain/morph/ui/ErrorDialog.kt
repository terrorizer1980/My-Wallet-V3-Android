package com.blockchain.morph.ui

import android.support.v4.app.FragmentManager
import com.blockchain.balance.errorIcon
import com.blockchain.morph.ui.homebrew.exchange.ExchangeMenuState
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog
import info.blockchain.balance.CryptoCurrency

private fun ExchangeMenuState.ErrorType.icon(cryptoCurrency: CryptoCurrency, userTier: Int): Int =
    when (this) {
        ExchangeMenuState.ErrorType.TIER -> if (userTier == 2)
            R.drawable.vector_gold_swap_error else R.drawable.vector_silver_swap_error
        else -> cryptoCurrency.errorIcon()
    }

internal fun showErrorDialog(fragmentManager: FragmentManager, error: ExchangeMenuState.ExchangeMenuError) {
    val bottomSheetDialog = ErrorBottomDialog.newInstance(error.toContent())
    bottomSheetDialog.show(fragmentManager, "BottomDialog")
}

private fun ExchangeMenuState.ExchangeMenuError.toContent(): ErrorBottomDialog.Content =
    ErrorBottomDialog.Content(
        title, message, 0, R.string.ok_cap, errorType.icon(fromCrypto, tier))