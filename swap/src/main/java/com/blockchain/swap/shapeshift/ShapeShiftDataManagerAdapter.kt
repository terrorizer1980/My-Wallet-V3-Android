package com.blockchain.swap.shapeshift

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataManager
import com.blockchain.swap.common.trade.MorphTradeStatus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import com.blockchain.swap.shapeshift.dataadapters.TradeAdapter
import com.blockchain.swap.shapeshift.dataadapters.TradeStatusResponseAdapter
import com.blockchain.swap.shapeshift.dataadapters.map

internal class ShapeShiftDataManagerAdapter(
    private val shapeShiftDataManager: ShapeShiftDataManager
) : MorphTradeDataManager {

    override fun findTrade(depositAddress: String): Single<MorphTrade> {
        return shapeShiftDataManager
            .findTrade(depositAddress)
            .map { TradeAdapter(it) }
    }

    override fun getTrades(): Single<List<MorphTrade>> {
        return shapeShiftDataManager
            .initShapeshiftTradeData()
            .andThen(
                shapeShiftDataManager.getTradesList()
                    .flatMapIterable { it }
                    .map<MorphTrade> {
                        TradeAdapter(
                            it
                        )
                    }
                    .toList()
            )
    }

    override fun getTradeStatus(depositAddress: String): Observable<MorphTradeStatus> {
        return shapeShiftDataManager.getTradeStatus(depositAddress)
            .map { TradeStatusResponseAdapter(it) }
    }

    override fun updateTrade(
        orderId: String,
        newStatus: MorphTrade.Status,
        newHashOut: String?
    ): Completable {
        if (newStatus == MorphTrade.Status.UNKNOWN) {
            return Completable.error(Throwable("Unknown Status"))
        }
        val foundTrade = shapeShiftDataManager.findTradeByOrderId(orderId)
        return if (foundTrade == null) {
            Completable.error(Throwable("Trade not found"))
        } else {
            foundTrade.apply {
                status = newStatus.map()
                hashOut = newHashOut
            }
            shapeShiftDataManager.updateTrade(foundTrade)
        }
    }
}
