package piuk.blockchain.android.ui.base

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.StringRes
import android.support.annotation.UiThread
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import com.blockchain.notifications.analytics.Analytics
import java.lang.IllegalStateException
import java.util.Locale

abstract class MvpFragment <V : MvpView, P : MvpPresenter<V> >
    : Fragment() {

    protected abstract val presenter: P
    protected abstract val view: V

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.attachView(view)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    @CallSuper
    override fun onPause() {
        presenter.onViewPaused()
        super.onPause()
    }

    @CallSuper
    override fun onDestroy() {
        presenter.detachView(view)
        super.onDestroy()
    }

    protected fun onViewReady() {
        presenter.onViewReady()
    }

    protected val activity: BlockchainActivity
        get() = requireActivity() as? BlockchainActivity
            ?: throw IllegalStateException("Root activity is not a BlockchainActivity")

    protected val analytics: Analytics
        get() = activity.analytics

    protected val locale: Locale
        get() = activity.locale

    @UiThread
    protected fun showAlert(dlg: AlertDialog) = activity.showAlert(dlg)

    @UiThread
    protected fun clearAlert() = activity.clearAlert()

    @UiThread
    fun showProgressDialog(@StringRes messageId: Int, onCancel: (() -> Unit)? = null) =
        activity.showProgressDialog(messageId, onCancel)

    @UiThread
    fun dismissProgressDialog() = activity.dismissProgressDialog()

    @UiThread
    fun updateProgressDialog(msg: String) = activity.updateProgressDialog(msg)

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment) =
        activity.showBottomSheet(bottomSheet)

    protected open fun initViewOnResume(): Boolean = true
    protected open fun clearViewOnPause(): Boolean = true
}
