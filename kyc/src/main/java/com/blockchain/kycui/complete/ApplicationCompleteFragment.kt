package com.blockchain.kycui.complete

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.navigate
import com.blockchain.kycui.status.KycStatusActivity
import com.blockchain.sunriver.ui.SunriverCampaignSignupBottomDialog
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlinx.android.synthetic.main.fragment_kyc_complete.button_kyc_complete_next as buttonNext

class ApplicationCompleteFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val compositeDisposable = CompositeDisposable()

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

        compositeDisposable +=
            buttonNext
                .throttledClicks()
                .subscribeBy(
                    onNext = {
                        if (progressListener.campaignType == CampaignType.BuySell) {
                            activity?.finish()
                            KycStatusActivity.start(context!!, CampaignType.BuySell)
                        } else {
                            navigate(ApplicationCompleteFragmentDirections.actionTier2Complete())
                        }
                    },
                    onError = { Timber.e(it) }
                )

        showCampaignRegisterPopup()
    }

    private fun showCampaignRegisterPopup() {
        val dialog = SunriverCampaignSignupBottomDialog()
        compositeDisposable += dialog
            .shouldShow()
            .doOnError(Timber::e)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { show ->
                if (show) {
                    dialog.show(fragmentManager, "BOTTOM_DIALOG")
                }
            }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }
}