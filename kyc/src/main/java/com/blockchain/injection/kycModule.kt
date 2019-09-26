@file:Suppress("USELESS_CAST")

package com.blockchain.injection

import com.blockchain.koin.moshiInterceptor
import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.datamanagers.nabu.CreateNabuTokenAdapter
import com.blockchain.kyc.datamanagers.nabu.NabuAuthenticator
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.nabu.NabuDataManagerImpl
import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProviderNabuDataManagerAdapter
import com.blockchain.kyc.datamanagers.nabu.NabuUserReporter
import com.blockchain.kyc.datamanagers.nabu.AnalyticsNabuUserReporterImpl
import com.blockchain.kyc.datamanagers.nabu.AnalyticsWalletReporter
import com.blockchain.kyc.datamanagers.nabu.NabuUserSyncUpdateUserWalletInfoWithJWT
import com.blockchain.kyc.datamanagers.nabu.UniqueAnalyticsNabuUserReporter
import com.blockchain.kyc.datamanagers.nabu.UniqueAnalyticsWalletReporter
import com.blockchain.kyc.datamanagers.nabu.WalletReporter
import com.blockchain.kyc.models.nabu.KycStateAdapter
import com.blockchain.kyc.models.nabu.KycTierStateAdapter
import com.blockchain.kyc.models.nabu.UserStateAdapter
import com.blockchain.kyc.services.nabu.NabuCoinifyAccountCreator
import com.blockchain.kyc.services.nabu.NabuCoinifyAccountService
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.services.nabu.NabuTierService
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kyc.services.nabu.TierUpdater
import com.blockchain.kyc.services.wallet.RetailWalletTokenService
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.CreateNabuToken
import com.blockchain.swap.nabu.NabuUserSync
import com.blockchain.swap.nabu.stores.NabuSessionTokenStore
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.repositories.AccessTokenStore
import piuk.blockchain.androidbuysell.services.CoinifyService
import retrofit2.Retrofit

val kycModule = applicationContext {

    bean { NabuSessionTokenStore() }

    bean { NabuService(get("nabu")) }

    bean { RetailWalletTokenService(get(), getProperty("api-code"), get("kotlin")) }

    context("Payload") {

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
                prefs = get(),
                walletReporter = get("unique_id"),
                userReporter = get("unique_user_analytics")
            ) as NabuDataManager
        }

        factory("unique_user_analytics") {
            UniqueAnalyticsNabuUserReporter(
                nabuUserReporter = get("user_analytics"),
                prefs = get()
            )
        }.bind(NabuUserReporter::class)

        factory("user_analytics") {
            AnalyticsNabuUserReporterImpl(
                userAnalytics = get()
            )
        }.bind(NabuUserReporter::class)

        factory("unique_id") {
            UniqueAnalyticsWalletReporter(get("wallet_analytics"), prefs = get())
        }.bind(WalletReporter::class)

        factory("wallet_analytics") {
            AnalyticsWalletReporter(userAnalytics = get())
        }.bind(WalletReporter::class)

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
            CreateNabuTokenAdapter(get()) as CreateNabuToken
        }
    }
}
