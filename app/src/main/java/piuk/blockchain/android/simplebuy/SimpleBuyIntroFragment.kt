package piuk.blockchain.android.simplebuy

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.NabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_simple_buy_intro.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class SimpleBuyIntroFragment : Fragment(), SimpleBuyScreen {

    private val walletSettings: SettingsDataManager by inject()
    private val nabuToken: NabuToken by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()

    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_intro)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.simple_buy_intro_title)
        skip_simple_buy.setOnClickListener { navigator().exitSimpleBuyFlow() }
        buy_crypto_now.setOnClickListener {
            nabuToken.fetchNabuToken(currency = currencyPrefs.selectedFiatCurrency, action = "simplebuy")
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    showLoadingState()
                }
                .subscribeBy(
                    onSuccess = {
                        simpleBuyPrefs.setFlowStartedAtLeastOnce()
                        simpleBuyPrefs.clearState()
                        navigator().goToBuyCryptoScreen()
                    },
                    onError = {
                        showError()
                    }
                )
        }
    }

    private fun showError() {
        buy_crypto_now.visible()
        progress.gone()
        ErrorSlidingBottomDialog.newInstance(ErrorDialogData(
            resources.getString(R.string.ops),
            resources.getString(R.string.something_went_wrong_try_again),
            resources.getString(R.string.ok_cap)))
            .show(childFragmentManager, "BOTTOM_SHEET")
    }

    private fun showLoadingState() {
        buy_crypto_now.gone()
        progress.visible()
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable += walletSettings.fetchSettings().subscribe {
            if (!it.isEmailVerified) {
                val emailString = resources.getString(R.string.simple_buy_verify_email_instruction, it.email)
                val spannableString = SpannableStringBuilder(emailString)
                spannableString.setSpan(StyleSpan(Typeface.BOLD),
                    emailString.indexOf(it.email),
                    emailString.indexOf(it.email) + it.email.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                email_confirmation_note.text = spannableString
            } else {
                email_confirmation_note.gone()
                separator.gone()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}