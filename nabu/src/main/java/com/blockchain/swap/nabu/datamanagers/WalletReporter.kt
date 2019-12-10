package com.blockchain.swap.nabu.datamanagers

import com.blockchain.notifications.analytics.UserAnalytics
import com.blockchain.notifications.analytics.UserProperty
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.security.MessageDigest

interface WalletReporter {
    fun reportWalletGuid(value: String)
}

class AnalyticsWalletReporter(private val userAnalytics: UserAnalytics) : WalletReporter {
    override fun reportWalletGuid(value: String) {
        userAnalytics.logUserProperty(UserProperty(UserAnalytics.WALLET_ID,
            String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(
                value.toByteArray(charset("UTF-8")))))))
    }
}

class UniqueAnalyticsWalletReporter(
    private val walletReporter: WalletReporter,
    private val prefs: PersistentPrefs
) : WalletReporter by walletReporter {
    override fun reportWalletGuid(value: String) {
        val reportedKey = prefs.getValue(ANALYTICS_REPORTED_WALLET_KEY)
        if (reportedKey == null || reportedKey != value) {
            walletReporter.reportWalletGuid(UserAnalytics.WALLET_ID)
            prefs.setValue(ANALYTICS_REPORTED_WALLET_KEY, value)
        }
    }

    companion object {
        private const val ANALYTICS_REPORTED_WALLET_KEY = "analytics_reported_wallet_key"
    }
}