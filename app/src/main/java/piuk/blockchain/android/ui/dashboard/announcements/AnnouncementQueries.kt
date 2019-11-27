package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.goldTierComplete
import com.blockchain.kyc.models.nabu.kycVerified
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.swap.nabu.NabuToken
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AnnouncementQueries(
    private val nabuToken: NabuToken,
    private val settings: SettingsDataManager,
    private val nabu: NabuDataManager,
    private val tierService: TierService
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

    fun isEligibleForStxSignup(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.currentTier == 2 && !it.isStxAirdropRegistered }
            .onErrorReturn { false }
    }

    fun isRegistedForStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.isStxAirdropRegistered }
            .onErrorReturn { false }
    }
}
