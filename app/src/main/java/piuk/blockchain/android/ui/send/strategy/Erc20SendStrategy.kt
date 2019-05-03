package piuk.blockchain.android.ui.send.strategy

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoCurrency

import info.blockchain.wallet.api.data.FeeOptions
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.androidcore.data.currency.CurrencyState

// TODO: AND-2003 - stubbed to resolve AND-2021 crashes. Implement me!
class Erc20SendStrategy(currencyState: CurrencyState)
    : SendStrategy<SendView>(currencyState) {

    override fun processURIScanAddress(address: String) { }

    override fun onContinueClicked() { }

    override fun onSpendMaxClicked() { }

    override fun onBroadcastReceived() { }

    override fun onResume() { }

    override fun onCurrencySelected() { currencyState.cryptoCurrency = CryptoCurrency.PAX }

    override fun handlePrivxScan(scanData: String?) { }

    override fun clearReceivingObject() { }

    override fun selectSendingAccount(account: JsonSerializableAccount?) { }

    override fun selectReceivingAccount(account: JsonSerializableAccount?) { }

    override fun selectDefaultOrFirstFundedSendingAccount() { }

    override fun submitPayment() { }

    override fun onAddressTextChange(address: String) { }

    override fun onCryptoTextChange(cryptoText: String) { }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) { }

    override fun onNoSecondPassword() { }

    override fun onSecondPasswordValidated(secondPassword: String) { }

    override fun getFeeOptions(): FeeOptions? = null

    override fun onViewReady() { }
}
