package piuk.blockchain.android.ui.transactions

import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payment.Payment
import org.apache.commons.lang3.tuple.Pair
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.math.BigInteger
import java.util.ArrayList
import java.util.HashMap
import java.util.TreeMap

class TransactionHelper(
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager
) {
    /**
     * Return a Pair of maps that correspond to the inputs and outputs of a transaction, whilst
     * filtering out Change addresses.
     *
     * @param tx A [TransactionSummary] object
     * @return A Pair of Maps representing the input and output of the transaction
     */
    fun filterNonChangeBtcAddresses(
        tx: ActivitySummaryItem
    ): Pair<Map<String, BigInteger?>, Map<String, BigInteger?>> {
        val inputMap = HashMap<String, BigInteger?>()
        val outputMap = HashMap<String, BigInteger?>()
        val inputXpubList = ArrayList<String>()

        // Inputs / From field
        if (tx.direction == TransactionSummary.Direction.RECEIVED && tx.inputsMap.isNotEmpty()) {
            // Only 1 addr for receive
            val treeMap = TreeMap(tx.inputsMap)
            inputMap[treeMap.lastKey()] = treeMap.lastEntry().value
        } else {
            for (inputAddress in tx.inputsMap.keys) {
                val inputValue = tx.inputsMap[inputAddress]
                // Move or Send
                // The address belongs to us
                val xpub = payloadDataManager.getXpubFromAddress(inputAddress)
                // Address belongs to xpub we own
                if (xpub != null) { // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap[inputAddress] = inputValue
                        inputXpubList.add(xpub)
                    }
                } else { // Legacy Address or someone else's address
                    inputMap[inputAddress] = inputValue
                }
            }
        }

        // Outputs / To field
        for (outputAddress in tx.outputsMap.keys) {
            val outputValue = tx.outputsMap[outputAddress]
            if (payloadDataManager.isOwnHDAddress(outputAddress)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                val xpub = payloadDataManager.getXpubFromAddress(outputAddress)
                if (inputXpubList.contains(xpub)) {
                    continue // change back to same xpub
                }
                // Receiving to same address multiple times?
                if (outputMap.containsKey(outputAddress)) {
                    val prevAmount = outputMap[outputAddress]!!.add(outputValue)
                    outputMap[outputAddress] = prevAmount
                } else {
                    outputMap[outputAddress] = outputValue
                }
            } else if (
                payloadDataManager.wallet!!.legacyAddressStringList.contains(outputAddress) ||
                payloadDataManager.wallet!!.watchOnlyAddressStringList.contains(outputAddress)
            ) { // If output address belongs to a legacy address we own - we have to check if
                // it's changes
                // If it goes back to same address AND if it's not the total amount sent
                // (inputs x and y could send to output y in which case y is not receiving change,
                // but rather the total amount)
                if (inputMap.containsKey(outputAddress) &&
                    outputValue!!.abs().compareTo(tx.totalCrypto.amount) != 0
                ) {
                    continue // change back to same input address
                }
                // Output more than tx amount - change
                if (outputValue!!.abs() > tx.totalCrypto.amount) {
                    continue
                }
                outputMap[outputAddress] = outputValue
            } else {
                if (tx.direction != TransactionSummary.Direction.RECEIVED) {
                    outputMap[outputAddress] = outputValue
                }
            }
        }
        return Pair.of(inputMap, outputMap)
    }

    fun filterNonChangeAddressesBch(
        tx: ActivitySummaryItem
    ): Pair<HashMap<String, BigInteger?>, HashMap<String, BigInteger?>> {
        val inputMap = HashMap<String, BigInteger?>()
        val outputMap = HashMap<String, BigInteger?>()
        val inputXpubList = ArrayList<String>()
        // Inputs / From field
        if (tx.direction == TransactionSummary.Direction.RECEIVED && tx.inputsMap.isNotEmpty()) {
            for ((address, value) in tx.inputsMap) {
                if (value == Payment.DUST) continue
                inputMap[address] = value
            }
        } else {
            for (inputAddress in tx.inputsMap.keys) {
                val inputValue = tx.inputsMap[inputAddress]
                // Move or Send
                // The address belongs to us
                val xpub = bchDataManager.getXpubFromAddress(inputAddress)
                // Skip dust input
                if (inputValue != null && inputValue == Payment.DUST) continue
                // Address belongs to xpub we own
                if (xpub != null) { // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap[inputAddress] = inputValue
                        inputXpubList.add(xpub)
                    }
                } else { // Legacy Address or someone else's address
                    inputMap[inputAddress] = inputValue
                }
            }
        }
        // Outputs / To field
        for (outputAddress in tx.outputsMap.keys) {
            val outputValue = tx.outputsMap[outputAddress]
            // Skip dust output
            if (outputValue != null && outputValue == Payment.DUST)
                continue

            if (bchDataManager.isOwnAddress(outputAddress)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                val xpub = bchDataManager.getXpubFromAddress(outputAddress)
                if (inputXpubList.contains(xpub)) {
                    continue // change back to same xpub
                }
                // Receiving to same address multiple times?
                if (outputMap.containsKey(outputAddress)) {
                    val prevAmount = outputMap[outputAddress]!!.add(outputValue)
                    outputMap[outputAddress] = prevAmount
                } else {
                    outputMap[outputAddress] = outputValue
                }
            } else if (
                bchDataManager.getLegacyAddressStringList().contains(outputAddress) ||
                bchDataManager.getWatchOnlyAddressStringList().contains(outputAddress)
            ) { // If output address belongs to a legacy address we own - we have to check if it's
                // change
                // If it goes back to same address AND if it's not the total amount sent
                // (inputs x and y could send to output y in which case y is not receiving change,
                // but rather the total amount)
                if (inputMap.containsKey(outputAddress) &&
                    outputValue!!.abs().compareTo(tx.totalCrypto.amount) != 0
                ) {
                    continue // change back to same input address
                }
                // Output more than tx amount - change
                if (outputValue!!.abs() > tx.totalCrypto.amount) {
                    continue
                }
                outputMap[outputAddress] = outputValue
            } else {
                if (tx.direction != TransactionSummary.Direction.RECEIVED) {
                    outputMap[outputAddress] = outputValue
                }
            }
        }
        return Pair.of(
            inputMap,
            outputMap
        )
    }
}