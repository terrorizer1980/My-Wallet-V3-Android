package piuk.blockchain.android.ui.base

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.design.widget.BottomSheetDialogFragment
import android.view.View
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog

abstract class SlidingModalBottomDialog : BottomSheetDialogFragment() {

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dlg = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        val view = View.inflate(context, layoutResource, null)
        dlg.setContentView(view)

        val bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)

        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {
                when (i) {
                    BottomSheetBehavior.STATE_EXPANDED -> onSheetExpanded()
                    BottomSheetBehavior.STATE_COLLAPSED -> onSheetCollapsed()
                    BottomSheetBehavior.STATE_HIDDEN -> onSheetHidden()
                    else -> { /* shouldn't get here! */ }
                }
            }

            override fun onSlide(view: View, v: Float) {}
        })

        initControls(view)

        dlg.setOnShowListener {
            bottomSheetBehavior.skipCollapsed = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        return dlg
    }

    @CallSuper
    protected open fun onSheetExpanded() { }

    @CallSuper
    protected open fun onSheetCollapsed() { }

    @CallSuper
    protected open fun onSheetHidden() { dismiss() }

    protected abstract val layoutResource: Int
    protected abstract fun initControls(view: View)
}
