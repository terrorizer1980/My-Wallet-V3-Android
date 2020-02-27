package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import org.amshove.kluent.mock
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.math.BigInteger

internal class TestActivitySummaryItem(
    exchangeRates: ExchangeRateDataManager = mock(),
    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.BTC,
    override val direction: TransactionSummary.Direction = TransactionSummary.Direction.RECEIVED,
    override val timeStamp: Long = 0,
    override val totalCrypto: CryptoValue = CryptoValue.ZeroBtc,
    override val fee: Observable<BigInteger> = Observable.just(0.toBigInteger()),
    override val hash: String = "",
    override val inputsMap: Map<String, CryptoValue> = emptyMap(),
    override val outputsMap: Map<String, CryptoValue> = emptyMap(),
    override val description: String? = null
) : ActivitySummaryItem(exchangeRates)