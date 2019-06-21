package piuk.blockchain.android.ui.thepit

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import io.reactivex.rxkotlin.addTo
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class PitPermissionsPresenter(private val nabuDataManager: NabuDataManager, private val nabuToken: NabuToken)
    : BasePresenter<PitPermissionsView>() {

    override fun onViewReady() {

    }

    fun connect() {
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.linkWalletWithMercury(it)
        }.doAfterTerminate { }
            .doOnSubscribe {
            }.doOnError {

            }
            .subscribe({
            }) {
            }.addTo(compositeDisposable)
    }

}