package com.blockchain.swap.shapeshift.datastore

import com.blockchain.swap.shapeshift.ShapeShiftTrades
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore

/**
 * A simple class for persisting ShapeShift Trade data.
 */
class ShapeShiftDataStore : SimpleDataStore {

    var tradeData: ShapeShiftTrades? = null

    override fun clearData() {
        tradeData = null
    }
}
