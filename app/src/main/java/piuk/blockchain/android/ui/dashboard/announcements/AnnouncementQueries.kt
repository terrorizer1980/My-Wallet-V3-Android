package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.Scope
import com.blockchain.swap.nabu.models.nabu.goldTierComplete
import com.blockchain.swap.nabu.models.nabu.kycVerified
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.models.nabu.UserCampaignState
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.simplebuy.SimpleBuyUtils
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AnnouncementQueries(
    private val nabuToken: NabuToken,
    private val settings: SettingsDataManager,
    private val nabu: NabuDataManager,
    private val tierService: TierService,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val sbUtils: SimpleBuyUtils,
    private val custodialWalletManager: CustodialWalletManager
) {
    // Attempt to figure out if KYC/swap etc is allowed based on location...
    fun canKyc(): Single<Boolean> {

        return Singles.zip(
            settings.getSettings()
                .map { it.countryCode }
                .singleOrError(),
            nabu.getCountriesList(Scope.None)
        ).map { (country, list) ->
            list.any { it.code == country && it.isKycAllowed }
        }.onErrorReturn { false }
    }

    // Have we moved past kyc tier 1 - silver?
    fun isKycGoldStartedOrComplete(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.tierInProgressOrCurrentTier == 2 }
            .onErrorReturn { false }
    }

    // Have we been through the Gold KYC process? ie are we Tier2InReview, Tier2Approved or Tier2Failed (cf TierJson)
    fun isGoldComplete(): Single<Boolean> =
        tierService.tiers()
            .map { it.combinedState in goldTierComplete }

    fun isTier1Or2Verified(): Single<Boolean> =
        tierService.tiers().map { it.combinedState in kycVerified }

    fun isRegistedForStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.isStxAirdropRegistered }
            .onErrorReturn { false }
    }

    fun hasReceivedStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getAirdropCampaignStatus(token) }
            .map { it[blockstackCampaignName]?.userState == UserCampaignState.RewardReceived }
    }

    fun isSimpleBuyKycInProgress(): Single<Boolean> {
        // If we have a local simple buy in progress and it has the kyc unfinished state set
        val state = sbUtils.inflateSimpleBuyState(simpleBuyPrefs)

        return state?.let {
            Single.just(it.kycStartedButNotCompleted)
        } ?: Single.just(false)
    }

    // This logic will need revisiting, once we have a backend connection and have finialise how we manage
    // simple buy state across platforms and how we make it re-entrant TODO
    fun isSimpleBuyTransactionPending(): Single<Boolean> {
        val state = sbUtils.inflateSimpleBuyState(simpleBuyPrefs)

        return state?.let {
            if (it.order.orderState == OrderState.FINISHED) {
                custodialWalletManager.getBuyOrderStatus(it.id!!)
                    // Bit of a hack here - if the order is COMPLETE, then we wipe our local copy of the order state
                    // TODO: Find a better place to do this. Because, Ugh! Unexpected side effects!
                    .doOnSuccess { order -> if (order.status == OrderState.FINISHED) simpleBuyPrefs.clearState() }
                    .map { order -> order.status == OrderState.AWAITING_FUNDS }
                    .onErrorReturn { false }
            } else {
                Single.just(false)
            }
        } ?: Single.just(false)
    }
}
