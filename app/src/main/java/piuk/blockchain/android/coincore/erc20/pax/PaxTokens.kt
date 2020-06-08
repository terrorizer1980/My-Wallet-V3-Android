package piuk.blockchain.android.coincore.erc20.pax

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

internal class PaxTokens(
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
    CryptoCurrency.PAX,
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
        Single.just(listOf(getNonCustodialPaxAccount()))

    private fun getNonCustodialPaxAccount(): CryptoSingleAccount {
        val paxAddress = erc20Account.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No pax wallet found")

        val label = stringUtils.getString(R.string.pax_default_account_label_1)

        return PaxCryptoWalletAccount(
            label, paxAddress, erc20Account, exchangeRates)
    }
}
