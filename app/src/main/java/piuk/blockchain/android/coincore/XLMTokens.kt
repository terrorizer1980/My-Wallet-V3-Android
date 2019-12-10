package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.lang.IllegalArgumentException

class XLMTokens(
    private val xlmDataManager: XlmDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs
) : AssetTokens {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.XLM

    override fun defaultAccount(): Single<AccountReference> {
        TODO("not implemented")
    }

    override fun totalBalance(filter: BalanceFilter): Single<CryptoValue> =
        xlmDataManager.getBalance()

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Xlm
            ?: throw IllegalArgumentException("Not an XLM Account Ref")
        return xlmDataManager.getBalance(ref)
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.XLM, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.XLM, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.XLM, currencyPrefs.selectedFiatCurrency, period)
}
