package piuk.blockchain.android.coincore.old
//
//import com.blockchain.android.testutils.rxInit
//import com.nhaarman.mockito_kotlin.mock
//import info.blockchain.balance.CryptoCurrency
//import info.blockchain.wallet.multiaddress.TransactionSummary
//import info.blockchain.wallet.payload.PayloadManager
//import info.blockchain.wallet.payload.data.Account
//import info.blockchain.wallet.payload.data.LegacyAddress
//import io.reactivex.Observable
//import io.reactivex.plugins.RxJavaPlugins
//import io.reactivex.schedulers.TestScheduler
//import org.junit.Before
//import org.junit.Test
//import piuk.blockchain.android.ui.account.ItemAccount
//import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
//import piuk.blockchain.android.coincore.model.ActivitySummaryItem
//import java.math.BigInteger
//import java.util.NoSuchElementException
//import org.junit.Assert.assertEquals
//import org.junit.Rule
//import org.mockito.Mockito.mock
//import org.mockito.Mockito.verify
//import org.mockito.Mockito.`when`
//import piuk.blockchain.android.coincore.model.TransactionListStore
//
//class TransactionListDataManagerTest {
//
//    private val payloadManager: PayloadManager = mock()
//    private val bchDataManager: BchDataManager = mock()
//
//    private val transactionListStore = TransactionListStore()
//
//    private lateinit var subject: TransactionListDataManager
//
//    @get:Rule
//    val rxSchedulers = rxInit {
//        mainTrampoline()
//        ioTrampoline()
//        newThreadTrampoline()
//        singleTrampoline()
//        computation(testScheduler)
//        RxJavaPlugins.setErrorHandler { it.printStackTrace() }
//    }
//
//    private val testScheduler: TestScheduler = TestScheduler()
//
//    @Before
//    fun setUp() {
//        subject =
//            TransactionListDataManager(
//                transactionListStore = transactionListStore
//            )
//    }
//
//    @Test
//    fun clearTransactionList() {
//        // Arrange
//        transactionListStore.insertTransactions(listOf(mock(ActivitySummaryItem::class.java)))
//        // Act
//        subject.clearTransactionList()
//        // Assert
//        assertEquals(emptyList<Any>(), subject.getTransactionList())
//    }
//
//    @Test
//    fun getBtcBalanceAccountTagAll() {
//        // Arrange
//        val account = Account()
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(payloadManager.walletBalance).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = account
//        itemAccount.type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
//        // Act
//        val value = subject.getBtcBalance(itemAccount)
//        // Assert
//        verify(payloadManager).walletBalance
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBtcBalanceAccountTagImported() {
//        // Arrange
//        val account = Account()
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(payloadManager.importedAddressesBalance).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = account
//        itemAccount.type = ItemAccount.TYPE.ALL_LEGACY
//        // Act
//        val value = subject.getBtcBalance(itemAccount)
//        // Assert
//        verify(payloadManager).importedAddressesBalance
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBtcBalanceAccount() {
//        // Arrange
//        val account = Account()
//        val xPub = "X_PUB"
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(payloadManager.getAddressBalance(xPub)).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = account
//        itemAccount.address = xPub
//        // Act
//        val value = subject.getBtcBalance(itemAccount)
//        // Assert
//        verify(payloadManager).getAddressBalance(xPub)
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBtcBalanceLegacyAddress() {
//        // Arrange
//        val legacyAddress = LegacyAddress()
//        val address = "ADDRESS"
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(payloadManager.getAddressBalance(address)).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = legacyAddress
//        itemAccount.address = address
//        // Act
//        val value = subject.getBtcBalance(itemAccount)
//        // Assert
//        verify(payloadManager).getAddressBalance(address)
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBchBalanceAccountTagAll() {
//        // Arrange
//        val account = Account()
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(bchDataManager.getWalletBalance()).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = account
//        itemAccount.type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
//        // Act
//        val value = subject.getBchBalance(itemAccount)
//        // Assert
//        verify(bchDataManager).getWalletBalance()
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBchBalanceAccountTagImported() {
//        // Arrange
//        val account = Account()
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(bchDataManager.getImportedAddressBalance()).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = account
//        itemAccount.type = ItemAccount.TYPE.ALL_LEGACY
//        // Act
//        val value = subject.getBchBalance(itemAccount)
//        // Assert
//        verify(bchDataManager).getImportedAddressBalance()
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBchBalanceAccount() {
//        // Arrange
//        val account = Account()
//        val xPub = "X_PUB"
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(bchDataManager.getAddressBalance(xPub)).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = account
//        itemAccount.address = xPub
//        // Act
//        val value = subject.getBchBalance(itemAccount)
//        // Assert
//        verify(bchDataManager).getAddressBalance(xPub)
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    @Test
//    fun getBchBalanceLegacyAddress() {
//        // Arrange
//        val legacyAddress = LegacyAddress()
//        val address = "ADDRESS"
//        val balance = BigInteger.valueOf(1_000_000_000_000L)
//        `when`(bchDataManager.getAddressBalance(address)).thenReturn(balance)
//        val itemAccount = ItemAccount()
//        itemAccount.accountObject = legacyAddress
//        itemAccount.address = address
//        // Act
//        val value = subject.getBchBalance(itemAccount)
//        // Assert
//        verify(bchDataManager).getAddressBalance(address)
//        assertEquals(1_000_000_000_000L, value)
//    }
//
//    class TestActivitySummaryItem(
//        override val cryptoCurrency: CryptoCurrency = CryptoCurrency.BTC,
//        override val direction: TransactionSummary.Direction = TransactionSummary.Direction.RECEIVED,
//        override val timeStamp: Long = 0,
//        override val total: BigInteger = 0.toBigInteger(),
//        override val fee: Observable<BigInteger> = Observable.just(0.toBigInteger()),
//        override val hash: String = "",
//        override val inputsMap: Map<String, BigInteger> = emptyMap(),
//        override val outputsMap: Map<String, BigInteger> = emptyMap()
//    ) : ActivitySummaryItem()
//
//    @Test
//    fun getTxFromHashFound() {
//
//        val txHash = "TX_HASH"
//        val tx0 = TestActivitySummaryItem(hash = "")
//        val tx1 = TestActivitySummaryItem(hash = "")
//        val tx2 = TestActivitySummaryItem(hash = txHash)
//
//        transactionListStore.insertTransactions(listOf(tx0, tx1, tx2))
//
//        subject.getTxFromHash(txHash)
//            .test()
//            .assertComplete()
//            .assertNoErrors()
//            .assertValue(tx2)
//    }
//
//    @Test
//    fun getTxFromHashNotFound() {
//
//        val txHash = "TX_HASH"
//        val tx0 = TestActivitySummaryItem(hash = "")
//        val tx1 = TestActivitySummaryItem(hash = "")
//        val tx2 = TestActivitySummaryItem(hash = "")
//        transactionListStore.insertTransactions(listOf(tx0, tx1, tx2))
//
//        // Act
//        val testObserver = subject.getTxFromHash(txHash).test()
//        // Assert
//        testObserver.assertTerminated()
//        testObserver.assertNoValues()
//        testObserver.assertError(NoSuchElementException::class.java)
//    }
//
////    @Test
////    fun emptyListShouldBeReturnedOnError() {
////        // Arrange
////        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
////        whenever(paxAccount.getTransactions())
////            .thenReturn(Observable.error<List<Erc20Transfer>>(Throwable()))
////        whenever(ethDataManager.getTransaction("0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121" +
////                "b71addb7d06a9e1e305ae8ff"))
////            .thenReturn(Observable.just(EthTransaction().apply {
////                gasPrice = 100.toBigInteger()
////                gasUsed = 2.toBigInteger()
////            }))
////        whenever(paxAccount.getAccountHash())
////            .thenReturn(Observable.just("0x4058a004dd718babab47e14dd0d744742e5b9903"))
////        whenever(ethDataManager.getLatestBlockNumber()).thenReturn(Observable.just(EthLatestBlockNumber().apply {
////            number = 1000.toBigInteger()
////        }))
////
////        val testObserver = subject.fetchTransactions(ItemAccount(
////            "PAX",
////            "1.0 PAX",
////            null,
////            1L, null,
////            "AccountID"
////        ), 50, 0).test()
////        verify(paxAccount).getTransactions()
////        testObserver.assertValueCount(1)
////        testObserver.assertComplete()
////        testObserver.assertNoErrors()
////        testObserver.assertValue {
////            it.isEmpty()
////        }
////    }
//}