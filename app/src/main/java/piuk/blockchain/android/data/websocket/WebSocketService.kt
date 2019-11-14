package piuk.blockchain.android.data.websocket

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.reactivex.disposables.CompositeDisposable
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.utils.AppUtil

class WebSocketService : Service() {

    private val binder = LocalBinder()
    private val payloadDataManager: PayloadDataManager by inject()
    private val bchDataManager: BchDataManager by inject()
    private val prefs: PersistentPrefs by inject()
    private val notificationManager: NotificationManager by inject()
    private val swipeToReceiveHelper: SwipeToReceiveHelper by inject()
    private val okHttpClient: OkHttpClient by inject()
    private val rxBus: RxBus by inject()
    private val accessState: AccessState by inject()
    private val appUtil: AppUtil by inject()

    private val currencyFormatManager: CurrencyFormatManager by inject()
    private val environmentConfig: EnvironmentConfig by inject()

    private var webSocketHandler: WebSocketHandler? = null

    private val xpubsBtc: List<String>
        get() {
            val nbAccounts: Int
            if (payloadDataManager.wallet?.isUpgraded == true) {
                nbAccounts = try {
                    payloadDataManager.wallet?.hdWallets?.get(0)?.accounts?.size ?: 0
                } catch (e: IndexOutOfBoundsException) {
                    0
                }

                val xpubs = mutableListOf<String>()
                for (i in 0 until nbAccounts) {
                    val xPub = payloadDataManager.wallet!!.hdWallets[0].accounts[i].xpub
                    if (xPub != null && xPub.isNotEmpty()) {
                        xpubs.add(xPub)
                    }
                }
                return xpubs
            } else {
                return emptyList()
            }
        }

    private val addressesBtc: List<String>
        get() {
            when {
                payloadDataManager.wallet != null -> {
                    val nbLegacy = payloadDataManager.wallet?.legacyAddressList?.size ?: 0
                    val addresses = mutableListOf<String>()
                    for (i in 0 until nbLegacy) {
                        val address = payloadDataManager.wallet?.legacyAddressList?.get(i)?.address
                        if (address?.isNotEmpty() == true) {
                            addresses.add(address)
                        }
                    }
                    return addresses
                }
                swipeToReceiveHelper.getBitcoinReceiveAddresses().isNotEmpty() -> {
                    val addresses = mutableListOf<String>()
                    val receiveAddresses =
                        swipeToReceiveHelper.getBitcoinReceiveAddresses()
                    receiveAddresses.forEach { address ->
                        addresses.add(address)
                    }
                    return addresses
                }
                else -> return emptyList()
            }
        }

    private val xpubsBch: List<String>
        get() {
            val nbAccounts: Int
            return if (payloadDataManager.wallet?.isUpgraded == true) {
                nbAccounts = bchDataManager.getActiveXpubs().size
                val xpubs = mutableListOf<String>()
                for (i in 0 until nbAccounts) {
                    val activeXpubs = bchDataManager.getActiveXpubs()
                    if (activeXpubs[i] != null && activeXpubs[i].isNotEmpty()) {
                        xpubs.add(bchDataManager.getActiveXpubs()[i])
                    }
                }
                xpubs
            } else {
                emptyList()
            }
        }

    private val addressesBch: List<String>
        get() {
            when {
                payloadDataManager.wallet != null -> {
                    val nbLegacy = bchDataManager.getLegacyAddressStringList().size
                    val addrs = mutableListOf<String>()
                    for (i in 0 until nbLegacy) {
<<<<<<< HEAD
                        val address = bchDataManager.getLegacyAddressStringList().get(i)
                        if (address.isNotEmpty()) {
=======
                        val address = bchDataManager.getLegacyAddressStringList()[i]
                        if (address.isEmpty().not()) {
>>>>>>> develop
                            addrs.add(address)
                        }
                    }

                    return addrs
                }
                swipeToReceiveHelper.getBitcoinCashReceiveAddresses().isNotEmpty() -> {
                    val addrs = mutableListOf<String>()
                    val receiveAddresses =
                        swipeToReceiveHelper.getBitcoinCashReceiveAddresses()
                    receiveAddresses.forEach { address ->
                        addrs.add(address)
                    }
                    return addrs
                }
                else -> return emptyList()
            }
        }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()

        webSocketHandler = WebSocketHandler(
            applicationContext,
            okHttpClient,
            payloadDataManager,
            bchDataManager,
            notificationManager,
            environmentConfig,
            currencyFormatManager,
            prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, ""),
            rxBus,
            accessState,
            appUtil).apply {
            start()
        }
    }

    override fun onDestroy() {
        if (webSocketHandler != null) webSocketHandler!!.stopPermanently()
        compositeDisposable.clear()
        super.onDestroy()
    }

    private inner class LocalBinder internal constructor() // Empty constructor
        : Binder() {

        // Necessary for implementing bound Android Service
        val service: WebSocketService
            get() = this@WebSocketService
    }
}