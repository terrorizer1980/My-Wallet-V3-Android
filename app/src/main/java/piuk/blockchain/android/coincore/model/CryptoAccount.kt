package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.AvailableActions


// Build out a 'standard' account interface; make compatible with ItemAccount and AccountRef and then purge those
// two classes. There will be - going forwards - only one interface to an account and only one account  chooser.

typealias CryptoAccountsList = List<CryptoAccount>

abstract class CryptoAccount {
    abstract val label: String

    abstract val cryptoCurrency: CryptoCurrency

    abstract val receiveAddress: Single<String>

    abstract val balance: Single<CryptoValue>

    abstract val activity: Single<ActivitySummaryList>

    abstract val actions: AvailableActions
    abstract val hasTransactions: Boolean
    abstract val isFunded: Boolean
}

abstract class CryptoAccountGroup {

}