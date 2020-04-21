package com.blockchain.notifications.analytics

interface Analytics {
    fun logEvent(analyticsEvent: AnalyticsEvent)
    fun logEventOnce(analyticsEvent: AnalyticsEvent)
}

interface UserAnalytics {
    fun logUserProperty(userPropery: UserProperty)
    fun logUserId(userId: String)

    companion object {
        const val KYC_LEVEL = "kyc_level"
        const val KYC_UPDATED_DATE = "kyc_updated_date"
        const val WALLET_ID = "wallet_id"
        const val KYC_CREATION_DATE = "kyc_creation_date"
        const val EMAIL_VERIFIED = "email_verified"
        const val TWOFA_ENABLED = "two_fa_enabled"
        const val FUNDED_COINS = "funded_coins"
        const val USD_BALANCE = "usd_balance"
    }
}

interface AnalyticsEvent {
    val event: String
    val params: Map<String, String>
}

data class UserProperty(val property: String, val value: String) {
    companion object {
        const val MAX_VALUE_LEN = 36
    }
}
