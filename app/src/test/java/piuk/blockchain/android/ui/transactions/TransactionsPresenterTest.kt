package piuk.blockchain.android.ui.transactions

import com.blockchain.android.testutils.rxInit
import com.blockchain.notifications.models.NotificationPayload
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.Environment
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.activity.TransactionNoteUpdater
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

class TransactionsPresenterTest {

    private lateinit var subject: TransactionsPresenter
    private val view: TransactionsView = mock()

    private val exchangeRateDataManager: ExchangeRateDataManager = mock()
    private val assetSelect: AssetTokenLookup = mock()
    private val ethDataManager: EthDataManager = mock()
    private val paxAccount: Erc20Account = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val rxBus: RxBus = mock()
    private val currencyState: CurrencyState = mock()
    private val bchDataManager: BchDataManager = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val transactionNotes: TransactionNoteUpdater = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {

        subject = TransactionsPresenter(
            exchangeRateDataManager = exchangeRateDataManager,
            assetSelect = assetSelect,
            ethDataManager = ethDataManager,
            paxAccount = paxAccount,
            payloadDataManager = payloadDataManager,
            buyDataManager = buyDataManager,
            rxBus = rxBus,
            currencyState = currencyState,
            bchDataManager = bchDataManager,
            walletAccountHelper = walletAccountHelper,
            environmentSettings = environmentSettings,
            transactionNotes = transactionNotes // Move to asset token/coincore
        )

        whenever(rxBus.register(NotificationPayload::class.java)).thenReturn(Observable.empty())
        whenever(rxBus.register(AuthEvent::class.java)).thenReturn(Observable.empty())

        subject.attachView(view)
    }

    @Test
    fun onViewReady() {
        //  Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)

        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(Single.just(mutableListOf(account)))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        //  Act
        subject.onViewResumed()

        //  Assert
        verify(view).setupAccountsAdapter(mutableListOf(account))
        verify(view).setupTxFeedAdapter(true)
    }

    @Test
    fun onViewDestroyed() {
        //  Arrange
        val notificationObservable = Observable.just(NotificationPayload(emptyMap()))
        val authEventObservable = Observable.just(AuthEvent.LOGOUT)
        subject.notificationObservable = notificationObservable
        subject.authEventObservable = authEventObservable

        //  Act
        subject.onViewPaused()

        //  Assert
        verify(rxBus).unregister(NotificationPayload::class.java, notificationObservable)
        verify(rxBus).unregister(AuthEvent::class.java, authEventObservable)
    }

//     @Test
//     fun requestRefresh() {
//         //  Arrange
// 
//         //  getCurrentAccount()
//         whenever(view.getCurrentAccountPosition()).thenReturn(0)
//         val account: ItemAccount = mock()
//         whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(Single.just(mutableListOf(account)))
//         whenever(account.displayBalance).thenReturn("0.052 BTC")
// 
//         whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
//         whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
//         whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.empty())
//         whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//         whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
//         whenever(transactionListDataManager.fetchTransactions(any(), any(), any()))
//             .thenReturn(Observable.empty())
// 
//         whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
// 
//         whenever(swipeToReceiveHelper.storeAll())
//             .thenReturn(Completable.complete())
//         //  Act
//         subject.requestRefresh()
// 
//         //  Assert
//         verify(view).setUiState(UiState.LOADING)
//         verify(view).updateSelectedCurrency(CryptoCurrency.BTC)
//         verify(view).updateBalanceHeader("0.052 BTC")
//         verify(view).updateAccountsDataSet(mutableListOf(account))
//         verify(view).generateLauncherShortcuts()
//         verify(view).updateTransactionValueType(true)
//     }
// 

    @Test
    fun `onGetBitcoinClicked canBuy returns true`() {
        //  Arrange
        whenever(buyDataManager.canBuy).thenReturn(Single.just(true))

        //  Act
        subject.onGetBitcoinClicked()

        //  Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startBuyActivity()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onGetBitcoinClicked canBuy returns false`() {
        //  Arrange
        whenever(buyDataManager.canBuy).thenReturn(Single.just(false))

        //  Act
        subject.onGetBitcoinClicked()

        //  Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }
}
