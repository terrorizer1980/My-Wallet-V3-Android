package piuk.blockchain.android.ui.thepit

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.preferences.ThePitLinkingPrefs
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class PitPermissionsPresenter(
    private val nabu: NabuDataManager,
    private val nabuToken: NabuToken,
    private val pitLinking: PitLinking,
    private val prefs: ThePitLinkingPrefs
) : BasePresenter<PitPermissionsView>() {

    private fun linkWallet() = nabuToken.fetchNabuToken().flatMap { nabu.linkWalletWithMercury(it) }
    private fun fetchUser() = nabuToken.fetchNabuToken().flatMap { nabu.getUser(it) }
    private fun linkPit(linkId: String) = nabuToken.fetchNabuToken()
        .flatMapCompletable { nabu.linkMercuryWithWallet(it, linkId) }

    override fun onViewReady() {}

    fun checkEmailIsVerified() {
        compositeDisposable += fetchUser()
            .flatMap { isEmailVerified(it) }
            .subscribeBy(
                onSuccess = { view?.showEmailVerifiedDialog() },
                onError = { Timber.d("Email not verified") }
            )
    }

    fun tryToConnectWalletToPit() {
        compositeDisposable += fetchUser()
            .flatMap { isEmailVerified(it) }
            .doOnError { if (it is EmailNotVerifiedException) view?.promptForEmailVerification(it.email) }
            .flatMap {
                Singles.zip(
                    linkWallet(),
                    Single.just(it),
                    Single.timer(2, TimeUnit.SECONDS)
                ) { linkId, email, _ -> Pair(linkId, email) }
            }
            .doOnSuccess { pitLinking.sendWalletAddressToThePit() }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showLoading() }
            .doFinally { view?.hideLoading() }
            .subscribeBy(
                onError = { if (it !is EmailNotVerifiedException) view?.onLinkFailed(it.message ?: "") },
                onSuccess = { doOnWalletToPitSuccess(it) }
            )
    }

    private fun doOnWalletToPitSuccess(pair: Pair<String, String>) {
        val linkId = pair.first
        val email = pair.second
        val encodedEmail = URLEncoder.encode(email, "utf-8")
        val link = BuildConfig.PIT_LINKING_URL + "$linkId?email=$encodedEmail"

        view?.onLinkSuccess(link)
    }

    fun tryToConnectPitToWallet(linkId: String) {
        compositeDisposable += fetchUser()
            .flatMap { isEmailVerified(it) }
            .doOnError {
                if (it is EmailNotVerifiedException) {
                    prefs.pitToWalletLinkId = linkId
                    view?.promptForEmailVerification(it.email)
                }
            }
            .flatMapCompletable {
                linkPit(linkId)
                    .mergeWith(Completable.timer(2, TimeUnit.SECONDS))
            }
            .doOnComplete { pitLinking.sendWalletAddressToThePit() }
            .doOnComplete { prefs.clearPitToWalletLinkId() }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showLoading() }
            .doFinally { view?.hideLoading() }
            .subscribeBy(
                onError = { if (it !is EmailNotVerifiedException) view?.onLinkFailed(it.message ?: "") },
                onComplete = { view?.onPitLinked() }
            )
    }

    private fun isEmailVerified(user: NabuUser): Single<String> =
        if (user.emailVerified)
            Single.just(user.email)
        else
            Single.error(EmailNotVerifiedException(user.email))

    fun clearLinkPrefs() {
        prefs.clearPitToWalletLinkId()
    }

    private class EmailNotVerifiedException(val email: String) : Throwable()
}
