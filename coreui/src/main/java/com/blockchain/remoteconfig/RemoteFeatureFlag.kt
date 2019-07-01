package com.blockchain.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.Single
import piuk.blockchain.androidcoreui.BuildConfig
import timber.log.Timber

interface RemoteConfig {

    fun getIfFeatureEnabled(key: String): Single<Boolean>
    fun getABVariant(key: String): Single<Boolean>
    fun getRawJson(key: String): Single<String>

    companion object {
        const val AB_PAX_POPUP = "ab_show_pax_popup"
    }
}

class RemoteConfiguration(private val remoteConfig: FirebaseRemoteConfig) : RemoteConfig {

    private val configuration: Single<FirebaseRemoteConfig> =
        Single.just(remoteConfig.fetch(if (BuildConfig.DEBUG) 0L else 43200L))
            .cache()
            .doOnSuccess { remoteConfig.activateFetched() }
            .doOnError { Timber.e(it, "Failed to load Firebase Remote Config") }
            .map { remoteConfig }

    override fun getRawJson(key: String): Single<String> = configuration.map {
        it.getString(key)
    }

    override fun getIfFeatureEnabled(key: String): Single<Boolean> =
        configuration.map { it.getBoolean(key) }

    override fun getABVariant(key: String): Single<Boolean> =
        configuration.map { it.getBoolean(key) }
}

fun RemoteConfig.featureFlag(key: String): FeatureFlag = object : FeatureFlag {
    override val enabled: Single<Boolean> get() = getIfFeatureEnabled(key)
}
