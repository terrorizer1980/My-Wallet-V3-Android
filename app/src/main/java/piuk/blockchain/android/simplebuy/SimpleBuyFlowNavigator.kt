package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Observable
import io.reactivex.Single

class SimpleBuyFlowNavigator(private val simpleBuyModel: SimpleBuyModel, private val tierService: TierService) {

    fun navigateTo(): Single<FlowScreen> = simpleBuyModel.state.flatMap {
        if (it.currentScreen != FlowScreen.KYC) {
            Observable.just(it.currentScreen)
        } else {
            tierService.tiers().toObservable().map { tier ->
                if (tier.combinedState == Kyc2TierState.Tier2Approved) {
                    FlowScreen.CHECKOUT
                } else {
                    FlowScreen.KYC
                }
            }
        }
    }.firstOrError()
}