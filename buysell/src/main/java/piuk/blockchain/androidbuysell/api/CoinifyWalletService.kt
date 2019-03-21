package piuk.blockchain.androidbuysell.api

import io.reactivex.Maybe
import piuk.blockchain.androidbuysell.models.CoinifyData

interface CoinifyWalletService {
    fun getCoinifyData(): Maybe<CoinifyData>
}
