package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`

import org.junit.Test
import java.math.BigInteger

private class TestActivitySummaryItem(
    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.BTC,
    override val direction: TransactionSummary.Direction = TransactionSummary.Direction.RECEIVED,
    override val timeStamp: Long = 0,
    override val total: BigInteger = 0.toBigInteger(),
    override val fee: Observable<BigInteger> = Observable.just(0.toBigInteger()),
    override val hash: String = "",
    override val inputsMap: Map<String, BigInteger> = emptyMap(),
    override val outputsMap: Map<String, BigInteger> = emptyMap()
) : ActivitySummaryItem()

class ActivitySummaryItemTest {

    @Test
    fun `ensure not equal when compared to different type`() {

        val activityItem = TestActivitySummaryItem()
        val objectToCompare = Any()

        activityItem.toString() `should not equal` objectToCompare.toString()
        activityItem.hashCode() `should not equal` objectToCompare.hashCode()
        activityItem `should not equal` objectToCompare
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with subtly different objects`() {

        val itemOne = TestActivitySummaryItem()
            .apply { note = "note 1" }

        val itemTwo = TestActivitySummaryItem()
            .apply { note = "note 2" }

        itemOne.toString() `should not equal` itemTwo.toString()
        itemOne.hashCode() `should not equal` itemTwo.hashCode()
        itemOne `should not equal` itemTwo
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with identical objects`() {

        val itemOne = TestActivitySummaryItem()
            .apply { note = "note" }

        val itemTwo = TestActivitySummaryItem()
            .apply { note = "note" }

        itemOne.toString() `should equal` itemTwo.toString()
        itemOne.hashCode() `should equal` itemTwo.hashCode()
        itemOne `should equal` itemTwo
    }
}
