package piuk.blockchain.android.coincore.activity

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Assert.*
import org.junit.Test
import piuk.blockchain.android.coincore.model.TestActivitySummaryItem
import piuk.blockchain.android.ui.account.ItemAccount

class TransactionNoteUpdaterTest {


    @Test
    fun `update transactions list with coinify labels`() {
        //  Arrange
        //  Transaction setup
        val itemAccount = ItemAccount()
        val txHash = "TX_HASH"
        val item = TestActivitySummaryItem(
            hash = txHash
        )

        //  Exchange token setup
        val token = "TOKEN"
        val coinifyData = CoinifyData(1, token)
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        //  Coinify trade setup
        val coinifyTrade: CoinifyTrade = mock()
        val tradeId = 12345
        whenever(coinifyTrade.id).thenReturn(tradeId)
        whenever(coinifyTrade.isSellTransaction()).thenReturn(false)
        val transferOut: Transfer = mock()
        whenever(coinifyTrade.transferOut).thenReturn(transferOut)
        val details = BlockchainDetails("", null, EventData(txHash, ""))
        whenever(transferOut.details).thenReturn(details)
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.just(coinifyTrade))
        //  ShapeShift
        whenever(shapeShiftDataManager.getTradesList()).thenReturn(Observable.just(emptyList()))
        //  Utils
        whenever(stringUtils.getFormattedString(any(), any())).thenReturn(tradeId.toString())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any())).thenReturn("")
        whenever(
            currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(
                any(),
                any(),
                isNull()
            )
        ).thenReturn("")
        whenever(fiatExchangeRates.getFiat(any())).thenReturn(10.gbp())
        whenever(swipeToReceiveHelper.storeAll())
            .thenReturn(Completable.complete())
        //  Act
        val testObserver = subject.updateTransactionsListCompletable(itemAccount).test()
        //  Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(view).updateTransactionDataSet(any(), any())
        verify(displayable).note = tradeId.toString()
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
    }
}