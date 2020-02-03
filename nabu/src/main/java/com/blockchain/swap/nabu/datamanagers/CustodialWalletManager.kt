package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.simplebuy.BankDetail
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single

// We've no idea what these returned API objects are going to look like, but I need something to mock and develop
// against, so I'll guess...
enum class OrderStatus {
    UNKNOWN_ORDER, // The server has never heard of this trade
    AWAITING_FUNDS, // Waiting for a bank transfer etc
    PENDING, // Funds received, but crypto not yet released (don't know if we'll need this?)
    COMPLETE, // All done
    EXPIRED // Timeout
}

data class BuyOrderStatus(val status: OrderStatus)

// inject an instance of this to provide simple buy and custodial balance/transfer services.
// In the short-term, use aa instance which provides mock data - for development and testing.
// Once the UI and business logic are all working, we can then have NabuDataManager - or something similar -
// implement this, and use koin.bind to have that instance injected instead to provide live data

interface CustodialWalletManager {

    fun getBalanceForAsset(
        crypto: CryptoCurrency
    ): Single<CryptoValue>

    fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs>

    fun getBankAccount(): Single<BankAccount>

    fun getPredefinedAmounts(
        currency: String
    ): Single<List<FiatValue>>

    fun isEligibleForSimpleBuy(
        currency: String
    ): Single<SimpleBuyEligibility>

    fun getBuyOrderStatus(orderId: String): Single<BuyOrderStatus>

    fun deleteBuyOrder(orderId: String): Completable
}

// Provide mock data for development and testing etc
class MockCustodialWalletManager(
    private val nabuToken: NabuToken
) : CustodialWalletManager {

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs> =
        Single.just(
            SimpleBuyPairs(
                listOf(
                    SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 5024558)),
                    SimpleBuyPair(pair = "ETH-USD", buyLimits = BuyLimits(100, 5024558)),
                    SimpleBuyPair(pair = "BCH-USD", buyLimits = BuyLimits(100, 5024558)),
                    SimpleBuyPair(pair = "XLM-USD", buyLimits = BuyLimits(100, 5024558)),
                    SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000)),
                    SimpleBuyPair(pair = "BTC-GBP", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-GBP", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-GBP", buyLimits = BuyLimits(1001, 10000))
                )
            )
        )

    override fun getBankAccount(): Single<BankAccount> =
        Single.just(
            BankAccount(
                listOf(
                    BankDetail("Bank Name", "LHV"),
                    BankDetail("Bank ID", "DE81 1234 5678 9101 1234 33", true),
                    BankDetail("Bank Code (SWIFT/BIC", "DEKTDE7GSSS", true),
                    BankDetail("Recipient", "Fred Wilson")
                )
            )
        )

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> = Single.just(
        listOf(
            FiatValue.fromMinor(currency, 100000),
            FiatValue.fromMinor(currency, 5000),
            FiatValue.fromMinor(currency, 1000),
            FiatValue.fromMinor(currency, 500)
        )
    )

    override fun isEligibleForSimpleBuy(currency: String): Single<SimpleBuyEligibility> =
        Single.just(SimpleBuyEligibility(true))

    override fun getBalanceForAsset(
        crypto: CryptoCurrency
    ): Single<CryptoValue> =
        nabuToken.fetchNabuToken()
            .flatMap {
                when (crypto) {
                    CryptoCurrency.BTC -> Single.just(CryptoValue.bitcoinFromSatoshis(726800000))
                    CryptoCurrency.ETHER -> Single.just(CryptoValue.ZeroEth)
                    CryptoCurrency.BCH -> Single.just(CryptoValue.ZeroBch)
                    CryptoCurrency.XLM -> Single.just(CryptoValue.ZeroXlm)
                    CryptoCurrency.PAX -> Single.just(CryptoValue.usdPaxFromMajor(2785.toBigDecimal()))
                    CryptoCurrency.STX -> Single.just(CryptoValue.ZeroStx)
                }
            }

    override fun getBuyOrderStatus(orderId: String): Single<BuyOrderStatus> {
        return Single.just(
            BuyOrderStatus(
                status = OrderStatus.AWAITING_FUNDS
            )
        )
    }

    override fun deleteBuyOrder(orderId: String): Completable {
        return Completable.complete()
    }
}

class LiveCustodialWalletManager(
    private val nabuToken: NabuToken,
    private val nabuService: NabuService,
    private val nabuDataManager: NabuDataManager
) : CustodialWalletManager {

    override fun getBankAccount(): Single<BankAccount> {
        TODO("not implemented")
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
                    BuyLimits(responsePair.buyMin, responsePair.buyMax)
                )
            })
        }

    override fun getBalanceForAsset(crypto: CryptoCurrency): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> {
        TODO("not implemented")
    }

    override fun isEligibleForSimpleBuy(currency: String): Single<SimpleBuyEligibility> =
        nabuService.isEligibleForSimpleBuy(currency).onErrorReturn { SimpleBuyEligibility(false) }

    override fun getBuyOrderStatus(orderId: String): Single<BuyOrderStatus> {
        TODO("not implemented")
    }

    override fun deleteBuyOrder(orderId: String): Completable {
        TODO("not implemented")
    }
}

data class SimpleBuyPairs(val pairs: List<SimpleBuyPair>)

data class SimpleBuyPair(private val pair: String, val buyLimits: BuyLimits) {
    val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.values().first { it.symbol == pair.split("-")[0] }
    val fiatCurrency: String = pair.split("-")[1]
}

data class BuyLimits(private val min: Long, private val max: Long) {
    fun minLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, min)
    fun maxLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, max)
}