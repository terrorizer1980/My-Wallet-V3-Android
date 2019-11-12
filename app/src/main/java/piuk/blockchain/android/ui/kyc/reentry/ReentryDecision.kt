package piuk.blockchain.android.ui.kyc.reentry

import androidx.navigation.NavDirections
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuUser
import piuk.blockchain.android.ui.kyc.navhost.toProfileModel
import com.blockchain.swap.nabu.NabuToken
import io.reactivex.Single
import piuk.blockchain.android.KycNavXmlDirections

interface ReentryDecision {

    fun findReentryPoint(user: NabuUser): ReentryPoint
}

interface KycNavigator {

    /**
     * Will fetch user, if you have it, user overload.
     */
    fun findNextStep(): Single<NavDirections>

    fun findNextStep(user: NabuUser): NavDirections

    fun userAndReentryPointToDirections(user: NabuUser, reentryPoint: ReentryPoint): NavDirections
}

internal class ReentryDecisionKycNavigator(
    private val token: NabuToken,
    private val dataManager: NabuDataManager,
    private val reentryDecision: ReentryDecision
) : KycNavigator {

    override fun findNextStep(): Single<NavDirections> =
        token.fetchNabuToken()
            .flatMap(dataManager::getUser)
            .flatMap { Single.just(findNextStep(it)) }

    override fun findNextStep(user: NabuUser) =
        userAndReentryPointToDirections(user, reentryDecision.findReentryPoint(user))

    override fun userAndReentryPointToDirections(user: NabuUser, reentryPoint: ReentryPoint) =
        when (reentryPoint) {
            ReentryPoint.EmailEntry -> KycNavXmlDirections.ActionStartEmailVerification()
            ReentryPoint.CountrySelection -> KycNavXmlDirections.ActionStartCountrySelection()
            ReentryPoint.Profile -> KycNavXmlDirections.ActionStartProfile(user.requireCountryCode())
            ReentryPoint.Address -> KycNavXmlDirections.ActionStartAddressEntry(user.toProfileModel())
            ReentryPoint.MobileEntry -> KycNavXmlDirections.ActionStartMobileVerification(user.requireCountryCode())
            ReentryPoint.Veriff -> {
                val countryCode = user.requireCountryCode()
                KycNavXmlDirections.ActionStartVeriff(countryCode)
            }
        }
}
