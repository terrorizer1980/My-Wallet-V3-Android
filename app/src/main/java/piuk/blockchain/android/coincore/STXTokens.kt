package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.stx.STXAccount
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan

class STXTokens(
    private val payloadManager: PayloadManager,
    private val currencyPrefs: CurrencyPrefs
) : AssetTokens {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.STX

    override fun defaultAccount(): Single<AccountReference> {
        val hdWallets = payloadManager.payload?.hdWallets
            ?: return Single.error(IllegalStateException("Wallet not available"))
        return Single.just(hdWallets[0].stxAccount.toAccountReference())
    }

    override fun totalBalance(filter: BalanceFilter): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun exchangeRate(): Single<FiatValue> {
        TODO("not implemented")
    }

    override fun historicRate(epochWhen: Long): Single<FiatValue> {
        TODO("not implemented")
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> {
        TODO("not implemented")
    }
}

private fun STXAccount.toAccountReference(): AccountReference.Stx =
    AccountReference.Stx(
        _label = "STX Account",
        address = bitcoinSerializedBase58Address
    )
