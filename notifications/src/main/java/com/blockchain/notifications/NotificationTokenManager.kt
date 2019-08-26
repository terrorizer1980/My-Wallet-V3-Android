package com.blockchain.notifications

import android.annotation.SuppressLint
import com.google.common.base.Optional
import com.google.firebase.iid.FirebaseInstanceId
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

class NotificationTokenManager(
    private val notificationService: NotificationService,
    private val payloadManager: PayloadManager,
    private val prefs: PersistentPrefs,
    private val firebaseInstanceId: FirebaseInstanceId,
    private val rxBus: RxBus
) {

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by [InstanceIdService]
     *
     * @return The Firebase token
     */
    private val storedFirebaseToken: Observable<Optional<String>>
        get() {
            val storedToken = prefs.getValue(PersistentPrefs.KEY_FIREBASE_TOKEN, "")

            return if (storedToken.isNotEmpty()) {
                Observable.just(Optional.of(storedToken))
            } else {
                Observable.create { subscriber ->
                    FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                        val newToken = instanceIdResult.token
                        subscriber.onNext(Optional.of(newToken))
                        subscriber.onComplete()
                    }
                    FirebaseInstanceId.getInstance().instanceId.addOnFailureListener {
                        if (!subscriber.isDisposed)
                            subscriber.onError(it)
                    }
                }
            }
        }

    /**
     * Sends the access token to the update-firebase endpoint once the user is logged in fully, as
     * the token is generally only generated on first install or first start after updating.
     *
     * @param token A Firebase access token
     */
    @SuppressLint("CheckResult")
    fun storeAndUpdateToken(token: String) {
        prefs.setValue(PersistentPrefs.KEY_FIREBASE_TOKEN, token)
        if (token.isNotEmpty()) {
            sendFirebaseToken(token)
                .subscribeOn(Schedulers.io())
                .subscribe({ /*no-op*/ }, { Timber.e(it) })
        }
    }

    @SuppressLint("CheckResult")
    fun registerAuthEvent() {
        val loginObservable = rxBus.register(AuthEvent::class.java)

        loginObservable
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { authEvent ->
                if (authEvent == AuthEvent.LOGIN) {
                    val storedToken = prefs.getValue(PersistentPrefs.KEY_FIREBASE_TOKEN, "")
                    if (storedToken.isNotEmpty()) {
                        return@flatMapCompletable sendFirebaseToken(storedToken)
                    } else {
                        return@flatMapCompletable resendNotificationToken()
                    }
                } else if (authEvent == AuthEvent.FORGET) {
                    prefs.removeValue(PersistentPrefs.KEY_PUSH_NOTIFICATION_ENABLED)
                    return@flatMapCompletable revokeAccessToken()
                } else {
                    return@flatMapCompletable Completable.complete()
                }
            }
            .subscribe({
                // no-op
            }, { Timber.e(it) })
    }

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    fun disableNotifications(): Completable {
        prefs.setValue(PersistentPrefs.KEY_PUSH_NOTIFICATION_ENABLED, false)
        return revokeAccessToken()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    private fun revokeAccessToken(): Completable {
        return Completable.fromCallable {
            firebaseInstanceId.deleteInstanceId()
            Void.TYPE
        }.andThen(removeNotificationToken())
            .doOnComplete { this.clearStoredToken() }
            .subscribeOn(Schedulers.io())
    }

    /**
     * Enables push notifications flag.
     * Resend stored notification token, or generate and send new token if no stored token exists.
     */
    fun enableNotifications(): Completable {
        prefs.setValue(PersistentPrefs.KEY_PUSH_NOTIFICATION_ENABLED, true)
        return resendNotificationToken()
    }

    /**
     * Re-sends the notification token. The token is only ever generated on app installation, so it
     * may be preferable to store the token and resend it on startup rather than have an app end up
     * in a state where it may not have registered with the right endpoint or similar.
     *
     *
     * If no stored notification token exists, it will be refreshed
     * and will be handled appropriately by FcmCallbackService
     */
    private fun resendNotificationToken(): Completable {
        return storedFirebaseToken
            .flatMapCompletable { optional ->
                if (optional.isPresent) {
                    return@flatMapCompletable sendFirebaseToken(optional.get())
                } else {
                    return@flatMapCompletable Completable.complete()
                }
            }
    }

    private fun sendFirebaseToken(refreshedToken: String): Completable {
        if (prefs.getValue(PersistentPrefs.KEY_PUSH_NOTIFICATION_ENABLED, true) && payloadManager.payload != null) {

            val payload = payloadManager.payload
            val guid = payload!!.guid
            val sharedKey = payload.sharedKey

            // TODO: 09/11/2016 Decide what to do if sending fails, perhaps retry?
            return notificationService.sendNotificationToken(refreshedToken, guid, sharedKey)
                .subscribeOn(Schedulers.io())
        } else {
            return Completable.complete()
        }
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private fun clearStoredToken() {
        prefs.removeValue(PersistentPrefs.KEY_FIREBASE_TOKEN)
    }

    /**
     * Removes the stored token from back end
     */
    private fun removeNotificationToken(): Completable {

        val token = prefs.getValue(PersistentPrefs.KEY_FIREBASE_TOKEN, "")

        return if (token.isNotEmpty()) {
            notificationService.removeNotificationToken(token)
        } else {
            Completable.complete()
        }
    }
}
