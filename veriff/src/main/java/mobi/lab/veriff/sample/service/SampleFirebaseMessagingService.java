package mobi.lab.veriff.sample.service;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import mobi.lab.veriff.data.PushNotificationReply;
import mobi.lab.veriff.data.VeriffConstants;
import mobi.lab.veriff.service.VeriffFirebaseHandler;
import mobi.lab.veriff.util.GeneralUtil;
import mobi.lab.veriff.util.Log;

public class SampleFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = SampleFirebaseMessagingService.class.getSimpleName();
    private static final Log log = Log.getInstance(TAG);

    private static final String WAKELOCK_KEY = "mobi.lab.veriff.sample.service.SampleFirebaseMessagingService.WAKELOCK_KEY";
    private static final long WAKELOCK_TIMEOUT = 2 * 60 * 1000L;

    private static volatile PowerManager.WakeLock wakeLock;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        GeneralUtil.acquireWakeLock(getWakeLock(this), WAKELOCK_TIMEOUT);
        try {
            if (remoteMessage == null) {
                log.d("onMessageReceived - message is null, aborting ..");
                return;
            }
            log.d("onMessageReceived - from: " + remoteMessage.getFrom());

            // Check if message contains a data payload.
            if (remoteMessage.getData() != null && remoteMessage.getData().size() > 0) {
                log.d("onMessageReceived - Message data payload: " + remoteMessage.getData());
            } else {
                log.w("onMessageReceived - Message has not data, aborting ..");
                return;
            }
            handleMessage(remoteMessage);
        } catch (Error e) {
            log.w("onMessageReceived", e);
        } finally {
            GeneralUtil.releaseWakeLock(getWakeLock(this));
        }
    }

    private static @NonNull
    PowerManager.WakeLock getWakeLock(@NonNull final Context context) {
        if (wakeLock == null) {
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            // Wake lock that ensures that the CPU is running. The screen might not be on.
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        }
        return wakeLock;
    }

    private void handleMessage(@NonNull final RemoteMessage remoteMessage) {
        // If notification is intended for Veriff library, then client application notification handling is never called
        if (!handleVeriffNotifications(remoteMessage)) {
            handleClientApplicationNotifications(remoteMessage);
        }
    }

    private boolean handleVeriffNotifications(@NonNull final RemoteMessage remoteMessage) {
        PushNotificationReply reply = VeriffFirebaseHandler.handleNotification(remoteMessage, this);
        log.d(reply.getState() + ", " + reply.getNotification());
        return VeriffConstants.NOTIFICATION_HANDLED.equals(reply.getState());
    }

    private void handleClientApplicationNotifications(@NonNull final RemoteMessage remoteMessage) {

        //TODO handle client application notifications here

    }
}
