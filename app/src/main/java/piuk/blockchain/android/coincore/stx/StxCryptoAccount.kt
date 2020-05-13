package piuk.blockchain.android.coincore.stx

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountCustodialBase
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class StxCryptoAccountCustodial(
    override val label: String,
    override val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountCustodialBase() {
    override val cryptoCurrencies = setOf(CryptoCurrency.STX)
}

class StxCryptoAccountNonCustodial(
    override val label: String,
    private val address: String,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase() {

    override val cryptoCurrencies = setOf(CryptoCurrency.PAX)

    override val isDefault: Boolean = true // Only one account ever, so always default

    override val balance: Single<CryptoValue>
        get() = TODO("not implemented")

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())
}