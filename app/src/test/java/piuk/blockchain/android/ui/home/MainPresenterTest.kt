package piuk.blockchain.android.ui.home

import com.blockchain.android.testutils.rxInit
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.nabu.CurrentTier
import com.blockchain.sunriver.XlmDataManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.PayloadManagerWiper
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.PromptManager
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.utils.AppUtil

class MainPresenterTest {

    private lateinit var subject: MainPresenter

    private val view: MainView = mock()

    private val prefs: PersistentPrefs = mock()
    private val appUtil: AppUtil = mock()
    private val accessState: AccessState = mock()
    private val payloadManagerWiper: PayloadManagerWiper = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val dynamicFeeCache: DynamicFeeCache = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val rxBus: RxBus = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val promptManager: PromptManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val paxAccount: Erc20Account = mock()
    private val bchDataManager: BchDataManager = mock()
    private val currencyState: CurrencyState = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val exchangeService: ExchangeService = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val currentKycTier: CurrentTier = mock()
    private val lockboxDataManager: LockboxDataManager = mock()
    private val deepLinkProcessor: DeepLinkProcessor = mock()
    private val sunriverCampaignHelper: SunriverCampaignHelper = mock()
    private val xlmDataManager: XlmDataManager = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = MainPresenter(
            prefs,
            appUtil,
            accessState,
            payloadManagerWiper,
            payloadDataManager,
            settingsDataManager,
            buyDataManager,
            dynamicFeeCache,
            exchangeRateFactory,
            rxBus,
            feeDataManager,
            promptManager,
            ethDataManager,
            bchDataManager,
            currencyState,
            walletOptionsDataManager,
            metadataManager,
            stringUtils,
            shapeShiftDataManager,
            environmentSettings,
            coinifyDataManager,
            exchangeService,
            kycStatusHelper,
            currentKycTier,
            lockboxDataManager,
            deepLinkProcessor,
            sunriverCampaignHelper,
            xlmDataManager,
            paxAccount
        )

        subject.initView(view)
    }

    @Test
    fun `should go to kyc if tier is zero`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(currentKycTier.usersCurrentTier()).thenReturn(Single.just(0))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view, never()).launchSwap(any(), any())
        verify(view).launchKyc(CampaignType.Swap)
    }

    @Test
    fun `should go to swap if tier is equal to 1`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(currentKycTier.usersCurrentTier()).thenReturn(Single.just(1))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view).launchSwap("USD", CryptoCurrency.ETHER)
        verify(view, never()).launchKyc(CampaignType.Swap)
    }

    @Test
    fun `should go to swap if tier is higher that 1`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(currentKycTier.usersCurrentTier()).thenReturn(Single.just(2))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view).launchSwap("USD", CryptoCurrency.ETHER)
        verify(view, never()).launchKyc(CampaignType.Swap)
    }
}