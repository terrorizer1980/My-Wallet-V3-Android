package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import java.math.BigInteger

internal class TestActivitySummaryItem(
    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.BTC,
    override val direction: TransactionSummary.Direction = TransactionSummary.Direction.RECEIVED,
    override val timeStamp: Long = 0,
    override val totalCrypto: CryptoValue = CryptoValue.ZeroBtc,
    override val totalFiat: FiatValue = FiatValue.zero("USD"),
    override val fee: Observable<BigInteger> = Observable.just(0.toBigInteger()),
    override val hash: String = "",
    override val inputsMap: Map<String, BigInteger> = emptyMap(),
    override val outputsMap: Map<String, BigInteger> = emptyMap(),
    override val description: String? = null
) : ActivitySummaryItem()