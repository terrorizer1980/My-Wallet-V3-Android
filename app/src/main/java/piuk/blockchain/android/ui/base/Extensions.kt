package piuk.blockchain.android.ui.base

import androidx.fragment.app.FragmentActivity

fun FragmentActivity.setupToolbar(resource: Int) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
    }
}