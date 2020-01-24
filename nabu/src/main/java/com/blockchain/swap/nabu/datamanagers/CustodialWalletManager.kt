package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.simplebuy.BankDetail
import com.blockchain.swap.nabu.models.simplebuy.BuyLimits
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPair
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPairs
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single

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
                    SimpleBuyPair(pair = "ETH-USD", buyLimits = BuyLimits(100, 1224558)),
                    SimpleBuyPair(pair = "BCH-USD", buyLimits = BuyLimits(100, 3024558)),
                    SimpleBuyPair(pair = "PAX-USD", buyLimits = BuyLimits(100, 7024558)),
                    SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000))
                )
            )
        )

    override fun getBankAccount(): Single<BankAccount> =
        Single.just(BankAccount(
            listOf(
                BankDetail("Bank Name", "LHV"),
                BankDetail("Bank ID", "DE81 1234 5678 9101 1234 33", true),
                BankDetail("Bank Code (SWIFT/BIC", "DEKTDE7GSSS", true),
                BankDetail("Recipient", "Fred Wilson")
            )
        ))

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> = Single.just(listOf(
        FiatValue.fromMinor(currency, 100000),
        FiatValue.fromMinor(currency, 5000),
        FiatValue.fromMinor(currency, 1000),
        FiatValue.fromMinor(currency, 500)))

    override fun getBalanceForAsset(
        crypto: CryptoCurrency
    ): Single<CryptoValue> =
        when (crypto) {
            CryptoCurrency.BTC -> Single.just(CryptoValue.ZeroBtc)
            CryptoCurrency.ETHER -> Single.just(CryptoValue.ZeroEth)
            CryptoCurrency.BCH -> Single.just(CryptoValue.ZeroBch)
            CryptoCurrency.XLM -> Single.just(CryptoValue.ZeroXlm)
            CryptoCurrency.PAX -> Single.just(CryptoValue.ZeroPax)
            CryptoCurrency.STX -> Single.just(CryptoValue.ZeroStx)
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
        }

    override fun getBalanceForAsset(crypto: CryptoCurrency): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> {
        TODO("not implemented")
    }
}