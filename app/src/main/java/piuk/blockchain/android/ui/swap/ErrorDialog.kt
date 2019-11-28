package piuk.blockchain.android.ui.swap

import androidx.fragment.app.FragmentManager
import com.blockchain.balance.errorIcon
import piuk.blockchain.android.ui.swap.homebrew.exchange.ExchangeMenuState
import com.blockchain.ui.dialog.ErrorBottomDialog
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R

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
    ErrorBottomDialog.Content(title, message, 0, R.string.ok_cap, errorType.icon(fromCrypto, tier))