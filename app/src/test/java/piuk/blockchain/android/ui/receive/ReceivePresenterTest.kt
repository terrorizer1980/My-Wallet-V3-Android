package piuk.blockchain.android.ui.receive

import com.blockchain.android.testutils.rxInit
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.toUri
import com.blockchain.testutils.after
import com.blockchain.testutils.before
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.itReturns
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.ui.NotImplementedFrameworkInterface
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

class ReceivePresenterTest {

    private lateinit var subject: ReceivePresenter

    private val payloadDataManager: PayloadDataManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val prefsUtil: PersistentPrefs = mock()
    private val qrCodeDataManager: QrCodeDataManager = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val activity: ReceiveView = mock()
    private val ethDataStore: EthDataStore = mock()
    private val bchDataManager: BchDataManager = mock()
    private val xlmDataManager: XlmDataManager = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val currencyState: CurrencyState = mock {
        on { cryptoCurrency } itReturns CryptoCurrency.BTC
        on { fiatUnit } itReturns "USD"
    }
    private val exchangeRates: ExchangeRateDataManager = mock()

    @Before
    fun setUp() {
        initFramework()

        subject = ReceivePresenter(
            prefsUtil,
            qrCodeDataManager,
            walletAccountHelper,
            payloadDataManager,
            ethDataStore,
            bchDataManager,
            xlmDataManager,
            environmentSettings,
            currencyState,
            exchangeRates
        )
        subject.attachView(activity)
    }

