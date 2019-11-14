package piuk.blockchain.android.data.websocket;

import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import com.blockchain.network.EnvironmentUrls;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.exceptions.DecryptionException;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.events.ActionEvent;
import piuk.blockchain.androidcore.data.events.WalletAndTransactionsUpdatedEvent;
import piuk.blockchain.androidcore.data.events.WebSocketMessageEvent;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.annotations.Thunk;
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AppUtil;
import timber.log.Timber;

@SuppressWarnings("WeakerAccess")
class WebSocketHandler {

    private final static long RETRY_INTERVAL = 5 * 1000L;
    /**
     * Websocket status code as defined by <a href="http://tools.ietf.org/html/rfc6455#section-7.4">Section
     * 7.4 of RFC 6455</a>
     */
    private static final int STATUS_CODE_NORMAL_CLOSURE = 1000;

    private boolean stoppedDeliberately = false;
    @Thunk
    BchDataManager bchDataManager;
    private NotificationManager notificationManager;
    private String guid;
    private HashSet<String> btcSubHashSet = new HashSet<>();
    private HashSet<String> btcOnChangeHashSet = new HashSet<>();
    private HashSet<String> bchSubHashSet = new HashSet<>();
    private EnvironmentUrls environmentUrls;
    private CurrencyFormatManager currencyFormatManager;
    private Context context;
    private OkHttpClient okHttpClient;
    private WebSocket btcConnection, bchConnection;
    boolean connected;
    @Thunk
    PayloadDataManager payloadDataManager;
    @Thunk
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    private RxBus rxBus;
    private AccessState accessState;
    private AppUtil appUtil;

    public WebSocketHandler(Context context,
                            OkHttpClient okHttpClient,
                            PayloadDataManager payloadDataManager,
                            BchDataManager bchDataManager,
                            NotificationManager notificationManager,
                            EnvironmentUrls environmentUrls,
                            CurrencyFormatManager currencyFormatManager,
                            String guid,
                            RxBus rxBus,
                            AccessState accessState,
                            AppUtil appUtil) {

        this.context = context;
        this.okHttpClient = okHttpClient;
        this.payloadDataManager = payloadDataManager;
        this.bchDataManager = bchDataManager;
        this.notificationManager = notificationManager;
        this.environmentUrls = environmentUrls;
        this.currencyFormatManager = currencyFormatManager;
        this.guid = guid;
        this.rxBus = rxBus;
        this.accessState = accessState;
        this.appUtil = appUtil;
    }

    /**
     * Starts listening for updates to subscribed xpubsBtc and addresses. Will attempt reconnection
     * every 5 seconds if it cannot connect immediately.
     */
    public void start() {
        stop();
        stoppedDeliberately = false;
        connectToWebSocket()
                .doOnError(throwable -> attemptReconnection())
                .subscribe(new IgnorableDefaultObserver<>());
    }

    /**
     * Halts and disconnects the WebSocket service whilst preventing reconnection until {@link
     * #start()} is called
     */
    public void stopPermanently() {
        stoppedDeliberately = true;
        stop();
    }

    private void stop() {
        if (isConnected()) {
            btcConnection.close(STATUS_CODE_NORMAL_CLOSURE, "BTC Websocket deliberately stopped");
            bchConnection.close(STATUS_CODE_NORMAL_CLOSURE, "BCH Websocket deliberately stopped");
            btcConnection = bchConnection = null;
        }
    }

    private void sendToBtcConnection(String message) {
        // Make sure each message is only sent once per socket lifetime
        if (!btcSubHashSet.contains(message)) {
            try {
                if (isConnected()) {
                    btcConnection.send(message);
                    btcSubHashSet.add(message);
                }
            } catch (Exception e) {
                Timber.e(e, "Send to BTC websocket failed");
            }
        }
    }


    @Thunk
    void subscribe() {
        if (guid == null) {
            return;
        }
        sendToBtcConnection("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");
    }

    @Thunk
    void attemptReconnection() {
        if (compositeDisposable.size() == 0 && !stoppedDeliberately) {
            compositeDisposable.add(
                    getReconnectionObservable()
                            .subscribe(
                                    value -> Timber.d("attemptReconnection: %s", value),
                                    throwable -> Timber.e(throwable, "Attempt reconnection failed")));
        }
    }

