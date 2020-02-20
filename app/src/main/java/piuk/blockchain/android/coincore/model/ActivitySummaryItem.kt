package piuk.blockchain.android.coincore.model

import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.compareTo
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.erc20.Erc20Transfer
import piuk.blockchain.androidcore.data.erc20.FeedErc20Transfer
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.utils.helperfunctions.JavaHashCode
import java.math.BigInteger
import kotlin.math.sign

abstract class ActivitySummaryItem : Comparable<ActivitySummaryItem> {

    abstract val cryptoCurrency: CryptoCurrency
    abstract val direction: TransactionSummary.Direction
    abstract val timeStamp: Long
    abstract val total: BigInteger
    abstract val fee: Observable<BigInteger>
    abstract val hash: String
    abstract val inputsMap: Map<String, BigInteger>
    abstract val outputsMap: Map<String, BigInteger>

    open val confirmations = 0
    open val watchOnly: Boolean = false
    open val doubleSpend: Boolean = false
    open val isFeeTransaction = false
    open val isPending: Boolean = false
    open var totalDisplayableCrypto: String? = null
    open var totalDisplayableFiat: String? = null
    open var note: String? = null

    override fun toString(): String = "cryptoCurrency = $cryptoCurrency" +
            "direction  = $direction " +
            "timeStamp  = $timeStamp " +
            "total  = $total " +
            "hash  = $hash " +
            "inputsMap  = $inputsMap " +
            "outputsMap  = $outputsMap " +
            "confirmations  = $confirmations " +
            "watchOnly  = $watchOnly " +
            "doubleSpend  = $doubleSpend " +
            "isPending  = $isPending " +
            "totalDisplayableCrypto  = $totalDisplayableCrypto " +
            "totalDisplayableFiat  = $totalDisplayableFiat " +
            "note = $note"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ActivitySummaryItem?

        return this.cryptoCurrency == that?.cryptoCurrency &&
                this.direction == that.direction &&
                this.timeStamp == that.timeStamp &&
                this.total == that.total &&
                this.hash == that.hash &&
                this.inputsMap == that.inputsMap &&
                this.outputsMap == that.outputsMap &&
                this.confirmations == that.confirmations &&
                this.watchOnly == that.watchOnly &&
                this.doubleSpend == that.doubleSpend &&
                this.isFeeTransaction == that.isFeeTransaction &&
                this.isPending == that.isPending &&
                this.totalDisplayableCrypto == that.totalDisplayableCrypto &&
                this.totalDisplayableFiat == that.totalDisplayableFiat &&
                this.note == that.note
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + cryptoCurrency.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + JavaHashCode.hashCode(timeStamp)
        result = 31 * result + total.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + inputsMap.hashCode()
        result = 31 * result + outputsMap.hashCode()
        result = 31 * result + JavaHashCode.hashCode(confirmations)
        result = 31 * result + JavaHashCode.hashCode(isFeeTransaction)
        result = 31 * result + JavaHashCode.hashCode(watchOnly)
        result = 31 * result + JavaHashCode.hashCode(doubleSpend)
        result = 31 * result + (totalDisplayableCrypto?.hashCode() ?: 0)
        result = 31 * result + (totalDisplayableFiat?.hashCode() ?: 0)
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }

    override operator fun compareTo(other: ActivitySummaryItem) = (other.timeStamp - timeStamp).sign
}




class Erc20ActivitySummaryItem(
    private val feedTransfer: FeedErc20Transfer,
    private val accountHash: String,
    private val lastBlockNumber: BigInteger
) :
    ActivitySummaryItem() {

    private val transfer: Erc20Transfer
        get() = feedTransfer.transfer
    override val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.PAX
    override val direction: TransactionSummary.Direction
        get() = when {
            transfer.isToAccount(accountHash)
                    && transfer.isFromAccount(accountHash) -> TransactionSummary.Direction.TRANSFERRED
            transfer.isFromAccount(accountHash) -> TransactionSummary.Direction.SENT
            else -> TransactionSummary.Direction.RECEIVED
        }
    override val timeStamp: Long
        get() = transfer.timestamp
    override val total: BigInteger
        get() = transfer.value
    override val fee: Observable<BigInteger>
        get() = feedTransfer.feeObservable
    override val hash: String
        get() = transfer.transactionHash
    override val inputsMap: HashMap<String, BigInteger>
        get() = HashMap<String, BigInteger>().apply {
            put(transfer.from, transfer.value)
        }
    override val outputsMap: HashMap<String, BigInteger>
        get() = HashMap<String, BigInteger>().apply {
            put(transfer.to, transfer.value)
        }
    override val confirmations: Int
        get() = (lastBlockNumber - transfer.blockNumber).toInt()
}

