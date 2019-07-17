package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_pit_kyc_promo_layout.*
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog

class PitPermissionsActivity : PitPermissionsView, BaseMvpActivity<PitPermissionsView, PitPermissionsPresenter>() {

    override fun createPresenter(): PitPermissionsPresenter = get()
    override fun getView(): PitPermissionsView = this

    private var loadingDialog: PitStateBottomDialog? = null
    private var errorDialog: PitStateBottomDialog? = null

    private val isPitToWalletLink: Boolean by lazy {
        intent?.extras?.getBoolean(PARAM_LINK_WALLET_TO_PIT, true) ?: true
    }

    private val pitToWalletLinkId: String? by lazy {
        intent?.extras?.getString(PARAM_LINK_ID, null)
    }

    override fun promptForEmailVerification(email: String) {
        PitVerifyEmailActivity.start(this, email, REQUEST_VERIFY_EMAIL)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_kyc_promo_layout)

        connect_now.setOnClickListener {
            doLinkClickHandler()
        }
        onViewReady()
    }

    private fun doLinkClickHandler() {
        if (isPitToWalletLink) {
            presenter.tryToConnectWalletToPit()
        } else {
            val linkId = pitToWalletLinkId ?: throw IllegalStateException("Link id is missing")
            presenter.tryToConnectPitToWallet(linkId)
        }
    }

    override fun onLinkSuccess(pitLinkingUrl: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pitLinkingUrl)))
    }

    override fun onLinkFailed(reason: String) {
        if (errorDialog == null) {
            errorDialog = PitStateBottomDialog.newInstance(
                PitStateBottomDialog.StateContent(ErrorBottomDialog.Content(
                    getString(R.string.pit_connection_error_title),
                    getString(R.string.pit_connection_error_description),
                    R.string.try_again,
                    0,
                    R.drawable.vector_pit_request_failure), false
                )).apply {
                    onCtaClick = {
                        doLinkClickHandler()
                        dismissErrorDialog()
                    }
            }
            errorDialog?.show(supportFragmentManager, "LoadingBottomDialog")
        }
    }

    private fun dismissErrorDialog() {
        errorDialog?.dismiss()
        errorDialog = null
    }

    override fun showLoading() {
        if (loadingDialog == null) {
            loadingDialog = PitStateBottomDialog.newInstance(
                PitStateBottomDialog.StateContent(ErrorBottomDialog.Content(
                    getString(R.string.pit_loading_dialog_title),
                    getString(R.string.pit_loading_dialog_description),
                    0,
                    0,
                    0
                ),
                    true
                ))
            loadingDialog?.show(supportFragmentManager, "LoadingBottomDialog")
        }
    }

    override fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VERIFY_EMAIL) {
            presenter.checkEmailIsVerified()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showEmailVerifiedDialog() {
        val emailVerifiedBottomDialog =
            PitEmailVerifiedBottomDialog.newInstance(
                ErrorBottomDialog.Content(
                    getString(R.string.pit_email_verified_title),
                    getString(R.string.pit_email_verified_description),
                    R.string.pit_connect_now,
                    0,
                    R.drawable.vector_email_verified
                )
            ).apply {
                onCtaClick = { doLinkClickHandler() }
            }
        emailVerifiedBottomDialog.show(supportFragmentManager, "BottomDialog")
    }

    companion object {
        private const val REQUEST_VERIFY_EMAIL = 7396
        private const val PARAM_LINK_WALLET_TO_PIT = "link_wallet_to_pit"
        private const val PARAM_LINK_ID = "link_id"

        @JvmStatic
        fun start(ctx: Context, isWalletToPit: Boolean, linkId: String? = null) {
            Intent(ctx, PitPermissionsActivity::class.java).apply {
                putExtra(PARAM_LINK_WALLET_TO_PIT, isWalletToPit)
                putExtra(PARAM_LINK_ID, linkId)
            }.run { ctx.startActivity(this) }
        }
    }
}
