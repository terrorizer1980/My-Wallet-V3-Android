package com.blockchain.kycui.navhost

import com.blockchain.BaseKycPresenter
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kyc.services.nabu.TierUpdater
import com.blockchain.kycui.logging.KycResumedEvent
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.profile.models.ProfileModel
import com.blockchain.kycui.reentry.KycNavigator
import com.blockchain.kycui.reentry.ReentryDecision
import com.blockchain.nabu.NabuToken
import com.blockchain.sunriver.SunriverCampaignSignUp
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycNavHostPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val sunriverCampaignSignUp: SunriverCampaignSignUp,
    private val reentryDecision: ReentryDecision,
    private val kycNavigator: KycNavigator,
    private val tierUpdater: TierUpdater
) : BaseKycPresenter<KycNavHostView>(nabuToken) {

    override fun onViewReady() {
        compositeDisposable +=
            fetchOfflineToken.flatMap {
                nabuDataManager.getUser(it)
                    .subscribeOn(Schedulers.io())
            }.observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.displayLoading(true) }
                .subscribeBy(
                    onSuccess = {
                        registerForCampaignIfNeeded()
                        updateTier2SelectedTierIfNeeded()
                        redirectUserFlow(it)
                    },
                    onError = {
                        Timber.e(it)
                        if (it is MetadataNotFoundException) {
                            // No user, hide loading and start full KYC flow
                            view.displayLoading(false)
                        } else {
                            view.showErrorToastAndFinish(R.string.kyc_status_error)
                        }
                    }
                )
    }

    /**
     * Registers the user to the sunriver campaign if they are not yet registered and the view campaignType is Sunriver
     */
    private fun registerForCampaignIfNeeded() {
        // Check if Sunriver campaign
        if (view.campaignType != CampaignType.Sunriver) {
            return
        }

        compositeDisposable += sunriverCampaignSignUp.userIsInSunRiverCampaign()
            .flatMapCompletable { isInCampaign ->
                if (isInCampaign) {
                    Completable.complete()
                } else {
                    sunriverCampaignSignUp.registerSunRiverCampaign()
                }
            }
            .subscribeOn(Schedulers.io())
            .doOnError(Timber::e)
            .subscribe()
    }

    private fun updateTier2SelectedTierIfNeeded() {
        if (view.campaignType != CampaignType.Sunriver || view.campaignType != CampaignType.BuySell) {
            return
        }

        compositeDisposable += tierUpdater
            .setUserTier(2)
            .doOnError(Timber::e)
            .subscribe()
    }

    private fun redirectUserFlow(user: NabuUser) {
        if (view.campaignType == CampaignType.BuySell) {
            view.navigateToKycSplash()
        } else if (view.campaignType == CampaignType.Resubmission || user.isMarkedForResubmission) {
            view.navigateToResubmissionSplash()
        } else if (user.state != UserState.None && user.kycState == KycState.None) {
            val current = user.tiers?.current
            if (current == null || current == 0) {
                val reentryPoint = reentryDecision.findReentryPoint(user)
                val directions = kycNavigator.userAndReentryPointToDirections(user, reentryPoint)
                view.navigate(directions)
                Logging.logCustom(KycResumedEvent(reentryPoint))
            }
        } else if (view.campaignType == CampaignType.Sunriver) {
            view.navigateToKycSplash()
        }

        // If no other methods are triggered, this will start KYC from scratch. If others have been called,
        // this will make the host fragment visible.
        view.displayLoading(false)
    }
}

internal fun NabuUser.toProfileModel(): ProfileModel = ProfileModel(
    firstName ?: throw IllegalStateException("First Name is null"),
    lastName ?: throw IllegalStateException("Last Name is null"),
    address?.countryCode ?: throw IllegalStateException("Country Code is null")
)