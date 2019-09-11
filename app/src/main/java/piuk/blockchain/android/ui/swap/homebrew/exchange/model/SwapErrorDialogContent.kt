package piuk.blockchain.android.ui.swap.homebrew.exchange.model

import com.blockchain.ui.dialog.ErrorBottomDialog

data class SwapErrorDialogContent(
    val content: ErrorBottomDialog.Content,
    val ctaClick: (() -> Unit)?,
    val dismissClick: (() -> Unit)?
)