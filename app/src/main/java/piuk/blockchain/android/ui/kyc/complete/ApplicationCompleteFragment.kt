package piuk.blockchain.android.ui.kyc.complete

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.KYCAnalyticsEvents
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.status.KycStatusActivity
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_kyc_complete.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.BlockstackCampaignRegistration
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber

class ApplicationCompleteFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()
    private val analytics: Analytics by inject()
    private val stxCampaign: BlockstackCampaignRegistration by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_complete)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_complete_title)
        progressListener.incrementProgress(KycStep.CompletePage)
        progressListener.hideBackButton()
    }

    override fun onResume() {
        super.onResume()

        // We'll need to give the campaign registration time to complete, so:
        if (progressListener.campaignType == CampaignType.Blockstack) {
            setButtonEnabled(false)
            compositeDisposable += stxCampaign.registerCampaign()
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { setButtonEnabled(true) }
                .emptySubscribe()
        }

        compositeDisposable +=
            button_done
                .throttledClicks()
                .subscribeBy(
                    onNext = {
                        when (progressListener.campaignType) {
                            CampaignType.BuySell -> {
                                activity?.finish()
                                KycStatusActivity.start(requireContext(), CampaignType.BuySell)
                            }
                            CampaignType.Blockstack -> {
                                activity?.setResult(KycNavHostActivity.RESULT_KYC_STX_COMPLETE)
                                activity?.finish()
                            }
                            else -> navigate(ApplicationCompleteFragmentDirections.actionTier2Complete())
                        }
                        analytics.logEvent(KYCAnalyticsEvents.VeriffInfoSubmitted)
                    },
                    onError = { Timber.e(it) }
                )
    }

    private fun setButtonEnabled(enabled: Boolean) {
        button_done.isEnabled = enabled
        button_done.alpha = if (enabled) 1.0f else 0.2f
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }
}