package piuk.blockchain.android.ui.swap.homebrew.exchange.model

import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog

data class SwapErrorDialogContent(
    val content: ErrorBottomDialog.Content,
    val ctaClick: (() -> Unit)?,
    val dismissClick: (() -> Unit)?
)