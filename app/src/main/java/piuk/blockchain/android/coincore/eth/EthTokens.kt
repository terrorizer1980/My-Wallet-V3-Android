package piuk.blockchain.android.coincore.eth

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
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.impl.fetchLastPrice
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal class EthTokens(
    private val ethDataManager: EthDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val custodialWalletManager: CustodialWalletManager,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokensBase(labels, crashLogger, rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            stringUtils.getString(R.string.eth_default_account_label),
            stringUtils.getString(R.string.pax_default_account_label_1)
        )

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.just(
            listOf(
                EthCryptoAccountNonCustodial(
                    ethDataManager,
                    ethDataManager.getEthWallet()?.account ?: throw Exception("No ether wallet found"),
                    exchangeRates
                )
            )
        )

    override fun loadCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.just(
            listOf(
                EthCryptoAccountCustodial(
                    labels.getDefaultCustodialWalletLabel(asset),
                    custodialWalletManager,
                    exchangeRates
                )
            )
        )

    override fun defaultAccountRef(): Single<AccountReference> =
        Single.just(getDefaultEthAccountRef())

    override fun receiveAddress(): Single<String> =
        Single.just(getDefaultEthAccountRef().receiveAddress)

    private fun getDefaultEthAccountRef(): AccountReference =
        ethDataManager.getEthWallet()?.account?.toAccountReference()
            ?: throw Exception("No ether wallet found")

//    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
//        custodialWalletManager.getBalanceForAsset(CryptoCurrency.ETHER)
//
//    override fun noncustodialBalance(): Single<CryptoValue> =
//        ethDataManager.fetchEthAddress()
//            .singleOrError()
//            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
//
    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Ethereum
            ?: throw IllegalArgumentException("Not an XLM Account Ref")

        return ethDataManager.getBalance(ref.address)
            .map { CryptoValue.etherFromWei(it) }
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(
            CryptoCurrency.ETHER,
            currencyPrefs.selectedFiatCurrency,
            epochWhen
        )

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(
            CryptoCurrency.ETHER,
            currencyPrefs.selectedFiatCurrency,
            period
        )

    override fun onLogoutSignal(event: AuthEvent) {
        if (event != AuthEvent.LOGIN) {
            ethDataManager.clearEthAccountDetails()
        }
    }
}
