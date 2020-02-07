package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.datamanagers.BuyOrderState
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderCreation
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber

class CustodialWalletManagerSwitcher(
    private val mockCustodialWalletManager: StubCustodialWalletManager,
    private val liveCustodialWalletManager: LiveCustodialWalletManager
) : CustodialWalletManager {

    override fun getBalanceForAsset(crypto: CryptoCurrency): Single<CryptoValue> =
        liveCustodialWalletManager.getBalanceForAsset(crypto)

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

    override fun isEligibleForSimpleBuy(currency: String): Single<SimpleBuyEligibility> =
        liveCustodialWalletManager.isEligibleForSimpleBuy(currency)

    override fun getBuyOrderStatus(orderId: String): Single<BuyOrderState> =
        mockCustodialWalletManager.getBuyOrderStatus(orderId)

    override fun deleteBuyOrder(orderId: String): Completable =
        mockCustodialWalletManager.deleteBuyOrder(orderId)

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        mockCustodialWalletManager.transferFundsToWallet(amount, walletAddress)
}
