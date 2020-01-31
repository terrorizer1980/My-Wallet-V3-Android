package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_simple_buy_kyc_pending.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

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
        kyc_progress.visibleIf { newState.kycVerificationState == KycState.PENDING }
        kyc_failed_icon.visibleIf {
            newState.kycVerificationState == KycState.FAILED ||
                    newState.kycVerificationState == KycState.UNDECIDED
        }

        kyc_pending_subtitle.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_pending_subtitle)
            else -> ""
        }

        verif_text.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_verifying_info)
            KycState.FAILED -> resources.getString(R.string.kyc_manual_review_required)
            KycState.UNDECIDED -> resources.getString(R.string.kyc_pending_review)
            else -> ""
        }

        verif_time.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_verifying_time_info)
            KycState.FAILED,
            KycState.UNDECIDED -> resources.getString(R.string.kyc_verifying_manual_review_required_info)
            else -> ""
        }

        continue_to_wallet.visibleIf {
            newState.kycVerificationState == KycState.FAILED ||
                    newState.kycVerificationState == KycState.UNDECIDED
        }

        if (newState.kycVerificationState == KycState.VERIFIED) {
            navigator().goToCheckOutScreen()
        }

        kyc_failed_icon.setImageResource(
            if (newState.kycVerificationState == KycState.FAILED)
                R.drawable.ic_kyc_failed_warning
            else R.drawable.ic_kyc_pending
        )
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}