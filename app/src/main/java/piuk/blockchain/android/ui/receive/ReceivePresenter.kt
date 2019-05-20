package piuk.blockchain.android.ui.receive

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.isValidXlmQr
import com.blockchain.sunriver.toUri
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.format
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.uri.BitcoinURI
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.currency.toSafeLong
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigInteger
import java.util.Locale

@Suppress("MemberVisibilityCanPrivate")
class ReceivePresenter(
    private val prefs: PersistentPrefs,
    private val qrCodeDataManager: QrCodeDataManager,
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val ethDataStore: EthDataStore,
    private val bchDataManager: BchDataManager,
    private val xlmDataManager: XlmDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val currencyState: CurrencyState,
    private val fiatExchangeRates: FiatExchangeRates
) : BasePresenter<ReceiveView>() {

    @VisibleForTesting
    internal var selectedAddress: String? = null
    @VisibleForTesting
    internal var selectedAccount: Account? = null
    @VisibleForTesting
    internal var selectedBchAccount: GenericMetadataAccount? = null

    fun getMaxCryptoDecimalLength() = currencyState.cryptoCurrency.dp

    fun getCryptoUnit() = currencyState.cryptoCurrency.symbol
    fun getFiatUnit() = fiatExchangeRates.fiatUnit

    override fun onViewReady() {
        if (environmentSettings.environment == Environment.TESTNET) {
            currencyState.cryptoCurrency = CryptoCurrency.BTC
            view.disableCurrencyHeader()
        }
    }

    internal fun onResume(defaultAccountPosition: Int) {
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> onSelectDefault(defaultAccountPosition)
            CryptoCurrency.ETHER -> onEthSelected()
            CryptoCurrency.BCH -> onSelectBchDefault()
            CryptoCurrency.XLM -> onXlmSelected()
            CryptoCurrency.PAX -> onPaxSelected()
        }
    }

    internal fun isValidAmount(btcAmount: String) = btcAmount.toSafeLong(Locale.getDefault()) > 0

    internal fun shouldShowDropdown() =
        walletAccountHelper.hasMultipleEntries(currencyState.cryptoCurrency)

    internal fun onLegacyAddressSelected(legacyAddress: LegacyAddress) {
        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view.showWatchOnlyWarning()
        }

        selectedAccount = null
        selectedBchAccount = null
        view.updateReceiveLabel(
            if (!legacyAddress.label.isNullOrEmpty()) {
                legacyAddress.label
            } else {
                legacyAddress.address
            }
        )

        legacyAddress.address.let {
            selectedAddress = it
            view.updateReceiveAddress(it)
            generateQrCode(getBitcoinUri(it, view.getBtcAmount()))
        }
    }

    internal fun onLegacyBchAddressSelected(legacyAddress: LegacyAddress) {
        // Here we are assuming that the legacy address is in Base58. This may change in the future
        // if we decide to allow importing BECH32 paper wallets.
        val address = Address.fromBase58(
            environmentSettings.bitcoinCashNetworkParameters,
            legacyAddress.address
        )
        val bech32 = address.toCashAddress()
        val bech32Display = bech32.removeBchUri()

        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view.showWatchOnlyWarning()
        }

        selectedAccount = null
        selectedBchAccount = null
        view.updateReceiveLabel(
            if (!legacyAddress.label.isNullOrEmpty()) {
                legacyAddress.label
            } else {
                bech32Display
            }
        )

        selectedAddress = bech32
        view.updateReceiveAddress(bech32Display)
        generateQrCode(bech32)
    }

    @SuppressLint("CheckResult")
    internal fun onAccountBtcSelected(account: Account) {
        currencyState.cryptoCurrency = CryptoCurrency.BTC
        view.setSelectedCurrency(currencyState.cryptoCurrency)
        selectedAccount = account
        selectedBchAccount = null
        view.updateReceiveLabel(account.label)

        payloadDataManager.updateAllTransactions()
            .doOnSubscribe { view.showQrLoading() }
            .onErrorComplete()
            .andThen(payloadDataManager.getNextReceiveAddress(account))
            .addToCompositeDisposable(this)
            .doOnNext {
                selectedAddress = it
                view.updateReceiveAddress(it)
                generateQrCode(getBitcoinUri(it, view.getBtcAmount()))
            }
            .doOnError { Timber.e(it) }
            .subscribe(
                { /* No-op */ },
                { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    internal fun onEthSelected() {
        currencyState.cryptoCurrency = CryptoCurrency.ETHER
        compositeDisposable.clear()
        view.setSelectedCurrency(CryptoCurrency.ETHER)
        selectedAccount = null
        selectedBchAccount = null

        lookupEthAccountAndUpdateView()
    }

    internal fun onPaxSelected() {
        currencyState.cryptoCurrency = CryptoCurrency.PAX
        compositeDisposable.clear()
        view.setSelectedCurrency(CryptoCurrency.PAX)
        selectedAccount = null
        selectedBchAccount = null

        lookupEthAccountAndUpdateView()
    }

    private fun lookupEthAccountAndUpdateView() {
        // This can be null at this stage for some reason - TODO investigate thoroughly
        val account: String? = ethDataStore.ethAddressResponse?.getAddressResponse()?.account
        if (account != null) {
            account.let {
                selectedAddress = it
                view.updateReceiveAddress(it)
                generateQrCode(it)
            }
        } else {
            view.finishPage()
        }
    }

    @SuppressLint("CheckResult")
    internal fun onXlmSelected() {
        currencyState.cryptoCurrency = CryptoCurrency.XLM
        compositeDisposable.clear()
        view.setSelectedCurrency(CryptoCurrency.XLM)
        selectedAccount = null
        selectedBchAccount = null
        xlmDataManager.defaultAccount()
            .addToCompositeDisposable(this)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { account ->
                    account.let {
                        selectedAddress = it.toUri()
                        view.updateReceiveAddress(it.accountId)
                        generateQrCode(it.toUri())
                    }
                },
                onError = {
                    view.finishPage()
                })
    }

    internal fun onSelectBchDefault() {
        compositeDisposable.clear()
        onBchAccountSelected(bchDataManager.getDefaultGenericMetadataAccount()!!)
    }

    @SuppressLint("CheckResult")
    internal fun onBchAccountSelected(account: GenericMetadataAccount) {
        currencyState.cryptoCurrency = CryptoCurrency.BCH
        view.setSelectedCurrency(CryptoCurrency.BCH)
        selectedAccount = null
        selectedBchAccount = account
        view.updateReceiveLabel(account.label)
        val position =
            bchDataManager.getAccountMetadataList().indexOfFirst { it.xpub == account.xpub }

        bchDataManager.updateAllBalances()
            .doOnSubscribe { view.showQrLoading() }
            .andThen(
                bchDataManager.getWalletTransactions(50, 0)
                    .onErrorReturn { emptyList() }
            )
            .flatMap { bchDataManager.getNextReceiveAddress(position) }
            .addToCompositeDisposable(this)
            .doOnNext {
                val address =
                    Address.fromBase58(environmentSettings.bitcoinCashNetworkParameters, it)
                val bech32 = address.toCashAddress()
                selectedAddress = bech32
                view.updateReceiveAddress(bech32.removeBchUri())
                generateQrCode(bech32)
            }
            .doOnError { Timber.e(it) }
            .subscribe(
                { /* No-op */ },
                { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) }
            )
    }

    internal fun onSelectDefault(defaultAccountPosition: Int) {
        compositeDisposable.clear()
        onAccountBtcSelected(
            if (defaultAccountPosition > -1) {
                payloadDataManager.getAccount(defaultAccountPosition)
            } else {
                payloadDataManager.defaultAccount
            }
        )
    }

    internal fun onBitcoinAmountChanged(amount: String) {
        val amountBigInt = amount.toSafeLong(Locale.getDefault())

        if (isValidAmount(amountBigInt)) {
            view.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
        }

        generateQrCode(getBitcoinUri(selectedAddress!!, amount))
    }

    private fun isValidAmount(amount: Long): Boolean {
        return BigInteger.valueOf(amount).compareTo(BigInteger.valueOf(2_100_000_000_000_000L)) == 1
    }

    internal fun getSelectedAccountPosition(): Int {
        return if (currencyState.cryptoCurrency == CryptoCurrency.ETHER) {
            -1
        } else {
            val position = payloadDataManager.accounts.asIterable()
                .indexOfFirst { it.xpub == selectedAccount?.xpub }
            payloadDataManager.getPositionOfAccountInActiveList(
                if (position > -1) position else payloadDataManager.defaultAccountIndex
            )
        }
    }

    internal fun setWarnWatchOnlySpend(warn: Boolean) {
        prefs.setValue(KEY_WARN_WATCH_ONLY_SPEND, warn)
    }

    internal fun onShowBottomShareSheetSelected() {
        selectedAddress?.let {
            when {
                FormatsUtil.isValidBitcoinAddress(it) ->
                    view.showShareBottomSheet(getBitcoinUri(it, view.getBtcAmount()))
                FormatsUtil.isValidEthereumAddress(it) ||
                FormatsUtil.isValidBitcoinCashAddress(environmentSettings.bitcoinCashNetworkParameters, it) ||
                it.isValidXlmQr() ->
                    view.showShareBottomSheet(it)
                else -> throw IllegalStateException("Unknown address format $selectedAddress")
            }
        }
    }

    internal fun updateFiatTextField(bitcoin: String) {
        view.updateFiatTextField(
            currencyState.cryptoCurrency.withMajorValueOrZero(bitcoin)
                .toFiat(fiatExchangeRates)
                .toStringWithoutSymbol()
        )
    }

    internal fun updateBtcTextField(fiat: String) {
        view.updateBtcTextField(
            FiatValue.fromMajorOrZero(fiatExchangeRates.fiatUnit, fiat)
                .toCrypto(fiatExchangeRates, currencyState.cryptoCurrency)
                .format()
        )
    }

    private fun getBitcoinUri(address: String, amount: String): String {
        require(FormatsUtil.isValidBitcoinAddress(address)) {
            "$address is not a valid Bitcoin address"
        }

        val amountLong = amount.toSafeLong(Locale.getDefault())

        return if (amountLong > 0L) {
            BitcoinURI.convertToBitcoinURI(
                Address.fromBase58(environmentSettings.bitcoinNetworkParameters, address),
                Coin.valueOf(amountLong),
                "",
                ""
            )
        } else {
            "bitcoin:$address"
        }
    }

    @SuppressLint("CheckResult")
    private fun generateQrCode(uri: String) {
        view.showQrLoading()
        compositeDisposable.clear()
        qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
            .addToCompositeDisposable(this)
            .subscribe(
                { view.showQrCode(it) },
                { view.showQrCode(null) })
    }

    private fun shouldWarnWatchOnly() = prefs.getValue(KEY_WARN_WATCH_ONLY_SPEND, true)

    private fun String.removeBchUri(): String = this.replace("bitcoincash:", "")

    companion object {

        @VisibleForTesting
        const val KEY_WARN_WATCH_ONLY_SPEND = "warn_watch_only_spend"
        private const val DIMENSION_QR_CODE = 600
    }
}
