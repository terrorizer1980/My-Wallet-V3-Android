package piuk.blockchain.android.coincore.btc

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.impl.BitcoinLikeTokens
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.impl.toCryptoSingle
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal class BtcTokens(
    private val payloadDataManager: PayloadDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val payloadManager: PayloadManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : BitcoinLikeTokens(labels, crashLogger, rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun initToken(): Completable =
        updater()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            with(payloadDataManager) {
                val result = mutableListOf<CryptoSingleAccount>()
                val defaultIndex = defaultAccountIndex
                accounts.forEachIndexed { i, a ->
                    result.add(
                        BtcCryptoAccountNonCustodial(
                            a,
                            payloadManager,
                            payloadDataManager,
                            i == defaultIndex,
                            exchangeRates,
                            txActivityCache
                        )
                    )
                }

                legacyAddresses.forEach { a ->
                    result.add(
                        BtcCryptoAccountNonCustodial(
                            a,
                            payloadManager,
                            payloadDataManager,
                            exchangeRates,
                            txActivityCache
                        )
                    )
                }
                result
            }
        }

    override fun loadCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            listOf(
                BtcCryptoAccountCustodial(
                    labels.getDefaultCustodialWalletLabel(asset),
                    custodialWalletManager,
                    exchangeRates,
                    txActivityCache
                )
            )
        }

    override fun defaultAccountRef(): Single<AccountReference> =
        Single.just(payloadDataManager.defaultAccount.toAccountReference())

    override fun receiveAddress(): Single<String> =
        payloadDataManager.getNextReceiveAddress(payloadDataManager.getAccount(payloadDataManager.defaultAccountIndex))
            .singleOrError()

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.BTC)

    override fun noncustodialBalance(): Single<CryptoValue> =
        updater().toCryptoSingle(CryptoCurrency.BTC) { payloadManager.walletBalance }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = accountReference(account)
        return updater()
            .toCryptoSingle(CryptoCurrency.BTC) { payloadManager.getAddressBalance(ref.xpub) }
    }

    override fun doUpdateBalances(): Completable =
        payloadDataManager.updateAllBalances()

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.BTC, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.BTC, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.BTC, currencyPrefs.selectedFiatCurrency, period)
}
