package piuk.blockchain.android.ui.receive

import android.graphics.Bitmap
import android.support.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface ReceiveView : View {

    val locale: Locale

    fun getQrBitmap(): Bitmap

    fun getBtcAmount(): String

    fun showQrLoading()

    fun showQrCode(bitmap: Bitmap?)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateFiatTextField(text: String)

    fun updateBtcTextField(text: String)

    fun updateReceiveAddress(address: String)

    fun showWatchOnlyWarning()

    fun updateReceiveLabel(label: String)

    fun showBottomSheet(uri: String)

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun finishPage()

    fun disableCurrencyHeader()
}
