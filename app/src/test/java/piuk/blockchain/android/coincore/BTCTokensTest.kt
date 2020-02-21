package piuk.blockchain.android.coincore

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.math.BigInteger

class BTCTokensTest {

    private val payloadManager: PayloadManager = mock()
    private val currencyState: CurrencyState = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val historicRates: ChartsDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val rxBus: RxBus = spy()

    private val subject: AssetTokensBase = BTCTokens(
        payloadDataManager = payloadDataManager,
        exchangeRates = exchangeRates,
        payloadManager = payloadManager,
        historicRates = historicRates,
        currencyPrefs = currencyPrefs,
        custodialWalletManager = custodialWalletManager,
        rxBus = rxBus
    )

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun fetchTransactionsOnAccount() {

        val account = Account()
        val xPub = TEST_XPUB

        val summary = TransactionSummary().apply {
            confirmations = 3
            direction = TransactionSummary.Direction.RECEIVED
            fee = BigInteger.ONE
            total = BigInteger.TEN
            hash = "hash"
            inputsMap = HashMap()
            outputsMap = HashMap()
            time = 1000000L
        }

        val transactionSummaries = listOf(summary)

        whenever(payloadManager.getAccountTransactions(any(), any(), any()))
            .thenReturn(transactionSummaries)

        val itemAccount = ItemAccount().apply {
            accountObject = account
            type = ItemAccount.TYPE.SINGLE_ACCOUNT
            address = xPub
        }

        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        subject.doFetchActivity(itemAccount)
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(payloadManager).getAccountTransactions(any(), any(), any())

        // TODO: Validate returned list
    }

    @Test
    fun fetchTransactionsAccountTagAll() {
        val account = Account()
        val summary = TransactionSummary().apply {
            confirmations = 3
            direction = TransactionSummary.Direction.RECEIVED
            fee = BigInteger.ONE
            total = BigInteger.TEN
            hash = "hash"
            inputsMap = HashMap()
            outputsMap = HashMap()
            time = 1000000L
        }

        val transactionSummaries = listOf(summary)
        whenever(payloadManager.getAllTransactions(any(), any()))
            .thenReturn(transactionSummaries)

        val itemAccount = ItemAccount().apply {
            accountObject = account
            type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
        }

        whenever(currencyState.cryptoCurrency)
            .thenReturn(CryptoCurrency.BTC)

        subject.doFetchActivity(itemAccount)
            .test()
            .assertComplete()
            .assertNoErrors()

        Mockito.verify(payloadManager).getAllTransactions(any(), any())
    }

    @Test
    fun fetchTransactionsAccountTagImported() {
        val account = Account()
        val summary = TransactionSummary().apply {
            confirmations = 3
            direction = TransactionSummary.Direction.RECEIVED
            fee = BigInteger.ONE
            total = BigInteger.TEN
            hash = "hash"
            inputsMap = HashMap()
            outputsMap = HashMap()
            time = 1000000L
        }

        val transactionSummaries = listOf(summary)
        whenever(payloadManager.getImportedAddressesTransactions(any(), any()))
            .thenReturn(transactionSummaries)

        val itemAccount = ItemAccount().apply {
            accountObject = account
            type = ItemAccount.TYPE.ALL_LEGACY
        }

        whenever(currencyState.cryptoCurrency)
            .thenReturn(CryptoCurrency.BTC)

        subject.doFetchActivity(itemAccount)
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(payloadManager).getImportedAddressesTransactions(any(), any())
    }

    @Test
    @Throws(Exception::class)
    fun fetchTransactionsAccountNoXpub() {
        val account = Account()
        val xPub = "invalid xpub"
        val summary = TransactionSummary().apply {
            confirmations = 3
            direction = TransactionSummary.Direction.RECEIVED
            fee = BigInteger.ONE
            total = BigInteger.TEN
            hash = "hash"
            inputsMap = HashMap()
            outputsMap = HashMap()
            time = 1000000L
        }

        val transactionSummaries = listOf(summary)
        whenever(payloadManager.getImportedAddressesTransactions(any(), any()))
            .thenReturn(transactionSummaries)

        val itemAccount = ItemAccount().apply {
            accountObject = account
            type = ItemAccount.TYPE.ALL_LEGACY
            address = xPub
        }

        whenever(currencyState.cryptoCurrency)
            .thenReturn(CryptoCurrency.BTC)

        subject.doFetchActivity(itemAccount)
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(payloadManager).getImportedAddressesTransactions(any(), any())
    }

    companion object {
        private const val TEST_XPUB =
            "xpub6CfLQa8fLgtp8E7tc1khAhrZYPm82okmugxP7TrhMPkPFKANhdCU" +
            "d4TDJKUYLCxZskG2U7Q689CVBxs2EjJA7dyvjCzN5UYWwZbY2qVpymw"
    }
}