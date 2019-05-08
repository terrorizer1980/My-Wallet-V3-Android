package piuk.blockchain.android.ui.buysell.launcher

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.NabuCoinifyAccountCreator
import com.blockchain.kycui.settings.KycStatusHelper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class BuySellLauncherPresenter constructor(
    private val kycStatusHelper: KycStatusHelper,
    private val nabuCoinifyAccountCreator: NabuCoinifyAccountCreator
) : BasePresenter<BuySellLauncherView>() {

    override fun onViewReady() {
        compositeDisposable += kycStatusHelper.getKyc2TierStatus()
            .subscribeOn(Schedulers.io())
            .flatMap {
                if (it == Kyc2TierState.Tier2Approved) {
                    nabuCoinifyAccountCreator.createCoinifyAccountIfNeeded()
                        .toSingle { Single.just(it) }
                }
                Single.just(it)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.displayProgressDialog() }
            .doAfterTerminate { view.dismissProgressDialog() }
            .doOnError(Timber::e)
            .subscribeBy(
                onError = {
                    view.showErrorToast(R.string.buy_sell_launcher_error)
                    view.finishPage()
                },
                onSuccess = {
                    when (it) {
                        Kyc2TierState.Tier2Approved -> view.onStartCoinifyOverview()
                        Kyc2TierState.Hidden,
                        Kyc2TierState.Locked -> view.onStartCoinifySignUp()
                        Kyc2TierState.Tier1InReview,
                        Kyc2TierState.Tier1Approved,
                        Kyc2TierState.Tier1Failed,
                        Kyc2TierState.Tier2InReview,
                        Kyc2TierState.Tier2Failed -> view.showPendingVerificationView()
                    }
                }
            )
    }
}