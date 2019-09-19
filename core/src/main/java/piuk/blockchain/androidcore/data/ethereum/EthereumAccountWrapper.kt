package piuk.blockchain.androidcore.data.ethereum

import info.blockchain.wallet.ethereum.EthereumAccount
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey

/**
 * This class is simply for making [EthereumAccount.deriveECKey] mockable for testing.
 */
class EthereumAccountWrapper {

    fun deriveECKey(masterKey: DeterministicKey, accountIndex: Int): ECKey =
        EthereumAccount.deriveECKey(masterKey, accountIndex)
}