package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_simple_buy_kyc_pending.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SimpleBuyPendingKycFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(), SimpleBuyScreen {

    override val model: SimpleBuyModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_kyc_pending)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(SimpleBuyIntent.FetchKycState)
        continue_to_wallet.setOnClickListener {
            navigator().exitSimpleBuyFlow()
        }
    }

    override fun render(newState: SimpleBuyState) {
        kyc_progress.goneIf(newState.kycVerificationState != KycState.PENDING)
        kyc_failed_icon.goneIf(newState.kycVerificationState == KycState.PENDING)
        verif_text.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_verifying_info)
            KycState.FAILED -> resources.getString(R.string.kyc_manual_review_required)
            else -> ""
        }

        kyc_pending_title.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_pending_title)
            KycState.FAILED -> resources.getString(R.string.kyc_manual_review_required)
            else -> ""
        }
        continue_to_wallet.goneIf(newState.kycVerificationState != KycState.FAILED)
        if (newState.kycVerificationState == KycState.VERIFIED) {
            navigator().goToCheckOutScreen()
        }
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}