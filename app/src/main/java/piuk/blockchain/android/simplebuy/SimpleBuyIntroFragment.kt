package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_simple_buy_intro.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.lang.IllegalStateException

class SimpleBuyIntroFragment : Fragment(), SimpleBuyScreen {

    private val walletSettings: SettingsDataManager by inject()

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
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable += walletSettings.fetchSettings().subscribe {
            if (!it.isEmailVerified) {
                email_confirmation_note.text =
                    resources.getString(R.string.simple_buy_verify_email_instruction, it.email)
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

    override fun onBackPressed(): Boolean = false
}