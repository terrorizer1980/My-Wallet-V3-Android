package piuk.blockchain.android.data.datamanagers

import com.blockchain.sunriver.HorizonKeyPair
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.models.XlmTransaction
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.data.EthLatestBlock
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.transactions.TransactionListStore
import piuk.blockchain.androidcore.data.transactions.models.BtcDisplayable
import piuk.blockchain.androidcore.data.transactions.models.Displayable
import java.math.BigInteger
import java.util.Arrays
import java.util.HashMap
import java.util.NoSuchElementException
import junit.framework.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.Erc20Transfer
import piuk.blockchain.androidcore.data.transactions.models.Erc20Displayable

class TransactionListDataManagerTest : RxTest() {

    @Mock
    private lateinit var payloadManager: PayloadManager
    @Mock
    private lateinit var ethDataManager: EthDataManager
    @Mock
    private lateinit var bchDataManager: BchDataManager
    @Mock
    private lateinit var currencyState: CurrencyState
    @Mock
    private lateinit var xlmDataManager: XlmDataManager
    @Mock
    private lateinit var paxAccount: Erc20Account
    private val transactionListStore = TransactionListStore()
    private lateinit var subject: TransactionListDataManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        subject = TransactionListDataManager(payloadManager,
            ethDataManager,
            bchDataManager,
            xlmDataManager,
            paxAccount,
            transactionListStore,
            currencyState)
    }

    @Test
    @Throws(Exception::class)
    fun fetchTransactionsAccountTagAll() {
        // Arrange
        val account = Account()
        val summary = TransactionSummary()
        summary.confirmations = 3
        summary.direction = TransactionSummary.Direction.RECEIVED
        summary.fee = BigInteger.ONE
        summary.total = BigInteger.TEN
        summary.hash = "hash"
        summary.inputsMap = HashMap()
        summary.outputsMap = HashMap()
        summary.time = 1000000L
        val transactionSummaries = listOf(summary)
        `when`(payloadManager.getAllTransactions(0, 0)).thenReturn(transactionSummaries)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        val testObserver = subject.fetchTransactions(itemAccount, 0, 0).test()
        // Assert
        verify(payloadManager).getAllTransactions(0, 0)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchTransactionsAccountTagImported() {
        // Arrange
        val account = Account()
        val summary = TransactionSummary()
        summary.confirmations = 3
        summary.direction = TransactionSummary.Direction.RECEIVED
        summary.fee = BigInteger.ONE
        summary.total = BigInteger.TEN
        summary.hash = "hash"
        summary.inputsMap = HashMap()
        summary.outputsMap = HashMap()
        summary.time = 1000000L
        val transactionSummaries = listOf(summary)
        `when`(payloadManager.getImportedAddressesTransactions(0, 0)).thenReturn(transactionSummaries)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_LEGACY
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        val testObserver = subject.fetchTransactions(itemAccount, 0, 0).test()
        // Assert
        verify(payloadManager).getImportedAddressesTransactions(0, 0)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchTransactionsAccount() {
        // Arrange
        val account = Account()
        val xPub =
            "xpub6CfLQa8fLgtp8E7tc1khAhrZYPm82okmugxP7TrhMPkPFKANhdCU" +
                    "d4TDJKUYLCxZskG2U7Q689CVBxs2EjJA7dyvjCzN5UYWwZbY2qVpymw"
        val summary = TransactionSummary()
        summary.confirmations = 3
        summary.direction = TransactionSummary.Direction.RECEIVED
        summary.fee = BigInteger.ONE
        summary.total = BigInteger.TEN
        summary.hash = "hash"
        summary.inputsMap = HashMap()
        summary.outputsMap = HashMap()
        summary.time = 1000000L
        val transactionSummaries = listOf(summary)
        `when`(payloadManager.getAccountTransactions(xPub, 0, 0)).thenReturn(transactionSummaries)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.SINGLE_ACCOUNT
        itemAccount.address = xPub
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        val testObserver = subject.fetchTransactions(itemAccount, 0, 0).test()
        // Assert
        verify(payloadManager).getAccountTransactions(xPub, 0, 0)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun fetchTransactionsAccountNoXpub() {
        // Arrange
        val account = Account()
        val xPub = "invalid xpub"
        val summary = TransactionSummary()
        summary.confirmations = 3
        summary.direction = TransactionSummary.Direction.RECEIVED
        summary.fee = BigInteger.ONE
        summary.total = BigInteger.TEN
        summary.hash = "hash"
        summary.inputsMap = HashMap()
        summary.outputsMap = HashMap()
        summary.time = 1000000L
        val transactionSummaries = listOf(summary)
        `when`(payloadManager.getImportedAddressesTransactions(0, 0)).thenReturn(transactionSummaries)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_LEGACY
        itemAccount.address = xPub
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        val testObserver = subject.fetchTransactions(itemAccount, 0, 0).test()
        // Assert
        verify(payloadManager).getImportedAddressesTransactions(0, 0)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun fetchTransactionsEthereum() {
        // Arrange
        val latestBlock = mock(EthLatestBlock::class.java)
        val transaction = mock(EthTransaction::class.java)
        `when`(transaction.hash).thenReturn("hash")
        val ethModel = mock(CombinedEthModel::class.java)
        `when`(ethDataManager.getLatestBlock()).thenReturn(Observable.just(latestBlock))
        `when`(ethDataManager.getEthTransactions()).thenReturn(Observable.just(transaction))
        `when`<CombinedEthModel>(ethDataManager.getEthResponseModel()).thenReturn(ethModel)
        `when`(ethDataManager.getErc20TokenData(CryptoCurrency.PAX)).thenReturn(Erc20TokenData.createPaxTokenData(""))
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        val itemAccount = ItemAccount()
        itemAccount.type = ItemAccount.TYPE.SINGLE_ACCOUNT
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        // Act
        val testObserver = subject.fetchTransactions(itemAccount, 0, 0).test()
        // Assert
        verify(ethDataManager).getLatestBlock()
        verify(ethDataManager).getEthTransactions()
        verify(ethDataManager).getEthResponseModel()
        verify(ethDataManager).getErc20TokenData(CryptoCurrency.PAX)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun getTransactionList() {
        // Arrange

        // Act
        val value = subject.getTransactionList()
        // Assert
        assertEquals(transactionListStore.list, value)
        assertEquals(emptyList<Any>(), value)
    }

    @Test
    fun clearTransactionList() {
        // Arrange
        transactionListStore.list.add(mock(Displayable::class.java))
        // Act
        subject.clearTransactionList()
        // Assert
        assertEquals(emptyList<Any>(), subject.getTransactionList())
    }

    @Test
    fun insertTransactionIntoListAndReturnSorted() {
        // Arrange
        val tx0 = mock(BtcDisplayable::class.java)
        `when`(tx0.timeStamp).thenReturn(0L)
        val tx1 = mock(BtcDisplayable::class.java)
        `when`(tx1.timeStamp).thenReturn(500L)
        val tx2 = mock(BtcDisplayable::class.java)
        `when`(tx2.timeStamp).thenReturn(1000L)
        transactionListStore.insertTransactions(Arrays.asList<Displayable>(tx1, tx0))
        // Act
        val value = subject.insertTransactionIntoListAndReturnSorted(tx2)
        // Assert
        assertNotNull(value)
        assertEquals(tx2, value[0])
        assertEquals(tx1, value[1])
        assertEquals(tx0, value[2])
    }

    @Test
    fun getBtcBalanceAccountTagAll() {
        // Arrange
        val account = Account()
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(payloadManager.walletBalance).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
        // Act
        val value = subject.getBtcBalance(itemAccount)
        // Assert
        verify(payloadManager).walletBalance
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBtcBalanceAccountTagImported() {
        // Arrange
        val account = Account()
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(payloadManager.importedAddressesBalance).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_LEGACY
        // Act
        val value = subject.getBtcBalance(itemAccount)
        // Assert
        verify(payloadManager).importedAddressesBalance
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBtcBalanceAccount() {
        // Arrange
        val account = Account()
        val xPub = "X_PUB"
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(payloadManager.getAddressBalance(xPub)).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.address = xPub
        // Act
        val value = subject.getBtcBalance(itemAccount)
        // Assert
        verify(payloadManager).getAddressBalance(xPub)
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBtcBalanceLegacyAddress() {
        // Arrange
        val legacyAddress = LegacyAddress()
        val address = "ADDRESS"
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(payloadManager.getAddressBalance(address)).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = legacyAddress
        itemAccount.address = address
        // Act
        val value = subject.getBtcBalance(itemAccount)
        // Assert
        verify(payloadManager).getAddressBalance(address)
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBchBalanceAccountTagAll() {
        // Arrange
        val account = Account()
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(bchDataManager.getWalletBalance()).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
        // Act
        val value = subject.getBchBalance(itemAccount)
        // Assert
        verify(bchDataManager).getWalletBalance()
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBchBalanceAccountTagImported() {
        // Arrange
        val account = Account()
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(bchDataManager.getImportedAddressBalance()).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.type = ItemAccount.TYPE.ALL_LEGACY
        // Act
        val value = subject.getBchBalance(itemAccount)
        // Assert
        verify(bchDataManager).getImportedAddressBalance()
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBchBalanceAccount() {
        // Arrange
        val account = Account()
        val xPub = "X_PUB"
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(bchDataManager.getAddressBalance(xPub)).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = account
        itemAccount.address = xPub
        // Act
        val value = subject.getBchBalance(itemAccount)
        // Assert
        verify(bchDataManager).getAddressBalance(xPub)
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getBchBalanceLegacyAddress() {
        // Arrange
        val legacyAddress = LegacyAddress()
        val address = "ADDRESS"
        val balance = BigInteger.valueOf(1_000_000_000_000L)
        `when`(bchDataManager.getAddressBalance(address)).thenReturn(balance)
        val itemAccount = ItemAccount()
        itemAccount.accountObject = legacyAddress
        itemAccount.address = address
        // Act
        val value = subject.getBchBalance(itemAccount)
        // Assert
        verify(bchDataManager).getAddressBalance(address)
        assertEquals(1_000_000_000_000L, value)
    }

    @Test
    fun getTxFromHashFound() {
        // Arrange
        val txHash = "TX_HASH"
        val tx0 = mock(BtcDisplayable::class.java)
        `when`(tx0.hash).thenReturn("")
        val tx1 = mock(BtcDisplayable::class.java)
        `when`(tx1.hash).thenReturn("")
        val tx2 = mock(BtcDisplayable::class.java)
        `when`(tx2.hash).thenReturn(txHash)
        transactionListStore.insertTransactions(Arrays.asList<Displayable>(tx0, tx1, tx2))
        // Act
        val testObserver = subject.getTxFromHash(txHash).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(tx2)
    }

    @Test
    fun getTxFromHashNotFound() {
        // Arrange
        val txHash = "TX_HASH"
        val tx0 = mock(BtcDisplayable::class.java)
        `when`(tx0.hash).thenReturn("")
        val tx1 = mock(BtcDisplayable::class.java)
        `when`(tx1.hash).thenReturn("")
        val tx2 = mock(BtcDisplayable::class.java)
        `when`(tx2.hash).thenReturn("")
        transactionListStore.insertTransactions(Arrays.asList<Displayable>(tx0, tx1, tx2))
        // Act
        val testObserver = subject.getTxFromHash(txHash).test()
        // Assert
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(NoSuchElementException::class.java)
    }

    @Test
    fun getTxConfirmationsMap() {
        // Arrange

        // Act
        val result = subject.getTxConfirmationsMap()
        // Assert
        assertNotNull(result)
    }

    @Test
    fun getXlmTransactionList() {
        // Arrange
        `when`(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.XLM)
        val output = BigInteger.valueOf(1000000L)
        val xlmTransaction = XlmTransaction(
            "2018-10-11T12:54:15Z",
            CryptoValue.lumensFromStroop(output),
            CryptoValue.lumensFromStroop(BigInteger.valueOf(100)),
            "hash",
            HorizonKeyPair.Public("GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR"),
            HorizonKeyPair.Public("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        )
        `when`(xlmDataManager.getTransactionList())
            .thenReturn(Single.just(listOf(xlmTransaction)))
        // Act
        val testObserver = subject.fetchTransactions(ItemAccount(
            "XLM",
            "1.0 XLM",
            null,
            1L, null,
            "AccountID"
        ), 50, 0).test()
        // Assert
        verify(xlmDataManager).getTransactionList()
        val displayables = testObserver.values()[0]
        assertEquals(1, displayables.size.toLong())
        val displayable = displayables[0]
        assertEquals(CryptoCurrency.XLM, displayable.cryptoCurrency)
        assertEquals("hash", displayable.hash)
        assertEquals(TransactionSummary.Direction.RECEIVED, displayable.direction)
        assertEquals(1, displayable.confirmations.toLong())
        assertFalse(displayable.isFeeTransaction)
        assertEquals(output, displayable.total)
        assertEquals(mapOf("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4" to BigInteger.ZERO),
            displayable.inputsMap)
        assertEquals(mapOf("GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR" to output),
            displayable.outputsMap)
    }

    @Test
    fun getErc20TransactionsList() {
        // Arrange
        val erc20Transfer = Erc20Transfer(
            logIndex = "132",
            from = "0x4058a004dd718babab47e14dd0d744742e5b9903",
            to = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29",
            value = 10000.toBigInteger(),
            transactionHash = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff",
            blockNumber = 7721219.toBigInteger(),
            timestamp = 1557334297
        )
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
        whenever(paxAccount.getTransactions()).thenReturn(Observable.just(
            listOf(
                erc20Transfer
            )
        ))
        whenever(ethDataManager
            .getTransaction("0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff"))
            .thenReturn(Observable.just(EthTransaction().apply {
                gasPrice = 100.toBigInteger()
                gasUsed = 2.toBigInteger()
            }))
        whenever(paxAccount.getAccountHash())
            .thenReturn(Observable.just("0x4058a004dd718babab47e14dd0d744742e5b9903"))
        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Observable.just(EthLatestBlockNumber().apply {
                number = erc20Transfer.blockNumber.plus(3.toBigInteger())
            }))

        val testObserver = subject.fetchTransactions(ItemAccount(
            "PAX",
            "1.0 PAX",
            null,
            1L, null,
            "AccountID"
        ), 50, 0).test()
        verify(paxAccount).getTransactions()
        testObserver.assertValueCount(1)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.size == 1 &&
                    it[0] is Erc20Displayable &&
                    it[0].cryptoCurrency == CryptoCurrency.PAX &&
                    !it[0].doubleSpend &&
                    !it[0].isFeeTransaction &&
                    it[0].confirmations == 3 &&
                    it[0].timeStamp == 1557334297L &&
                    it[0].direction == TransactionSummary.Direction.SENT &&
                    it[0].hash == "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff" &&
                    it[0].confirmations == 3 &&
                    it[0].total == 10000.toBigInteger() &&
                    it[0].inputsMap["0x4058a004dd718babab47e14dd0d744742e5b9903"] == 10000.toBigInteger() &&
                    it[0].outputsMap["0x2ca28ffadd20474ffe2705580279a1e67cd10a29"] == 10000.toBigInteger()
        }
    }

    @Test
    fun getEthTransactionsListWithOneErc20FeeTransactionInTheList() {
        // Arrange
        val ethTransaction = EthTransaction().apply {
            to = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        }

        whenever(ethDataManager.getLatestBlock()).thenReturn(Observable.just(EthLatestBlock()))
        whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX)).thenReturn(Erc20TokenData.createPaxTokenData(""))
        whenever(ethDataManager.getEthResponseModel()).thenReturn(mock())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        whenever(ethDataManager.getEthTransactions()).thenReturn(Observable.just(ethTransaction))

        val testObserver = subject.fetchTransactions(ItemAccount(
            "ETH",
            "1.0 ETH",
            null,
            1L, null,
            "AccountID"
        ), 50, 0).test()
        verify(ethDataManager).getEthTransactions()
        testObserver.assertValueCount(1)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.size == 1 &&
                    it[0].isFeeTransaction
        }
        verify(ethDataManager).getErc20TokenData(CryptoCurrency.PAX)
    }

    @Test
    fun getEthTransactionsListWithNoErc20FeeTransactionInTheList() {
        // Arrange
        val ethTransaction = EthTransaction().apply {
            to = "0x8E870D234660D95d5be530380D0eC0bd388289E1"
        }

        whenever(ethDataManager.getLatestBlock()).thenReturn(Observable.just(EthLatestBlock()))
        whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX)).thenReturn(Erc20TokenData.createPaxTokenData(""))
        whenever(ethDataManager.getEthResponseModel()).thenReturn(mock())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        whenever(ethDataManager.getEthTransactions()).thenReturn(Observable.just(ethTransaction))

        val testObserver = subject.fetchTransactions(ItemAccount(
            "ETH",
            "1.0 ETH",
            null,
            1L, null,
            "AccountID"
        ), 50, 0).test()
        verify(ethDataManager).getEthTransactions()
        testObserver.assertValueCount(1)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.size == 1 &&
                    !it[0].isFeeTransaction
        }
        verify(ethDataManager).getErc20TokenData(CryptoCurrency.PAX)
    }

    @Test
    fun emptyListShouldBeReturnedOnError() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
        whenever(paxAccount.getTransactions())
            .thenReturn(Observable.error<List<Erc20Transfer>>(Throwable()))
        whenever(ethDataManager.getTransaction("0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121" +
                "b71addb7d06a9e1e305ae8ff"))
            .thenReturn(Observable.just(EthTransaction().apply {
                gasPrice = 100.toBigInteger()
                gasUsed = 2.toBigInteger()
            }))
        whenever(paxAccount.getAccountHash())
            .thenReturn(Observable.just("0x4058a004dd718babab47e14dd0d744742e5b9903"))
        whenever(ethDataManager.getLatestBlockNumber()).thenReturn(Observable.just(EthLatestBlockNumber().apply {
            number = 1000.toBigInteger()
        }))

        val testObserver = subject.fetchTransactions(ItemAccount(
            "PAX",
            "1.0 PAX",
            null,
            1L, null,
            "AccountID"
        ), 50, 0).test()
        verify(paxAccount).getTransactions()
        testObserver.assertValueCount(1)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isEmpty()
        }
    }
}