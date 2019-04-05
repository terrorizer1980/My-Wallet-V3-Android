package piuk.blockchain.android.ui.send.send2

import android.content.Intent

import com.blockchain.transactions.Memo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.SendPresenterStrategy

// TODO: AND-2003 - stubbed to resolve AND-2021 crashes. Implement me!
class Erc20SendStrategy : SendPresenterStrategy<SendView>() {
    override fun onContinueClicked() { }

    override fun onSpendMaxClicked() { }

    override fun onBroadcastReceived() { }

    override fun onResume() { }

    override fun onCurrencySelected(currency: CryptoCurrency) { }

    override fun handleURIScan(untrimmedscanData: String?) { }

    override fun handlePrivxScan(scanData: String?) { }

    override fun clearReceivingObject() { }

    override fun selectSendingAccount(data: Intent?, currency: CryptoCurrency) { }

    override fun selectReceivingAccount(data: Intent?, currency: CryptoCurrency) { }

    override fun selectDefaultOrFirstFundedSendingAccount() { }

    override fun submitPayment() { }

    override fun shouldShowAdvancedFeeWarning(): Boolean = false

    override fun onAddressTextChange(address: String) { }

    override fun onMemoChange(memo: Memo) { }

    override fun onCryptoTextChange(cryptoText: String) { }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) { }

    override fun setWarnWatchOnlySpend(warn: Boolean) { }

    override fun onNoSecondPassword() { }

    override fun onSecondPasswordValidated(validateSecondPassword: String) { }

    override fun disableAdvancedFeeWarning() { }

    override fun getBitcoinFeeOptions(): FeeOptions? = null

    override fun onViewReady() { }
}
