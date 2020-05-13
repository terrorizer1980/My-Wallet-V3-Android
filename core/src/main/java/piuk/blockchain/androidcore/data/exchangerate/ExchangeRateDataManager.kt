package piuk.blockchain.androidcore.data.exchangerate

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import java.math.RoundingMode

/**
 * This data manager is responsible for storing and updating the latest exchange rates information
 * for all crypto currencies.
 * Historic prices for all crypto currencies can be queried from here.
 */
class ExchangeRateDataManager(
    private val exchangeRateDataStore: ExchangeRateDataStore,
    rxBus: RxBus
) {
    private val rxPinning = RxPinning(rxBus)

    fun updateTickers(): Completable =
        rxPinning.call { exchangeRateDataStore.updateExchangeRates() }
            .subscribeOn(Schedulers.io())

    fun getLastPrice(cryptoCurrency: CryptoCurrency, currencyName: String) =
        exchangeRateDataStore.getLastPrice(cryptoCurrency, currencyName)

    fun getLastPriceOfFiat(targetFiat: String, sourceFiat: String) =
        exchangeRateDataStore.getFiatLastPrice(targetFiat = targetFiat, sourceFiat = sourceFiat)

    fun getHistoricPrice(value: CryptoValue, fiat: String, timeInSeconds: Long): Single<FiatValue> =
        exchangeRateDataStore.getHistoricPrice(value.currency, fiat, timeInSeconds)
            .map { FiatValue.fromMajor(fiat, it * value.toBigDecimal()) }
            .subscribeOn(Schedulers.io())

    fun getHistoricPrice(currency: CryptoCurrency, fiat: String, timeInSeconds: Long): Single<FiatValue> =
        exchangeRateDataStore.getHistoricPrice(currency, fiat, timeInSeconds)
            .map { FiatValue.fromMajor(fiat, it) }
            .subscribeOn(Schedulers.io())

    fun getCurrencyLabels() = exchangeRateDataStore.getCurrencyLabels()
}

fun CryptoValue.toFiat(exchangeRateDataManager: ExchangeRateDataManager, fiatUnit: String) =
    FiatValue.fromMajor(
        fiatUnit,
        exchangeRateDataManager.getLastPrice(currency, fiatUnit).toBigDecimal() * toBigDecimal()
    )

fun FiatValue.toFiatWithCurrency(
    exchangeRateDataManager: ExchangeRateDataManager,
    targetCurrency: String
) = FiatValue.fromMajor(targetCurrency,
        exchangeRateDataManager.getLastPriceOfFiat(
            sourceFiat = this.currencyCode,
            targetFiat = targetCurrency
        ).toBigDecimal() * toBigDecimal()
    )

fun FiatValue.toCrypto(exchangeRateDataManager: ExchangeRateDataManager, cryptoCurrency: CryptoCurrency) =
    toCryptoOrNull(exchangeRateDataManager, cryptoCurrency) ?: CryptoValue.zero(cryptoCurrency)

fun FiatValue.toCryptoOrNull(exchangeRateDataManager: ExchangeRateDataManager, cryptoCurrency: CryptoCurrency) =
    if (isZero) {
        CryptoValue.zero(cryptoCurrency)
    } else {
        val rate = exchangeRateDataManager.getLastPrice(cryptoCurrency, this.currencyCode).toBigDecimal()
        if (rate.signum() == 0) {
            null
        } else {
            CryptoValue.fromMajor(
                cryptoCurrency,
                toBigDecimal().divide(rate, cryptoCurrency.dp, RoundingMode.HALF_UP)
            )
        }
    }
