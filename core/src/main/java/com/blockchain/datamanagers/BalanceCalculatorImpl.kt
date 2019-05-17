package com.blockchain.datamanagers

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

class BalanceCalculatorImpl(val ethDataManager: EthDataManager) : BalanceCalculator {
    // TODO: Add the other currencies
    override fun balance(cryptoCurrency: CryptoCurrency): Observable<CryptoValue> =
        when (cryptoCurrency) {
            CryptoCurrency.ETHER -> ethDataManager.getEthResponseModel()?.let {
                Observable.just(CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()))
            } ?: ethDataManager.fetchEthAddress().map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            else -> Observable.empty()
        }
}