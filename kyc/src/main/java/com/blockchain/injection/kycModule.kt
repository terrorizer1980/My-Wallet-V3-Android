package com.blockchain.injection

import com.blockchain.koin.moshiInterceptor
import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.datamanagers.nabu.CreateNabuTokenAdapter
import com.blockchain.kyc.datamanagers.nabu.NabuAuthenticator
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.nabu.NabuDataManagerImpl
import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProviderNabuDataManagerAdapter
import com.blockchain.kyc.datamanagers.nabu.NabuUserSyncUpdateUserWalletInfoWithJWT
import com.blockchain.kyc.datamanagers.onfido.OnfidoDataManager
import com.blockchain.kyc.models.nabu.KycStateAdapter
import com.blockchain.kyc.models.nabu.KycTierStateAdapter
import com.blockchain.kyc.models.nabu.UserStateAdapter
import com.blockchain.kyc.services.nabu.NabuCoinifyAccountCreator
import com.blockchain.kyc.services.nabu.NabuCoinifyAccountService
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.services.nabu.NabuTierService
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kyc.services.nabu.TierUpdater
import com.blockchain.kyc.services.onfido.OnfidoService
import com.blockchain.kyc.services.wallet.RetailWalletTokenService
import com.blockchain.kyc.smsVerificationRemoteConfig
import com.blockchain.kyc.stableCoinRemoteConfig
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kyc.sunriverAirdropRemoteConfig
import com.blockchain.kycui.address.EligibilityForFreeEthAdapter
import com.blockchain.kycui.address.CurrentTierAdapter
import com.blockchain.kycui.address.KycHomeAddressPresenter
import com.blockchain.kycui.address.Tier2Decision
import com.blockchain.kycui.address.Tier2DecisionAdapter
import com.blockchain.kycui.countryselection.KycCountrySelectionPresenter
import com.blockchain.kycui.email.entry.KycEmailEntryPresenter
import com.blockchain.kycui.email.validation.KycEmailValidationPresenter
import com.blockchain.kycui.invalidcountry.KycInvalidCountryPresenter
import com.blockchain.kycui.mobile.entry.KycMobileEntryPresenter
import com.blockchain.kycui.mobile.validation.KycMobileValidationPresenter
import com.blockchain.kycui.navhost.KycNavHostPresenter
import com.blockchain.kycui.navhost.KycStarter
import com.blockchain.kycui.navhost.KycStarterAirdrop
import com.blockchain.kycui.navhost.KycStarterBuySell
import com.blockchain.kycui.onfidosplash.OnfidoSplashPresenter
import com.blockchain.kycui.profile.KycProfilePresenter
import com.blockchain.kycui.reentry.KycNavigator
import com.blockchain.kycui.reentry.ReentryDecision
import com.blockchain.kycui.reentry.ReentryDecisionKycNavigator
import com.blockchain.kycui.reentry.TiersReentryDecision
import com.blockchain.kycui.splash.KycSplashPresenter
import com.blockchain.kycui.stablecoin.StableCoinCampaignHelper
import com.blockchain.kycui.status.KycStatusPresenter
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.kycui.tiersplash.KycTierSplashPresenter
import com.blockchain.kycui.veriffsplash.VeriffSplashPresenter
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.EthEligibility
import com.blockchain.nabu.StartKyc
import com.blockchain.nabu.StartKycAirdrop
import com.blockchain.nabu.StartKycForBuySell
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.sunriver.SunriverCampaignSignUp
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.repositories.AccessTokenStore
import piuk.blockchain.androidbuysell.services.CoinifyService
import retrofit2.Retrofit

val kycModule = applicationContext {

    factory { KycStarter() as StartKyc }

    factory { KycStarterAirdrop() as StartKycAirdrop }

    factory { KycStarterBuySell() as StartKycForBuySell }

    bean { NabuSessionTokenStore() }

    bean { OnfidoService(get("kotlin")) }

    bean { NabuService(get("nabu")) }

    bean { RetailWalletTokenService(get(), getProperty("api-code"), get("kotlin")) }

    factory { OnfidoDataManager(get()) }

    factory { TiersReentryDecision() as ReentryDecision }

    context("Payload") {

        factory { ReentryDecisionKycNavigator(get(), get(), get()) as KycNavigator }

        factory {
            KycTierSplashPresenter(
                get(),
                get(),
                get(),
                get("ff_sunriver_has_large_backlog")
            )
        }

        factory { KycSplashPresenter(get(), get(), get(), get(), get()) }

        factory { KycCountrySelectionPresenter(get(), get()) }

        factory { KycProfilePresenter(get(), get(), get(), get(), get()) }

        factory { KycHomeAddressPresenter(get(), get(), get(), get()) }

        factory { KycMobileEntryPresenter(get(), get()) }

        factory { KycMobileValidationPresenter(get(), get()) }

        factory { KycEmailEntryPresenter(get()) }

        factory { KycEmailValidationPresenter(get(), get()) }

        factory { OnfidoSplashPresenter(get(), get(), get()) }

        factory {
            VeriffSplashPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                analytics = get()
            )
        }

        factory { KycStatusPresenter(get(), get(), get()) }

        factory { KycNavHostPresenter(get(), get(), get(), get(), get(), get()) }

        factory { KycInvalidCountryPresenter(get(), get()) }

        factory("sunriver") { sunriverAirdropRemoteConfig(get()) }

        factory("stablecoin") { stableCoinRemoteConfig(get()) }

        factory("ff_sms_verification") { smsVerificationRemoteConfig(get()) }

        factory { NabuDataUserProviderNabuDataManagerAdapter(get(), get()) as NabuDataUserProvider }

        factory { NabuUserSyncUpdateUserWalletInfoWithJWT(get(), get()) as NabuUserSync }

        factory { KycTiersQueries(get(), get()) }
    }

    moshiInterceptor("kyc") { builder ->
        builder
            .add(KycStateAdapter())
            .add(KycTierStateAdapter())
            .add(UserStateAdapter())
    }
}

val kycCoinifyModule = applicationContext {

    context("Payload") {

        bean { AccessTokenStore() }

        factory { CoinifyDataManager(get(), get(), get()) }

        factory { CoinifyService(get(), get("kotlin"), get()) }
    }
}

val kycNabuModule = applicationContext {

    context("Payload") {

        factory {
            NabuDataManagerImpl(
                nabuService = get(),
                retailWalletTokenService = get(),
                nabuTokenStore = get(),
                appVersion = getProperty("app-version"),
                settingsDataManager = get(),
                payloadDataManager = get(),
                prefs = get()
            ) as NabuDataManager
        }

        factory {
            NabuCoinifyAccountService(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            ) as NabuCoinifyAccountCreator
        }

        factory {
            NabuAuthenticator(get(), get()) as Authenticator
        }

        factory {
            get<Retrofit>("nabu").create(Nabu::class.java)
        }

        factory { NabuTierService(get(), get()) }
            .bind(TierService::class)
            .bind(TierUpdater::class)

        factory {
            Tier2DecisionAdapter(get(), get()) as Tier2Decision
        }

        factory {
            CurrentTierAdapter(get(), get()) as CurrentTier
        }

        factory {
            EligibilityForFreeEthAdapter(nabuToken = get(), nabuDataManager = get()) as EthEligibility
        }

        factory {
            CreateNabuTokenAdapter(get()) as CreateNabuToken
        }

        factory { SunriverCampaignHelper(get("sunriver"), get(), get(), get(), get()) }
            .bind(SunriverCampaignSignUp::class)

        factory { StableCoinCampaignHelper(get("stablecoin")) }
    }
}
