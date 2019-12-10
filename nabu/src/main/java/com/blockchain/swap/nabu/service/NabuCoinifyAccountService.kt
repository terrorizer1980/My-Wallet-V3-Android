package com.blockchain.swap.nabu.service

import com.blockchain.swap.nabu.api.nabu.Nabu
import com.blockchain.swap.nabu.models.nabu.NabuUser
import com.blockchain.swap.nabu.models.nabu.UpdateCoinifyTraderIdRequest
import com.blockchain.swap.nabu.Authenticator
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.services.BuyConditions
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString

internal class NabuCoinifyAccountService(
    private val endpoint: Nabu,
    private val authenticator: Authenticator,
    private val coinifyDataManager: CoinifyDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val currencyState: CurrencyState,
    private val exchangeService: ExchangeService,
    private val metadataManager: MetadataManager,
    private val buyConditions: BuyConditions
) : NabuCoinifyAccountCreator {

    private val user: Single<NabuUser> by lazy {
        authenticator.authenticate {
            endpoint.getUser(it.authHeader)
        }
    }

    /**
     * Creates a Coinify account for the nabu user, if no such user exists.
     */
    override fun createCoinifyAccountIfNeeded(): Completable =
        coinifyDataInMetadata()
            .switchIfEmpty(
                createCoinifyAccount()
                    .flatMap { saveCoinifyMetadata(it) }
            )
            .flatMapCompletable { coinifyData ->
                hasCoinifyAccountStoredInNabu()
                    .flatMapCompletable { hasCoinifyStoredInNabu ->
                        if (hasCoinifyStoredInNabu) {
                            Completable.complete()
                        } else {
                            saveCoinifyTraderIdToNabu(coinifyData.user)
                        }
                    }
            }

    private fun coinifyDataInMetadata(): Maybe<CoinifyData> =
        exchangeService.getCoinifyData()
            .filter { it.user != 0 }

    private fun validatedNabuUser(): Single<NabuUser> =
        Singles.zip(
            user,
            buyConditions.buySellCountries()
        ).map { (user, supportedCountries) ->
            val countryCode = user.address?.countryCode ?: "US"
            if (!supportedCountries.contains(countryCode)) {
                throw IllegalStateException("Cannot create Coinify account for country")
            } else {
                user
            }
        }

    private fun createCoinifyAccount(): Single<CoinifyData> =
        Singles.zip(
            validatedNabuUser(),
            walletOptionsDataManager.getCoinifyPartnerId().firstOrError()
        ).flatMap { (user, partnerId) ->
            val countryCode = user.address?.countryCode ?: "US"
            coinifyDataManager.getEmailTokenAndSignUp(
                payloadDataManager.guid,
                payloadDataManager.sharedKey,
                user.email,
                currencyState.fiatUnit,
                countryCode,
                partnerId
            )
        }.map {
            CoinifyData(it.trader.id, it.offlineToken)
        }

    private fun hasCoinifyAccountStoredInNabu(): Single<Boolean> =
        user.map {
            it.tags?.contains("COINIFY") ?: false
        }

    private fun saveCoinifyMetadata(coinifyData: CoinifyData): Single<CoinifyData> =
        exchangeService.getExchangeMetaData()
            .doOnNext {
                it.coinify = coinifyData
            }
            .flatMapCompletable {
                metadataManager.saveToMetadata(
                    it.toSerialisedString(),
                    MetadataManager.METADATA_TYPE_EXCHANGE
                )
            }
            .toSingle { coinifyData }

    private fun saveCoinifyTraderIdToNabu(traderId: Int): Completable =
        authenticator.authenticateCompletable {
            endpoint.setCoinifyTraderId(
                UpdateCoinifyTraderIdRequest(traderId),
                it.authHeader
            )
        }
}