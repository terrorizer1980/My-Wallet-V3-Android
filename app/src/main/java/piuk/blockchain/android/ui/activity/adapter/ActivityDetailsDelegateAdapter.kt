package piuk.blockchain.android.ui.activity.adapter

import android.view.View
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class ActivityDetailsDelegateAdapter(
    onActionItemClicked: (View) -> Unit,
    onDescriptionItemClicked: () -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(ActivityDetailInfoItemDelegate())
            addAdapterDelegate(ActivityDetailDescriptionItemDelegate(onDescriptionItemClicked))
            addAdapterDelegate(ActivityDetailActionItemDelegate(onActionItemClicked))
        }
    }
}