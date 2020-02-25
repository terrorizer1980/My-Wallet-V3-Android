package piuk.blockchain.android.coincore.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import piuk.blockchain.androidcore.utils.helperfunctions.JavaHashCode
import java.math.BigInteger
import kotlin.math.sign

abstract class ActivitySummaryItem : Comparable<ActivitySummaryItem> {

    abstract val cryptoCurrency: CryptoCurrency
    abstract val direction: TransactionSummary.Direction
    abstract val timeStamp: Long
    abstract val fee: Observable<BigInteger>
    abstract val hash: String
    abstract val inputsMap: Map<String, BigInteger>
    abstract val outputsMap: Map<String, BigInteger>

    abstract val totalCrypto: CryptoValue
    abstract val totalFiat: FiatValue

    open val confirmations = 0
    open val watchOnly: Boolean = false
    open val doubleSpend: Boolean = false
    open val isFeeTransaction = false
    open val isPending: Boolean = false
    open var note: String? = null

    override fun toString(): String = "cryptoCurrency = $cryptoCurrency" +
            "direction  = $direction " +
            "timeStamp  = $timeStamp " +
            "total  = ${totalCrypto.toStringWithSymbol()} " +
            "hash  = $hash " +
            "inputsMap  = $inputsMap " +
            "outputsMap  = $outputsMap " +
            "confirmations  = $confirmations " +
            "watchOnly  = $watchOnly " +
            "doubleSpend  = $doubleSpend " +
            "isPending  = $isPending " +
            "totalDisplayableFiat  = ${totalFiat.toStringWithSymbol()} " +
            "note = $note"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ActivitySummaryItem?

        return this.cryptoCurrency == that?.cryptoCurrency &&
                this.direction == that.direction &&
                this.timeStamp == that.timeStamp &&
                this.totalCrypto == that.totalCrypto &&
                this.hash == that.hash &&
                this.inputsMap == that.inputsMap &&
                this.outputsMap == that.outputsMap &&
                this.confirmations == that.confirmations &&
                this.watchOnly == that.watchOnly &&
                this.doubleSpend == that.doubleSpend &&
                this.isFeeTransaction == that.isFeeTransaction &&
                this.isPending == that.isPending &&
                this.totalFiat == that.totalFiat &&
                this.note == that.note
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + cryptoCurrency.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + JavaHashCode.hashCode(timeStamp)
        result = 31 * result + totalCrypto.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + inputsMap.hashCode()
        result = 31 * result + outputsMap.hashCode()
        result = 31 * result + JavaHashCode.hashCode(confirmations)
        result = 31 * result + JavaHashCode.hashCode(isFeeTransaction)
        result = 31 * result + JavaHashCode.hashCode(watchOnly)
        result = 31 * result + JavaHashCode.hashCode(doubleSpend)
        result = 31 * result + totalFiat.hashCode()
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }

    override operator fun compareTo(other: ActivitySummaryItem) = (other.timeStamp - timeStamp).sign
}

typealias ActivitySummaryList = List<ActivitySummaryItem>
