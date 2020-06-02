package piuk.blockchain.android.coincore.alg

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import org.apache.commons.lang3.NotImplementedException
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class AlgCryptoWalletAccount(
    override val label: String,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.ALG)

    override val balance: Single<CryptoValue>
        get() = Single.just(CryptoValue.ZeroAlg)

    override val receiveAddress: Single<String>
        get() = Single.error(NotImplementedException("Need implementation of ALG receive"))

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions
        get() = emptySet()

    override val isFunded: Boolean
        get() = false
}