    private Observable<Long> getReconnectionObservable() {
        return Observable.interval(RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .takeUntil((ObservableSource<Object>) aLong -> isConnected())
                .doOnNext(tick -> start());
    }

    private boolean isConnected() {
        return btcConnection != null && bchConnection != null && connected;
    }

    private void updateBtcBalancesAndTransactions() {
        payloadDataManager.updateAllBalances()
                .andThen(payloadDataManager.updateAllTransactions())
                .doOnComplete(() -> sendBroadcast(new WalletAndTransactionsUpdatedEvent()))
                .subscribe(new IgnorableDefaultObserver<>());
    }

    @Thunk
    void sendBroadcast(ActionEvent event) {
        rxBus.emitEvent(ActionEvent.class, event);
    }

    private void startWebSocket() {
        Request btcRequest = new Request.Builder()
                .url(environmentUrls.websocketUrl(CryptoCurrency.BTC))
                .addHeader("Origin", "https://blockchain.info")
                .build();

        btcConnection = okHttpClient.newWebSocket(btcRequest, new BtcWebsocketListener());
    }

    private Completable connectToWebSocket() {
        return Completable.fromCallable(() -> {
            btcSubHashSet.clear();
            bchSubHashSet.clear();
            startWebSocket();
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    // TODO: 20/09/2017 Here we should probably parse this info into objects rather than doing it manually
    // TODO: 20/09/2017 Get a list of all possible payloads construct objects
    @Thunk
    void attemptParseBtcMessage(String message, JSONObject jsonObject) {
        try {
            String op = (String) jsonObject.get("op");
            if (op.equals("on_change")) {
                final String localChecksum = payloadDataManager.getPayloadChecksum();
                boolean isSameChecksum = false;
                if (jsonObject.has("x")) {
                    JSONObject x = jsonObject.getJSONObject("x");
                    if (x.has("checksum")) {
                        final String remoteChecksum = x.getString("checksum");
                        isSameChecksum = remoteChecksum.equals(localChecksum);
                    }
                }

                if (!btcOnChangeHashSet.contains(message) && !isSameChecksum) {
                    // Remote update to wallet data detected
                    if (payloadDataManager.getTempPassword() != null) {
                        // Download changed payload
                        //noinspection ThrowableResultOfMethodCallIgnored
                        downloadChangedPayload().subscribe(
                                () -> showToast(R.string.wallet_updated).subscribe(new IgnorableDefaultObserver<>()),
                                Timber::e);
                    }

                    btcOnChangeHashSet.add(message);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "attemptParseBtcMessage");
        }
    }

    private Completable showToast(@StringRes int message) {
        return Completable.fromRunnable(
                () -> ToastCustom.makeText(
                        context,
                        context.getString(message),
                        ToastCustom.LENGTH_SHORT,
                        ToastCustom.TYPE_GENERAL))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    private Completable downloadChangedPayload() {
        return payloadDataManager.initializeAndDecrypt(
                payloadDataManager.getWallet().getSharedKey(),
                payloadDataManager.getWallet().getGuid(),
                payloadDataManager.getTempPassword()
        ).compose(RxUtil.applySchedulersToCompletable())
                .doOnComplete(this::updateBtcBalancesAndTransactions)
                .doOnError(throwable -> {
                    if (throwable instanceof DecryptionException) {
                        showToast(R.string.password_changed).subscribe(new IgnorableDefaultObserver<>());
                        // Password was changed on web, logout to force re-entry of password when
                        // app restarts
                        accessState.unpairWallet();
                        appUtil.restartApp(LauncherActivity.class);
                    }
                });
    }


    private class BtcWebsocketListener extends BaseWebsocketListener {
        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
            Timber.d("BtcWebsocketListener onMessage %s", text);

            if (payloadDataManager.getWallet() != null) {
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    attemptParseBtcMessage(text, jsonObject);
                } catch (JSONException je) {
                    Timber.e(je);
                }
            }
        }
    }

    private class BaseWebsocketListener extends WebSocketListener {
        @CallSuper
        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
            sendBroadcast(new WebSocketMessageEvent());
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            super.onOpen(webSocket, response);
            connected = true;
            compositeDisposable.clear();
            subscribe();
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
            connected = false;
            attemptReconnection();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            connected = false;
            attemptReconnection();
        }
    }

}
