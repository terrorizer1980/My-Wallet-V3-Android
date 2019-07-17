package piuk.blockchain.android.ui.thepit

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.WalletMercuryLink
import com.blockchain.nabu.NabuToken
import io.reactivex.Single
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
    private val pitLinking: PitLinking
) : BasePresenter<PitPermissionsView>() {

    private fun linkWallet() = nabuToken.fetchNabuToken().flatMap { nabu.linkWalletWithMercury(it) }
    private fun fetchUser() = nabuToken.fetchNabuToken().flatMap { nabu.getUser(it) }

    override fun onViewReady() { }

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
            .doOnSubscribe { view?.showLoading() }
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
            .doFinally { view?.hideLoading() }
            .subscribeBy(
                onError = { if (it !is EmailNotVerifiedException) view?.onLinkFailed(it.message ?: "") },
                onSuccess = { doOnWalletToPitSuccess(it) }
            )
    }

    private fun doOnWalletToPitSuccess(pair: Pair<WalletMercuryLink, String>) {
        val linkId = pair.first.linkId
        val email = pair.second
        val encodedEmail = URLEncoder.encode(email, "utf-8")
        val link = BuildConfig.PIT_URL + "$linkId?email=$encodedEmail"

        view?.onLinkSuccess(link)
    }

    fun tryToConnectPitToWallet(linkId: String) {
        // TODO: The pit will send us a dynamic link, containing a link id
        // call should be down to: /users/link-account/existing in nabu
        // and when complete should share wallets address and pop a "linked OK" dialog
    }

    private fun isEmailVerified(user: NabuUser): Single<String> =
        if (user.emailVerified)
            Single.just(user.email)
        else
            Single.error(EmailNotVerifiedException(user.email))

    private class EmailNotVerifiedException(val email: String) : Throwable()
}
