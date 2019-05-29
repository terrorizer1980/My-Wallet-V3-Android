package com.blockchain.sunriver.ui

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.sunriver.SunriverCampaignSignUp
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R
import timber.log.Timber

class SunriverCampaignSignupBottomDialog : BaseAirdropBottomDialog(
    Content(
        title = R.string.claim_your_free_crypto,
        description = R.string.claim_your_free_crypto_body,
        ctaButtonText = R.string.claim_your_free_crypto_cta,
        dismissText = R.string.claim_your_free_crypto_dismiss
    )
) {
    private val analytics: Analytics by inject()

    private val sunriverCampaignSignUp: SunriverCampaignSignUp by inject()

    override fun onStart() {
        super.onStart()
        analytics.logEvent(AnalyticsEvents.SunRiverBottomCampaignDialog)
    }

    override fun ctaButtonClick() {
        analytics.logEvent(AnalyticsEvents.SunRiverBottomCampaignDialogClicked)

        compositeDisposable += sunriverCampaignSignUp
            .registerSunRiverCampaign()
            .doOnError(Timber::e)
            .subscribeBy {
                dismiss()
            }
    }

    override fun xlmLogoClick() {
        analytics.logEvent(AnalyticsEvents.SunRiverBottomCampaignDialogClickedRocket)
    }

    override fun dismissButtonClick() {
        analytics.logEvent(AnalyticsEvents.SunRiverBottomCampaignDialogDismissClicked)
        super.dismissButtonClick()
    }

    fun shouldShow(): Single<Boolean> = sunriverCampaignSignUp.userIsInSunRiverCampaign().map { !it }
}
