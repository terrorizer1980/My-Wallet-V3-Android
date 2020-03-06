package piuk.blockchain.android.accounts

import com.blockchain.accounts.AccountList
import com.blockchain.accounts.AsyncAllAccountList
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class AsyncAllAccountsImplementationTest {

    @Test
    fun `can get full list of all accounts`() {
        val btcAccountList = mock<AccountList> {
            on { accounts() } `it returns` Single.just(
                listOf(
                    AccountReference.BitcoinLike(CryptoCurrency.BTC, "Bitcoin 1", "xpub 1") as AccountReference,
                    AccountReference.BitcoinLike(CryptoCurrency.BTC, "Bitcoin 2", "xpub 2") as AccountReference
                )
            )
        }

        val bchAccountList = mock<AccountList> {
            on { accounts() } `it returns` Single.just(
                listOf(
                    AccountReference.BitcoinLike(CryptoCurrency.BCH, "Bitcoin Cash 1", "xpub 1") as AccountReference,
                    AccountReference.BitcoinLike(CryptoCurrency.BCH, "Bitcoin Cash 2", "xpub 3") as AccountReference
                )
            )
        }

        val ethAccountList = mock<AccountList> {
            on { accounts() } `it returns` Single.just(
                listOf(
                    AccountReference.Ethereum("Ether 1", "0xAddress") as AccountReference
                )
            )
        }

        val xlmAccountList = mock<AccountList> {
            on { accounts() } `it returns` Single.just(
                listOf(
                    AccountReference.Xlm("Xlm 1", "GABC") as AccountReference
                )
            )
        }

        val allAccountList: AsyncAllAccountList =
            AsyncAllAccountListImplementation(
                mapOf(
                    CryptoCurrency.BTC to btcAccountList,
                    CryptoCurrency.ETHER to bchAccountList,
                    CryptoCurrency.BCH to ethAccountList,
                    CryptoCurrency.XLM to xlmAccountList
                )
            )

        allAccountList.allAccounts()
            .test()
            .assertComplete()
            .values()
            .single() `should equal`
            listOf(
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Bitcoin 1", "xpub 1") as AccountReference,
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "Bitcoin 2", "xpub 2") as AccountReference,
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "Bitcoin Cash 1", "xpub 1") as AccountReference,
                AccountReference.BitcoinLike(CryptoCurrency.BCH, "Bitcoin Cash 2", "xpub 3") as AccountReference,
                AccountReference.Ethereum("Ether 1", "0xAddress") as AccountReference,
                AccountReference.Xlm("Xlm 1", "GABC") as AccountReference
            )
    }
}
