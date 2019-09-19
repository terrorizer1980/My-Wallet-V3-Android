package piuk.blockchain.android

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import com.blockchain.koin.KoinStarter
import com.blockchain.logging.CrashLogger
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import org.bitcoinj.core.NetworkParameters
import piuk.blockchain.android.data.connectivity.ConnectivityManager
import com.blockchain.ui.CurrentContextAccess
import com.facebook.stetho.Stetho
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.auth.LogoutActivity
import piuk.blockchain.android.ui.ssl.SSLVerifyActivity
import piuk.blockchain.android.util.lifecycle.AppLifecycleListener
import piuk.blockchain.android.util.lifecycle.LifecycleInterestedComponent
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.connectivity.ConnectionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.annotations.Thunk
import piuk.blockchain.androidcoreui.ApplicationLifeCycle
import piuk.blockchain.androidcoreui.BuildConfig
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.AppLaunchEvent
import piuk.blockchain.androidcoreui.utils.logging.Logging
import retrofit2.Retrofit
import timber.log.Timber

open class BlockchainApplication : Application(), FrameworkInterface {

    private val retrofitApi: Retrofit by inject("api")
    private val retrofitExplorer: Retrofit by inject("explorer")
    private val environmentSettings: EnvironmentConfig by inject()
    private val loginState: AccessState by inject()
    private val lifeCycleInterestedComponent: LifecycleInterestedComponent by inject()
    private val rxBus: RxBus by inject()
    private val currentContextAccess: CurrentContextAccess by inject()
    private val appUtils: AppUtil by inject()
    private val crashLogger: CrashLogger by inject()

    private val lifecycleListener: AppLifecycleListener by lazy {
        AppLifecycleListener(lifeCycleInterestedComponent)
    }

    override fun onCreate() {

        if (MissingSplitsManagerFactory.create(this).disableAppIfMissingRequiredSplits()) {
            // Skip rest of the initialization to prevent the app from crashing.
            return
        }

        super.onCreate()

        // Init Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Build the DI graphs:
        KoinStarter.start(this)
        initLifecycleListener()
        crashLogger.init(this)
        crashLogger.userLanguageLocale(resources.configuration.locale.language)

        if (environmentSettings.shouldShowDebugMenu()) {
            Stetho.initializeWithDefaults(this)
        }

        // Pass objects to JAR - TODO: Remove this and use DI/Koin
        BlockchainFramework.init(this)

        UncaughtExceptionHandler.install(appUtils)

        RxJavaPlugins.setErrorHandler { throwable -> Timber.tag(RX_ERROR_TAG).e(throwable) }

        loginState.setLogoutActivity(LogoutActivity::class.java)

        // Apply PRNG fixes on app start if needed
        val prngUpdater: PrngFixer = get()
        prngUpdater.applyPRNGFixes()

        ApplicationLifeCycle.getInstance()
            .addListener(object : ApplicationLifeCycle.LifeCycleListener {
                override fun onBecameForeground() {
                    // Ensure that PRNG fixes are always current for the session
                    prngUpdater.applyPRNGFixes()
                }

                override fun onBecameBackground() {
                }
            })

        ConnectivityManager.getInstance().registerNetworkListener(this)

        checkSecurityProviderAndPatchIfNeeded()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        registerActivityLifecycleCallbacks(currentContextAccess.createCallBacks())

        // Report Google Play Services availability
        Logging.logCustom(AppLaunchEvent(isGooglePlayServicesAvailable(this)))

        // Set screen shots to enabled in staging builds, to simplify automation and QA processes
        val prefs: PersistentPrefs = get()
        prefs.setValue(
            PersistentPrefs.KEY_SCREENSHOTS_ENABLED,
            environmentSettings.shouldShowDebugMenu()
        )

        initRxBus()
    }

    @SuppressLint("CheckResult")
    private fun initRxBus() {
        rxBus.register(ConnectionEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { connectionEvent ->
                SSLVerifyActivity.start(
                    applicationContext,
                    connectionEvent
                )
            }
    }

    private fun initLifecycleListener() {
        ProcessLifecycleOwner.get().lifecycle
            .addObserver(lifecycleListener)
    }

    // FrameworkInterface
    // Pass instances to JAR Framework, evaluate after object graph instantiated fully
    override fun getRetrofitApiInstance(): Retrofit {
        return retrofitApi
    }

    override fun getRetrofitExplorerInstance(): Retrofit {
        return retrofitExplorer
    }

    override fun getEnvironment(): Environment {
        return environmentSettings.environment
    }

    override fun getBitcoinParams(): NetworkParameters {
        return environmentSettings.bitcoinNetworkParameters
    }

    override fun getBitcoinCashParams(): NetworkParameters {
        return environmentSettings.bitcoinCashNetworkParameters
    }

    override fun getDevice(): String {
        return "android"
    }

    override fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override val apiCode: String
        get() = "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3"

    /**
     * This patches a device's Security Provider asynchronously to help defend against various
     * vulnerabilities. This provider is normally updated in Google Play Services anyway, but this
     * will catch any immediate issues that haven't been fixed in a slow rollout.
     *
     * In the future, we may want to show some kind of warning to users or even stop the app, but
     * this will harm users with versions of Android without GMS approval.
     *
     * @see [Updating
     * Your Security Provider](https://developer.android.com/training/articles/security-gms-provider.html)
     */
    protected open fun checkSecurityProviderAndPatchIfNeeded() {
        ProviderInstaller.installIfNeededAsync(
            this,
            object : ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {
                    Timber.i("Security Provider installed")
                }

                override fun onProviderInstallFailed(errorCode: Int, intent: Intent) {
                    if (GoogleApiAvailability.getInstance().isUserResolvableError(errorCode)) {
                        showError(errorCode)
                    } else {
                        // Google Play services is not available.
                        onProviderInstallerNotAvailable()
                    }
                }
            })
    }

    /**
     * Show a dialog prompting the user to install/update/enable Google Play services.
     *
     * @param errorCode Recoverable error code
     */
    @Thunk
    internal fun showError(errorCode: Int) {
        // TODO: 05/08/2016 Decide if we should alert users here or not
        Timber.e(
            "Security Provider install failed with recoverable error: %s",
            GoogleApiAvailability.getInstance().getErrorString(errorCode)
        )
    }

    /**
     * This is reached if the provider cannot be updated for some reason. App should consider all
     * HTTP communication to be vulnerable, and take appropriate action.
     */
    @Thunk
    internal fun onProviderInstallerNotAvailable() {
        // TODO: 05/08/2016 Decide if we should take action here or not
        Timber.wtf("Security Provider Installer not available")
    }

    /**
     * Returns true if Google Play Services are found and ready to use.
     *
     * @param context The current Application Context
     */
    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        return availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    companion object {
        const val RX_ERROR_TAG = "RxJava Error"
    }
}
