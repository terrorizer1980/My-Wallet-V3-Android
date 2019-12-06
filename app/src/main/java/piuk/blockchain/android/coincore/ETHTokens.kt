package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.lang.IllegalArgumentException

class ETHTokens(
    private val ethDataManager: EthDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs
) : AssetTokens {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun defaultAccount(): Single<AccountReference> {
        TODO("not implemented")
    }

    override fun totalBalance(filter: BalanceFilter): Single<CryptoValue> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Ethereum
            ?: throw IllegalArgumentException("Not an XLM Account Ref")

        return ethDataManager.getBalance(ref.address)
            .map { CryptoValue.etherFromWei(it) }
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency, period)
}
