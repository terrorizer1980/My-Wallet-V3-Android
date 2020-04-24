package piuk.blockchain.android.ui.activity.detail

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem

class ActivityDetailsInteractor(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs
) {

    fun getCompositeActivityDetails(cryptoCurrency: CryptoCurrency,
                                    txHash: String): Single<ActivityDetailsComposite> {
        val item = coincore[cryptoCurrency].findCachedActivityItem(
            txHash) as NonCustodialActivitySummaryItem
        return item.fee.singleOrError().flatMap { cryptoValue ->
            item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency).map { fiatValue ->
                ActivityDetailsComposite(item, cryptoValue.toStringWithSymbol(),
                    fiatValue.toStringWithSymbol())
            }
        }
    }

}