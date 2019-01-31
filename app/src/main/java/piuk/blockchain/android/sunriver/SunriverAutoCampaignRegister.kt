package piuk.blockchain.android.sunriver

import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.models.nabu.CampaignData
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.sunriver.XlmDataManager
import io.reactivex.Completable
import timber.log.Timber

class SunriverAutoCampaignRegister(
    private val nabuDataUserProvider: NabuDataUserProvider,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    private val xlmDataManager: XlmDataManager
) {
    fun autoRegisterForCampaign(): Completable =
        nabuDataUserProvider
            .getUser()
            .filter { user -> user.isTier2Verified() && !user.isRegisteredForCampaign() }
            .map { user -> CampaignData("SUNRIVER", newUser = user.kycState == KycState.None) }
            .flatMapCompletable { data ->
                xlmDataManager.maybeDefaultAccount()
                    .flatMapCompletable { account ->
                        Timber.d("AutoRegistered for airdrop")
                        sunriverCampaignHelper.registerCampaignAndSignUpIfNeeded(account, data)
                    }
            }
}

private fun NabuUser.isRegisteredForCampaign() = tags?.get("SUNRIVER")?.get("x-campaign-address") != null

private fun NabuUser.isTier2Verified() = tiers?.current == 2
