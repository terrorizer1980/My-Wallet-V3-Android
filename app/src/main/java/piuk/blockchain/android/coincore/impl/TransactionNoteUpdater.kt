package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import com.blockchain.swap.shapeshift.data.Trade
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.services.ExchangeService
import timber.log.Timber

class TransactionNoteUpdater(
    private val exchangeService: ExchangeService,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val coinifyDataManager: CoinifyDataManager,
    private val stringUtils: StringUtils
) {

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .singleOrError()
            .map { it.coinify?.token ?: "" }

    fun updateWithNotes(
        txList: ActivitySummaryList
    ): Single<ActivitySummaryList> =
        Singles.zip(
            getShapeShiftTxNotes(),
            getCoinifyTxNotes()
        ) { ssTx, cyTx -> mergeNotes(ssTx, cyTx) }
        .map { txNotesMap ->
            processNotes(txList, txNotesMap)
        }
        .onErrorReturn { txList } // If we can't get the notes, it's not the end of the world.

    private fun mergeNotes(
        shapeshiftNotes: Map<String, String>,
        coinifyNotes: Map<String, String>
    ): Map<String, String> =
        mutableMapOf<String, String>().apply {
            putAll(shapeshiftNotes)
            putAll(coinifyNotes)
        }.toMap()

    private fun processNotes(
        txList: ActivitySummaryList,
        txNotesMap: Map<String, String>
    ): ActivitySummaryList =
        txList.map { tx -> updateTransactionNote(tx, txNotesMap[tx.hash]) }

    private fun updateTransactionNote(tx: ActivitySummaryItem, note: String?) =
        note?.let { tx.note = note; tx } ?: tx

    private fun getShapeShiftTxNotes() =
        shapeShiftDataManager.getTradesList()
            .map { processShapeShiftTxNotes(it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { emptyMap() }
            .singleOrError()

    private fun processShapeShiftTxNotes(list: List<Trade>): Map<String, String> { // TxHash -> description
        val mutableMap: MutableMap<String, String> = mutableMapOf()

        for (trade in list) {
            trade.hashIn?.let {
                mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_to))
            }
            trade.hashOut?.let {
                mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_from))
            }
        }
        return mutableMap.toMap()
    }

    private fun getCoinifyTxNotes() =
        tokenSingle.flatMap {
            coinifyDataManager.getTrades(it).toList()
        }.doOnError { Timber.e(it) }
            .map { processCoinifyTxNotes(it) }
            .toObservable()
            .onErrorReturn { emptyMap() }
            .singleOrError()

    private fun processCoinifyTxNotes(list: List<CoinifyTrade>): Map<String, String> { // TxId -> description
        val mutableMap: MutableMap<String, String> = mutableMapOf()
        for (trade in list) {
            val transfer = if (trade.isSellTransaction()) {
                trade.transferIn.details as BlockchainDetails
            } else {
                trade.transferOut.details as BlockchainDetails
            }
            transfer.eventData?.txId?.let {
                mutableMap.put(
                    it,
                    stringUtils.getFormattedString(
                        R.string.buy_sell_transaction_list_label,
                        trade.id
                    )
                )
            }
        }
        return mutableMap.toMap()
    }
}
