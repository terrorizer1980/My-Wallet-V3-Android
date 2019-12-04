package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class PAXTokens(
    private val erc20Account: Erc20Account,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs
) : AssetTokens {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.PAX

    override fun defaultAccount(): Single<AccountReference> {
        TODO("not implemented")
    }

    override fun totalBalance(filter: BalanceFilter): Single<CryptoValue> =
        erc20Account.getBalance()
            .map { CryptoValue.usdPaxFromMinor(it) }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.PAX, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.PAX, currencyPrefs.selectedFiatCurrency, epochWhen)

    // PAX does not support historic prices, so return an empty list
    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())
}
