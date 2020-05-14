package piuk.blockchain.android.coincore.bch

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.bitcoinj.core.Address
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.BitcoinLikeTokens
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.coincore.impl.toCryptoSingle
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

internal class BchTokens(
    private val bchDataManager: BchDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val custodialWalletManager: CustodialWalletManager,
    private val environmentSettings: EnvironmentConfig,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : BitcoinLikeTokens(labels, crashLogger, rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun initToken(): Completable =
        bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
            .then { updater() }
            .doOnError { Timber.e("Unable to init BCH, because: $it") }
            .onErrorComplete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            with(bchDataManager) {
                val result = mutableListOf<CryptoSingleAccount>()
                val defaultIndex = getDefaultAccountPosition()

                val accounts = getAccountMetadataList()
                accounts.forEachIndexed { i, a ->
                    result.add(
                        BchCryptoAccountNonCustodial(
                            a,
                            bchDataManager,
                            i == defaultIndex,
                            exchangeRates
                        )
                    )
                }
                result
            }
        }

    override fun loadCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            listOf(
                BchCryptoAccountCustodial(
                    labels.getDefaultCustodialWalletLabel(asset),
                    custodialWalletManager,
                    exchangeRates
                )
            )
        }

    override fun defaultAccountRef(): Single<AccountReference> =
        with(bchDataManager) {
            val a = getAccountMetadataList()[getDefaultAccountPosition()]
            Single.just(a.toAccountReference())
        }

    override fun receiveAddress(): Single<String> =
        bchDataManager.getNextReceiveAddress(
            bchDataManager.getAccountMetadataList().indexOfFirst {
                it.xpub == bchDataManager.getDefaultGenericMetadataAccount()!!.xpub
            }
        ).map {
            val address = Address.fromBase58(environmentSettings.bitcoinCashNetworkParameters, it)
            address.toCashAddress()
        }.singleOrError()

//    override fun balance(account: AccountReference): Single<CryptoValue> {
//        val ref = accountReference(account)
//        return updater().toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getAddressBalance(ref.xpub) }
//    }

    override fun doUpdateBalances(): Completable =
        bchDataManager.updateAllBalances()
            .doOnComplete { Timber.d("Got btc balance") }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, period)

    override fun onLogoutSignal(event: AuthEvent) {
        if (event != AuthEvent.LOGIN) {
            bchDataManager.clearBchAccountDetails()
        }
        super.onLogoutSignal(event)
    }
}
