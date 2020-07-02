package piuk.blockchain.android.coincore.erc20.pax

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.impl.Erc20CryptoNonCustodialAccountBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class PaxCryptoWalletAccount(
    label: String,
    private val address: String,
    account: Erc20Account,
    exchangeRates: ExchangeRateDataManager
) : Erc20CryptoNonCustodialAccountBase(
    CryptoCurrency.PAX,
    label,
    account,
    exchangeRates
) {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            PaxAddress(address, label)
        )
}