    @get:Rule
    val locale = before {
        Locale.setDefault(Locale.US)
    } after {
        Locale.setDefault(Locale.US)
    }

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
    }

    @Test
    fun isValidAmount() {
        // Arrange
        val amount = "-1"
        // Act
        val result = subject.isValidAmount(amount)
        // Assert
        result `should be` false
    }

    @Test
    fun `shouldShowDropdown true`() {
        // Arrange
        whenever(walletAccountHelper.hasMultipleEntries(CryptoCurrency.BTC)) `it returns` true
        // Act
        subject.shouldShowAccountDropdown() `should be` true
    }

    @Test
    fun `shouldShowDropdown false`() {
        // Arrange
        whenever(walletAccountHelper.hasMultipleEntries(CryptoCurrency.BTC)) `it returns` false
        // Act
        subject.shouldShowAccountDropdown() `should be` false
    }

    @Test
    fun `onLegacyAddressSelected no label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val legacyAddress = LegacyAddress().apply { this.address = address }
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        // Act
        subject.onLegacyAddressSelected(legacyAddress)
        // Assert
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(address)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onLegacyAddressSelected with label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val legacyAddress = LegacyAddress().apply {
            this.address = address
            this.label = label
        }
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        // Act
        subject.onLegacyAddressSelected(legacyAddress)
        // Assert
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onLegacyAddressSelected BCH with no label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val legacyAddress = LegacyAddress().apply {
            this.address = address
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
        // Act
        subject.onLegacyBchAddressSelected(legacyAddress)
        // Assert
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(bech32Display)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress!! `should equal to` bech32Address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onLegacyAddressSelected BCH with label`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val label = "BCH LABEL"
        val legacyAddress = LegacyAddress().apply {
            this.address = address
            this.label = label
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
        // Act
        subject.onLegacyBchAddressSelected(legacyAddress)
        // Assert
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(label)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` bech32Address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onAccountSelected success`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(payloadDataManager.getNextReceiveAddress(account))
            .thenReturn(Observable.just(address))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        subject.onAccountBtcSelected(account)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.BTC)
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(payloadDataManager).getNextReceiveAddress(account)
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onAccountSelected address derivation failure`() {
        // Arrange
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(payloadDataManager.getNextReceiveAddress(account))
            .thenReturn(Observable.error { Throwable() })
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        subject.onAccountBtcSelected(account)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.BTC)
        verify(activity).showQrLoading()
        verify(activity).updateReceiveLabel(label)
        verify(activity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(activity)
        verify(payloadDataManager).updateAllTransactions()
        verify(payloadDataManager).getNextReceiveAddress(account)
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` null
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun onEthSelected() {
        // Arrange
        val ethAccount = "0x879dBFdE84B0239feB355f55F81fb29f898C778C"
        val combinedEthModel: CombinedEthModel = mock()
        val ethResponse: EthAddressResponse = mock()
        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getAddressResponse()).thenReturn(ethResponse)
        whenever(ethResponse.account).thenReturn(ethAccount)
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        // Act
        subject.onEthSelected()
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.ETHER)
        verify(activity).updateReceiveAddress(ethAccount)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.ETHER
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` ethAccount
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun onPaxSelected() {
        // Arrange
        val ethAccount = "0x879dBFdE84B0239feB355f55F81fb29f898C778C"
        val combinedEthModel: CombinedEthModel = mock()
        val ethResponse: EthAddressResponse = mock()

        whenever(ethDataStore.ethAddressResponse).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getAddressResponse()).thenReturn(ethResponse)
        whenever(ethResponse.account).thenReturn(ethAccount)
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.PAX)

        // Act
        subject.onPaxSelected()

        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.PAX)
        verify(activity).updateReceiveAddress(ethAccount)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.PAX
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should be` ethAccount
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun onXlmSelected() {
        // Arrange
        val accountReference = AccountReference.Xlm("", "GABC123")
        whenever(xlmDataManager.defaultAccount()) `it returns` Single.just(accountReference)
        whenever(qrCodeDataManager.generateQrCode(eq(accountReference.toUri()), anyInt())) `it returns`
            Observable.empty()
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.XLM)
        // Act
        subject.onXlmSelected()
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.XLM)
        verify(activity).updateReceiveAddress(accountReference.accountId)
        verify(activity).showQrLoading()
        verifyNoMoreInteractions(activity)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` accountReference.toUri()
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onBchAccountSelected success`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val xPub = "X_PUB"
        val label = "LABEL"
        val account = GenericMetadataAccount().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(bchDataManager.updateAllBalances())
            .thenReturn(Completable.complete())
        whenever(bchDataManager.getAccountMetadataList())
            .thenReturn(listOf(account))
        whenever(bchDataManager.getNextReceiveAddress(0))
            .thenReturn(Observable.just(address))
        whenever(bchDataManager.getWalletTransactions(50, 0))
            .thenReturn(Observable.just(emptyList()))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
        // Act
        subject.onBchAccountSelected(account)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.BCH)
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(bchDataManager).updateAllBalances()
        verify(bchDataManager).getAccountMetadataList()
        verify(bchDataManager).getNextReceiveAddress(0)
        verify(bchDataManager).getWalletTransactions(50, 0)
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.BCH
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` bech32Address
        subject.selectedBchAccount `should be` account
    }

    @Test
    fun `onSelectBchDefault success`() {
        // Arrange
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val bech32Address = "bitcoincash:qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val bech32Display = "qpna9wa3akewwj4umm0asx6jnt70hrdxpycrd7gy6u"
        val xPub = "X_PUB"
        val label = "LABEL"
        val account = GenericMetadataAccount().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(bchDataManager.getDefaultGenericMetadataAccount()).thenReturn(account)
        whenever(bchDataManager.updateAllBalances())
            .thenReturn(Completable.complete())
        whenever(bchDataManager.getAccountMetadataList())
            .thenReturn(listOf(account))
        whenever(bchDataManager.getNextReceiveAddress(0))
            .thenReturn(Observable.just(address))
        whenever(bchDataManager.getWalletTransactions(50, 0))
            .thenReturn(Observable.just(emptyList()))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
        // Act
        subject.onSelectBchDefault()
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.BCH)
        verify(activity).updateReceiveAddress(bech32Display)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(eq(bech32Address), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(bchDataManager).getDefaultGenericMetadataAccount()
        verify(bchDataManager).updateAllBalances()
        verify(bchDataManager).getAccountMetadataList()
        verify(bchDataManager).getNextReceiveAddress(0)
        verify(bchDataManager).getWalletTransactions(50, 0)
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.BCH
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` null
        subject.selectedAddress `should equal` bech32Address
        subject.selectedBchAccount `should be` account
    }

    @Test
    fun `onSelectDefault account valid account position`() {
        val accountPosition = 2
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(payloadDataManager.getAccount(accountPosition))
            .thenReturn(account)
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(payloadDataManager.getNextReceiveAddress(account))
            .thenReturn(Observable.just(address))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        subject.onSelectDefault(accountPosition)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.BTC)
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(payloadDataManager).getNextReceiveAddress(account)
        verify(payloadDataManager).getAccount(accountPosition)
        verify(payloadDataManager).updateAllTransactions()
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun `onSelectDefault account invalid account position`() {
        val accountPosition = -1
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        val label = "LABEL"
        val account = Account().apply { this.label = label }
        whenever(payloadDataManager.defaultAccount)
            .thenReturn(account)
        whenever(activity.getBtcAmount()).thenReturn("0")
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        whenever(payloadDataManager.getNextReceiveAddress(account))
            .thenReturn(Observable.just(address))
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        subject.onSelectDefault(accountPosition)
        // Assert
        verify(activity).setSelectedCurrency(CryptoCurrency.BTC)
        verify(activity).getBtcAmount()
        verify(activity).updateReceiveAddress(address)
        verify(activity).updateReceiveLabel(label)
        verify(activity, times(2)).showQrLoading()
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(payloadDataManager).getNextReceiveAddress(account)
        verify(payloadDataManager).updateAllTransactions()
        verify(payloadDataManager).defaultAccount
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState).cryptoCurrency = CryptoCurrency.BTC
        verify(currencyState).cryptoCurrency
        verifyNoMoreInteractions(currencyState)
        subject.selectedAccount `should be` account
        subject.selectedAddress `should be` address
        subject.selectedBchAccount `should be` null
    }

    @Test
    fun onBitcoinAmountChanged() {
        // Arrange
        val amount = "2100000000000000"
        val address = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        subject.selectedAddress = address
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.empty())
        // Act
        subject.onBitcoinAmountChanged(amount)
        // Assert
        verify(activity).showQrLoading()
        verify(activity).showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(activity)
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
    }

    @Test
    fun `getSelectedAccountPosition ETH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        val xPub = "X_PUB"
        val account = Account().apply { xpub = xPub }
        subject.selectedAccount = account
        whenever(payloadDataManager.accounts).thenReturn(listOf(account))
        whenever(payloadDataManager.getPositionOfAccountInActiveList(0))
            .thenReturn(10)
        // Act
        val result = subject.getSelectedAccountPosition()
        // Assert
        result `should equal to` 10
    }

    @Test
    fun `getSelectedAccountPosition BTC`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        // Act
        val result = subject.getSelectedAccountPosition()
        // Assert
        result `should equal to` -1
    }

    @Test
    fun setWarnWatchOnlySpend() {
        // Arrange

        // Act
        subject.setWarnWatchOnlySpend(true)
        // Assert
        verify(prefsUtil).setValue(ReceivePresenter.KEY_WARN_WATCH_ONLY_SPEND, true)
    }

    @Test
    fun `onShowBottomSheetSelected btc`() {
        // Arrange
        subject.selectedAddress = "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
        whenever(activity.getBtcAmount()).thenReturn("0")
//        whenever()
        // Act
        subject.onShowBottomShareSheetSelected()
        // Assert
        verify(activity).getBtcAmount()
        verify(activity).showShareBottomSheet(anyString())
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun `onShowBottomSheetSelected eth`() {
        // Arrange
        subject.selectedAddress = "0x879dBFdE84B0239feB355f55F81fb29f898C778C"
        // Act
        subject.onShowBottomShareSheetSelected()
        // Assert
        verify(activity).showShareBottomSheet(anyString())
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun `onShowBottomSheetSelected xlm`() {
        // Arrange
        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(BitcoinCashMainNetParams.get())
        val address = "GAX3ML5G7DLJBPVTNW7GR2Z2YCML2MOJTWNYXN44SVAPQQYMD6NF7DP2"
        subject.selectedAddress = address
        // Act
        subject.onShowBottomShareSheetSelected()
        // Assert
        verify(activity).showShareBottomSheet(address)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun `onShowBottomSheetSelected xlm full QR code uri`() {
        // Arrange
        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(
            BitcoinCashMainNetParams.get()
        )
        val uri = "web+stellar:pay?" +
            "destination=GAX3ML5G7DLJBPVTNW7GR2Z2YCML2MOJTWNYXN44SVAPQQYMD6NF7DP2&" +
            "amount=120.1234567&" +
            "memo=skdjfasf&" +
            "msg=pay%20me%20with%20lumens"
        subject.selectedAddress = uri
        // Act
        subject.onShowBottomShareSheetSelected()
        // Assert
        verify(activity).showShareBottomSheet(uri)
        verifyNoMoreInteractions(activity)
    }

    @Test(expected = IllegalStateException::class)
    fun `onShowBottomSheetSelected xlm invalid checksum`() {
        // Arrange
        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(
            BitcoinCashMainNetParams.get()
        )
        subject.selectedAddress = "GAX3ML5G7DLJBPVTNW7GR2Z2YCML2MOJTWNYXN44SVAPQQYMD6NF7DP3"
        // Act
        subject.onShowBottomShareSheetSelected()
    }

    @Test(expected = IllegalStateException::class)
    fun `onShowBottomSheetSelected xlm invalid checksum inside QR code uri`() {
        // Arrange
        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(
            BitcoinCashMainNetParams.get()
        )
        val uri = "web+stellar:pay?" +
            "destination=GAX3ML5G7DLJBPVTNW7GR2Z2YCML2MOJTWNYXN44SVAPQQYMD6NF7DP3&" +
            "amount=120.1234567&" +
            "memo=skdjfasf&" +
            "msg=pay%20me%20with%20lumens"
        subject.selectedAddress = uri
        // Act
        subject.onShowBottomShareSheetSelected()
    }

    @Test(expected = IllegalStateException::class)
    fun `onShowBottomSheetSelected unknown`() {
        // Arrange
        whenever(environmentSettings.bitcoinCashNetworkParameters).thenReturn(
            BitcoinCashMainNetParams.get()
        )
        subject.selectedAddress = "I am not a valid address"
        // Act
        subject.onShowBottomShareSheetSelected()
        // Assert
        verifyZeroInteractions(activity)
    }

    @Test
    fun updateBtcTextField() {
        // Arrange
        whenever(prefsUtil.selectedFiatCurrency) `it returns` "GBP"
        whenever(exchangeRates.getLastPrice(any(), any())).thenReturn(4.0)
        // Act
        subject.updateBtcTextField("2.0")
        // Assert
        verify(activity).updateBtcTextField("0.5")
        verifyNoMoreInteractions(activity)
    }

    private fun initFramework() {
        BlockchainFramework.init(NotImplementedFrameworkInterface)
    }
}
