package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.BuyOrderState
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.CustodialWalletOrder
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.datamanagers.OrderCreation
import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.simplebuy.OrderStateResponse
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber

class LiveCustodialWalletManager(
    private val nabuToken: NabuToken,
    private val nabuService: NabuService,
    private val nabuDataManager: NabuDataManager
) : CustodialWalletManager {

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.authenticate(it) { nabuSessionTokenResp ->
                nabuService.getSimpleBuyQuote(
                    sessionToken = nabuSessionTokenResp,
                    action = action,
                    currencyPair = "${crypto.symbol}-${amount.currencyCode}",
                    amount = amount.valueMinor.toString()
                )
            }.map { quoteResponse ->
                Quote(date = quoteResponse.time.toLocalTime())
            }
        }

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String
    ): Single<OrderCreation> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.authenticate(it) { nabuSessionTokenResp ->
                nabuService.createOrder(nabuSessionTokenResp,
                    CustodialWalletOrder(
                        pair = "${cryptoCurrency.symbol}-${amount.currencyCode}",
                        action = action,
                        input = OrderInput(
                            amount.currencyCode, amount.valueMinor.toString()
                        ),
                        output = OrderOutput(
                            cryptoCurrency.symbol
                        )
                    )
                )
            }
        }.map {
            OrderCreation(
                id = it.id,
                pair = it.pair,
                expiresAt = it.expiresAt,
                state = it.state.toLocalState()
            )
        }

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.authenticate(it) { nabuSessionTokenResp ->
                nabuService.getSupportCurrencies(nabuSessionTokenResp)
            }
        }.map {
            SimpleBuyPairs(it.pairs.map { responsePair ->
                SimpleBuyPair(
                    responsePair.pair,
                    BuyLimits(
                        responsePair.buyMin,
                        responsePair.buyMax
                    )
                )
            })
        }

    private fun OrderStateResponse.toLocalState(): OrderState =
        when (this) {
            OrderStateResponse.PENDING_DEPOSIT -> OrderState.AWAITING_FUNDS
            OrderStateResponse.FINISHED -> OrderState.FINISHED
            OrderStateResponse.PENDING_EXECUTION,
            OrderStateResponse.DEPOSIT_MATCHED -> OrderState.PENDING
            OrderStateResponse.FAILED,
            OrderStateResponse.EXPIRED -> OrderState.FAILED
            OrderStateResponse.CANCELED -> OrderState.CANCELED
        }

    override fun getBalanceForAsset(crypto: CryptoCurrency): Single<CryptoValue> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.authenticate(it) { nabuSessionTokenResp ->
                nabuService.getBalanceForAsset(nabuSessionTokenResp, crypto)
                    .map { balance -> CryptoValue.fromMinor(crypto, balance.available.toBigDecimal()) }
                    .doOnError {
                        // TODO: Map an error for unknown - ie unowned - assets here?
                        // TODO: Perhaps I should make this a Maybe?
                    }
            }
        }

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.authenticate(it) { nabuSessionTokenResp ->
                nabuService.getPredefinedAmounts(nabuSessionTokenResp, currency)
            }.map { response ->
                val currencyAmounts = response.firstOrNull { it[currency] != null } ?: emptyMap()
                currencyAmounts[currency]?.map { value ->
                    FiatValue.fromMinor(currency, value)
                } ?: emptyList()
            }
        }

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.authenticate(it) { nabuSessionTokenResp ->
                nabuService.getSimpleBuyBankAccountDetails(nabuSessionTokenResp, currency)
            }
        }

    override fun isEligibleForSimpleBuy(currency: String): Single<SimpleBuyEligibility> =
        nabuService.isEligibleForSimpleBuy(currency).onErrorReturn { SimpleBuyEligibility(false) }

    override fun getBuyOrderStatus(orderId: String): Single<BuyOrderState> {
        TODO("not implemented")
    }

    override fun deleteBuyOrder(orderId: String): Completable {
        TODO("not implemented")
    }

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable {
        TODO("not implemented")
    }
}