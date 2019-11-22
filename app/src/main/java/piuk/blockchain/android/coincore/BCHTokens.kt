package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class BCHTokens(
    private val bchDataManager: BchDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs
) : BitcoinLikeTokens() {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun defaultAccount(): Single<AccountReference> {
        TODO("not implemented")
    }

    override fun totalBalance(filter: BalanceFilter): Single<CryptoValue> =
        updater()
            .toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getWalletBalance() }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = accountReference(account)

        return updater()
            .toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getAddressBalance(ref.xpub) }
    }

    override fun doUpdateBalances(): Completable = bchDataManager.updateAllBalances()

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, period)
}
