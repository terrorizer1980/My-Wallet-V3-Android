package piuk.blockchain.android.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_about.*

import java.util.Calendar

import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber

class AboutDialog : AppCompatDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_about, null)

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        about.text = getString(
            R.string.about,
            BuildConfig.VERSION_NAME,
            Calendar.getInstance().get(Calendar.YEAR).toString()
        )

        rate_us.setOnClickListener {
            var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flags = flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            }

            try {
                val appPackageName = requireActivity().packageName
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                ).let {
                    it.addFlags(flags)
                    startActivity(it)
                }
            } catch (e: ActivityNotFoundException) {
                Timber.e(e, "Google Play Store not found")
            }
        }

        licenses.setOnClickListener {
            val layout = View.inflate(activity, R.layout.dialog_licenses, null)

            layout.findViewById<WebView>(R.id.webview).apply {
                loadUrl("file:///android_asset/licenses.html")
            }

            AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        if (hasWallet()) {
            free_wallet.gone()
        } else {
            free_wallet.setOnClickListener {
                try {
                    val marketIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$STR_MERCHANT_PACKAGE")
                        )
                    startActivity(marketIntent)
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e, "Google Play Store not found")
                }
            }
        }
    }

    private fun hasWallet(): Boolean {
        val pm = requireActivity().packageManager
        return try {
            pm.getPackageInfo(STR_MERCHANT_PACKAGE, 0)
            true
        } catch (e: NameNotFoundException) {
            false
        }
    }

    companion object {
        private const val STR_MERCHANT_PACKAGE = "info.blockchain.merchant"
    }
}
