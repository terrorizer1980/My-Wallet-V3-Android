package piuk.blockchain.android.ui.base

import android.support.annotation.StringRes
import io.reactivex.disposables.CompositeDisposable

interface MvpView {
    fun showProgressDialog(@StringRes messageId: Int, onCancel: (() -> Unit)? = null)
    fun dismissProgressDialog()
}

abstract class MvpPresenter<T : MvpView> {

    val compositeDisposable = CompositeDisposable()

    protected var view: T? = null
        private set(value) { field = value }

    fun attachView(view: T) {
        this.view = view
        onViewAttached()
    }

    fun detachView(view: T) {
        assert(this.view === view)

        onViewDetached()
        compositeDisposable.clear()
        this.view = null
    }

    protected abstract fun onViewAttached()
    protected abstract fun onViewDetached()

    abstract val alwaysDisableScreenshots: Boolean
    abstract val enableLogoutTimer: Boolean
}
