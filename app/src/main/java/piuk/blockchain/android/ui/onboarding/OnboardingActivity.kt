package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import org.koin.android.ext.android.inject

import piuk.blockchain.android.R
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog
import piuk.blockchain.android.ui.fingerprint.FingerprintStage
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import timber.log.Timber

internal class OnboardingActivity : BaseMvpActivity<OnboardingView, OnboardingPresenter>(),
    OnboardingView,
    FingerprintPromptFragment.OnFragmentInteractionListener,
    EmailPromptFragment.OnFragmentInteractionListener {

    private val onboardingPresenter: OnboardingPresenter by inject()
    private val deepLinkPersistence: DeepLinkPersistence by inject()
    private var emailLaunched = false

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        progressDialog = MaterialProgressDialog(this).apply {
            setMessage(R.string.please_wait)
            setCancelable(false)
            show()
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()

        if (emailLaunched && intent.canDismiss) {
            startMainActivity()
        } else if (emailLaunched) {
            finish()
        }
    }

    override val isEmailOnly: Boolean
        get() = intent.isEmailOnly

    override fun showFingerprintPrompt() {
        if (!isFinishing) {
            dismissDialog()
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.content_frame, FingerprintPromptFragment.newInstance())
                .commit()
        }
    }

    override fun showEmailPrompt() {
        if (!isFinishing) {
            dismissDialog()
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(
                    R.id.content_frame,
                    EmailPromptFragment.newInstance(presenter.email!!, intent.canDismiss)
                )
                .commit()
        }
    }

    override fun onEnableFingerprintClicked() {
        presenter.onEnableFingerprintClicked()
    }

    override fun onCompleteLaterClicked() {
        showEmailPrompt()
    }

    override fun showFingerprintDialog(pincode: String) {
        if (!isFinishing) {
            val dialog = FingerprintDialog.newInstance(
                pincode,
                FingerprintStage.REGISTER_FINGERPRINT
            )

            dialog.setAuthCallback(object : FingerprintDialog.FingerprintAuthCallback {
                override fun onAuthenticated(data: String?) {
                    dialog.dismissAllowingStateLoss()
                    presenter.setFingerprintUnlockEnabled(true)
                    showEmailPrompt()
                }

                override fun onCanceled() {
                    dialog.dismissAllowingStateLoss()
                    presenter.setFingerprintUnlockEnabled(true)
                }
            })

            dialog.show(supportFragmentManager, FingerprintDialog.TAG)
        }
    }

    override fun showEnrollFingerprintsDialog() {
        if (!isFinishing) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.fingerprint_no_fingerprints_added)
                .setCancelable(true)
                .setPositiveButton(R.string.yes) { dialog, which ->
                    startActivityForResult(
                        Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS),
                        0
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onVerifyEmailClicked() {
        presenter.disableAutoLogout()
        emailLaunched = true
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
    }

    override fun onVerifyLaterClicked() {
        startMainActivity()
    }

    override fun createPresenter(): OnboardingPresenter {
        return onboardingPresenter
    }

    override fun getView(): OnboardingView {
        return this
    }

    private fun startMainActivity() {
        dismissDialog()

        Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            data = deepLinkPersistence.popUriFromSharedPrefs()

            Timber.d("DeepLink: Starting main activity with %s", intent.data)

            startActivityForResult(this, EMAIL_CLIENT_REQUEST)
        }
    }

    private fun dismissDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EMAIL_CLIENT_REQUEST) {
            presenter.enableAutoLogout()
        }
    }

    companion object {

        private const val EMAIL_CLIENT_REQUEST = 5400

        fun launch(ctx: Context, emailOnly: Boolean, canDismiss: Boolean = false) {

            Intent(ctx, OnboardingActivity::class.java).let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                it.isEmailOnly = emailOnly
                it.canDismiss = canDismiss
                ctx.startActivity(it)
            }
        }

        /**
         * Flag for showing only the email verification prompt. This is for use when signup was
         * completed some other time, but the user hasn't verified their email yet.
         */
        private const val EXTRAS_EMAIL_ONLY = "email_only"
        private const val EXTRAS_OPTION_TO_DISMISS = "has_option_for_dismiss"

        private var Intent.isEmailOnly: Boolean
            get() = extras?.getBoolean(EXTRAS_EMAIL_ONLY, true) ?: true
            set(v) {
                putExtra(EXTRAS_EMAIL_ONLY, v)
            }

        private var Intent.canDismiss: Boolean
            get() = extras?.getBoolean(EXTRAS_OPTION_TO_DISMISS, false) ?: false
            set(v) {
                putExtra(EXTRAS_OPTION_TO_DISMISS, v)
            }
    }
}
