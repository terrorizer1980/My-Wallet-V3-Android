package com.blockchain.logging

import info.blockchain.balance.CryptoValue
import java.math.BigInteger

interface SwapDiagnostics {
    fun logBalance(balance: CryptoValue?)
    fun logMaxSpendable(maxSpendable: CryptoValue?)
    fun logAbsoluteFee(spendable: BigInteger)
    fun logSwapAmount(amount: CryptoValue?)

    fun logIsMax(isMax: Boolean)

    fun logStateVariable(name: String, value: String)

    fun log(msg: String)
    fun logFailure(hash: String?, errorMsg: String?)
    fun logSuccess(hash: String?)
}
