package piuk.blockchain.android.ui.login;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import com.blockchain.notifications.analytics.Analytics;
import com.blockchain.notifications.analytics.AnalyticsEvents;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.androidcore.data.auth.AuthDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.utils.PersistentPrefs;
import piuk.blockchain.androidcore.utils.annotations.Thunk;
import piuk.blockchain.androidcoreui.ui.base.BasePresenter;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AppUtil;
import piuk.blockchain.androidcoreui.utils.logging.Logging;
import piuk.blockchain.androidcoreui.utils.logging.PairingEvent;
import piuk.blockchain.androidcoreui.utils.logging.PairingMethod;
import retrofit2.Response;
import timber.log.Timber;

import java.io.IOException;

public class ManualPairingPresenter extends BasePresenter<ManualPairingView> {

    @VisibleForTesting
    static final String KEY_AUTH_REQUIRED = "authorization_required";

    private String sessionId;
    private final AppUtil appUtil;
    private final AuthDataManager authDataManager;
    private final PayloadDataManager payloadDataManager;
    private final PersistentPrefs prefs;
    private final Analytics analytics;

    @VisibleForTesting
    boolean waitingForAuth = false;

    public ManualPairingPresenter(AppUtil appUtil,
                                  AuthDataManager authDataManager,
                                  PayloadDataManager payloadDataManager,
                                  PersistentPrefs prefs,
                                  Analytics analytics) {

        this.appUtil = appUtil;
        this.authDataManager = authDataManager;
        this.payloadDataManager = payloadDataManager;
        this.prefs = prefs;
        this.analytics = analytics;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void onContinueClicked() {
        // Seems that on low memory devices it's quite possible that the view is null here
        if (getView() != null) {
            String guid = getView().getGuid();
            String password = getView().getPassword();

            if (guid.isEmpty()) {
                showErrorToast(R.string.invalid_guid);
            } else if (password.isEmpty()) {
                showErrorToast(R.string.invalid_password);
            } else {
                verifyPassword(password, guid);
            }
        }
    }

    void submitTwoFactorCode(JSONObject responseObject, String sessionId, String guid, String password, String code) {
        if (code == null || code.isEmpty()) {
            getView().showToast(R.string.two_factor_null_error, ToastCustom.TYPE_ERROR);
        } else {
            getCompositeDisposable().add(
                    authDataManager.submitTwoFactorCode(sessionId, guid, code)
                            .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.please_wait, null, false))
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(response -> {
                                        // This is slightly hacky, but if the user requires 2FA login,
                                        // the payload comes in two parts. Here we combine them and
                                        // parse/decrypt normally.
                                        responseObject.put("payload", response.string());
                                        ResponseBody responseBody = ResponseBody.create(
                                                MediaType.parse("application/json"),
                                                responseObject.toString());

                                        Response<ResponseBody> payload = Response.success(responseBody);
                                        handleResponse(password, guid, payload);
                                    },
                                    throwable -> showErrorToast(R.string.two_factor_incorrect_error)));
        }
    }

    private Observable<String> getSessionId(String guid) {

        if (sessionId == null) {
            return authDataManager.getSessionId(guid);
        } else {
            return Observable.just(sessionId);
        }
    }

    private void verifyPassword(String password, String guid) {
        waitingForAuth = true;

        getCompositeDisposable().add(
                getSessionId(guid)
                        .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.validating_password, null, false))
                        .doOnNext(s -> sessionId = s)
                        .flatMap(sessionId -> authDataManager.getEncryptedPayload(guid, sessionId))
                        .subscribe(response -> handleResponse(password, guid, response),
                                throwable -> {
                                    Timber.e(throwable);
                                    sessionId = null;
                                    showErrorToast(R.string.auth_failed);
                                }));
    }

    private void handleResponse(String password, String guid, Response<ResponseBody> response) throws IOException, JSONException {
        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
        if (errorBody.contains(KEY_AUTH_REQUIRED)) {
            //2FA
            showCheckEmailDialog();

            getCompositeDisposable().add(
                    authDataManager.startPollingAuthStatus(guid, sessionId)
                            .subscribe(payloadResponse -> {
                                waitingForAuth = false;

                                if (payloadResponse == null || payloadResponse.contains(KEY_AUTH_REQUIRED)) {
                                    showErrorToast(R.string.auth_failed);
                                    return;
                                }

                                ResponseBody responseBody = ResponseBody.create(
                                        MediaType.parse("application/json"),
                                        payloadResponse);
                                checkTwoFactor(password, guid, Response.success(responseBody));
                            }, throwable -> {
                                waitingForAuth = false;
                                showErrorToast(R.string.auth_failed);
                            }));
        } else {
            //No 2FA
            waitingForAuth = false;
            checkTwoFactor(password, guid, response);
        }
    }

    private void checkTwoFactor(String password, String guid, Response<ResponseBody> response) throws
            IOException, JSONException {

        String responseBody = response.body().string();
        JSONObject jsonObject = new JSONObject(responseBody);
        // Check if the response has a 2FA Auth Type but is also missing the payload,
        // as it comes in two parts if 2FA enabled.
        if (jsonObject.has("auth_type") && !jsonObject.has("payload")
                && (jsonObject.getInt("auth_type") == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR
                || jsonObject.getInt("auth_type") == Settings.AUTH_TYPE_SMS)) {

            getView().dismissProgressDialog();
            getView().showTwoFactorCodeNeededDialog(jsonObject,
                    sessionId,
                    jsonObject.getInt("auth_type"),
                    guid,
                    password);
        } else {
            attemptDecryptPayload(password, responseBody);
        }
    }

    private void attemptDecryptPayload(String password, String payload) {
        getCompositeDisposable().add(
                payloadDataManager.initializeFromPayload(payload, password)
                        .doOnComplete(() -> {
                            prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, payloadDataManager.getWallet().getGuid());
                            appUtil.setSharedKey(payloadDataManager.getWallet().getSharedKey());
                            prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true);
                        })
                        .subscribe(() -> {
                                    getView().goToPinPage();
                                    Logging.INSTANCE.logCustom(new PairingEvent()
                                            .putMethod(PairingMethod.MANUAL)
                                            .putSuccess(true));
                                    analytics.logEvent(AnalyticsEvents.WalletManualLogin);
                                },
                                throwable -> {
                                    Logging.INSTANCE.logCustom(new PairingEvent()
                                            .putMethod(PairingMethod.MANUAL)
                                            .putSuccess(false));

                                    if (throwable instanceof HDWalletException) {
                                        showErrorToast(R.string.pairing_failed);
                                    } else if (throwable instanceof DecryptionException) {
                                        showErrorToast(R.string.invalid_password);
                                    } else {
                                        showErrorToastAndRestartApp(R.string.auth_failed);
                                    }
                                }));
    }

    private void showCheckEmailDialog() {
        getCompositeDisposable().add(
                authDataManager.createCheckEmailTimer()
                        .doOnSubscribe(disposable -> getView().showProgressDialog(R.string.check_email_to_auth_login, "120", true))
                        .takeUntil(integer -> !waitingForAuth)
                        .subscribe(integer -> {
                            if (integer <= 0) {
                                // Only called if timer has run out
                                showErrorToastAndRestartApp(R.string.pairing_failed);
                            } else {
                                getView().updateWaitingForAuthDialog(integer);
                            }
                        }, throwable -> {
                            showErrorToast(R.string.auth_failed);
                            waitingForAuth = false;
                        }));
    }

    void onProgressCancelled() {
        waitingForAuth = false;
        onViewDestroyed();
    }

    @Thunk
    private void showErrorToast(@StringRes int message) {
        getView().dismissProgressDialog();
        getView().resetPasswordField();
        getView().showToast(message, ToastCustom.TYPE_ERROR);
    }

    private void showErrorToastAndRestartApp(@StringRes int message) {
        getView().resetPasswordField();
        getView().dismissProgressDialog();
        getView().showToast(message, ToastCustom.TYPE_ERROR);
        appUtil.clearCredentialsAndRestart(LauncherActivity.class);
    }

    @NonNull
    AppUtil getAppUtil() {
        return appUtil;
    }

}
