package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.CardToBeActivated
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.PartnerCredentials
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import om.blockchain.swap.nabu.BuildConfig

class CustodialWalletManagerSwitcher(
    private val mockCustodialWalletManager: StubCustodialWalletManager,
    private val liveCustodialWalletManager: LiveCustodialWalletManager
) : CustodialWalletManager {

    @Suppress("ConstantConditionIf")
    private val proxy: CustodialWalletManager by lazy {
        if (BuildConfig.USE_MOCK_SIMPLE_BUY_BACKEND)
            mockCustodialWalletManager
        else
            liveCustodialWalletManager
    }

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        fiatCurrency: String
    ): Single<SimpleBuyPairs> =
        proxy.getBuyLimitsAndSupportedCryptoCurrencies(nabuOfflineTokenResponse, fiatCurrency)

    override fun getSupportedFiatCurrencies(nabuOfflineTokenResponse: NabuOfflineTokenResponse): Single<List<String>> =
        proxy.getSupportedFiatCurrencies(nabuOfflineTokenResponse)

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        proxy.getQuote(action, crypto, amount)

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String,
        paymentMethodId: String?,
        stateAction: String?
    ): Single<BuyOrder> =
        proxy.createOrder(
            cryptoCurrency,
            amount,
            action,
            paymentMethodId,
            stateAction
        )

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> =
        proxy.getPredefinedAmounts(currency)

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
        proxy.getBankAccountDetails(currency)

    override fun isEligibleForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        proxy.isEligibleForSimpleBuy(fiatCurrency)

    override fun getBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        proxy.getBalanceForAsset(crypto)

    override fun isCurrencySupportedForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        proxy.isCurrencySupportedForSimpleBuy(fiatCurrency)

    override fun getOutstandingBuyOrders(crypto: CryptoCurrency): Single<BuyOrderList> =
        proxy.getOutstandingBuyOrders(crypto)

    override fun getAllOutstandingBuyOrders(): Single<BuyOrderList> =
        proxy.getAllOutstandingBuyOrders()

    override fun getAllBuyOrdersFor(crypto: CryptoCurrency): Single<BuyOrderList> =
        proxy.getAllBuyOrdersFor(crypto)

    override fun getBuyOrder(orderId: String): Single<BuyOrder> =
        proxy.getBuyOrder(orderId)

    override fun deleteBuyOrder(orderId: String): Completable =
        proxy.deleteBuyOrder(orderId)

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        proxy.transferFundsToWallet(amount, walletAddress)

    override fun cancelAllPendingBuys(): Completable =
        proxy.cancelAllPendingBuys()

    override fun fetchSuggestedPaymentMethod(fiatCurrency: String, isTier2Approved: Boolean):
            Single<List<PaymentMethod>> =
        proxy.fetchSuggestedPaymentMethod(fiatCurrency, isTier2Approved)

    override fun addNewCard(fiatCurrency: String, billingAddress: BillingAddress): Single<CardToBeActivated> =
        proxy.addNewCard(fiatCurrency, billingAddress)

    override fun activateCard(cardId: String, attributes: CardPartnerAttributes): Single<PartnerCredentials> =
        proxy.activateCard(cardId, attributes)

    override fun getCardDetails(cardId: String): Single<PaymentMethod.Card> =
        proxy.getCardDetails(cardId)

    override fun confirmOrder(orderId: String, attributes: CardPartnerAttributes?): Single<BuyOrder> =
        proxy.confirmOrder(orderId, attributes)
}
