package com.blockchain

import com.blockchain.logging.CrashLogger
import com.blockchain.logging.SwapDiagnostics
import info.blockchain.balance.CryptoValue
import java.math.BigInteger

class SwapDiagnosticsImpl(
    private val crashLogger: CrashLogger
) : SwapDiagnostics {

    override fun logBalance(balance: CryptoValue?) =
        crashLogger.logState("SWAP_TX_BALANCE", balance?.toStringWithSymbol() ?: "<UNKNOWN>")

    override fun logAbsoluteFee(spendable: BigInteger) =
        crashLogger.logState("SWAP_TX_ABS_FEE", spendable.toString())

    override fun logSwapAmount(amount: CryptoValue?) =
        crashLogger.logState("SWAP_TX_AMOUNT", amount?.toStringWithSymbol() ?: "<UNKNOWN>")

    override fun logMaxSpendable(maxSpendable: CryptoValue?) =
        crashLogger.logState("SWAP_MAX_SPENDABLE", maxSpendable?.toStringWithSymbol() ?: "<UNKNOWN>")

    override fun logIsMax(isMax: Boolean) =
        crashLogger.logState("SWAP_MAX_SELECTED", isMax.toString())

    override fun logStateVariable(name: String, value: String) =
        crashLogger.logState("SWAP_$name", value)

    override fun log(msg: String) =
        crashLogger.logEvent("SWAP: $msg")

    override fun logFailure(hash: String?, errorMsg: String?) {
        crashLogger.logEvent("SWAP - FAILED: h:$hash -> $errorMsg")
        crashLogger.logException(SwapFailureException())
    }

    override fun logSuccess(hash: String?) {
        crashLogger.logEvent("SWAP - COMPLETE: h:$hash")
        crashLogger.logException(SwapSuccessException())
    }
}

private class SwapFailureException : Throwable()
private class SwapSuccessException : Throwable()
