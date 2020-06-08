package piuk.blockchain.android.coincore.erc20.pax

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.impl.Erc20CryptoSingleNonCustodialAccountBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class PaxCryptoWalletAccount(
    label: String,
    address: String,
    account: Erc20Account,
    exchangeRates: ExchangeRateDataManager
) : Erc20CryptoSingleNonCustodialAccountBase(
    CryptoCurrency.PAX,
    label,
    address,
    account,
    exchangeRates
)
