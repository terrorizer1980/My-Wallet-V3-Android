package piuk.blockchain.android.coincore.activity

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.amshove.kluent.itReturns
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.TestNonCustodialSummaryItem
import piuk.blockchain.android.coincore.impl.TransactionNoteUpdater
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.EventData
import piuk.blockchain.androidbuysell.models.coinify.Transfer
import piuk.blockchain.androidbuysell.services.ExchangeService

class TransactionNoteUpdaterTest {

    private val exchangeService: ExchangeService = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val stringUtils: StringUtils = mock()

    private val subject =
        TransactionNoteUpdater(
            exchangeService = exchangeService,
            shapeShiftDataManager = shapeShiftDataManager,
            coinifyDataManager = coinifyDataManager,
            stringUtils = stringUtils
        )

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun `update transactions list with coinify labels`() {
        //  Arrange
        val txHash = "TX_HASH"
        val item = TestNonCustodialSummaryItem(
            txId = txHash
        )

        //  Exchange token setup
        val token = "TOKEN"
        val coinifyData = CoinifyData(1, token)
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        //  Coinify trade setup
        val tradeId = 12345
        val theTransferOut: Transfer = mock {
            on { details } itReturns BlockchainDetails("", null, EventData(txHash, ""))
        }

        val coinifyTrade: CoinifyTrade = mock {
            on { id } itReturns tradeId
            on { isSellTransaction() } itReturns false
            on { transferOut } itReturns theTransferOut
        }

        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.just(coinifyTrade))
        whenever(shapeShiftDataManager.getTradesList()).thenReturn(Observable.just(emptyList()))

        //  Act
        subject.updateWithNotes(listOf(item))
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
    }
}
