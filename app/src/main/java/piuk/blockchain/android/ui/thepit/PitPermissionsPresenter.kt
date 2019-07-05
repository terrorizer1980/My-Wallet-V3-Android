package piuk.blockchain.android.ui.thepit

import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class PitPermissionsPresenter(private val settingsDataManager: SettingsDataManager) :
    BasePresenter<PitPermissionsView>() {

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
    }

    private val connectNow = PublishSubject.create<Unit>()
    val tryToConnect = PublishSubject.create<Unit>()
    val checkEmailIfEmailIsVerified = PublishSubject.create<Unit>()
}