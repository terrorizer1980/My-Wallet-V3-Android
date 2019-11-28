package piuk.blockchain.android.ui.backup.verify

import android.os.Bundle
import androidx.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface BackupVerifyView : View {

    fun getPageBundle(): Bundle?

    fun showProgressDialog()

    fun hideProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showCompletedFragment()

    fun showStartingFragment()

    fun showWordHints(hints: List<Int>)
}