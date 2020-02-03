package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single

class CustodialWalletManagerImpl(
    private val mockCustodialWalletManager: MockCustodialWalletManager,
    private val liveCustodialWalletManager: LiveCustodialWalletManager
) : CustodialWalletManager {

    override fun getBalanceForAsset(crypto: CryptoCurrency): Single<CryptoValue> =
        mockCustodialWalletManager.getBalanceForAsset(crypto)

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs> =
        liveCustodialWalletManager.getBuyLimitsAndSupportedCryptoCurrencies(nabuOfflineTokenResponse, currency)

    override fun getBankAccount(): Single<BankAccount> = mockCustodialWalletManager.getBankAccount()

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> =
        liveCustodialWalletManager.getPredefinedAmounts(currency)

    override fun isEligibleForSimpleBuy(currency: String): Single<SimpleBuyEligibility> =
        liveCustodialWalletManager.isEligibleForSimpleBuy(currency)

    override fun getBuyOrderStatus(orderId: String): Single<BuyOrderStatus> =
        mockCustodialWalletManager.getBuyOrderStatus(orderId)

    override fun deleteBuyOrder(orderId: String): Completable =
        mockCustodialWalletManager.deleteBuyOrder(orderId)
}