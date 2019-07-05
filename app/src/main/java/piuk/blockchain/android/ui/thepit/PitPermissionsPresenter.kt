package piuk.blockchain.android.ui.thepit

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class PitPermissionsPresenter(private val nabuDataManager: NabuDataManager, private val nabuToken: NabuToken)
    : BasePresenter<PitPermissionsView>() {

    override fun onViewReady() {

    }

    fun connect() {
        compositeDisposable += nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.linkWalletWithMercury(it)
        }.doOnSubscribe {
            view?.showLoading()
        }.doFinally {
            view?.hideLoading()
        }.doOnError {
            view?.hideLoading()
        }.subscribeBy(onError = {
            view?.onLinkFailed(it.message ?: "")
        }, onSuccess = {
            view?.onLinkSuccess(it.linkId)
        })
    }

}