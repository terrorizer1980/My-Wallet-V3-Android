package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.sunriver.ui.BaseAirdropBottomDialog
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber

interface AnnouncementHost {
    val disposables: CompositeDisposable

    fun clearAllAnnouncements()
    fun showAnnouncementCard(card: AnnouncementCard)
    fun dismissAnnouncementCard(prefsKey: String)

    fun showAnnouncmentPopup(popup: BaseAirdropBottomDialog)

    // Actions
    fun signupToSunRiverCampaign()
    fun startSwapOrKyc(swapTarget: CryptoCurrency? = null)
    fun startKyc(campaignType: CampaignType)
    fun startPitLinking()
}

interface AnnouncementRule {
    val dismissKey: String
    fun shouldShow(): Single<Boolean>
    fun show(host: AnnouncementHost)
}

class AnnouncementList(private val mainScheduler: Scheduler) {

    private val list = mutableListOf<AnnouncementRule>()

    fun add(announcement: AnnouncementRule): AnnouncementList {
        list.add(announcement)
        return this
    }

    fun checkLatest(host: AnnouncementHost, disposables: CompositeDisposable) {
        host.clearAllAnnouncements()

        disposables += showNextAnnouncement(host)
            .subscribeBy(onError = Timber::e)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showNextAnnouncement(host: AnnouncementHost): Maybe<AnnouncementRule> =
        getNextAnnouncement()
            .observeOn(mainScheduler)
            .doOnSuccess { it.show(host) }

    private fun getNextAnnouncement(): Maybe<AnnouncementRule> =
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

    internal fun dismissKeys(): List<String> = list.map { it.dismissKey }
}