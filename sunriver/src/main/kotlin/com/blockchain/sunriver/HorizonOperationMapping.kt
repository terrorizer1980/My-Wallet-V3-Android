package com.blockchain.sunriver

import com.blockchain.sunriver.models.XlmTransaction
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.withMajorValue
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse

internal fun List<OperationResponse>.map(accountId: String, horizonProxy: HorizonProxy): List<XlmTransaction> =
    filter { it is CreateAccountOperationResponse || it is PaymentOperationResponse }
        .map { mapOperationResponse(it, accountId, horizonProxy) }

internal fun mapOperationResponse(
    operationResponse: OperationResponse,
    usersAccountId: String,
    horizonProxy: HorizonProxy
): XlmTransaction =
    when (operationResponse) {
        is CreateAccountOperationResponse -> operationResponse.mapCreate(usersAccountId, horizonProxy)
        is PaymentOperationResponse -> operationResponse.mapPayment(usersAccountId, horizonProxy)
        else -> throw IllegalArgumentException("Unsupported operation type ${operationResponse.javaClass.simpleName}")
    }

private fun CreateAccountOperationResponse.mapCreate(
    usersAccountId: String,
    horizonProxy: HorizonProxy
): XlmTransaction {
    val transactionResponse = horizonProxy.getTransaction(transactionHash)
    val fee = CryptoValue.lumensFromStroop(transactionResponse.feePaid.toBigInteger())
    return XlmTransaction(
        timeStamp = createdAt,
        value = deltaValueForAccount(usersAccountId, funder, startingBalance),
        fee = fee,
        hash = transactionHash,
        to = account.toHorizonKeyPair().neuter(),
        from = funder.toHorizonKeyPair().neuter()
    )
}

private fun PaymentOperationResponse.mapPayment(
    usersAccountId: String,
    horizonProxy: HorizonProxy
): XlmTransaction {
    val transactionResponse = horizonProxy.getTransaction(transactionHash)
    val fee = CryptoValue.lumensFromStroop(transactionResponse.feePaid.toBigInteger())
    return XlmTransaction(
        timeStamp = createdAt,
        value = deltaValueForAccount(usersAccountId, from, amount),
        fee = fee,
        hash = transactionHash,
        to = to.toHorizonKeyPair().neuter(),
        from = from.toHorizonKeyPair().neuter()
    )
}

private fun deltaValueForAccount(
    usersAccountId: String,
    from: KeyPair,
    value: String
): CryptoValue {
    val valueAsBigDecimal = value.toBigDecimal()
    val deltaForThisAccount =
        if (from.accountId == usersAccountId) {
            valueAsBigDecimal.negate()
        } else {
            valueAsBigDecimal
        }
    return CryptoCurrency.XLM.withMajorValue(deltaForThisAccount)
}
