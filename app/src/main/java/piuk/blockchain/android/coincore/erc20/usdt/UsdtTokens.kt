package piuk.blockchain.android.coincore.erc20.usdt

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.erc20.Erc20TokensBase
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal class UsdtTokens(
    private val erc20Account: Erc20Account,
    private val stringUtils: StringUtils,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : Erc20TokensBase(
    CryptoCurrency.USDT,
    erc20Account,
    custodialManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    crashLogger,
    rxBus
) {
    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.just(listOf(getNonCustodialUsdtAccount()))

    private fun getNonCustodialUsdtAccount(): CryptoSingleAccount {
        val usdtAddress = erc20Account.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No USDT wallet found")

        val label = stringUtils.getString(R.string.usdt_default_account_label)

        return UsdtCryptoWalletAccount(label, usdtAddress, erc20Account, exchangeRates)
    }
}
