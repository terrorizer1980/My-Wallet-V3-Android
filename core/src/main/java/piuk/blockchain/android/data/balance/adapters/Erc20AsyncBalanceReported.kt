package piuk.blockchain.android.data.balance.adapters

import com.blockchain.balance.AsyncBalanceReporter
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.androidcore.data.erc20.Erc20Manager

fun Erc20Manager.toAsyncBalanceReporter(): AsyncBalanceReporter = Erc20BalanceReportAdapter(this)

private class Erc20BalanceReportAdapter(
    private val erc20Manager: Erc20Manager
) : AsyncBalanceReporter {

    override fun entireBalance(): Single<CryptoValue> =
        erc20Manager.fetchErc20Address()
            .singleOrError()
            .map {
                it.totalBalance
            }

    private val zero = Single.just(CryptoValue.ZeroEth)

    override fun watchOnlyBalance(): Single<CryptoValue> = zero

    override fun importedAddressBalance(): Single<CryptoValue> = zero

    override fun addressBalance(address: String): Single<CryptoValue> =
        erc20Manager.getBalance(CryptoCurrency.PAX)
            .map {
                CryptoValue(
                    CryptoCurrency.PAX,
                    it
                )
            }
}