package piuk.blockchain.android.coincore.erc20.usdt

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.Erc20CryptoSingleNonCustodialAccountBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class UsdtCryptoWalletAccount(
    label: String,
    address: String,
    account: Erc20Account,
    exchangeRates: ExchangeRateDataManager
) : Erc20CryptoSingleNonCustodialAccountBase(
    CryptoCurrency.USDT,
    label,
    address,
    account,
    exchangeRates
) {
    override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Swap
    )
}
