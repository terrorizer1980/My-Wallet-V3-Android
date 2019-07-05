package piuk.blockchain.android.ui.thepit

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class PitPermissionsPresenter(
    private val nabuDataManager: NabuDataManager,
    nabuToken: NabuToken,
    private val settingsDataManager: SettingsDataManager
) : BasePresenter<PitPermissionsView>() {

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
            settingsDataManager.getSettings()
        }.subscribe {
            if (it.isEmailVerified) {
                view?.showEmailVerifiedDialog()
            }
        }

        compositeDisposable += connectNow.switchMapSingle {
            linkWallet
        }.subscribeBy(onError = {
            view?.onLinkFailed(it.message ?: "")
        }, onNext = {
            view?.onLinkSuccess(it.linkId)
        })
    }

    private val linkWallet = nabuToken.fetchNabuToken().flatMap {
        nabuDataManager.linkWalletWithMercury(it)
    }.doOnSubscribe {
        view?.showLoading()
    }.doFinally {
        view?.hideLoading()
    }.doOnError {
        view?.hideLoading()
    }


    private val connectNow = PublishSubject.create<Unit>()
    val tryToConnect = PublishSubject.create<Unit>()
    val checkEmailIfEmailIsVerified = PublishSubject.create<Unit>()
}