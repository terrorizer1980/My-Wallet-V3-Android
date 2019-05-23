package com.blockchain.kycui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import com.blockchain.kycui.hyperlinks.renderTermsLinks
import com.blockchain.kycui.navhost.KycProgressListener
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.navhost.models.KycStep
import com.blockchain.kycui.navigate
import com.blockchain.nabu.StartBuySell
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.constants.URL_COINIFY_POLICY
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlinx.android.synthetic.main.fragment_kyc_splash.button_kyc_splash_apply_now as buttonContinue
import kotlinx.android.synthetic.main.fragment_kyc_splash.image_view_cityscape as imageView
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_splash_message as textViewMessage
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_terms_and_conditions as textViewTerms

class KycSplashFragment : BaseFragment<KycSplashView, KycSplashPresenter>(), KycSplashView {

    private val startBuySell: StartBuySell by inject()

    private val presenter: KycSplashPresenter by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val campaignType = progressListener.campaignType
        logEvent(
            when (campaignType) {
                CampaignType.BuySell,
                CampaignType.Swap -> AnalyticsEvents.KycWelcome
                CampaignType.Sunriver -> AnalyticsEvents.KycSunriverStart
                CampaignType.Resubmission -> AnalyticsEvents.KycResubmission
            }
        )

        val title = when (progressListener.campaignType) {
            CampaignType.BuySell -> R.string.buy_sell_splash_title
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.Sunriver, CampaignType.Resubmission -> R.string.sunriver_splash_title
        }

        progressListener.setHostTitle(title)
        progressListener.incrementProgress(KycStep.SplashPage)

        if (campaignType == CampaignType.BuySell) {
            textViewTerms.renderTermsLinks(
                R.string.buy_sell_splash_terms_and_conditions,
                URL_COINIFY_POLICY,
                URL_COINIFY_POLICY
            )
            textViewTerms.visible()
        }

        if (campaignType == CampaignType.Sunriver) {
            imageView.setImageResource(R.drawable.vector_stellar_rocket)
            textViewMessage.setText(R.string.sunriver_splash_message)
        } else if (campaignType == CampaignType.BuySell) {
            textViewMessage.setText(R.string.buy_sell_splash_message)
        }
    }

    private val disposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        disposable += buttonContinue
            .throttledClicks()
            .subscribeBy(
                onNext = { presenter.onCTATapped(progressListener.campaignType) },
                onError = { Timber.e(it) }
            )
    }

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }

    override fun goToNextKycStep(direction: NavDirections) =
        navigate(direction)

    override fun goToBuySellView() {
        activity?.finish()
        startBuySell.startBuySellActivity(requireContext())
    }

    override fun displayLoading(isLoading: Boolean) {
        if (isLoading) {
            progressDialog = MaterialProgressDialog(activity).apply {
                setMessage(R.string.buy_sell_please_wait)
                show()
            }
        } else {
            progressDialog?.apply { dismiss() }
            progressDialog = null
        }
    }
    override fun showError(message: String) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun createPresenter(): KycSplashPresenter = presenter

    override fun getMvpView(): KycSplashView = this
}
