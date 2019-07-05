package piuk.blockchain.android.ui.thepit

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.WalletMercuryLink
import com.blockchain.nabu.NabuToken
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import java.util.concurrent.TimeUnit

class PitPermissionsPresenter(
    private val nabuDataManager: NabuDataManager,
    nabuToken: NabuToken,
    private val settingsDataManager: SettingsDataManager
) : BasePresenter<PitPermissionsView>() {

    private val connectNow = PublishSubject.create<Unit>()
    val tryToConnect = PublishSubject.create<Unit>()
    val checkEmailIfEmailIsVerified = PublishSubject.create<Unit>()

    private val linkWallet = nabuToken.fetchNabuToken().flatMap {
        nabuDataManager.linkWalletWithMercury(it)
    }.toObservable()

    private val connect = Observables.zip(
        linkWallet,
        settingsDataManager.getSettings().map { it.email },
        Observable.timer(2, TimeUnit.SECONDS)
    ) { walletLinking, email, _ -> Pair(walletLinking, email) }
        .doOnSubscribe {
            view?.showLoading()
        }.doFinally {
            view?.hideLoading()
        }.doOnError {
            view?.hideLoading()
        }

    override fun onViewReady() {
        compositeDisposable += tryToConnect.switchMap {
            settingsDataManager.getSettings()
        }.subscribe {
            if (it.isEmailVerified.not()) {
                view?.promptForEmailVerification(it.email)
            } else {
                connectNow.onNext(Unit)
            }
        }

        compositeDisposable += checkEmailIfEmailIsVerified.switchMap {
            settingsDataManager.fetchSettings()
        }.subscribe {
            if (it.isEmailVerified) {
                view?.showEmailVerifiedDialog()
            }
        }

        compositeDisposable += connectNow.switchMap {
            connect
        }.subscribeBy(onError = {
            view?.onLinkFailed(it.message ?: "")
        }, onNext = {
            view?.onLinkSuccess(it.buildPitLinkingUrl())
        })
    }
}

private fun Pair<WalletMercuryLink, String>.buildPitLinkingUrl(): String =
    BuildConfig.PIT_URL + "/${first.linkId}?email=$second"