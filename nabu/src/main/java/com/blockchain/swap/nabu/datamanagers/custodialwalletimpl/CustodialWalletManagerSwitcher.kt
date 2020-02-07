package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderCreation
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

class CustodialWalletManagerSwitcher(
    private val mockCustodialWalletManager: StubCustodialWalletManager,
    private val liveCustodialWalletManager: LiveCustodialWalletManager
) : CustodialWalletManager {

    override fun getBalanceForAsset(crypto: CryptoCurrency): Single<CryptoValue> =
        mockCustodialWalletManager.getBalanceForAsset(crypto)

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs> =
        liveCustodialWalletManager.getBuyLimitsAndSupportedCryptoCurrencies(nabuOfflineTokenResponse, currency)

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        liveCustodialWalletManager.getQuote(action, crypto, amount)

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String
    ): Single<OrderCreation> =
        liveCustodialWalletManager.createOrder(
            cryptoCurrency,
            amount,
            action
        )

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> =
        liveCustodialWalletManager.getPredefinedAmounts(currency)

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
        mockCustodialWalletManager.getBankAccountDetails(currency)

    override fun isEligibleForSimpleBuy(): Single<Boolean> =
        mockCustodialWalletManager.isEligibleForSimpleBuy()

    override fun isCurrencySupportedForSimpleBuy(currency: String): Single<Boolean> =
        liveCustodialWalletManager.isCurrencySupportedForSimpleBuy(currency)

    override fun getOutstandingBuyOrders(): Single<BuyOrderList> =
        mockCustodialWalletManager.getOutstandingBuyOrders()

    override fun getBuyOrder(orderId: String): Maybe<BuyOrder> =
        mockCustodialWalletManager.getBuyOrder(orderId)

    override fun deleteBuyOrder(orderId: String): Completable =
        mockCustodialWalletManager.deleteBuyOrder(orderId)

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        mockCustodialWalletManager.transferFundsToWallet(amount, walletAddress)
}
