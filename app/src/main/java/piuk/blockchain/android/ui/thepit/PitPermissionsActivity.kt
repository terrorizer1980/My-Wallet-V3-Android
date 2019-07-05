package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_pit_kyc_promo_layout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog

class PitPermissionsActivity : BaseMvpActivity<PitPermissionsView, PitPermissionsPresenter>(), PitPermissionsView {

    private val pitPermissionsPresenter: PitPermissionsPresenter by inject()

    private var promptForEmailVerification = false

    override fun promptForEmailVerification(email: String) {
        PitVerifyEmailActivity.start(this, email)
        promptForEmailVerification = true
    }

    override fun createPresenter(): PitPermissionsPresenter = pitPermissionsPresenter

    override fun getView(): PitPermissionsView = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_kyc_promo_layout)
        connect_now.setOnClickListener {
            presenter.tryToConnect.onNext(Unit)
        }
        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        if (promptForEmailVerification) {
            promptForEmailVerification = false
            presenter.checkEmailIfEmailIsVerified.onNext(Unit)
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
                onCtaClick = { presenter.tryToConnect.onNext(Unit) }
            }
        emailVerifiedBottomDialog.show(supportFragmentManager, "BottomDialog")
        promptForEmailVerification = false
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, PitPermissionsActivity::class.java))
        }
    }
}
