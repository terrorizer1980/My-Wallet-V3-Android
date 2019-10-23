package piuk.blockchain.android.data.connectivity;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import piuk.blockchain.androidcore.data.events.ActionEvent;
import piuk.blockchain.androidcore.data.events.SpottyNetworkConnectionEvent;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {

    private static final long COOL_DOWN_INTERVAL = 1000 * 30L;
    private final NetworkRequest networkRequest;
    private long lastBroadcastTime;
    private Context context;
    private RxBus rxBus;

    ConnectionStateMonitor(Context context, RxBus rxBus) {
        this.context = context;
        this.rxBus = rxBus;
        networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }

    public void enable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        connectivityManager.registerNetworkCallback(networkRequest, this);
    }

    @Override
    public void onAvailable(Network network) {
        // Sends max of one broadcast every 30s if network connection is spotty
        if (System.currentTimeMillis() - lastBroadcastTime > COOL_DOWN_INTERVAL) {
            broadcastOnMainThread().subscribe(new IgnorableDefaultObserver<>());
            lastBroadcastTime = System.currentTimeMillis();
        }
    }

    private Completable broadcastOnMainThread() {
        return Completable.fromAction(() ->
                rxBus.emitEvent(ActionEvent.class, new SpottyNetworkConnectionEvent()))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

}
