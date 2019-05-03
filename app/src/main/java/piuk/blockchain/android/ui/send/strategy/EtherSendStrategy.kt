package piuk.blockchain.android.ui.send.strategy

import android.annotation.SuppressLint
import android.support.design.widget.Snackbar
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.currency.ETHDenomination
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.HashMap
import java.util.concurrent.TimeUnit

class EtherSendStrategy(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val stringUtils: StringUtils,
    private val sendDataManager: SendDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val privateKeyFactory: PrivateKeyFactory,
    private val environmentSettings: EnvironmentConfig,
    private val currencyFormatter: CurrencyFormatManager,
    private val exchangeRates: FiatExchangeRates,
    currencyState: CurrencyState,
    environmentConfig: EnvironmentConfig
) : SendStrategy<SendView>(currencyState) {

    override fun onCurrencySelected() {
        currencyState.cryptoCurrency = CryptoCurrency.ETHER
        onEtherChosen()
    }

    override fun selectSendingAccount(account: JsonSerializableAccount?) {
        throw IllegalArgumentException("Multiple accounts not supported for ETH")
    }

    override fun selectReceivingAccount(account: JsonSerializableAccount?) {
        throw IllegalArgumentException("Multiple accounts not supported for ETH")
    }

    private val pendingTransaction by unsafeLazy { PendingTransaction() }
    private val unspentApiResponsesBtc by unsafeLazy { HashMap<String, UnspentOutputs>() }
    private val unspentApiResponsesBch by unsafeLazy { HashMap<String, UnspentOutputs>() }
    private val networkParameters = environmentConfig.bitcoinNetworkParameters

    private var feeOptions: FeeOptions? = null
    private var textChangeSubject = PublishSubject.create<String>()
    private var absoluteSuggestedFee = BigInteger.ZERO
    private var maxAvailable = BigInteger.ZERO
    private var verifiedSecondPassword: String? = null

    /**
     * External changes.
     * Possible currency change, Account/address archive, Balance change
     */
    override fun onBroadcastReceived() {
        resetAccountList()
    }

    override fun onViewReady() {
        resetAccountList()
        setupTextChangeSubject()
    }

    override fun onResume() {
        onEtherChosen()
    }

    private fun onEtherChosen() {
        view.hideFeePriority()
        view.setFeePrioritySelection(0)
        view.disableFeeDropdown()
        view.setCryptoMaxLength(30)
        resetState()
    }

    private fun resetState() {
        compositeDisposable.clear()
        pendingTransaction.clear()
        absoluteSuggestedFee = BigInteger.ZERO

        view?.setSendButtonEnabled(true)
        view.clearFeeAmount()
        view.hideMaxAvailable()
        view.updateCryptoAmount(CryptoValue.zero(CryptoCurrency.ETHER))
        view.updateReceivingAddress("")
        resetAccountList()

        selectDefaultOrFirstFundedSendingAccount()
    }

    private fun resetAccountList() {
        val addressList = walletAccountHelper.getAccountItems(CryptoCurrency.ETHER)
        view.updateReceivingHintAndAccountDropDowns(CryptoCurrency.ETHER, addressList.size)
    }

    override fun processURIScanAddress(address: String) {
        pendingTransaction.receivingObject = null
        pendingTransaction.receivingAddress = address
    }

    @SuppressLint("CheckResult")
    override fun onContinueClicked() {
        view?.showProgressDialog(R.string.app_name)

        checkManualAddressInput()

        validateTransaction()
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate { view?.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .addToCompositeDisposable(this)
            .subscribe(
                { (validated, errorMessage) ->
                    when {
                    //  Checks if second pw needed then -> onNoSecondPassword()
                        validated -> view.showSecondPasswordDialog()
                        errorMessage == R.string.eth_support_contract_not_allowed -> view.showEthContractSnackbar()
                        else -> view.showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
                    }
                },
                {
                    view.showSnackbar(R.string.unexpected_error, Snackbar.LENGTH_LONG)
                    view.finishPage()
                }
            )
    }

    /**
     * Executes transaction
     */
    override fun submitPayment() {
        submitTransaction()
    }

    @SuppressLint("CheckResult")
    private fun submitTransaction() {
        createEthTransaction()
            .addToCompositeDisposable(this)
            .flatMap {
                if (payloadDataManager.isDoubleEncrypted) {
                    payloadDataManager.decryptHDWallet(networkParameters, verifiedSecondPassword)
                }

                val ecKey = EthereumAccount.deriveECKey(payloadDataManager.wallet!!.hdWallets[0].masterKey, 0)
                return@flatMap ethDataManager.signEthTransaction(it, ecKey)
            }
            .flatMap { ethDataManager.pushEthTx(it) }
            .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog(R.string.app_name) }
            .doOnError {
                view.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_INDEFINITE)
            }
            .doOnTerminate {
                view.dismissProgressDialog()
                view.dismissConfirmationDialog()
            }
            .subscribe(
                {
                    logPaymentSentEvent(true, CryptoCurrency.ETHER, pendingTransaction.bigIntAmount)

                    // handleSuccessfulPayment(...) clears PendingTransaction object
                    handleSuccessfulPayment(it)
                },
                {
                    Timber.e(it)
                    logPaymentSentEvent(false, CryptoCurrency.ETHER, pendingTransaction.bigIntAmount)
                    view.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_INDEFINITE)
                }
            )
    }

    private fun createEthTransaction(): Observable<RawTransaction> {
        val feeGwei = BigDecimal.valueOf(feeOptions!!.regularFee)
        val feeWei = Convert.toWei(feeGwei, Convert.Unit.GWEI)

        return ethDataManager.fetchEthAddress()
            .map { ethDataManager.getEthResponseModel()!!.getNonce() }
            .map {
                ethDataManager.createEthTransaction(
                    nonce = it,
                    to = pendingTransaction.receivingAddress,
                    gasPriceWei = feeWei.toBigInteger(),
                    gasLimitGwei = BigInteger.valueOf(feeOptions!!.gasLimit),
                    weiValue = pendingTransaction.bigIntAmount
                )
            }
    }

    private fun handleSuccessfulPayment(hash: String): String {
        view?.showTransactionSuccess(CryptoCurrency.ETHER)

        pendingTransaction.clear()
        unspentApiResponsesBtc.clear()
        unspentApiResponsesBch.clear()

        return hash
    }

    override fun onNoSecondPassword() {
        showPaymentReview()
    }

    override fun onSecondPasswordValidated(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        showPaymentReview()
    }

    private fun showPaymentReview() {
        view.showPaymentDetails(getConfirmationDetails(), null, null, false)
    }

    private fun checkManualAddressInput() {
        val address = view.getReceivingAddress()
        address?.let {
            // Only if valid address so we don't override with a label
            if (FormatsUtil.isValidEthereumAddress(address)) {
                pendingTransaction.receivingAddress = address
            }
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val pendingTransaction = pendingTransaction

        return PaymentConfirmationDetails().apply {
            fromLabel = pendingTransaction.sendingObject!!.label
            toLabel = pendingTransaction.displayableReceivingLabel!!.removeBchUri()

            cryptoUnit = CryptoCurrency.ETHER.symbol
            fiatUnit = exchangeRates.fiatUnit
            fiatSymbol = currencyFormatter.getFiatSymbol(currencyFormatter.fiatCountryCode, view.locale)

            // --------
            var ethAmount = Convert.fromWei(pendingTransaction.bigIntAmount.toString(), Convert.Unit.ETHER)
            var ethFee = Convert.fromWei(pendingTransaction.bigIntFee.toString(), Convert.Unit.ETHER)

            ethAmount = ethAmount.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
            ethFee = ethFee.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()

            val ethTotal = ethAmount.add(ethFee)

            cryptoAmount = ethAmount.toString()
            cryptoFee = ethFee.toString()
            cryptoTotal = ethTotal.toString()

            fiatFee = currencyFormatter.getFormattedFiatValueFromSelectedCoinValue(
                coinValue = ethFee,
                convertEthDenomination = ETHDenomination.ETH
            )
            fiatAmount = currencyFormatter.getFormattedFiatValueFromSelectedCoinValue(
                coinValue = ethAmount,
                convertEthDenomination = ETHDenomination.ETH
            )
            fiatTotal = currencyFormatter.getFormattedFiatValueFromSelectedCoinValue(
                coinValue = ethTotal,
                convertEthDenomination = ETHDenomination.ETH
            )
        }
    }

    override fun clearReceivingObject() {
        pendingTransaction.receivingObject = null
    }

    override fun selectDefaultOrFirstFundedSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultOrFirstFundedAccount()
        view.updateSendingAddress(accountItem.label ?: accountItem.address!!)
        pendingTransaction.sendingObject = accountItem
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    @SuppressLint("CheckResult")
    private fun getSuggestedFee() {
        val observable = feeDataManager.ethFeeOptions
                .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
                .doOnNext { dynamicFeeCache.ethFeeOptions = it }

        // ----------
        observable.addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    view.showSnackbar(R.string.confirm_payment_fee_sync_error, Snackbar.LENGTH_LONG)
                    view.finishPage()
                }
            )
    }

    override fun getFeeOptions(): FeeOptions? = dynamicFeeCache.btcFeeOptions

    /**
     * Update absolute fee with smallest denomination of crypto currency (satoshi, wei, etc)
     */
    private fun updateFee(fee: BigInteger) {
        absoluteSuggestedFee = fee

        val cryptoValue = CryptoValue(CryptoCurrency.ETHER, absoluteSuggestedFee)
        view.updateFeeAmount(cryptoValue, cryptoValue.toFiat(exchangeRates))
    }

    override fun onCryptoTextChange(cryptoText: String) {
        textChangeSubject.onNext(cryptoText)
    }

    override fun onAddressTextChange(address: String) {}

    /**
     * Calculate amounts on crypto text change
     */
    private fun setupTextChangeSubject() {
        textChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                calculateSpendableAmounts(spendAll = false, amountToSendText = it)
            }
            .emptySubscribe()
    }

    override fun onSpendMaxClicked() {
        calculateSpendableAmounts(spendAll = true, amountToSendText = null)
    }

    private fun calculateSpendableAmounts(spendAll: Boolean, amountToSendText: String?) {
        view.setSendButtonEnabled(true)
        view.hideMaxAvailable()
        view.clearWarning()

        // val feePerKb = getFeePerKbFromPriority(view.getFeePriority())
        // ^^ has side-effects, ie calls:
        getSuggestedFee()

        // ----------
        getAccountResponse(spendAll, amountToSendText)
    }

    @SuppressLint("CheckResult")
    private fun getAccountResponse(spendAll: Boolean, amountToSendText: String?) {
        view.showMaxAvailable()

        if (ethDataManager.getEthResponseModel() == null) {
            ethDataManager.fetchEthAddress()
                .addToCompositeDisposable(this)
                .doOnError { view.showSnackbar(R.string.api_fail, Snackbar.LENGTH_INDEFINITE) }
                .subscribe { calculateUnspent(it, spendAll, amountToSendText) }
        } else {
            ethDataManager.getEthResponseModel()?.let {
                calculateUnspent(it, spendAll, amountToSendText)
            }
        }
    }

    private fun calculateUnspent(combinedEthModel: CombinedEthModel, spendAll: Boolean, amountToSendText: String?) {

        val amountToSendSanitised = if (amountToSendText.isNullOrEmpty()) "0" else amountToSendText

        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

        updateFee(wei.toBigInteger())
        pendingTransaction.bigIntFee = wei.toBigInteger()

        val addressResponse = combinedEthModel.getAddressResponse()
        maxAvailable = addressResponse!!.balance.minus(wei.toBigInteger())
        maxAvailable = maxAvailable.max(BigInteger.ZERO)

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
        val cryptoValue = CryptoValue.etherFromMajor(availableEth ?: BigDecimal.ZERO)

        if (spendAll) {
            view?.updateCryptoAmount(cryptoValue)
            pendingTransaction.bigIntAmount = availableEth.toBigInteger()
        } else {
            pendingTransaction.bigIntAmount =
                currencyFormatter.getWeiFromText(
                    amountToSendSanitised,
                    getDefaultDecimalSeparator()
                )
        }

        // Format for display
        val number = currencyFormatter.getFormattedValueWithUnit(cryptoValue)
        view.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} $number")

        // No dust in Ethereum
        if (maxAvailable <= BigInteger.ZERO) {
            view.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.updateMaxAvailableColor(R.color.primary_blue_accent)
        }

        // Check if any pending ether txs exist and warn user
        isLastTxPending()
            .addToCompositeDisposable(this)
            .subscribe(
                {
                    /* No-op */
                },
                { Timber.e(it) }
            )
    }

    override fun handlePrivxScan(scanData: String?) {
        if (scanData == null) return

        val format = privateKeyFactory.getFormat(scanData)

        if (format == null) {
            view?.showSnackbar(R.string.privkey_error, Snackbar.LENGTH_LONG)
            return
        }

        when (format) {
            PrivateKeyFactory.BIP38 -> view?.showBIP38PassphrasePrompt(scanData) // BIP38 needs passphrase
            else -> spendFromWatchOnlyNonBIP38(format, scanData)
        }
    }

    private fun spendFromWatchOnlyNonBIP38(format: String, scanData: String) {
        try {
            val key = privateKeyFactory.getKey(format, scanData)
            val legacyAddress = pendingTransaction.sendingObject!!.accountObject as LegacyAddress
            setTempLegacyAddressPrivateKey(legacyAddress, key)
        } catch (e: Exception) {
            view?.showSnackbar(R.string.no_private_key, Snackbar.LENGTH_LONG)
            Timber.e(e)
        }
    }

    @SuppressLint("CheckResult")
    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        sendDataManager.getEcKeyFromBip38(
            pw,
            scanData,
            environmentSettings.bitcoinNetworkParameters
        ).addToCompositeDisposable(this)
            .subscribe(
                {
                    val legacyAddress =
                        pendingTransaction.sendingObject!!.accountObject as LegacyAddress
                    setTempLegacyAddressPrivateKey(legacyAddress, it)
                },
                { view?.showSnackbar(R.string.bip38_error, Snackbar.LENGTH_LONG) }
            )
    }

    private fun setTempLegacyAddressPrivateKey(legacyAddress: LegacyAddress, key: ECKey?) {
        if (key != null && key.hasPrivKey() && legacyAddress.address == key.toAddress(
                environmentSettings.bitcoinNetworkParameters
            ).toString()
        ) {
            // Create copy, otherwise pass by ref will override private key in wallet payload
            val tempLegacyAddress = LegacyAddress()
            tempLegacyAddress.setPrivateKeyFromBytes(key.privKeyBytes)
            tempLegacyAddress.address = key.toAddress(environmentSettings.bitcoinNetworkParameters).toString()
            tempLegacyAddress.label = legacyAddress.label
            tempLegacyAddress.tag = PendingTransaction.WATCH_ONLY_SPEND_TAG
            pendingTransaction.sendingObject!!.accountObject = tempLegacyAddress

            showPaymentReview()
        } else {
            view?.showSnackbar(R.string.invalid_private_key, Snackbar.LENGTH_LONG)
        }
    }

    private fun isValidAmount(bAmount: BigInteger?): Boolean {
        return (bAmount != null && bAmount >= BigInteger.ZERO)
    }

    private fun validateTransaction(): Observable<Pair<Boolean, Int>> {
        if (pendingTransaction.receivingAddress == null) {
            return Observable.just(Pair.of(false, R.string.eth_invalid_address))
        } else {
            return ethDataManager.getIfContract(pendingTransaction.receivingAddress)
                .map { isContract ->
                    var validated = true
                    var errorMessage = R.string.unexpected_error

                    // Validate not contract
                    if (isContract) {
                        errorMessage = R.string.eth_support_contract_not_allowed
                        validated = false
                    }
                    Pair.of(validated, errorMessage)
                }.map { errorPair ->
                    if (errorPair.left) {
                        var validated = true
                        var errorMessage = R.string.unexpected_error

                        // Validate address
                        if (pendingTransaction.receivingAddress == null ||
                            !FormatsUtil.isValidEthereumAddress(
                                pendingTransaction.receivingAddress
                            )
                        ) {
                            errorMessage = R.string.eth_invalid_address
                            validated = false
                        }

                        // Validate amount
                        if (!isValidAmount(pendingTransaction.bigIntAmount) ||
                            pendingTransaction.bigIntAmount <= BigInteger.ZERO
                        ) {
                            errorMessage = R.string.invalid_amount
                            validated = false
                        }

                        // Validate sufficient funds
                        if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
                            errorMessage = R.string.insufficient_funds
                            validated = false
                        }
                        Pair.of(validated, errorMessage)
                    } else {
                        errorPair
                    }
                }.flatMap { errorPair ->
                    if (errorPair.left) {
                        // Validate address does not have unconfirmed funds
                        isLastTxPending()
                    } else {
                        Observable.just(errorPair)
                    }
                }
        }
    }

    private fun isLastTxPending() =
        ethDataManager.isLastTxPending()
            .observeOn(AndroidSchedulers.mainThread())
            .map { hasUnconfirmed: Boolean ->

                if (hasUnconfirmed) {
                    view?.disableInput()
                    view?.updateMaxAvailable(stringUtils.getString(R.string.eth_unconfirmed_wait))
                    view?.updateMaxAvailableColor(R.color.product_red_medium)
                } else {
                    view.enableInput()
                }

                val errorMessage = R.string.eth_unconfirmed_wait
                Pair.of(!hasUnconfirmed, errorMessage)
            }
}
