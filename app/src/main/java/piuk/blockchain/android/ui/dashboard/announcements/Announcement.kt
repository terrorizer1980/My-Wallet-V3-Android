package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.kycui.sunriver.SunriverCardType
import com.blockchain.sunriver.ui.BaseAirdropBottomDialog
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import timber.log.Timber

interface AnnouncementHost {
    fun clearAllAnnouncements()
    fun showAnnouncementCard(card: AnnouncementCard)
    fun dismissAnnouncementCard(prefsKey: String)

    fun showAnnouncmentPopup(popup: BaseAirdropBottomDialog)

    // TEMP: Actions
    fun signupToSunRiverCampaign()
    fun exchangeRequested(cryptoCurrency: CryptoCurrency? = null)
    fun startKyc(campaignType: CampaignType)
}

interface Announcement {
    fun shouldShow(): Single<Boolean>
    fun show(host: AnnouncementHost)
}

class AnnouncementList(
    private val dismissRecorder: DismissRecorder,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    private val kycTiersQueries: KycTiersQueries,
    private val mainScheduler: Scheduler
) {

    private val list = mutableListOf<Announcement>()

    fun add(announcement: Announcement): AnnouncementList {
        list.add(announcement)
        return this
    }

    fun checkLatest(host: AnnouncementHost, disposables: CompositeDisposable) {
        host.clearAllAnnouncements()

        disposables +=
            checkKycResubmissionPrompt(host)
                .switchIfEmpty(checkDashboardAnnouncements(host))
                .switchIfEmpty(checkKycPrompt(host))
                .switchIfEmpty(addSunriverPrompts(host))
                .subscribeBy(
                    onError = Timber::e
                )
    }

    private fun checkKycResubmissionPrompt(host: AnnouncementHost): Maybe<Unit> {
        val dismissEntry = dismissRecorder[KYC_RESUBMISSION_DISMISSED]

        val kycIncompleteData: (SunriverCardType) -> AnnouncementCard = {
            ImageRightAnnouncementCard(
                title = R.string.kyc_resubmission_card_title,
                description = R.string.kyc_resubmission_card_description,
                link = R.string.kyc_resubmission_card_button,
                image = R.drawable.vector_kyc_onboarding,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    host.startKyc(CampaignType.Resubmission)
                },
                prefsKey = dismissEntry.prefsKey
            )
        }
        return maybeDisplayAnnouncement(
            host = host,
            dismissEntry = dismissRecorder[IGNORE_USER_DISMISS], // ???
            show = kycTiersQueries.isKycResubmissionRequired(),
            createAnnouncement = kycIncompleteData
        )
    }

    private fun checkKycPrompt(host: AnnouncementHost): Maybe<Unit> {
        val dismissEntry = dismissRecorder[KYC_INCOMPLETE_DISMISSED]

        val kycIncompleteData: (SunriverCardType) -> AnnouncementCard = { campaignCard ->
            ImageRightAnnouncementCard(
                title = R.string.buy_sell_verify_your_identity,
                description = R.string.kyc_drop_off_card_description,
                link = R.string.kyc_drop_off_card_button,
                image = R.drawable.vector_kyc_onboarding,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    val campaignType = if (campaignCard == SunriverCardType.FinishSignUp) {
                        CampaignType.Sunriver
                    } else {
                        CampaignType.Swap
                    }

                    host.startKyc(campaignType)
                },
                prefsKey = dismissEntry.prefsKey
            )
        }
        return maybeDisplayAnnouncement(
            host = host,
            dismissEntry = dismissEntry,
            show = kycTiersQueries.isKycInProgress(),
            createAnnouncement = kycIncompleteData
        )
    }

    private fun maybeDisplayAnnouncement(
        host: AnnouncementHost,
        dismissEntry: DismissRecorder.DismissEntry,
        show: Single<Boolean>,
        createAnnouncement: (SunriverCardType) -> AnnouncementCard
    ): Maybe<Unit> {
        if (dismissEntry.isDismissed) return Maybe.empty()

        return Singles.zip(show, sunriverCampaignHelper.getCampaignCardType())
            .observeOn(mainScheduler)
            .doOnSuccess { (show, campaignCard) ->
                if (show) {
                    host.showAnnouncementCard(createAnnouncement(campaignCard))
                }
            }
            .flatMapMaybe { (show, _) ->
                if (show) Maybe.just(Unit) else Maybe.empty()
            }
            .onErrorResumeNext(Maybe.empty())
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addSunriverPrompts(host: AnnouncementHost): Maybe<Unit> {
        return sunriverCampaignHelper.getCampaignCardType()
            .observeOn(mainScheduler)
            .map { sunriverCardType ->
                when (sunriverCardType) {
                    SunriverCardType.None,
                    SunriverCardType.JoinWaitList -> false
                    SunriverCardType.FinishSignUp -> maybeAddContinueClaimSunriverCard(host)
                    SunriverCardType.Complete -> maybeAddOnTheWaySunriverCard(host)
                }
            }.flatMapMaybe { shown -> if (shown) Maybe.just(Unit) else Maybe.empty() }
    }

    private fun maybeAddContinueClaimSunriverCard(host: AnnouncementHost): Boolean {
        // Historical. But why would you do this? This class is now un-renameable. So DON'T rename
        val prefsKey = SunriverCardType.FinishSignUp.javaClass.simpleName

        val dismissEntry = dismissRecorder[prefsKey]

        if (!dismissEntry.isDismissed) {
            host.showAnnouncementCard(
                SunriverCard(
                    title = R.string.sunriver_announcement_stellar_claim_title,
                    description = R.string.sunriver_announcement_stellar_claim_message,
                    link = R.string.sunriver_announcement_stellar_claim_cta,
                    closeFunction = {
                        host.dismissAnnouncementCard(prefsKey)
                        dismissEntry.isDismissed = true
                    },
                    linkFunction = {
                        host.startKyc(CampaignType.Sunriver)
                    },
                    prefsKey = prefsKey
                )
            )
            return true
        }
        return false
    }

    private fun maybeAddOnTheWaySunriverCard(host: AnnouncementHost): Boolean {
        // Historical. But why would you do this? This class is now un-renameable. So DON'T rename
        val prefsKey = SunriverCardType.Complete.javaClass.simpleName

        val dismissEntry = dismissRecorder[prefsKey]

        if (!dismissEntry.isDismissed) {
            host.showAnnouncementCard(
                SunriverCard(
                    title = R.string.sunriver_announcement_stellar_on_the_way_title,
                    description = R.string.sunriver_announcement_stellar_on_the_way_message,
                    closeFunction = {
                        host.dismissAnnouncementCard(prefsKey)
                        dismissEntry.isDismissed = true
                    },
                    linkFunction = { },
                    prefsKey = prefsKey
                )
            )
            return true
        }
        return false
    }

    private fun checkDashboardAnnouncements(host: AnnouncementHost): Maybe<Unit> =
        showNextAnnouncement(host)
            .map { Unit }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showNextAnnouncement(host: AnnouncementHost): Maybe<Announcement> =
        getNextAnnouncement()
            .observeOn(mainScheduler)
            .doOnSuccess { it.show(host) }

    private fun getNextAnnouncement(): Maybe<Announcement> =
        Observable.concat(
            list.map { a ->
                Observable.defer {
                    a.shouldShow()
                        .filter { it }
                        .map { a }
                        .toObservable()
                }
            }
        ).firstElement()

    companion object {
        @VisibleForTesting
        internal const val KYC_INCOMPLETE_DISMISSED = "KYC_INCOMPLETE_DISMISSED"

        @VisibleForTesting
        internal const val KYC_RESUBMISSION_DISMISSED = "KYC_RESUBMISSION_DISMISSED"

        @VisibleForTesting
        internal const val IGNORE_USER_DISMISS = "IGNORE_USER_DISMISS"
    }
}