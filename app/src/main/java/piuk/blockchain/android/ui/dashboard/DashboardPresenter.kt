package piuk.blockchain.android.ui.dashboard

import android.annotation.SuppressLint
import android.support.annotation.DrawableRes
import android.support.annotation.VisibleForTesting
import com.blockchain.balance.drawableResFilled
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.nabu.CurrentTier
import com.blockchain.sunriver.ui.BaseAirdropBottomDialog
import com.blockchain.sunriver.ui.ClaimFreeCryptoSuccessDialog
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.charts.models.ArbitraryPrecisionFiatValue
import piuk.blockchain.android.ui.charts.models.toStringWithSymbol
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.models.OnboardingModel
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.logging.BalanceLoadedEvent
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.text.DecimalFormat

class DashboardPresenter(
    private val dashboardBalanceCalculator: DashboardData,
    private val prefs: PersistentPrefs,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val stringUtils: StringUtils,
    private val accessState: AccessState,
    private val buyDataManager: BuyDataManager,
    private val rxBus: RxBus,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val currencyFormatManager: CurrencyFormatManager,
    private val lockboxDataManager: LockboxDataManager,
    private val currentTier: CurrentTier,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    private val announcements: AnnouncementList
) : BasePresenter<DashboardView>(), AnnouncementHost {

    private val currencies = DashboardConfig.currencies

    private val displayList by unsafeLazy {
        (listOf(
            stringUtils.getString(R.string.dashboard_balances),
            PieChartsState.Loading,
            stringUtils.getString(R.string.dashboard_price_charts)
        ) + currencies.map {
            AssetPriceCardState.Loading(it)
        }).toMutableList()
    }

    private val metadataObservable by unsafeLazy {
        rxBus.register(
            MetadataEvent::class.java
        )
    }

    private val balanceUpdateDisposable = CompositeDisposable()

    override fun onViewReady() {
        with(view) {
            notifyItemAdded(displayList, 0)
            scrollToTop()
        }
        updatePrices()

        val observable = when (firstRun) {
            true -> metadataObservable
            false -> Observable.just(MetadataEvent.SETUP_COMPLETE)
                .applySchedulers()
                // If data is present, update with cached data
                // Data updates run anyway but this makes the UI nicer to look at whilst loading
                .doOnNext {
                    cachedData?.run { view.updatePieChartState(this) }
                }
        }

        firstRun = false

        compositeDisposable += balanceUpdateDisposable

        // Triggers various updates to the page once all metadata is loaded
        compositeDisposable += observable.flatMap { getOnboardingStatusObservable() }
            // Clears subscription after single event
            .firstOrError()
            .doOnSuccess { updateAllBalances() }
            .doOnSuccess { announcements.checkLatest(this, compositeDisposable) }
            .doOnSuccess { storeSwipeToReceiveAddresses() }
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )

        // Pit linking - if we have a pit link id, and are therefore probably mid flow and returning from
        // email verification - go to the pit permissions page to continue linking
        val linkId = prefs.pitToWalletLinkId
        if (linkId.isNotEmpty()) {
            view.startPitLinkingFlow(linkId)
        }
    }

    private fun storeSwipeToReceiveAddresses() {
        compositeDisposable += swipeToReceiveHelper.storeAll()
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    override fun onViewDestroyed() {
        rxBus.unregister(MetadataEvent::class.java, metadataObservable)
        super.onViewDestroyed()
    }

    fun updateBalances() {

        with(view) {
            scrollToTop()
        }

        updatePrices()
        updateAllBalances()
    }

    @SuppressLint("CheckResult")
    private fun updatePrices() {
        exchangeRateFactory.updateTickers()
            .observeOn(AndroidSchedulers.mainThread())
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    handleAssetPriceUpdate(
                        currencies.filter { it.hasFeature(CryptoCurrency.PRICE_CHARTING) }
                            .map { AssetPriceCardState.Data(getPriceString(it), it, it.drawableResFilled()) })
                },
                {
                    handleAssetPriceUpdate(currencies.map { AssetPriceCardState.Error(it) })
                }
            )
    }

    private fun handleAssetPriceUpdate(list: List<AssetPriceCardState>) {
        displayList.removeAll { it is AssetPriceCardState }
        displayList.addAll(list)

        val firstPosition = displayList.indexOfFirst { it is AssetPriceCardState }

        val positions = (firstPosition until firstPosition + list.size).toList()

        view.notifyItemUpdated(displayList, positions)
    }

    private val balanceFilter = BehaviorSubject.create<BalanceFilter>().apply {
        onNext(BalanceFilter.Total)
    }

    fun setBalanceFilter(balanceFilter: BalanceFilter) {
        this.balanceFilter.onNext(balanceFilter)
    }

    private fun updateAllBalances() {
        balanceUpdateDisposable.clear()
        val data = dashboardBalanceCalculator.getPieChartData(balanceFilter.distinctUntilChanged())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    Logging.logCustom(
                        BalanceLoadedEvent(
                            hasBtcBalance = !it.bitcoin.displayable.isZero,
                            hasBchBalance = !it.bitcoinCash.displayable.isZero,
                            hasEthBalance = !it.ether.displayable.isZero,
                            hasXlmBalance = !it.lumen.displayable.isZero,
                            hasPaxBalance = !it.usdPax.displayable.isZero
                        )
                    )
                    cachedData = it
                }
            .doOnNext { view.startWebsocketService() }

        balanceUpdateDisposable += Observables.combineLatest(data, shouldDisplayLockboxMessage().cache().toObservable())
            .map { (data, hasLockbox) -> data.copy(hasLockbox = hasLockbox) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                view::updatePieChartState,
                Timber::e
            )
    }

    private fun shouldDisplayLockboxMessage(): Single<Boolean> = Single.zip(
        lockboxDataManager.isLockboxAvailable(),
        lockboxDataManager.hasLockbox(),
        BiFunction { available: Boolean, hasLockbox: Boolean -> available && hasLockbox }
    )

    override val disposables: CompositeDisposable = compositeDisposable

    override fun clearAllAnnouncements() {
        displayList.removeAll { it is AnnouncementCard }
    }

    override fun showAnnouncementCard(card: AnnouncementCard) {
        displayList.add(0, card)
        with(view) {
            notifyItemAdded(displayList, 0)
            scrollToTop()
        }
    }

    override fun showAnnouncmentPopup(popup: BaseAirdropBottomDialog) {
        view.showBottomSheetDialog(popup)
    }

    override fun dismissAnnouncementCard(prefsKey: String) {
        displayList.filterIsInstance<AnnouncementCard>()
            .forEachIndexed { index, any ->
                if (any.prefsKey == prefsKey) {
                    displayList.remove(any)
                    with(view) {
                        notifyItemRemoved(displayList, index)
                        scrollToTop()
                    }
                }
            }
    }

    override fun startKyc(campaignType: CampaignType) {
        view.startKycFlow(campaignType)
    }

    override fun startSwapOrKyc(swapTarget: CryptoCurrency?) {
        val currency = swapTarget ?: CryptoCurrency.ETHER

        compositeDisposable += currentTier.usersCurrentTier()
            .subscribe { tier ->
                if (tier > 0) {
                    view.goToExchange(currency, prefs.selectedFiatCurrency)
                } else {
                    view.startKycFlow(CampaignType.Swap)
                }
            }
    }

    override fun startPitLinking() {
        view.startPitLinkingFlow()
    }

    override fun signupToSunRiverCampaign() {
        compositeDisposable += sunriverCampaignHelper
            .registerSunRiverCampaign()
            .doOnError(Timber::e)
            .subscribeBy(onComplete = {
                showSignUpToSunRiverCampaignSuccessDialog()
            })
    }

    private fun showSignUpToSunRiverCampaignSuccessDialog() {
        view.showBottomSheetDialog(ClaimFreeCryptoSuccessDialog())
    }

    private fun getOnboardingStatusObservable(): Observable<Boolean> = if (isOnboardingComplete()) {
        Observable.just(false)
    } else {
        buyDataManager.canBuy
            .addToCompositeDisposable(this)
            .doOnNext { displayList.removeAll { it is OnboardingModel } }
            .doOnNext { displayList.add(0, getOnboardingPages(it)) }
            .doOnNext { view.notifyItemAdded(displayList, 0) }
            .doOnNext { view.scrollToTop() }
            .doOnError { Timber.e(it) }
    }

    private fun getOnboardingPages(isBuyAllowed: Boolean): OnboardingModel {
        val pages = mutableListOf<OnboardingPagerContent>()

        if (isBuyAllowed) {
            // Buy bitcoin prompt
            pages.add(
                OnboardingPagerContent(
                    stringUtils.getString(R.string.onboarding_current_price),
                    getFormattedPriceString(CryptoCurrency.BTC),
                    stringUtils.getString(R.string.onboarding_buy_content),
                    stringUtils.getString(R.string.onboarding_buy_bitcoin),
                    MainActivity.ACTION_BUY,
                    R.color.primary_blue_accent,
                    R.drawable.vector_buy_offset
                )
            )
        }
        // Receive bitcoin
        pages.add(
            OnboardingPagerContent(
                stringUtils.getString(R.string.onboarding_receive_bitcoin),
                "",
                stringUtils.getString(R.string.onboarding_receive_content),
                stringUtils.getString(R.string.request),
                MainActivity.ACTION_RECEIVE,
                R.color.secondary_teal_medium,
                R.drawable.vector_receive_offset
            )
        )
        // QR Codes
        pages.add(
            OnboardingPagerContent(
                stringUtils.getString(R.string.onboarding_qr_codes),
                "",
                stringUtils.getString(R.string.onboarding_qr_codes_content),
                stringUtils.getString(R.string.onboarding_scan_address),
                MainActivity.ACTION_SEND,
                R.color.primary_navy_medium,
                R.drawable.vector_qr_offset
            )
        )

        return OnboardingModel(
            pages,
            // TODO: These are neat and clever, but make things pretty hard to test. Replace with callbacks.
            dismissOnboarding = {
                setOnboardingComplete(true)
                displayList.removeAll { it is OnboardingModel }
                view.notifyItemRemoved(displayList, 0)
                view.scrollToTop()
            },
            onboardingComplete = { setOnboardingComplete(true) },
            onboardingNotComplete = { setOnboardingComplete(false) }
        )
    }

    private fun isOnboardingComplete() =
        // If wallet isn't newly created, don't show onboarding
        prefs.isOnboardingComplete || !accessState.isNewlyCreated

    private fun setOnboardingComplete(completed: Boolean) {
        prefs.isOnboardingComplete = completed
    }

    // /////////////////////////////////////////////////////////////////////////
    // Units
    // /////////////////////////////////////////////////////////////////////////

    private fun getFormattedPriceString(cryptoCurrency: CryptoCurrency): String {
        val lastPrice = getLastPrice(cryptoCurrency, getFiatCurrency())
        val fiatSymbol = currencyFormatManager.getFiatSymbol(getFiatCurrency())
        val format = DecimalFormat().apply { minimumFractionDigits = 2 }

        return stringUtils.getFormattedString(
            R.string.current_price_btc,
            "$fiatSymbol${format.format(lastPrice)}"
        )
    }

    private fun getPriceString(cryptoCurrency: CryptoCurrency): String {
        val fiat = getFiatCurrency()
        return getLastPrice(cryptoCurrency, fiat).run {
            ArbitraryPrecisionFiatValue.fromMajor(fiat, this.toBigDecimal())
                .toStringWithSymbol()
        }
    }

    private fun getFiatCurrency() = prefs.selectedFiatCurrency

    private fun getLastPrice(cryptoCurrency: CryptoCurrency, fiat: String) =
        exchangeRateFactory.getLastPrice(cryptoCurrency, fiat)

    companion object {

        /**
         * This field stores whether or not the presenter has been run for the first time across
         * all instances. This allows the page to load without a metadata set-up event, which won't
         * be present if the the page is being returned to.
         */
        @VisibleForTesting
        var firstRun = true

        /**
         * This is intended to be a temporary solution to caching data on this page. In future,
         * I intend to organise the MainActivity fragment backstack so that the DashboardFragment
         * is never killed intentionally. However, this could introduce a lot of bugs so this will
         * do for now.
         */
        private var cachedData: PieChartsState.Data? = null

        @JvmStatic
        fun onLogout() {
            firstRun = true
            cachedData = null
        }
    }
}

sealed class AssetPriceCardState(val currency: CryptoCurrency) {

    data class Data(
        val priceString: String,
        val cryptoCurrency: CryptoCurrency,
        @DrawableRes val icon: Int
    ) : AssetPriceCardState(cryptoCurrency)

    class Loading(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
    class Error(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
}
