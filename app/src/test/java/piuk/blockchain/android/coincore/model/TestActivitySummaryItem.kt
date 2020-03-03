package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import org.amshove.kluent.mock
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class TestActivitySummaryItem(
    exchangeRates: ExchangeRateDataManager = mock(),
    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.BTC,
    override val direction: TransactionSummary.Direction = TransactionSummary.Direction.RECEIVED,
    override val timeStamp: Long = 0,
    override val totalCrypto: CryptoValue = CryptoValue.ZeroBtc,
    override val fee: Observable<CryptoValue> = Observable.just(CryptoValue.ZeroBtc),
    override val hash: String = "",
    override val inputsMap: Map<String, CryptoValue> = emptyMap(),
    override val outputsMap: Map<String, CryptoValue> = emptyMap(),
    override val description: String? = null,
    override val confirmations: Int = 0,
    override val isFeeTransaction: Boolean = false
) : ActivitySummaryItem(exchangeRates)