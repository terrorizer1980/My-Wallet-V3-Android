package piuk.blockchain.androidcore.data.erc20

import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import java.math.BigInteger

data class Erc20DataModel(val totalBalance: BigInteger, val transfers: List<Erc20Transfer>, val accountHash: String) {
    companion object {
        operator fun invoke(addressResponse: Erc20AddressResponse): Erc20DataModel =
            Erc20DataModel(addressResponse.balance,
                addressResponse.transfers.map { Erc20Transfer(it) },
                addressResponse.accountHash)
    }
}