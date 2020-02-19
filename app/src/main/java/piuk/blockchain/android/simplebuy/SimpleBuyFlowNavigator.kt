package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Observable
import io.reactivex.Single

class SimpleBuyFlowNavigator(private val simpleBuyModel: SimpleBuyModel, private val tierService: TierService) {

    fun navigateTo(startedFromKycResume: Boolean): Single<FlowScreen> =
        simpleBuyModel.state.flatMap {
            if (startedFromKycResume || it.currentScreen == FlowScreen.KYC) {
                tierService.tiers().toObservable().map { tier ->
                    when(tier.combinedState) {
                        Kyc2TierState.Tier2Approved -> FlowScreen.CHECKOUT
                        Kyc2TierState.Tier2InPending,
                        Kyc2TierState.Tier2InReview,
                        Kyc2TierState.Tier2Failed -> FlowScreen.KYC_VERIFICATION
                        else -> FlowScreen.KYC
                    }
            }
        } else {
            Observable.just(it.currentScreen)
        }
    }.firstOrError()
}