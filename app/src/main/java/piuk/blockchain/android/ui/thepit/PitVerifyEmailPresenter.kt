package piuk.blockchain.android.ui.thepit

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class PitVerifyEmailPresenter(private val emailSyncUpdater: EmailSyncUpdater) : BasePresenter<PitVerifyEmailView>() {

    override fun onViewReady() {
        compositeDisposable += resendMail.switchMapSingle {
            emailSyncUpdater.updateEmailAndSync(it)
        }.observeOn(AndroidSchedulers.mainThread()).subscribeBy(onError = {
            view?.mailResentFailed()
        }, onNext = {
            if (it.address.isNotEmpty())
                view?.mailResentSuccessfully()
            else
                view?.mailResentFailed()
        })
    }

    val resendMail = PublishSubject.create<String>()
}