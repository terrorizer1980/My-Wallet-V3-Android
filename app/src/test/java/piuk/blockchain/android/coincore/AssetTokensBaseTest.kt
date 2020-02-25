package piuk.blockchain.android.coincore

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.ui.account.ItemAccount

class AssetTokensBaseTest {

    private val subject: AssetTokensBase = mock(defaultAnswer = Mockito.CALLS_REAL_METHODS)

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun `activity fetch should return empty list on error`() {
        whenever(subject.doFetchActivity(any())).doReturn(Single.error(Exception()))

        val itemAccount: ItemAccount = mock()

        subject.fetchActivity(itemAccount)
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue { it.isEmpty() }
    }

    //    @Test
//    fun getTxFromHashFound() {
//
//        val txHash = "TX_HASH"
//        val tx0 = TestActivitySummaryItem(hash = "")
//        val tx1 = TestActivitySummaryItem(hash = "")
//        val tx2 = TestActivitySummaryItem(hash = txHash)
//
//        transactionListStore.insertTransactions(listOf(tx0, tx1, tx2))
//
//        subject.getTxFromHash(txHash)
//            .test()
//            .assertComplete()
//            .assertNoErrors()
//            .assertValue(tx2)
//    }
//
//    @Test
//    fun getTxFromHashNotFound() {
//
//        val txHash = "TX_HASH"
//        val tx0 = TestActivitySummaryItem(hash = "")
//        val tx1 = TestActivitySummaryItem(hash = "")
//        val tx2 = TestActivitySummaryItem(hash = "")
//        transactionListStore.insertTransactions(listOf(tx0, tx1, tx2))
//
//        // Act
//        val testObserver = subject.getTxFromHash(txHash).test()
//        // Assert
//        testObserver.assertTerminated()
//        testObserver.assertNoValues()
//        testObserver.assertError(NoSuchElementException::class.java)
//    }
}