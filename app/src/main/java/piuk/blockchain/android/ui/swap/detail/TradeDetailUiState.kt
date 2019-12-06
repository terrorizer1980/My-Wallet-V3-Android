package piuk.blockchain.android.ui.swap.detail

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class TradeDetailUiState(
    @StringRes val title: Int,
    @StringRes val heading: Int,
    val message: String,
    @DrawableRes val icon: Int,
    @ColorRes val receiveColor: Int
)