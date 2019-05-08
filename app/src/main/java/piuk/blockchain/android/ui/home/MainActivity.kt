package piuk.blockchain.android.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutManager
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.blockchain.annotations.ButWhy
import com.blockchain.annotations.CommonCode
import com.blockchain.koin.modules.MorphActivityLauncher
import com.blockchain.koin.modules.launchAsync
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.lockbox.ui.LockboxLandingActivity
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewNavHostActivity
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.PromptDlgFactory
import piuk.blockchain.android.databinding.ActivityMainBinding
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.AccountActivity
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.buy.BuyActivity
import piuk.blockchain.android.ui.buy.FrontendJavascript
import piuk.blockchain.android.ui.buy.FrontendJavascriptManager
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherActivity
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.dashboard.DashboardFragment
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.pairingcode.PairingCodeActivity
import piuk.blockchain.android.ui.receive.ReceiveFragment
import piuk.blockchain.android.ui.send.SendFragment
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.annotations.Thunk
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import timber.log.Timber

import javax.inject.Inject
import java.util.Arrays
import java.util.HashMap

class MainActivity
    : BaseMvpActivity<MainView, MainPresenter>(),
    HomeNavigator,
    MainView,
    ConfirmPaymentDialog.OnConfirmDialogInteractionListener,
    FrontendJavascript<String> {

    @Thunk
    var drawerOpen = false
        internal set

    private var handlingResult = false

    @Inject
    internal lateinit var mainPresenter: MainPresenter

    @Inject
    internal lateinit var appUtil: AppUtil

    @Inject
    internal lateinit var morphActivityLauncher: MorphActivityLauncher

    @Inject
    internal lateinit var eventLogger: EventLogger

    internal lateinit var binding: ActivityMainBinding

    private var progressDlg: MaterialProgressDialog? = null
    private var backPressed: Long = 0

    @Thunk
    internal var paymentMade = false
    private var frontendJavascriptManager: FrontendJavascriptManager? = null
    private var webViewLoginDetails: WebViewLoginDetails? = null

    private var initialized: Boolean = false

    // Fragment callbacks for currency header
    private val touchOutsideViews = HashMap<View, OnTouchOutsideViewListener>()

    private var balanceFragment: BalanceFragment = BalanceFragment.newInstance(false)

    private val tabSelectedListener =
        AHBottomNavigation.OnTabSelectedListener { position, wasSelected ->

            presenter.doTestnetCheck()

            if (!wasSelected) {
                when (position) {
                    ITEM_SEND -> if (currentFragment !is SendFragment) {
                        // This is a bit of a hack to allow the selection of the correct button
                        // On the bottom nav bar, but without starting the fragment again
                        startSendFragment(null)
                        ViewUtils.setElevation(binding.appbarLayout, 0f)
                    }
                    ITEM_HOME -> {
                        startDashboardFragment()
                        ViewUtils.setElevation(binding.appbarLayout, 4f)
                    }
                    ITEM_TRANSACTIONS -> {
                        startBalanceFragment(paymentMade)
                        ViewUtils.setElevation(binding.appbarLayout, 0f)
                    }
                    ITEM_RECEIVE -> {
                        startReceiveFragment()
                        ViewUtils.setElevation(binding.appbarLayout, 0f)
                    }
                }
            }
            true
        }

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.content_frame)

    internal val activity: Context
        @Thunk
        get() = this

    private val selectedAccountFromFragments: Int
        get() = if (currentFragment is ReceiveFragment) {
            (currentFragment as ReceiveFragment).getSelectedAccountPosition()
        } else {
            -1
        }

    private val menu: Menu
        get() = binding.navigationView.menu

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        receiver.registerIntents(this)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // No-op
            }

            override fun onDrawerOpened(drawerView: View) {
                drawerOpen = true
            }

            override fun onDrawerClosed(drawerView: View) {
                drawerOpen = false
            }

            override fun onDrawerStateChanged(newState: Int) {
                // No-op
            }
        })

        // Set up toolbar_constraint
        toolbar_general.navigationIcon = ContextCompat.getDrawable(this, R.drawable.vector_menu)
        toolbar_general.title = ""
        setSupportActionBar(toolbar_general)

        // Notify Presenter that page is setup
        onViewReady()

        // Add items
        bottom_navigation.addItems(
            Arrays.asList(
                AHBottomNavigationItem(R.string.send_bitcoin, R.drawable.vector_send, R.color.white),
                AHBottomNavigationItem(R.string.dashboard_title, R.drawable.vector_home, R.color.white),
                AHBottomNavigationItem(R.string.overview, R.drawable.vector_transactions, R.color.white),
                AHBottomNavigationItem(R.string.receive_bitcoin, R.drawable.vector_receive, R.color.white)
            )
        )

        // Styling
        bottom_navigation.apply {
            accentColor = ContextCompat.getColor(context, R.color.primary_blue_accent)
            inactiveColor = ContextCompat.getColor(context, R.color.primary_gray_dark)
            isForceTint = true
            setUseElevation(true)
            loadFont(context, CustomFont.MONTSERRAT_LIGHT) { t -> setTitleTypeface(t) }

            // Select Dashboard by default
            setOnTabSelectedListener(tabSelectedListener)
            currentItem = ITEM_HOME
        }

        processStartIntent()
    }

    @ButWhy("What does this really do? Who call it?")
    private fun processStartIntent() {
        with(intent) {
            if (hasExtra(EXTRA_URI)) {
                startPrepopulatedSend(
                    uri = getStringExtra(EXTRA_URI),
                    recipientId = getStringExtra(EXTRA_RECIPIENT_ID),
                    mdid = getStringExtra(EXTRA_MDID),
                    fctxId = getStringExtra(EXTRA_FCTX_ID)
                )
            }
        }
    }

    private fun startPrepopulatedSend(
        uri: String,
        recipientId: String,
        mdid: String,
        fctxId: String
    ) {
        setCurrentTabItem(ITEM_SEND)
        replaceContentFragment(SendFragment.newInstance(uri, recipientId, mdid, fctxId))
    }

    @SuppressLint("NewApi")
    override fun onResume() {
        super.onResume()
        // This can null out in low memory situations, so reset here
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
        appUtil.deleteQR()
        presenter.updateTicker()

        if (!handlingResult) {
            resetUi()
        }

        handlingResult = false
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_qr_main -> {
                requestScan()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingResult = true
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI &&
            data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null
        ) {
            val strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT)
            doScanInput(strResult)
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetUi()
        } else if (requestCode == SETTINGS_EDIT ||
            requestCode == ACCOUNT_EDIT ||
            requestCode == KYC_STARTED
        ) {
            // Re-init balance & dashboard fragment so that they reload all accounts/settings
            // incase of changes
            balanceFragment = BalanceFragment.newInstance(false)

            replaceContentFragment(DashboardFragment.newInstance())
            // Reset state incase of changing currency etc
            bottom_navigation.currentItem = ITEM_HOME

            // Pass this result to balance fragment
            for (fragment in supportFragmentManager.fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        val f = currentFragment
        val backHandled = when {
            drawerOpen -> {
                binding.drawerLayout.closeDrawers()
                true
            }
            f is BalanceFragment -> f.onBackPressed()
            f is SendFragment -> f.onBackPressed()
            f is ReceiveFragment -> f.onBackPressed()
            f is DashboardFragment -> f.onBackPressed()
            else -> {
                // Switch to balance fragment - it's not clear, though,
                // how we can ever wind up here...
                balanceFragment = BalanceFragment.newInstance(false)
                replaceContentFragment(balanceFragment)
                true
            }
        }

        if (!backHandled) {
            if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
                AccessState.getInstance().logout(this)
            } else {
                showExitConfirmToast()
                backPressed = System.currentTimeMillis()
            }
        }
    }

    private fun showExitConfirmToast() {
        ToastCustom.makeText(activity,
            getString(R.string.exit_confirm),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_GENERAL)
    }

    @Thunk
    internal fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this@MainActivity, CaptureActivity::class.java)
            startActivityForResult(intent, SCAN_URI)
        } else {
            ToastCustom.makeText(
                this@MainActivity,
                getString(R.string.camera_unavailable),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    private fun doScanInput(strResult: String) {
        if (FormatsUtil.isValidBitcoinAddress(strResult)) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.confirm_currency)
                .setMessage(R.string.confirm_currency_message)
                .setCancelable(true)
                .setPositiveButton(R.string.bitcoin_cash) { dialog, which ->
                    presenter.setCryptoCurrency(CryptoCurrency.BCH)
                    startSendFragment(strResult)
                }
                .setNegativeButton(R.string.bitcoin) { dialog, which ->
                    presenter.setCryptoCurrency(CryptoCurrency.BTC)
                    startSendFragment(strResult)
                }
                .create()
                .show()
        } else {
            startSendFragment(strResult)
        }
    }

    private fun selectDrawerItem(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.nav_lockbox -> LockboxLandingActivity.start(this)
            R.id.nav_backup -> startActivityForResult(
                Intent(this, BackupWalletActivity::class.java), REQUEST_BACKUP)
            R.id.nav_exchange -> morphActivityLauncher.launchAsync(this@MainActivity)
            R.id.nav_exchange_homebrew_debug -> HomebrewNavHostActivity.start(
                this,
                mainPresenter.defaultCurrency
            )
            R.id.nav_addresses -> startActivityForResult(
                Intent(this, AccountActivity::class.java), ACCOUNT_EDIT)
            R.id.nav_buy -> presenter.routeToBuySell()
            R.id.login_web_wallet -> PairingCodeActivity.start(this)
            R.id.nav_settings -> startActivityForResult(
                Intent(this, SettingsActivity::class.java), SETTINGS_EDIT)
            R.id.nav_support -> onSupportClicked()
            R.id.nav_logout -> showLogoutDialog()
        }
        binding.drawerLayout.closeDrawers()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.unpair_wallet)
            .setMessage(R.string.ask_you_sure_unpair)
            .setPositiveButton(R.string.unpair) { _, _ ->
                eventLogger.logEvent(LoggableEvent.Logout)
                presenter.unPair()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onSupportClicked() {
        eventLogger.logEvent(LoggableEvent.Support)
        calloutToExternalSupportLinkDlg(this, URL_BLOCKCHAIN_SUPPORT_PORTAL)
    }

    private fun resetUi() {
        toolbar_general.title = ""

        // Set selected appropriately.
        with(bottom_navigation) {
            when (currentFragment) {
                is DashboardFragment -> currentItem = ITEM_HOME
                is BalanceFragment -> currentItem = ITEM_TRANSACTIONS
                is SendFragment -> currentItem = ITEM_SEND
                is ReceiveFragment -> currentItem = ITEM_RECEIVE
            }
        }
    }

    @Thunk
    internal fun requestScan() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(binding.root, R.string.request_camera_permission)
            .withButton(android.R.string.ok) { v -> requestScan() }
            .build()

        val grantedPermissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                startScanActivity()
            }
        }

        val compositePermissionListener =
            CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(compositePermissionListener)
            .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
            .check()
    }

    private fun startSingleActivity(clazz: Class<*>) {
        val intent = Intent(this@MainActivity, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun showMetadataNodeFailure() {
        if (!isFinishing) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.metadata_load_failure)
                .setPositiveButton(R.string.retry) { _, _ -> presenter.initMetadataElements() }
                .setNegativeButton(R.string.exit) { _, _ -> AccessState.getInstance().logout(this) }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    override fun kickToLauncherPage() {
        startSingleActivity(LauncherActivity::class.java)
    }

    override fun launchKyc(campaignType: CampaignType) {
        startActivityForResult(KycNavHostActivity.intentArgs(this, campaignType), KYC_STARTED)
    }

    @ButWhy("What does this really do? Who calls it?")
    override fun refreshDashboard() {
        replaceContentFragment(DashboardFragment.newInstance())
    }

    @CommonCode("Move to base")
    override fun showProgressDialog(@StringRes message: Int) {
        hideProgressDialog()
        if (!isFinishing) {
            progressDlg = MaterialProgressDialog(this).apply {
                setCancelable(false)
                setMessage(message)
                show()
            }
        }
    }

    @CommonCode("Move to base")
    override fun hideProgressDialog() {
        if (!isFinishing && progressDlg != null) {
            progressDlg!!.dismiss()
            progressDlg = null
        }
    }

    override fun onScanInput(strUri: String) {
        doScanInput(strUri)
    }

    override fun getStartIntent(): Intent {
        return intent
    }

    override fun setBuySellEnabled(enabled: Boolean, useWebView: Boolean) {
        if (enabled && useWebView) {
            // For legacy SFOX + Unocoin only
            setupBuyWebView()
        }
        setBuyBitcoinVisible(enabled)
    }

    override fun showTradeCompleteMsg(txHash: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.trade_complete))
            .setMessage(R.string.trade_complete_details)
            .setCancelable(false)
            .setPositiveButton(R.string.ok_cap, null)
            .setNegativeButton(R.string.view_details) { _, _ ->
                startBalanceFragment(false)
                // Show transaction detail
                val bundle = Bundle()
                bundle.putString(BalanceFragment.KEY_TRANSACTION_HASH, txHash)
                TransactionDetailActivity.start(this, bundle)
            }.show()
    }

    private fun setBuyBitcoinVisible(visible: Boolean) {
        val menu = menu
        menu.findItem(R.id.nav_buy).isVisible = visible
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface")
    private fun setupBuyWebView() {
        if (AndroidUtils.is21orHigher()) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        }
        // Setup buy WebView
        val buyWebView = WebView(this).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(presenter.currentServerUrl)
        }

        frontendJavascriptManager = FrontendJavascriptManager(this, buyWebView)
        buyWebView.addJavascriptInterface(
            frontendJavascriptManager,
            FrontendJavascriptManager.JS_INTERFACE_NAME
        )
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun checkTradesIfReady() {
        if (initialized && webViewLoginDetails != null && AppUtil.isBuySellPermitted()) {
            frontendJavascriptManager!!.checkForCompletedTrades(webViewLoginDetails)
        }
    }

    override fun setWebViewLoginDetails(loginDetails: WebViewLoginDetails) {
        Timber.d("setWebViewLoginDetails: called")
        webViewLoginDetails = loginDetails
        checkTradesIfReady()
    }

    override fun onFrontendInitialized() {
        Timber.d("onFrontendInitialized: called")
        initialized = true
        checkTradesIfReady()
    }

    override fun onBuyCompleted() {
        // No-op
    }

    override fun onCompletedTrade(txHash: String) {
        /** Called from javascript in a webview at the end of a 'buy' operation, so ensure it runs
         * on the UI thread. see [piuk.blockchain.android.ui.buy.BuyActivity.java]
         */
        runOnUiThread { showTradeCompleteMsg(txHash) }
    }

    override fun onReceiveValue(value: String) {
        Timber.d("onReceiveValue: %s", value)
    }

    override fun onShowTx(txHash: String) {
        Timber.d("onShowTx: %s", txHash)
    }

    override fun clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java)!!.removeAllDynamicShortcuts()
        }
    }

    @ButWhy("Who calls this... it looks crazy")
    override fun onChangeFeeClicked() {
        val fragment = supportFragmentManager
            .findFragmentByTag(SendFragment::class.java.simpleName) as SendFragment
        fragment.onChangeFeeClicked()
    }

    @ButWhy("Who calls this")
    override fun onSendClicked() {
        val fragment = supportFragmentManager
            .findFragmentByTag(SendFragment::class.java.simpleName) as SendFragment
        fragment.onSendClicked()
    }

    override fun showTestnetWarning() {
        if (activity != null) {
            val snack = Snackbar.make(
                binding.coordinatorLayout,
                R.string.testnet_warning,
                Snackbar.LENGTH_SHORT
            )
            val view = snack.view
            view.setBackgroundColor(ContextCompat.getColor(activity, R.color.product_red_medium))
            snack.show()
        }
    }

    override fun showCustomPrompt(dlgFn: PromptDlgFactory) {
        if (!isFinishing) {
            dlgFn(this).apply {
                show(supportFragmentManager, tag)
            }
        }
    }

    override fun createPresenter(): MainPresenter? {
        return mainPresenter
    }

    override fun getView(): MainView {
        return this
    }

    override fun showSecondPasswordDialog() {
        val editText = AppCompatEditText(this)
        editText.setHint(R.string.password)
        editText.inputType =
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val frameLayout = ViewUtils.getAlertDialogPaddedView(this, editText)

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.eth_now_supporting)
            .setMessage(R.string.eth_second_password_prompt)
            .setView(frameLayout)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                ViewUtils.hideKeyboard(this)
                presenter.decryptAndSetupMetadata(editText.text.toString())
            }
            .create()
            .show()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun displayDialog(title: Int, message: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showExchange() {
        menu.findItem(R.id.nav_exchange).isVisible = true
    }

    override fun hideExchange() {
        menu.findItem(R.id.nav_exchange).isVisible = false
    }

    override fun displayLockbox(lockboxAvailable: Boolean) {
        menu.findItem(R.id.nav_lockbox).isVisible = lockboxAvailable
    }

    override fun showHomebrewDebug() {
        menu.findItem(R.id.nav_exchange_homebrew_debug).isVisible = true
    }

    fun setOnTouchOutsideViewListener(
        view: View,
        onTouchOutsideViewListener: OnTouchOutsideViewListener
    ) {
        touchOutsideViews[view] = onTouchOutsideViewListener
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // TODO: 16/02/2018 This is currently broken, revisit in the future
        //        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
        //            for (View view : touchOutsideViews.keySet()) {
        //                // Notify touchOutsideViewListeners if user tapped outside a given view
        //                Rect viewRect = new Rect();
        //                view.getGlobalVisibleRect(viewRect);
        //                if (!viewRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
        //                    touchOutsideViews.get(view).onTouchOutside(view, ev);
        //                }
        //            }
        //
        //        }
        return super.dispatchTouchEvent(ev)
    }

    override fun showNavigation() {
        bottom_navigation.restoreBottomNavigation()
        bottom_navigation.isBehaviorTranslationEnabled = true
    }

    override fun hideNavigation() {
        bottom_navigation.hideBottomNavigation()
        bottom_navigation.isBehaviorTranslationEnabled = false
    }

    override fun gotoSendFor(cryptoCurrency: CryptoCurrency) {
        presenter.setCryptoCurrency(cryptoCurrency)
        startSendFragment(null)
    }

    private fun startSendFragment(scanData: String?) {
        setCurrentTabItem(ITEM_SEND)

        ViewUtils.setElevation(binding.appbarLayout, 0f)

        val sendFragment = SendFragment.newInstance(scanData, selectedAccountFromFragments)
        replaceContentFragment(sendFragment)
    }

    override fun gotoReceiveFor(cryptoCurrency: CryptoCurrency) {
        presenter.setCryptoCurrency(cryptoCurrency)

        startReceiveFragment()
    }

    private fun startReceiveFragment() {
        val receiveFragment = ReceiveFragment.newInstance(selectedAccountFromFragments)
        replaceContentFragment(receiveFragment)
    }

    override fun gotoDashboard() {
        bottom_navigation.currentItem = ITEM_HOME
    }

    private fun startDashboardFragment() {
        val fragment = DashboardFragment.newInstance()
        replaceContentFragment(fragment)
    }

    override fun gotoTransactionsFor(cryptoCurrency: CryptoCurrency) {
        presenter.setCryptoCurrency(cryptoCurrency)
        // This forces the balance page to reload
        paymentMade = true
        bottom_navigation.currentItem = ITEM_TRANSACTIONS
    }

    override fun startBalanceFragment(paymentToContactMade: Boolean) {
        if (paymentToContactMade) {
            balanceFragment = BalanceFragment.newInstance(true)
            paymentMade = false
        }
        replaceContentFragment(balanceFragment)
        toolbar_general.title = ""

        balanceFragment.refreshSelectedCurrency()
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Can we lose this soon?")
    override fun onStartLegacyBuySell() {
        BuyActivity.start(this)
    }

    override fun onStartBuySell() {
        BuySellLauncherActivity.start(this)
    }

    private fun replaceContentFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment, fragment.javaClass.simpleName)
            .commitAllowingStateLoss()
    }

    /*** Silently switch the current tab in the tab_bar */
    private fun setCurrentTabItem(item: Int) {
        bottom_navigation.removeOnTabSelectedListener()
        bottom_navigation.currentItem = item
        bottom_navigation.setOnTabSelectedListener(tabSelectedListener)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == ACTION_SEND && activity != null) {
                requestScan()
            } else if (action == ACTION_RECEIVE && activity != null) {
                // Used from onboarding
                presenter.setCryptoCurrency(CryptoCurrency.BTC)
                bottom_navigation.currentItem = ITEM_RECEIVE
            } else if (action == ACTION_BUY && activity != null) {
                presenter.routeToBuySell()
            } else if (action == ACTION_EXCHANGE && activity != null) {
                morphActivityLauncher.launchAsync(this@MainActivity)
            } else if (action == ACTION_SUNRIVER_KYC && activity != null) {
                launchKyc(CampaignType.Sunriver)
            } else if (action == ACTION_EXCHANGE_KYC && activity != null) {
                launchKyc(CampaignType.Swap)
            } else if (action == ACTION_RESUBMIT_KYC && activity != null) {
                launchKyc(CampaignType.Resubmission)
            } else if (action == ACTION_BUY_SELL_KYC && activity != null) {
                launchKyc(CampaignType.BuySell)
            }
        }

        fun registerIntents(ctx: Context) {
            LocalBroadcastManager.getInstance(ctx).also {
                registerReceiver(this, IntentFilter(ACTION_SEND))
                registerReceiver(this, IntentFilter(ACTION_RECEIVE))
                registerReceiver(this, IntentFilter(ACTION_BUY))
                registerReceiver(this, IntentFilter(ACTION_EXCHANGE))
                registerReceiver(this, IntentFilter(ACTION_EXCHANGE_KYC))
                registerReceiver(this, IntentFilter(ACTION_SUNRIVER_KYC))
                registerReceiver(this, IntentFilter(ACTION_RESUBMIT_KYC))
                registerReceiver(this, IntentFilter(ACTION_BUY_SELL_KYC))
            }
        }
    }

    companion object {

        val TAG = MainActivity::class.java.simpleName!!

        const val ACTION_SEND = "info.blockchain.wallet.ui.BalanceFragment.SEND"
        const val ACTION_RECEIVE = "info.blockchain.wallet.ui.BalanceFragment.RECEIVE"
        const val ACTION_BUY = "info.blockchain.wallet.ui.BalanceFragment.BUY"
        const val ACTION_EXCHANGE = "info.blockchain.wallet.ui.BalanceFragment.ACTION_EXCHANGE"
        const val ACTION_BUY_SELL_KYC =
            "info.blockchain.wallet.ui.BalanceFragment.ACTION_BUY_SELL_KYC"
        const val ACTION_EXCHANGE_KYC =
            "info.blockchain.wallet.ui.BalanceFragment.ACTION_EXCHANGE_KYC"
        const val ACTION_SUNRIVER_KYC =
            "info.blockchain.wallet.ui.BalanceFragment.ACTION_SUNRIVER_KYC"
        const val ACTION_RESUBMIT_KYC =
            "info.blockchain.wallet.ui.BalanceFragment.ACTION_RESUBMIT_KYC"

        private const val REQUEST_BACKUP = 2225
        private const val COOL_DOWN_MILLIS = 2 * 1000

        const val EXTRA_URI = "transaction_uri"
        const val EXTRA_RECIPIENT_ID = "recipient_id"
        const val EXTRA_MDID = "mdid"
        const val EXTRA_FCTX_ID = "fctx_id"

        const val SCAN_URI = 2007
        const val ACCOUNT_EDIT = 2008
        const val SETTINGS_EDIT = 2009
        const val KYC_STARTED = 2011

        private const val ITEM_SEND = 0
        private const val ITEM_HOME = 1
        private const val ITEM_TRANSACTIONS = 2
        private const val ITEM_RECEIVE = 3

        fun start(context: Context, bundle: Bundle) {
            Intent(context, MainActivity::class.java).apply {
                putExtras(bundle)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(this)
            }
        }
    }
}
