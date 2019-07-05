package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import kotlinx.android.synthetic.main.activity_pit_verify_email_layout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.toast

class PitVerifyEmailActivity : BaseMvpActivity<PitVerifyEmailView, PitVerifyEmailPresenter>(), PitVerifyEmailView {
    private val presenter: PitVerifyEmailPresenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pit_verify_email_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.pit_verify_email_title)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        val email = intent.getStringExtra(ARGUMENT_EMAIL) ?: ""
        email_address.text = email

        send_again.setOnClickListener {
            presenter.resendMail.onNext(email)
        }

        open_app.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
        }

        presenter.onViewReady()
    }

    override fun createPresenter(): PitVerifyEmailPresenter = presenter

    override fun getView(): PitVerifyEmailView =
        this

    override fun mailResentFailed() {
        toast(R.string.mail_resent_failed, ToastCustom.TYPE_ERROR)
    }

    override fun mailResentSuccessfully() {
        toast(R.string.mail_resent_succeed, ToastCustom.TYPE_OK)
    }

    companion object {
        private const val ARGUMENT_EMAIL = "email"

        fun start(ctx: Context, email: String) {
            val intent = Intent(ctx, PitVerifyEmailActivity::class.java).apply {
                putExtra(ARGUMENT_EMAIL, email)
            }
            ctx.startActivity(intent)
        }
    }
}