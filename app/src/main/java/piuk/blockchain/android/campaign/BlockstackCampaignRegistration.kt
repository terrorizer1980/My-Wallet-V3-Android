package piuk.blockchain.android.campaign

import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.CampaignData
import com.blockchain.swap.nabu.models.nabu.RegisterCampaignRequest
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.AccountReference
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.STXTokens

class BlockstackCampaignRegistration(
    private val nabuDataManager: NabuDataManager,
    private val nabuToken: NabuToken,
    private val stx: STXTokens
) : CampaignRegistration {
    private fun defaultAccount(): Single<AccountReference> = stx.defaultAccount()

    override fun registerCampaign(): Completable =
        registerCampaign(CampaignData(blockstackCampaignName, false))

    override fun registerCampaign(campaignData: CampaignData): Completable =
        defaultAccount().flatMapCompletable { stxAccount ->
            nabuToken.fetchNabuToken()
                .flatMapCompletable {
                    doRegisterForCampaign(it, stxAccount as AccountReference.Stx, campaignData)
                }
        }

    private fun doRegisterForCampaign(
        token: NabuOfflineTokenResponse,
        stxAccount: AccountReference.Stx,
        campaignData: CampaignData
    ): Completable =
        nabuDataManager.registerCampaign(
            token,
            RegisterCampaignRequest.registerBlockstack(
                stxAccount.address,
                campaignData.newUser
            ),
            campaignData.campaignName
        ).subscribeOn(Schedulers.io())

    override fun userIsInCampaign(): Single<Boolean> =
        getCampaignList().map { it.contains(blockstackCampaignName) }

    private fun getCampaignList(): Single<List<String>> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.getCampaignList(it)
        }.onErrorReturn { emptyList() }
}
