package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.BuyOrderState
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderCreation
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.simplebuy.BankDetail
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.TimeUnit

// Provide mock data for development and testing etc
class StubCustodialWalletManager(
    private val nabuToken: NabuToken
) : CustodialWalletManager {

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs> =
        Single.just(
            SimpleBuyPairs(
                listOf(
                    SimpleBuyPair(
                        pair = "BTC-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "ETH-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BCH-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "XLM-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BTC-EUR",
                        buyLimits = BuyLimits(
                            1006,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "ETH-EUR",
                        buyLimits = BuyLimits(
                            1005,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BCH-EUR",
                        buyLimits = BuyLimits(
                            1001,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BTC-GBP",
                        buyLimits = BuyLimits(
                            1006,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "ETH-GBP",
                        buyLimits = BuyLimits(
                            1005,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BCH-GBP",
                        buyLimits = BuyLimits(
                            1001,
                            10000
                        )
                    )
                )
            )
        )

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
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

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        Single.just(Quote(date = Date()))

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String
    ): Single<OrderCreation> {
        TODO("not implemented")
    }

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> = Single.just(
        listOf(
            FiatValue.fromMinor(currency, 100000),
            FiatValue.fromMinor(currency, 5000),
            FiatValue.fromMinor(currency, 1000),
            FiatValue.fromMinor(currency, 500)

        ))

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

    override fun getBuyOrderStatus(orderId: String): Single<BuyOrderState> {
        return Single.just(
            BuyOrderState(
                status = OrderState.AWAITING_FUNDS
            )
        )
    }

    override fun deleteBuyOrder(orderId: String): Completable {
        return Completable.complete()
    }

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        Completable.timer(5, TimeUnit.SECONDS)
}
